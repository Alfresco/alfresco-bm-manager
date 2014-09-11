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
package org.alfresco.bm.event.producer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Basic services for the {@link EventProducer event producer} implementations
 * <p/>
 * <b>Read {@link EventProdcuer 'good practice'} notes for documenting behaviour.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public abstract class AbstractEventProducer implements EventProducer, BeanNameAware
{
    protected static final long DEFAULT_WARN_DELAY = Long.MAX_VALUE;
    protected static final boolean DEFAULT_CHART = Boolean.TRUE;
    protected static final boolean DEFAULT_AUTO_PROPAGATE_SESSION_ID = Boolean.TRUE;
    protected static final boolean DEFAULT_AUTO_CLOSE_SESSION_ID = Boolean.TRUE;

    /** Resource for derived classes to use for logging */
    protected Log logger = LogFactory.getLog(this.getClass());
    private EventProducerRegistry registry;
    private List<String> eventNames;
    
    /**
     * Default constructor
     */
    public AbstractEventProducer()
    {
        eventNames = new ArrayList<String>(1);
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
        String eventName = beanName.replaceFirst(EventProducer.PRODUCER_NAME_PREFIX, "");
        setEventName(eventName);
    }

    /**
     * Set the single event name that this producer can handle
     */
    public void setEventName(String eventName)
    {
        this.eventNames.add(eventName);
    }
    
    /**
     * Set the names of the events that this producer can handle 
     */
    public void setEventNames(List<String> eventNames)
    {
        this.eventNames.addAll(eventNames);
    }

    /**
     * Inject the registry that this instance will {@link #register() register} with.
     */
    public synchronized void setRegistry(EventProducerRegistry registry)
    {
        this.registry = registry;
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
}
