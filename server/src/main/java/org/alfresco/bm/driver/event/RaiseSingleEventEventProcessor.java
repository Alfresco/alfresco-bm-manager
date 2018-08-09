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

import java.util.ArrayList;
import java.util.List;

/**
 * Emits a each of a list of events exactly once.
 * 
 * <h1>Input</h1>
 * 
 * Any data
 * 
 * <h1>Actions</h1>
 * 
 * Emits an event for each of the output event names given.
 * 
 * <h1>Output</h1>
 * 
 * {@link #outputEventName}: Obtains data from {@link #getNextEventData()}<br/>
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class RaiseSingleEventEventProcessor extends AbstractEventProcessor
{
    private final List<String> outputEventNames;
    
    /**
     * Constructor with <b>essential</b> values
     * 
     * @param outputEventNames              the namse of the events to emit
     * @param timeBetweenEvents             the time between events
     * @param outputEventCount              the number of events to emit
     */
    public RaiseSingleEventEventProcessor(List<String> outputEventNames)
    {
        super();
        this.outputEventNames = outputEventNames;
    }

    @Override
    public EventResult processEvent(Event event)
    {
        List<Event> nextEvents = new ArrayList<Event>(outputEventNames.size());

        for(String eventName : outputEventNames)
        {
            Event nextEvent = new Event(eventName, System.currentTimeMillis(), null);
            nextEvents.add(nextEvent);
        }

        // Done
        return new EventResult(
                "Created " + outputEventNames.size() + " events",
                nextEvents,
                true);
    }
}
