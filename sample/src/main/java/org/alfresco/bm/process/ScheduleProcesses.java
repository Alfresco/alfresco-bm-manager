/*
 * #%L
 * Alfresco Benchmark Manager
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.bm.process;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.data.ProcessData;
import org.alfresco.bm.data.ProcessDataDAO;
import org.alfresco.bm.driver.event.AbstractEventProcessor;
import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.common.session.SessionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Schedule processing events.
 * 
 * <h1>Input</h1>
 * 
 * No input requirements
 * 
 * <h1>Data</h1>
 * 
 * A MongoDB collection containing unprocessed data.
 * 
 * <h1>Actions</h1>
 * 
 * The MongoDB collection is examined to find (see {@link ProcessData#isDone() unfinished} processes.
 * Unfinished processes are scheduled up to a batch size after which this processor reschedules itself.
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_PROCESS}: The process name<br/>
 * {@link #EVENT_NAME_SCHEDULE_PROCESSES}: No data<br/> 
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ScheduleProcesses extends AbstractEventProcessor
{
    public static final int DEFAULT_BATCH_SIZE = 100;
    public static final String EVENT_NAME_PROCESS = "process";
    
    private static Log logger = LogFactory.getLog(ScheduleProcesses.class);
    
    private final SessionService sessionService;
    private final ProcessDataDAO processDataDAO;
    private final String testRunFqn;
    private final int processCount;
    private final long timeBetweenProcesses;
    private String eventNameProcess;
    private int batchSize;

    /**
     * @param sessionService        service to create and manage persisted session data
     * @param processDataDAO        the DAO for storing process data
     * @param testRunFqn            the name of the test run
     * @param processCount          the number of processes to run
     * @param timeBetweenProcesses  how long between each process
     */
    public ScheduleProcesses(
            SessionService sessionService,
            ProcessDataDAO processDataDAO,
            String testRunFqn, int processCount, long timeBetweenProcesses)
    {
        super();
        this.sessionService = sessionService;
        this.processDataDAO = processDataDAO;
        this.testRunFqn = testRunFqn;
        this.processCount = processCount;
        this.timeBetweenProcesses = timeBetweenProcesses;
        this.eventNameProcess = EVENT_NAME_PROCESS;
        this.batchSize = DEFAULT_BATCH_SIZE;
    }

    /**
     * Override the {@link #EVENT_NAME_PROCESS default} event name to process an event
     */
    public void setEventNameProcess(String eventNameProcess)
    {
        this.eventNameProcess = eventNameProcess;
    }

    /**
     * Override the {@link #DEFAULT_BATCH_SIZE default} batch size for event processing
     */
    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        Integer alreadyScheduled = (Integer) event.getData();
        if (alreadyScheduled == null)
        {
            alreadyScheduled = Integer.valueOf(0);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Already scheduled " + alreadyScheduled + " " + EVENT_NAME_PROCESS + " events and will schedule up to " + batchSize + " more.");
        }
        
        List<Event> events = new ArrayList<Event>(batchSize + 1);
        long now = System.currentTimeMillis();
        long scheduled = now;
        int localCount = 0;
        int totalCount = (int) alreadyScheduled;
        for (int i = 0; i < batchSize && totalCount < processCount; i++)
        {
            String processName = testRunFqn + "-" + UUID.randomUUID();
            scheduled += timeBetweenProcesses;
            
            // Store it
            // We could also use an object and the DataCreationState.Scheduled
            processDataDAO.createProcess(processName);
            processDataDAO.updateProcessState(processName, DataCreationState.Scheduled);
            
            // We will attach process name as the event data
            Event eventOut = new Event(eventNameProcess, scheduled, processName);
            
            // We want to carry some arbitrary data around in a 'session'
            DBObject sessionObj = new BasicDBObject()
                    .append("key1", "value1")
                    .append("key2", "value2");
            String sessionId = sessionService.startSession(sessionObj);
            eventOut.setSessionId(sessionId);
            
            // Add the event to the list of events scheduled
            events.add(eventOut);
            localCount++;
            totalCount++;
        }
        
        // Reschedule this event, if necessary
        if (totalCount < processCount)
        {
            Event rescheduleEvent = new Event(event.getName(), scheduled, Integer.valueOf(totalCount));
            events.add(rescheduleEvent);
        }
        
        // The ResultBarrier will ensure that this gets rescheduled, if necessary
        EventResult result = new EventResult(
                "Created " + totalCount + " scheduled processes.",
                events);
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Scheduled " + localCount + " processes and " + (totalCount < processCount ? "rescheduled" : "did not reschedule") + " self.");
        }
        return result;
    }
}
