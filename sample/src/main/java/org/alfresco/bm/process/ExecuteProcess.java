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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.data.ProcessData;
import org.alfresco.bm.data.ProcessDataDAO;
import org.alfresco.bm.driver.event.AbstractEventProcessor;
import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.driver.file.TestFileService;
import org.alfresco.bm.common.session.SessionService;

import com.mongodb.DBObject;

/**
 * Execute an unfinished process
 * 
 * <h1>Input</h1>
 * 
 * Process name
 * 
 * <h1>Data</h1>
 * 
 * A MongoDB collection containing unprocessed data.
 * 
 * <h1>Actions</h1>
 * 
 * Fetches the process from the MongoDB collection and executes it.
 * There is no real processing for this sample, but the basic principle of
 * running a process from associated data, marking it as done and recording
 * the result is demonstrated.<br/>
 * A certain number of failures are demonstrated as well.
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_PROCESS_DONE}: The process name<br/>
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ExecuteProcess extends AbstractEventProcessor
{
    public static final String EVENT_NAME_PROCESS_DONE = "processDone";
    
    private final SessionService sessionService;
    private final ProcessDataDAO processDataDAO;
    private final TestFileService testFileService;
    private String eventNameProcessDone;

    /**
     * @param processDataDAO        general DAO for accessing data
     */
    public ExecuteProcess (SessionService sessionService, ProcessDataDAO processDataDAO, TestFileService testFileService)
    {
        super();
        this.sessionService = sessionService;
        this.processDataDAO = processDataDAO;
        this.testFileService = testFileService;
        this.eventNameProcessDone = EVENT_NAME_PROCESS_DONE;
    }

    /**
     * Override the {@link #EVENT_NAME_PROCESS_DONE default} event name for 'process done'.
     */
    public void setEventNameProcessDone(String eventNameProcessDone)
    {
        this.eventNameProcessDone = eventNameProcessDone;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        // Usually, the entire method is timed but we can choose to control this
        super.suspendTimer();
        
        // Get the user email
        String processName = (String) event.getData();
        
        // We can also get the less transient session data
        String sessionId = event.getSessionId();
        
        // Locate the process
        ProcessData process = processDataDAO.findProcessByName(processName);
        // A quick double-check of the inbound values
        EventResult result = null;
        if (process == null)
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  Process not found.",
                    false);
            return result;
        }
        else if (process.getState() != DataCreationState.Scheduled)
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  Process not scheduled.",
                    false);
            return result;
        }
        else if (sessionId == null)
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  No session ID available.",
                    false);
            return result;
        }
        
        // Get and check that we have access to the required test file
        File file = testFileService.getFile();
        if (file == null || !file.exists())
        {
            result = new EventResult(
                    "Skipping processing for '" + processName + "'.  No test file available.",
                    Collections.<Event>emptyList(),
                    false);
            return result;
        }
        
        // Play or use the session data
        DBObject sessionObj = sessionService.getSessionData(sessionId);
        @SuppressWarnings("unused")
        String value1 = (String) sessionObj.get("key1");
        @SuppressWarnings("unused")
        String value2 = (String) sessionObj.get("key2");
        
        // Restart the clock
        resumeTimer();
        
        // Simulate some process delay
        Object sync = new ArrayList<Object>(0);
        synchronized (sync)
        {
            try
            {
                sync.wait((long)(Math.random()* 50.0) + 10L);
            }
            catch (InterruptedException e)
            {
                // Ignore
            }
        }
        
        /*
         * Some work
         */
        
        switch ((int)(Math.random() * 10.0))
        {
            // 10% exceptions
            case 0:
                // This would normally be done in a try-catch
                processDataDAO.updateProcessState(processName, DataCreationState.Failed);
                // We let the framework handle this
                // The active session will automatically be closed
                throw new RuntimeException("A ficticious random exception during execution of '" + processName + "'.");
            // 20% some error condition
            case 1:
            case 2:
                processDataDAO.updateProcessState(processName, DataCreationState.Failed);
                result = new EventResult(
                        "A ficticious random error response for executing process '" + processName + "'.",
                        Collections.<Event>emptyList(),
                        false);
                break;
            // 70% success
            default:
                // Record the name of the process to reflect that is was created on the mythical server
                boolean updated = processDataDAO.updateProcessState(processName, DataCreationState.Created);
                if (updated)
                {
                    // Create 'done' event, which will not have any further associated event processors
                    Event doneEvent = new Event(eventNameProcessDone, 0L, processName);
                    result = new EventResult("Process " + processName + " completed.", doneEvent);
                    break;
                }
                else
                {
                    // This is a real error condition i.e. not a simulated error
                    throw new RuntimeException("Process " + processName + " was executed but not recorded.");
                }
        }
        
        // Session IDs are, by default, carried from originating events to new events.
        // Exceptions that escape to the framework will automatically trigger session closure.
        // Stop the session so that it is possible to count the active sessions
        sessionService.endSession(sessionId);
        
        // Done
        return result;
    }
}
