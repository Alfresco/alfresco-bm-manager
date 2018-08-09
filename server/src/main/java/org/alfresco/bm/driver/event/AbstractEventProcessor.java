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

import org.alfresco.bm.common.EventResult;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic services for the {@link EventProcessor event processor} implementations
 * <p/>
 * <b>Read {@link EventProcessor 'good practice'} notes for documenting behaviour.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public abstract class AbstractEventProcessor implements EventProcessor, BeanNameAware
{
    protected static final long DEFAULT_WARN_DELAY = Long.MAX_VALUE;
    protected static final boolean DEFAULT_CHART = Boolean.TRUE;
    protected static final boolean DEFAULT_AUTO_PROPAGATE_SESSION_ID = Boolean.TRUE;
    protected static final boolean DEFAULT_AUTO_CLOSE_SESSION_ID = Boolean.TRUE;

    /** Resource for derived classes to use for logging */
    protected Log logger = LogFactory.getLog(this.getClass());
    private EventProcessorRegistry registry;
    private String name = "unknown";
    private List<String> eventNames;
    private long warnDelay;
    private boolean chart;
    private boolean autoPropagateSessionId;
    private boolean autoCloseSessionId;
    
    /**
     * Default constructor
     */
    public AbstractEventProcessor()
    {
        eventNames = new ArrayList<String>(1);
        warnDelay = DEFAULT_WARN_DELAY;
        chart = DEFAULT_CHART;
        autoPropagateSessionId = DEFAULT_AUTO_PROPAGATE_SESSION_ID;
        autoCloseSessionId = DEFAULT_AUTO_CLOSE_SESSION_ID;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName() + " [eventNames=").append(eventNames);
        builder.append(", type=").append(this.getClass().getName());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void setBeanName(String beanName)
    {
        String eventName = beanName.replaceFirst(Event.EVENT_BEAN_PREFIX, "");
        setEventName(eventName);
        this.name = beanName;
    }

    /**
     * @return                  the name of the bean as configured in Spring
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Set the single event name that this processor can handle
     */
    public void setEventName(String eventName)
    {
        this.eventNames.add(eventName);
    }
    
    /**
     * Set the names of the events that this processor can handle 
     */
    public void setEventNames(List<String> eventNames)
    {
        this.eventNames.addAll(eventNames);
    }

    /**
     * Inject the registry that this instance will {@link #register() register} with.
     */
    public synchronized void setRegistry(EventProcessorRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * @return              the processing time after which an warning is logged (default {@link #DEFAULT_WARN_DELAY})
     */
    @Override
    public long getWarnDelay()
    {
        return warnDelay;
    }

    /**
     * @param warnDelay     the time allowed for processing before a warning is logged
     * @see #getWarnDelay()
     */
    public void setWarnDelay(long warnDelay)
    {
        this.warnDelay = warnDelay;
    }

    /**
     * @return              <tt>true</tt> if results from this processor must be included
     *                      in generated charts
     */
    @Override
    public boolean isChart()
    {
        return chart;
    }

    /**
     * @param chart         <tt>true</tt> to tag results for charting
     * @see #isChart()
     */
    public void setChart(boolean chart)
    {
        this.chart = chart;
    }

    /**
     * Change the {@link #DEFAULT_AUTO_PROPAGATE_SESSION_ID default} session ID propagation
     * behaviour
     * 
     * @param autoPropagateSessionId    <tt>true</tt> to propagate session IDs to the next event
     *                                  otherwise <tt>false</tt>  (default: <tt>true</tt>)
     */
    public void setAutoPropagateSessionId(boolean autoPropagateSessionId)
    {
        this.autoPropagateSessionId = autoPropagateSessionId;
    }

    /**
     * Change the {@link #DEFAULT_AUTO_CLOSE_SESSION_ID default} session ID auto-close
     * behaviour
     * 
     * @param autoCloseSessionId    <tt>true</tt> to allow the framework to aut-close sessions (default: <tt>true</tt>)
     */
    public void setAutoCloseSessionId(boolean autoCloseSessionId)
    {
        this.autoCloseSessionId = autoCloseSessionId;
    }

    @Override
    public boolean isAutoPropagateSessionId()
    {
        return autoPropagateSessionId;
    }

    @Override
    public boolean isAutoCloseSessionId()
    {
        return autoCloseSessionId;
    }

    /**
     * Register this instance for the event names that are handled.
     */
    public synchronized void register()
    {
        if (registry == null)
        {
            // Nothing to do
            return;
        }
        for (String eventName : eventNames)
        {
            registry.register(eventName, this);
        }
    }

    /** A thread-local timer that is reused as each event is processed */
    private ThreadLocal<StopWatch> stopWatchThreadLocal = new ThreadLocal<StopWatch>();
    /**
     * @return          Return the thread-local stop watch
     */
    private StopWatch getStopWatch()
    {
        StopWatch stopWatch = stopWatchThreadLocal.get();
        if (stopWatch == null)
        {
            throw new IllegalStateException("No stop watch has been provided to the processing thread.");
        }
        return stopWatch;
    }
    /**
     * Suspend the event processing timer.
     * 
     * @throws IllegalStateException        if the timer is not running
     */
    protected long suspendTimer()
    {
        StopWatch stopWatch = getStopWatch();
        stopWatch.suspend();
        return stopWatch.getTime();
    }
    /**
     * Continue timing the event processing
     * 
     * @throws IllegalStateException        if the timer is is not suspended
     */
    protected long resumeTimer()
    {
        StopWatch stopWatch = getStopWatch();
        stopWatch.resume();
        return stopWatch.getTime();
    }
    /**
     * Stop timing the event processing.  This does not need to be called by derived classes.
     * 
     * @throws IllegalStateException        if the timer is is not running or suspended
     */
    protected long stopTimer()
    {
        StopWatch stopWatch = getStopWatch();
        stopWatch.stop();
        return stopWatch.getTime();
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * The implementation overrides this method to store the {@link StopWatch} in a thread-local.
     */
    @Override
    public final EventResult processEvent(Event event, StopWatch stopWatch) throws Exception
    {
        if (stopWatch == null)
        {
            throw new IllegalArgumentException("'stopWatch' must be supplied.");
        }
        stopWatchThreadLocal.set(stopWatch);
        // We have to be able to start the timer here
        try
        {
            stopWatch.start();
        }
        catch (IllegalStateException e)
        {
            throw new IllegalArgumentException("The timer provided could not be started.", e);
        }
        try
        {
            // Do the actual processing
            return processEvent(event);
        }
        finally
        {
            // Stop the timer
            try
            {
                stopWatch.stop();
            }
            catch (IllegalStateException e)
            {
                // We are OK with this.  Derived classes could have stopped the timer.
            }
        }
    }
    
    /**
     * Process an event.
     * <p/>
     * <b>NOTE:</b> All errors are treated as <b>terminal</b> i.e. there will be no
     * follow-up events published.  The event and result services are provided to allow
     * event processing to be aware of previous and future work.  If the service is used
     * to modify queues, the results could be unexpected e.g. clearing the queue will not
     * always mean that no more events will be processed.
     * <p/>
     * Timing: Where required, implementations can manually control the times recorded
     *          for event processing.
     * 
     * @param event             the event (along with associated data)
     * @return                  the result of the process (includes errors and next events)
     * 
     * @see #suspendTimer()
     * @see #resumeTimer()
     * @see #stopTimer()
     */
    protected abstract EventResult processEvent(Event event) throws Exception;
}
