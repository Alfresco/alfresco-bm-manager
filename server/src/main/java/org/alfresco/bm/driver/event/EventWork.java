/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.driver.event;

import org.alfresco.bm.common.EventRecord;
import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.alfresco.bm.common.util.log.TestRunLogService;
import org.alfresco.bm.driver.event.producer.EventProducer;
import org.alfresco.bm.driver.event.producer.EventProducerRegistry;
import org.alfresco.bm.driver.session.SessionService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    private final String driverId;
    private final String testRunFqn;
    private final Event event;
    private final String[] driverIds;
    private final EventProcessor processor;
    private final EventProducerRegistry eventProducers;
    private final EventService eventService;
    private final ResultService resultService;
    private final SessionService sessionService;
    private final TestRunLogService logService;
    
    /**
     * Construct work to be executed by a thread
     * 
     * @param driverId          the identifier of the driver process handling the event
     * @param testRunFqn        the fully qualified name of the test run initiating the work
     * @param event             the event to be processed
     * @param driverIds         the current list of driver IDs operating on this test run
     * @param processor         the component that will do the actual processing
     * @param eventProducers    the registry to convert events before persistence
     * @param eventService      the queue events that will be updated with new events
     * @param resultService     the service to store results of the execution
     * @param sessionService    the service manage sessions
     * @param logService        the service to report any issues
     */
    public EventWork(
            String driverId, String testRunFqn,
            Event event,
            String[] driverIds,
            EventProcessor processor, EventProducerRegistry eventProducers,
            EventService eventService, ResultService resultService, SessionService sessionService,
            TestRunLogService logService)
    {
        this.driverId = driverId;
        this.testRunFqn = testRunFqn;
        this.event = event;
        this.driverIds = driverIds;
        this.processor = processor;
        this.eventProducers = eventProducers;
        this.eventService = eventService;
        this.resultService = resultService;
        this.sessionService = sessionService;
        this.logService = logService;
    }

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
                String msg = "Event processtor returned null result: " + processor;
                logService.log(LogLevel.FATAL, msg);
                throw new RuntimeException(msg);
            }
        }
        catch (Throwable e)
        {
            DateTime eventTime = new DateTime(stopWatch.getStartTime());
            String stack = ExceptionUtils.getStackTrace(e);
            String error = "[" + eventTime + "] Event processing exception; no further events will be published. \r\n" + stack;
            result = new EventResult(error, Collections.emptyList(), false);
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
        EventRecord recordedEvent = new EventRecord(driverId, wasSuccess, before, time, data, event);
        recordedEvent.setChart(chart);
        recordedEvent.setProcessedBy(processor.getName());
        
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
                    "   Driver:    " + driverId + "\n" +
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
            String stack = ExceptionUtils.getStackTrace(e);
            logService.log(LogLevel.ERROR, "Failed to record an result " + recordedEvent + ": " + stack);
            logger.error("Failed recorded event: " + recordedEvent, e);
        }
        
        // Pass the event(s) through the producers
        Set<String> eventNamesSeen = new HashSet<String>(nextEvents.size() + 17);
        nextEvents = getNextEvents(nextEvents, eventNamesSeen);
        
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
        
        // Use weightings (https://github.com/AlfrescoBenchmark/alfresco-benchmark/issues/54)
        RandomWeightedSelector<String> driverSelector = new RandomWeightedSelector<String>();
        for (String driverId : driverIds)
        {
            driverSelector.add(100, driverId);
        }

        // Publish the next events
        for (Event nextEvent : nextEvents)
        {
            if (nextEvent == null)
            {
                // Ignore it but log the error
                String msg =
                        "Null event in list of next events: \n" +
                        "   Driver:    " + driverId + "\n" +
                        "   Test Run:  " + testRunFqn + "\n" +
                        "   Event:     " + event + "\n" +
                        "   Time:      " + time + "\n" +
                        "   Processor: " + processor;
                logger.error("\n" + msg);
                logService.log(LogLevel.WARN, msg);
                continue;
            }
            // Carry over the session ID, if required
            if (propagateSessionId)
            {
                nextEvent.setSessionId(sessionId);
            }
            
            // Randomly distribute the event execution across the drivers, unless there is implied
            // data afinity, in which case use this driver instance
            if (!nextEvent.getDataInMemory())
            {
                // The data is not held in memory, so we can assign the event to any server
                String driverIdForNextEvent = driverSelector.next();
                nextEvent.setDriver(driverIdForNextEvent);
            }
            
            // Persist the event
            try
            {
                eventService.putEvent(nextEvent);
            }
            catch (Throwable e)
            {
                String stack = ExceptionUtils.getStackTrace(e);
                String msg =
                        "Failed to insert event into queue: \n" +
                        "  Event to insert:     " + nextEvent + "\n" +
                        "  Inbound event:       " + event + "\n" +
                        "  Process used:        " + processor + "\n" +
                        "  Events produced:     " + eventNamesSeen;
                logService.log(LogLevel.ERROR, msg + "\n" + stack);
                logger.error(msg, e);
            }
        }
        
        // Remove the event from the queue.
        try
        {
            boolean deleted = eventService.deleteEvent(event);
            if (!deleted)
            {
                String msg = "Event was not deleted from the queue: " + event;
                logger.error(msg);
                logService.log(LogLevel.ERROR, msg);
            }
        }
        catch (Throwable e)
        {
            String stack = ExceptionUtils.getStackTrace(e);
            String msg = "Failed to remove event from the queue: " + event;
            logger.error(msg, e);
            logService.log(LogLevel.ERROR, msg + "\n" + stack);
        }
    }

    /**
     * Go to the event producers and, recursively, keep producing events until:
     * <ul>
     * <li>there are no more producers for the events in hand</li>
     * </ul>
     * 
     * @throws IllegalStateException        if a cyclical producer relationship is detected
     */
    private List<Event> getNextEvents(List<Event> events, Set<String> eventNamesSeen)
    {
        List<Event> nextEvents = new ArrayList<Event>(events.size() * 2);
        for (Event event : events)
        {
            String eventName = event.getName();
            // Get a producer associated with the event
            EventProducer producer = eventProducers.getProducer(eventName);
            if (producer == null)
            {
                // There is no producer, so take the event at face value
                nextEvents.add(event);
                continue;
            }
            // See what new events need producing
            List<Event> producedEvents = producer.getNextEvents(event);
            if (!eventNamesSeen.add(eventName))
            {
                // The event was already present, which means we would enter a loop
                String msg = "Event is part of a cyclical production configuration: " + event;
                logService.log(LogLevel.ERROR, msg);
                throw new RuntimeException(msg);
            }
            // Recurse into each one of these
            producedEvents = getNextEvents(producedEvents, eventNamesSeen);
            // Add this to the list
            nextEvents.addAll(producedEvents);
            
            // CRITICAL!
            // The event names already seen must only apply to a single branch, so remove the event name
            eventNamesSeen.remove(eventName);
        }
        // Done
        return nextEvents;
    }
}
