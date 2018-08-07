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
package org.alfresco.bm.driver.event.selector;

import org.alfresco.bm.driver.event.EventProcessorRegistry;
import org.alfresco.bm.driver.event.EventWeight;
import org.alfresco.bm.driver.event.RandomWeightedSelector;

import java.util.List;

/**
 * Select a successor event from a set of relatively-weighted event successors.
 * <p/>
 * A <a href="http://stackoverflow.com/a/6409791">weighted selection algorithm</a> is used.
 * 
 * @see EventWeight
 * @author Steve Glover
 * @since 1.3
 */
public class RandomWeightedEventSelector extends AbstractEventSelector
{
    private final RandomWeightedSelector<EventSuccessor> selector = new RandomWeightedSelector<EventSuccessor>();

    /**
     * @param registry              registry that contains references to next events
     * @param eventWeights          list of events weights to select from
     */
    public RandomWeightedEventSelector(String name, EventProcessorRegistry registry, List<EventWeight> eventWeights)
    {
        super(name, registry);
        for (EventWeight eventWeight : eventWeights)
        {
            String eventName = eventWeight.getEventName();
            if(eventName == null || eventName.length() == 0)
            {
                throw new RuntimeException("No event name provided.");
            }

            double weight = eventWeight.getWeight();
            EventSuccessor eventSuccessor = new EventSuccessor(eventName, weight);
            selector.add(weight, eventSuccessor);
        }
    }
    
    /**
     * @param registry              registry that contains references to next events
     * @param eventWeights          list of events weights to select from
     */
    public RandomWeightedEventSelector(EventProcessorRegistry registry, List<EventWeight> eventWeights)
    {
        this(null, registry, eventWeights);
    }

    /**
     * Chooses randomly from the list of successor events based on the weightings provided.
     * 
     * @param input         ignored
     * @param response      ignored
     */
    @Override
    protected EventSuccessor next(Object input, Object response)
    {
        return selector.next();
    }

    @Override
    public int size()
    {
        return selector.size();
    }
}
