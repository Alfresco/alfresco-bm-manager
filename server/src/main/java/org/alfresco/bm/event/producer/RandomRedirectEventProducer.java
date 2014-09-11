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

import java.util.Collections;
import java.util.List;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventWeight;
import org.alfresco.bm.event.RandomWeightedSelector;

/**
 * Redirect events randomly based on relative weights.
 * <p/>
 * A <a href="http://stackoverflow.com/a/6409791">weighted selection algorithm</a> is used.
 * 
 * @see EventWeight
 * @see RandomWeightedSelector
 * 
 * @author Steve Glover
 * @author Derek Hulley
 * @since 2.0
 */
public class RandomRedirectEventProducer extends AbstractEventProducer
{
    private final RandomWeightedSelector<RedirectEventProducer> selector = new RandomWeightedSelector<RedirectEventProducer>();

    /**
     * @param eventWeights          list of events weights to select from
     */
    public RandomRedirectEventProducer(List<EventWeight> eventWeights)
    {
        for (EventWeight eventWeight : eventWeights)
        {
            String eventName = eventWeight.getEventName();
            if(eventName == null || eventName.length() == 0)
            {
                throw new RuntimeException("No event name provided.");
            }
            double weight = eventWeight.getWeight();
            // Construct a redirector for this
            RedirectEventProducer redirect = new RedirectEventProducer(eventName);
            selector.add(weight, redirect);
        }
    }

    @Override
    public List<Event> getNextEvents(Event event)
    {
        // Randomly choose a redirect
        RedirectEventProducer redirect = selector.next();
        if (redirect == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return redirect.getNextEvents(event);
        }
    }
}
