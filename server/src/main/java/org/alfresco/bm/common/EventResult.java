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
package org.alfresco.bm.common;

import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.driver.event.EventProcessor;

import java.util.Collections;
import java.util.List;

/**
 * The result of {@link EventProcessor#processEvent(Event) event processing}.
 * Instances can contain {@link #getNextEvents() future events} and additional
 * data to for record purposes.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class EventResult
{
    private final Object data;
    private final List<Event> nextEvents;
    private final boolean success;

    /**
     * Constructor for <b>successful</b> results containing {@link #getNextEvents() future events}
     * without any additional data to record.
     * 
     * @param nextEvents        any events that now follow on the processed event (may be empty)
     */
    public EventResult(List<Event> nextEvents)
    {
        this(null, nextEvents, true);
    }

    /**
     * Constructor for <b>successful</b> results containing {@link #getNextEvents() future events}
     * with additional data to record.
     * 
     * @param data              additional data to record (e.g. a {@link Throwable})
     * @param nextEvents        any events that now follow on the processed event (may be empty)
     */
    public EventResult(Object data, List<Event> nextEvents)
    {
        this(data, nextEvents, true);
    }
    
    /**
     * Constructor for <b>successful</b> results containing a {@link #getNextEvents() future event}
     * with additional data to record.
     * 
     * @param data              additional data to record (e.g. a {@link Throwable})
     * @param nextEvent         the event that now follow on the processed event (never <tt>null</tt>)
     */
    public EventResult(Object data, Event nextEvent)
    {
        this(data, Collections.singletonList(nextEvent), true);
    }
    
    /**
     * Constructor for results containing no future events.
     * 
     * @param data              additional data to record (e.g. a {@link Throwable})
     * @param success           <tt>true</tt> if the result represents a successful operation
     */
    public EventResult(Object data, boolean success)
    {
        this(data, Collections.emptyList(), success);
    }
    
    /**
     * Constructor for results containing {@link #getNextEvents() future events}
     * with additional data to record.
     * 
     * @param data              additional data to record (e.g. a {@link Throwable})
     * @param nextEvents        any events that now follow on the processed event (may be empty)
     * @param success           <tt>true</tt> if the result represents a successful operation
     */
    public EventResult(Object data, List<Event> nextEvents, boolean success)
    {
        this.data = data;
        this.nextEvents = nextEvents;
        this.success = success;
    }
    
    /**
     * @return                  Returns additional data to record or <tt>null</tt>
     */
    public Object getData()
    {
        return data;
    }

    /**
     * @return                  any events that now follow on the processed event (may be empty)
     */
    public List<Event> getNextEvents()
    {
        return nextEvents;
    }

    /**
     * @return                  <tt>true</tt> if the event was successful otherwise <tt>false</tt>
     */
    public boolean isSuccess()
    {
        return success;
    }

    @Override
    public String toString()
    {
        return "EventResult [data=" + data + ", nextEvents=" + nextEvents
                + ", success=" + success + "]";
    }
}
