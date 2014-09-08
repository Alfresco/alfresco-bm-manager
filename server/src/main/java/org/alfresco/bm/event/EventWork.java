/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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

import java.util.Collections;
import java.util.List;

import org.alfresco.bm.session.SessionService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

/**
 * A unit of work that can be executed by the event processing threads.
 * It delegates to the
 * {@link EventService#EventService(String, String, Event, EventProcessor, EventService) event processor},
 * which does not need to know anything about the events or event recording.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class EventWork implements Runnable
{
    private static final Log logger = LogFactory.getLog(EventService.class);
    
    private final String serverId;
    private final String testRunFqn;
    private final Event event;
    private final EventProcessor processor;
    private final EventService eventService;
    private final ResultService resultService;
    private final SessionService sessionService;
    
    /**
     * Construct work to be executed by a thread
     * 
     * @param serverId          the identifier of the server process handling the event
     * @param testRunFqn        the fully qualified name of the test run initiating the work
     * @param event             the event to be processed
     * @param processor         the component that will do the actual processing
     * @param eventService      the queue events that will be updated with new events
     * @param resultService     the service to store results of the execution
     * @param sessionService    the service manage sessions
     */
    public EventWork(
            String serverId, String testRunFqn,
            Event event,
            EventProcessor processor,
            EventService eventService, ResultService resultService, SessionService sessionService)
    {
        this.serverId = serverId;
        this.testRunFqn = testRunFqn;
        this.event = event;
        this.processor = processor;
        this.eventService = eventService;
        this.resultService = resultService;
        this.sessionService = sessionService;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run()
    {
        // Set the start and end times for the event
        long warnDelay = processor.getWarnDelay();
        boolean chart = processor.isChart();

        EventResult result = null;
        StopWatch stopWatch = new StopWatch();
        try
        {
            // Process the event
            result = processor.processEvent(event, stopWatch);
            if (result == null)
            {
                throw new RuntimeException("Event processtor returned null result: " + processor);
            }
        }
        catch (Throwable e)
        {
            DateTime eventTime = new DateTime(stopWatch.getStartTime());
            String stack = ExceptionUtils.getStackTrace(e);
            String error = "[" + eventTime + "] Event processing exception; no further events will be published. \r\n" + stack;
            result = new EventResult(error, Collections.<Event>emptyList(), false);
            // Close any associated session
            String sessionId = event.getSessionId();
            if (sessionId != null && processor.isAutoCloseSessionId())
            {
                sessionService.endSession(sessionId);
            }
        }
        // See how long it took
        long before = stopWatch.getStartTime();
        long time = stopWatch.getTime();
        
        // Get any supplemental data to be recorded
        Object data = result.getData();
        // Get the next events to publish
        List<Event> nextEvents = result.getNextEvents();
        // Was it successful?
        boolean wasSuccess = result.isSuccess();
        // Construct the recorded event
        EventRecord recordedEvent = new EventRecord(serverId, wasSuccess, before, time, data, event);
        recordedEvent.setChart(chart);
        
        // Check the time taken against the time allowed
        if (time > warnDelay)
        {
            String msg = "Event processing exceeded warning threshold by " + (time - warnDelay) + "ms.";
            recordedEvent.setWarning(msg);
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("\n" +
                    "Event processing completed: \n" +
                    "   Owner:     " + serverId + "\n" +
                    "   Test Run:  " + testRunFqn + "\n" +
                    "   Event:     " + event + "\n" +
                    "   Time:      " + time + "\n" +
                    "   Processor: " + processor);
        }

        // Record the event
        try
        {
            resultService.recordResult(recordedEvent);
        }
        catch (Throwable e)
        {
            logger.error("Failed recorded event: " + recordedEvent, e);
        }
        
        // Only propagate session IDs automatically if there is a 1:1 relationship between the event processed
        // and the next event i.e. we branching of the session is not intrinsically supported
        String sessionId = event.getSessionId();
        boolean propagateSessionId = (sessionId != null) && processor.isAutoPropagateSessionId();
        if (sessionId != null && nextEvents.size() != 1 && processor.isAutoCloseSessionId())
        {
            // No further events and we have to auto-close
            sessionService.endSession(sessionId);
            propagateSessionId = false;
        }
        
        // Publish the next events
        for (Event nextEvent : nextEvents)
        {
            if (nextEvent == null)
            {
                // Ignore it but log the error
                logger.error("\n" +
                        "Null event in list of next events: \n" +
                        "   Owner:     " + serverId + "\n" +
                        "   Test Run:  " + testRunFqn + "\n" +
                        "   Event:     " + event + "\n" +
                        "   Time:      " + time + "\n" +
                        "   Processor: " + processor);
                continue;
            }
            // Ensure that any locally-stored data has a data owner attached
            if (nextEvent.getDataKey() != null)
            {
                nextEvent.setDataOwner(serverId);
            }
            // Carry over the session ID, if required
            if (propagateSessionId)
            {
                nextEvent.setSessionId(sessionId);
            }
            
            // Persist the event
            try
            {
                eventService.putEvent(nextEvent);
            }
            catch (Throwable e)
            {
                logger.error("Failed to insert event into queue: " + nextEvent, e);
            }
        }
        
        // Clean up any locally-store data and remove the event from the queue.
        // This *must* come after the new events have been published;
        // otherwise it's possible to have a short time without events.
        try
        {
            event.cleanData();
            boolean deleted = eventService.deleteEvent(event);
            if (!deleted)
            {
                logger.error("Event was not deleted from the queue: " + event);
            }
        }
        catch (Throwable e)
        {
            logger.error("Failed to remove event from the queue: " + event, e);
        }
    }
}
