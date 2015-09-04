/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.bm.event;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.session.SessionService;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Ensures that a certain number of sessions are active when it takes samples
 * and raises more events for session creation, if required.
 * <p>
 * Note that derivations of this class can be used to generate the desired
 * data for each of the raised events.
 * 
 * <h1>Input</h1>
 * 
 * Any data
 * 
 * <h1>Actions</h1>
 * 
 * Emits events to create new sessions before rescheduling itself.
 * 
 * <h1>Output</h1>
 * 
 * {@link #setEventNameRaiseEvents(String)}: Self-triggering event<br/>
 * {@link #outputEventName}: Obtains data from {@link #getNextEventData()}<br/>
 * 
 * @author Derek Hulley
 * @since 1.4
 */
public class CreateSessionsEventProcessor extends AbstractEventProcessor
{
    private static final String ERR_INCORRECT_INBOUND_TYPE = "The event processor takes no initial input.";
    private static final String MSG_CREATED_EVENTS = "Scheduled %3d events named %s.";
    
    public static final String KEY_OUTPUT_EVENTS_RAISED = "outputEventsRaised";
    public static final String KEY_COMPLETED_SESSION_COUNT = "completedSessionCount";
    
    private static final String DEFAULT_EVENT_NAME_CREATE_SESSIONS = "createSessions";
    private static final long DEFAULT_CHECK_PERIOD = 10000L;
    private static final long DEFAULT_TIME_BETWEEN_SESSIONS = 100L;
    
    private final SessionService sessionService;
    private final String outputEventName;
    private final int concurrentSessions;
    private final int totalSessions;
    private long checkPeriod = DEFAULT_CHECK_PERIOD;
    private long timeBetweenSessions = DEFAULT_TIME_BETWEEN_SESSIONS;
    private String eventNameCreateSessions = DEFAULT_EVENT_NAME_CREATE_SESSIONS;
    
    /**
     * Constructor with <b>essential</b> values
     * 
     * @param sessionService                session information
     * @param outputEventName               the name of the event to emit
     * @param concurrentSessions            the number of concurrent sessions to maintain
     * @param totalSessions                 the maxiumum number of sessions to create
     */
    public CreateSessionsEventProcessor(
            SessionService sessionService,
            String outputEventName,
            int concurrentSessions,
            int totalSessions)
    {
        super();
        this.sessionService = sessionService;
        this.outputEventName = outputEventName;
        this.concurrentSessions = concurrentSessions;
        this.totalSessions = totalSessions;
        if (concurrentSessions > totalSessions)
        {
            throw new IllegalArgumentException("The number of concurrent sessions cannot exceed the total number of sessions.");
        }
    }

    /**
     * Override the {@link #DEFAULT_EVENT_NAME_CREATE_SESSIONS default} name for repeats
     */
    public void setEventNameCreateSessions(String eventNameCreateSessions)
    {
        this.eventNameCreateSessions = eventNameCreateSessions;
    }
    
    /**
     * Override the {@link #DEFAULT_CHECK_PERIOD default} time between session count checks
     */
    public void setCheckPeriod(long checkPeriod)
    {
        this.checkPeriod = checkPeriod;
    }

    /**
     * Override the {@link #DEFAULT_TIME_BETWEEN_SESSIONS default} time between sessions
     */
    public void setTimeBetweenSessions(long timeBetweenSessions)
    {
        if (timeBetweenSessions < 1L)
        {
            throw new IllegalArgumentException("There must be SOME time between session creation.");
        }
        this.timeBetweenSessions = timeBetweenSessions;
    }
    
    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        long now = System.currentTimeMillis();
        long nextCheckTime = now + checkPeriod;
        long completedSessions = 0L;
        int raisedBefore = 0;
        
        // Get the previous data associated with this processor
        DBObject data;
        try
        {
            data = (DBObject) event.getData();
        }
        catch (ClassCastException e)
        {
            EventResult result = new EventResult(ERR_INCORRECT_INBOUND_TYPE, false);
            return result;
        }
        int raised = 0;
        List<Event> nextEvents = new ArrayList<Event>(100);
        // The first scheduling creates all the anticipated sessions in an orderly queue
        if (data == null)
        {
            // This is the first time, so raise an event for each session
            long nextSessionTime = now;
            while (raised < concurrentSessions)
            {
                Object nextEventData = getNextEventData();
                Event nextEvent = new Event(outputEventName, nextSessionTime, nextEventData);
                nextEvents.add(nextEvent);
                // Increment
                raised++;
                nextSessionTime += timeBetweenSessions;
            }
        }
        else
        {
            raisedBefore = (Integer) data.get(KEY_OUTPUT_EVENTS_RAISED);
            // How many sessions need raising over the next period
            long completedBefore = (Long) data.get(KEY_COMPLETED_SESSION_COUNT);
            completedSessions = sessionService.getCompletedSessionsCount();
            long completedInWait = completedSessions - completedBefore;         // Could be zero
            long sessionDelay = completedInWait > 0 ? (long) ((double)checkPeriod/(double)completedInWait) : 0L;
            // Now raise session events for the sessions completed over the previous wait
            long nextSessionTime = now;
            while (raised < completedInWait && (raised + raisedBefore) < totalSessions)
            {
                Object nextEventData = getNextEventData();
                Event nextEvent = new Event(outputEventName, nextSessionTime, nextEventData);
                nextEvents.add(nextEvent);
                // Increment
                raised++;
                nextSessionTime += sessionDelay;
            }
        }
        // Reschedule, if necessary
        int totalRaised = raised + raisedBefore;
        if (totalRaised < totalSessions)
        {
            // There are more sessions to create
            data = new BasicDBObject()
                .append(KEY_COMPLETED_SESSION_COUNT, completedSessions)
                .append(KEY_OUTPUT_EVENTS_RAISED, totalRaised);
            Event nextEvent = new Event(eventNameCreateSessions, nextCheckTime, data);
            nextEvents.add(nextEvent);
        }
        
        // Done
        return new EventResult(
                String.format(MSG_CREATED_EVENTS, raised, outputEventName),
                nextEvents,
                true);
    }
    
    /**
     * Get data to provide for each event raised.  Implementations can override to
     * add any data that is required for the each of the events being raised.
     * <p/>
     * The default is to return <tt>null</tt> i.e. no data will be provided to each new event.
     * <p/>
     * Use a {@link DBObject} for complicated data transfer
     * 
     * @return              Return data to attach to the generated events
     */
    protected Object getNextEventData()
    {
        // No data
        return null;
    }
}
