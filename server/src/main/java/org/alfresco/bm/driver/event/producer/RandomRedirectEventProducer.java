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
package org.alfresco.bm.driver.event.producer;

import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.driver.event.EventWeight;
import org.alfresco.bm.driver.event.RandomWeightedSelector;

import java.util.Collections;
import java.util.List;

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
