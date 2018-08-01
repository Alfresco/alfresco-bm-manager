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
package org.alfresco.bm.driver.event.producer;

import org.alfresco.bm.driver.event.Event;

import java.util.Collections;
import java.util.List;

/**
 * Redirect the inbound event into a single, renamed, possibly-delayed event.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class RedirectEventProducer extends AbstractEventProducer
{
    private final String newEventName;
    private final long delay;
    
    /**
     * Set the event to redirect to but use the current system time for scheduling.
     * 
     * @param newEventName              the new event name that inbound events will be tranformed to
     */
    public RedirectEventProducer(String newEventName)
    {
        this (newEventName, 0L);
    }
    
    /**
     * Set the event to redirect to using the given time for scheduling.
     * 
     * @param newEventName              the new event name that inbound events will be tranformed to
     * @param delay                     a delay to add to the current time, <tt>0</tt> to use the current time
     *                                  or negative values to schedule for the past (whatever that means).  If
     *                                  the events are already future-dated, then this delay is ignored.
     */
    public RedirectEventProducer(String newEventName, long delay)
    {
        this.newEventName = newEventName;
        this.delay = delay;
    }
    
    @Override
    public List<Event> getNextEvents(Event event)
    {
        String oldId = event.getId();
        long oldScheduledTime = event.getScheduledTime();
        String oldSessionId = event.getSessionId();
        Object oldData = event.getData();
        boolean oldDataInMemory = event.getDataInMemory();
        
        long scheduledTime = System.currentTimeMillis() + delay;
        if (oldScheduledTime > scheduledTime)
        {
            scheduledTime = oldScheduledTime;
        }
        
        Event newEvent = new Event(newEventName, scheduledTime, oldData, oldDataInMemory);
        if (oldId != null)
        {
            newEvent.setId(oldId);
        }
        if (oldSessionId != null)
        {
            newEvent.setSessionId(oldSessionId);
        }
        
        return Collections.singletonList(newEvent);
    }
}
