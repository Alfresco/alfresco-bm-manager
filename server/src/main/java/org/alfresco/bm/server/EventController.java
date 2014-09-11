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
package org.alfresco.bm.server;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alfresco.bm.event.DoNothingEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventProcessor;
import org.alfresco.bm.event.EventProcessorRegistry;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.EventWork;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.event.producer.EventProducerRegistry;
import org.alfresco.bm.session.SessionService;
import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * A <i>master</i> controlling thread that ensures that:
 * <ul>
 *  <li>reads events from the queue</li>
 *  <li>checks out threads to process events</li>
 *  <li>monitors event processors</li>
 *  <li>records event executions</li>
 *  <li>handles exceptions e.g. events that take too long to process</li>
 * </ul>
 * Calls from the {@link LifecycleListener} control the execution phases and,
 * when the events run out, the application context is notified to shut down.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class EventController implements LifecycleListener, ApplicationContextAware, Runnable
{
    private static final int DEFAULT_EVENTS_PER_SECOND_PER_THREAD = 2;
    
    private static final Log logger = LogFactory.getLog(EventController.class);
    
    private final String serverId;
    private final String testRunFqn;
    private final EventService eventService;
    private final EventProducerRegistry eventProducers;
    private final EventProcessorRegistry eventProcessors;
    private final Thread thread;
    private final ExecutorService executor;
    private final ResultService resultService;
    private final SessionService sessionService;
    private final int threadCount;

    private int eventsPerSecondPerThread = DEFAULT_EVENTS_PER_SECOND_PER_THREAD;

    private ApplicationContext ctx;
    private boolean running;
    private EventProcessor doNothingProcessor = new DoNothingEventProcessor();
    
    /**
     * Construct the controller
     * 
     * @param serverId          the server controlling the events
     * @param testRunFqn        the fully qualified name of the test run
     * @param eventService      the source of events that will be pushed for execution
     * @param eventProducers    the registry of producers of events
     * @param eventProcessors   the registry of processors for events
     * @param resultService     the service used to store and retrieve event results
     * @param sessionService    the service to carry session IDs between events
     * @param threadCount       the number of threads available to the processor
     */
    public EventController(
            String serverId,
            String testRunFqn,
            EventService eventService,
            EventProducerRegistry eventProducers,
            EventProcessorRegistry eventProcessors,
            ResultService resultService,
            SessionService sessionService,
            int threadCount)
    {
        thread = new Thread(new ThreadGroup(testRunFqn), this, testRunFqn + "-Controller");
        thread.setDaemon(false);         // Requires explicit shutdown
        
        this.serverId = serverId;
        this.testRunFqn = testRunFqn;
        this.eventService = eventService;
        this.eventProducers = eventProducers;
        this.eventProcessors = eventProcessors;
        this.resultService = resultService;
        this.sessionService = sessionService;
        this.threadCount = threadCount;
        // Configure threads
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(testRunFqn + "-");
        threadFactory.setThreadGroup(thread.getThreadGroup());
        threadFactory.setDaemon(true);
        // Configure work queue
        SynchronousQueue<Runnable> queue = new SynchronousQueue<Runnable>(true);
        // Configure executor
        RejectedExecutionHandler abortPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
        executor = new ThreadPoolExecutor(
                threadCount, threadCount, 60, TimeUnit.SECONDS,
                queue, threadFactory, abortPolicy);
        
        setRunning(true);
    }

    /**
     * Override the {@link #DEFAULT_EVENTS_PER_SECOND_PER_THREAD maximum} number of events
     * that can be processed per second per thread.  This represents a maximum; lower volumes
     * will be processed at the required rate.  Ensure there are enough threads available to
     * process events at the anticipated rate or change this value.
     */
    public void setEventsPerSecondPerThread(int eventsPerSecondPerThread)
    {
        if (eventsPerSecondPerThread < 1)
        {
            throw new IllegalArgumentException("eventsPerSecondPerThread must be greater than zero.");
        }
        this.eventsPerSecondPerThread = eventsPerSecondPerThread;
    }

    /**
     * Record the application context for shutdown once processing has finished
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.ctx = applicationContext;
    }
    
    /**
     * Synchronized access to the run state
     */
    private synchronized boolean isRunning()
    {
        return running;
    }
    
    /**
     * Synchronized access to the run state
     */
    private synchronized void setRunning(boolean running)
    {
        this.running = running;
    }

    /**
     * Kick the thread off
     */
    @Override
    public void start()
    {
        thread.start();
    }
    
    @Override
    public void stop()
    {
        setRunning(false);
        // Stop the event processors
        this.executor.shutdown();
        // If another thread is making this call then make sure we wait for the thread to kill itself
        if (!Thread.currentThread().equals(thread))
        {
            synchronized (this)
            {
                // Wake the EventController thread up
                this.notify();
            }
            // Wait for the EventController thread to stop
            try { thread.join(); } catch (InterruptedException e) {}
        }
        // Now wait for the executing threads
        try { executor.awaitTermination(30L, TimeUnit.SECONDS); } catch (InterruptedException e) {}
        // And finally, force the threads to die
        if (!executor.isTerminated())
        {
            List<Runnable> runnables = executor.shutdownNow();
            if (runnables.size() > 0)
            {
                logger.warn(testRunFqn + ": " + runnables.size() + " event processor threads did not stop within 30s.");
            }
        }
    }
    
    @Override
    public void run()
    {
        try
        {
            runImpl();
        }
        catch (Throwable e)
        {
            // We can ignore errors if we have been told to stop
            if (isRunning())
            {
                logger.error("\tEvent processing terminated with error: " + testRunFqn, e);
            }
            else
            {
                logger.debug("\tEvent processing terminated with error: " + testRunFqn, e);
            }
        }
    }
    
    /**
     * Do the actual run but without concern for exceptions, which will be logged.
     */
    private void runImpl()
    {
        int eventsPerSecond = (threadCount * eventsPerSecondPerThread);
        logger.info(
                "\tEvent processing started: " + testRunFqn +
                " (" + eventsPerSecond + " events per second using " + threadCount + " threads)");

        // Keep details on when we started looking for events
        long eventProcessStartTime = System.currentTimeMillis();
        int eventSearchesPerformed = 0;
        
runStateChanged:
        while (isRunning())
        {
            long eventProcessSearchTime = System.currentTimeMillis();
            // Make sure we don't look for events too frequently
            while (true)
            {
                if (!isRunning())
                {
                    break runStateChanged;
                }
                // Calculate how many searches we are allowed to have performed
                long eventProcessElapsedTime = eventProcessSearchTime - eventProcessStartTime;
                int eventSearchesAllowed = (int) Math.floor((eventProcessElapsedTime/1000.0) * eventsPerSecond);
                if (eventSearchesPerformed < eventSearchesAllowed)
                {
                    // Yield to other threads
                    Thread.yield();
                    // We are allowed to do more
                    break;
                }
                // We need to wait and allow enough time to elapse.
                // We cut the mean time between checks in half
                long toSleep = (long) (1000L / eventsPerSecond) / 2;
                toSleep = (toSleep < 10L) ? 10L : toSleep;
                synchronized (this)
                {
                    try { this.wait(toSleep); } catch (InterruptedException e) {}
                }
                // Now go back around the see if we are allowed to proceed
                eventProcessSearchTime = System.currentTimeMillis();
            }
            // We record the event search regardless of missing or hit in the queue
            eventSearchesPerformed++;
            // Grab an event
            // First look for events specific to this server
            Event event = eventService.nextEvent(serverId, eventProcessSearchTime, true);
            if (event == null)
            {
                // Nothing found for the server.
                // Look for something that anyone can grab
                event = eventService.nextEvent(serverId, eventProcessSearchTime, false);
            }
            // Do we have an event to process?
            if (event == null)
            {
                long count = eventService.count();
                if (count == 0)
                {
                    // Look in the results to see if the run was started at some point
                    List<EventRecord> startRecords = resultService.getResults(Event.EVENT_NAME_START, 0, 1);
                    if (startRecords.size() == 0)
                    {
                        // The test has not *ever* been started.
                        // We do that now; note that the event name will enforce a unique ID
                        event = new Event(Event.EVENT_NAME_START, 0L, null);
                        try
                        {
                            eventService.putEvent(event);
                            // There is no guarantee that it actually went in
                        }
                        catch (RuntimeException e)
                        {
                            // We were unable to start the whole process.
                            // We assume that someone else has.
                        }
                    }
                    else
                    {
                        // The test was started but there are no more events remaining.
                        // Quit
                        if (ctx != null)        // The controller might have been run manually
                        {
                            ctx.publishEvent(new ContextStoppedEvent(ctx));
                        }
                    }
                }
                // Go back to the queue
                continue;
            }
            // Find the processor for the event
            EventProcessor processor = getProcessor(event);
            // Schedule it
            EventWork work = new EventWork(
                    serverId, testRunFqn,
                    event,
                    processor, eventProducers,
                    eventService, resultService, sessionService);
            try
            {
                // Grabbing an event automatically applies a short-lived lock to prevent
                // any other servers from grabbing the same event before the event is locked
                // for execution.
                executor.execute(work);
            }
            catch (RejectedExecutionException e)
            {
                // Should not occur as the caller executes
                eventSearchesPerformed += threadCount;
            }
            catch (RuntimeException e)
            {
                // Put here in case a CallerRunsPolicy is used
                logger.error("execute failed (pool or CallerRunsPolicy)", e);
            }
        }
        
        logger.info("\tEvent processing stopped: " + testRunFqn);
    }
    
    /**
     * Get a processor for the event.  If an event is not mapped, and error is logged
     * and the event is effectively absorbed.
     */
    private EventProcessor getProcessor(Event event)
    {
        String eventName = event.getName();
        EventProcessor processor = eventProcessors.getProcessor(eventName);
        if (processor == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("\n" +
                        "No event processor mapped to event: \n" +
                        "   Event name: " + eventName + "\n" +
                        "   Event:      " + event);
            }
            processor = doNothingProcessor;
        }
        return processor;
    }
}
