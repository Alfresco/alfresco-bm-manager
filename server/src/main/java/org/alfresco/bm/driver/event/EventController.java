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
package org.alfresco.bm.driver.event;

import org.alfresco.bm.common.EventRecord;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.common.spring.LifecycleListener;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.alfresco.bm.common.util.log.TestRunLogService;
import org.alfresco.bm.driver.event.producer.EventProducerRegistry;
import org.alfresco.bm.driver.session.SessionService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    /** How long a driver has to grab assigned events */
    private static final long DEFAULT_ASSIGNED_EVENT_GRACE_PERIOD = 5000L;
    
    private static final Log logger = LogFactory.getLog(EventController.class);
    
    private final String driverId;
    private final String testRunFqn;
    private final EventService eventService;
    private final EventProducerRegistry eventProducers;
    private final EventProcessorRegistry eventProcessors;
    private final Thread thread;
    private final ExecutorService executor;
    private final ResultService resultService;
    private final SessionService sessionService;
    private final TestRunLogService logService;
    private final int threadCount;

    private int eventsPerSecondPerThread = DEFAULT_EVENTS_PER_SECOND_PER_THREAD;
    private long assignedEventGracePeriod = DEFAULT_ASSIGNED_EVENT_GRACE_PERIOD;

    private volatile String[] driverIds = new String[0];
    private ApplicationContext ctx;
    private boolean running;
    private EventProcessor doNothingProcessor = new DoNothingEventProcessor();
    
    /**
     * Construct the controller
     * 
     * @param driverId          the ID of the driver controlling the events
     * @param testRunFqn        the fully qualified name of the test run
     * @param testDAO           the test DAO for accessing low level data
     * @param testRunId         the ID of the test run being controlled
     * @param eventService      the source of events that will be pushed for execution
     * @param eventProducers    the registry of producers of events
     * @param eventProcessors   the registry of processors for events
     * @param resultService     the service used to store and retrieve event results
     * @param sessionService    the service to carry session IDs between events
     * @param logService        the service to record log messages for the end user
     * @param threadCount       the number of threads available to the processor
     */
    public EventController(
            String driverId,
            String testRunFqn,
            EventService eventService,
            EventProducerRegistry eventProducers,
            EventProcessorRegistry eventProcessors,
            ResultService resultService,
            SessionService sessionService,
            TestRunLogService logService,
            int threadCount)
    {
        thread = new Thread(new ThreadGroup(testRunFqn), this, testRunFqn + "-Controller");
        thread.setDaemon(false);         // Requires explicit shutdown
        
        this.driverId = driverId;
        this.testRunFqn = testRunFqn;
        this.eventService = eventService;
        this.eventProducers = eventProducers;
        this.eventProcessors = eventProcessors;
        this.resultService = resultService;
        this.sessionService = sessionService;
        this.logService = logService;
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
     * Override the {@link #DEFAULT_ASSIGNED_EVENT_GRACE_PERIOD default} time that an event can
     * be available on the queue for a specific driver before any other driver can pick it up.
     */
    public void setAssignedEventGracePeriod(int assignedEventGracePeriod)
    {
        this.assignedEventGracePeriod = assignedEventGracePeriod;
    }

    /**
     * Update the list of driver IDs in use.  This list can change at run time.
     */
    public void setDriverIds(String[] driverIds)
    {
        if (driverIds == null)
        {
            throw new IllegalArgumentException("'driverIds' may not be null.");
        }
        this.driverIds = driverIds;
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
        while (isRunning())
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
                    String stack = ExceptionUtils.getStackTrace(e);
                    logger.error("\tEvent processing error: " + testRunFqn, e);
                    logService.log(LogLevel.ERROR, "EventController's run method was terminated.  Attempting a restart: " + stack);
                    synchronized (this)
                    {
                        try { wait(5000L); } catch (InterruptedException ee) {}
                    }
                    continue;
                }
                else
                {
                    logger.debug("\tEvent processing terminated with error: " + testRunFqn, e);
                    break;
                }
            }
        }
    }
    
    /**
     * Do the actual run but without concern for exceptions, which will be logged.
     */
    private void runImpl()
    {
        Set<String> staleDrivers = new HashSet<String>(3);              // Keep track of any stale drivers
        
        int eventsPerSecond = (threadCount * eventsPerSecondPerThread);
        String msgStarted = "Event processing started: " + testRunFqn + " (" + eventsPerSecond + " events per second using " + threadCount + " threads)";
        logger.info("\t" + msgStarted);
        logService.log(LogLevel.INFO, msgStarted);

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
                long toSleep = 1000L / eventsPerSecond / 2;
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
            // First look for events specific to this driver
            Event event = eventService.nextEvent(driverId, eventProcessSearchTime);
            if (event == null)
            {
                // Nothing found for the driver.
                // Look for events from other drivers, giving them a grace period
                event = eventService.nextEvent(null, eventProcessSearchTime - assignedEventGracePeriod);
                if (event != null)
                {
                    String driver = event.getDriver();
                    if (staleDrivers.add(driver))
                    {
                        logger.error("Driver " + driver + " is leaving stale events.  Check server load.");
                    }
                }
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
                    driverId, testRunFqn,
                    event,
                    driverIds,
                    processor, eventProducers,
                    eventService, resultService, sessionService,
                    logService);
            try
            {
                // Grabbing an event automatically applies a short-lived lock to prevent
                // any other drivers from grabbing the same event before the event is locked
                // for execution.
                executor.execute(work);
            }
            catch (RejectedExecutionException e)
            {
                // Should not occur as the caller executes
                eventSearchesPerformed += threadCount;
                // Log it
                logService.log(
                        LogLevel.WARN, "EventController's execution of an event was rejected.  "
                        + "Are there enough drivers to handle the event load?");
            }
            catch (RuntimeException e)
            {
                // Put here in case a CallerRunsPolicy is used
                logger.error("execute failed (pool or CallerRunsPolicy)", e);
            }
        }
        
        String msgStopped = "Event processing stopped: " + testRunFqn;
        logger.info("\t" + msgStopped);
        logService.log(LogLevel.INFO, msgStopped);
    }
    
    /** Keep track of event names that have been warned about w.r.t. missing event processors. */
    private Set<String> nullEventProcessorWarnings = Collections.synchronizedSet(new HashSet<String>());
    /**
     * Get a processor for the event.  If an event is not mapped, an error is logged
     * and the event is effectively absorbed.
     */
    private EventProcessor getProcessor(Event event)
    {
        String eventName = event.getName();
        EventProcessor processor = eventProcessors.getProcessor(eventName);
        if (processor == null)
        {
            String msg =
                    "No event processor mapped to event.  Use a TerminateEventProducer to silently route events to nowhere: \n" +
                    "   Event name: " + eventName + "\n" +
                    "   Event:      " + event;
            // We are only here if we have not issued a warning already
            if (logger.isDebugEnabled())
            {
                logger.debug("\n" + msg);
            }
            if (nullEventProcessorWarnings.add(eventName))
            {
                logService.log(LogLevel.WARN, msg);
            }
            
            // Assign to do nothing
            processor = doNothingProcessor;
        }
        return processor;
    }
}
