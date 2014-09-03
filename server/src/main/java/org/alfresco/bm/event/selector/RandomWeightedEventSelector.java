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
package org.alfresco.bm.event.selector;

import java.util.List;

import org.alfresco.bm.event.EventProcessorRegistry;

/**
 * Select a successor event from a set of relatively-weighted event successors.
 * <p/>
 * A <a href="http://stackoverflow.com/a/6409791">weighted selection algorithm</a> is used.
 * <p/>
 * The list of successor events is defined thus:
 * <pre><![CDATA[
            <bean class="org.alfresco.bm.event.selector.RandomWeightedEventSelector">
                <constructor-arg name="registry" ref="eventProcessors"/>
                <constructor-arg name="eventSuccessors">
                    <list>
                        <value>publicapi.getSites,1,${delay}</value>
                        <value>publicapi.query,5,${delay}</value>
                    </list>
                </constructor-arg>
            </bean>
 * ]]></pre>
 * The <code>query</code> event is five times more likely to occur than <code>getSites</code><br/>
 * The <code>${delay}</code> is optional and defaults to zero (i.e. no delay).
 *  
 * @author Steve Glover
 * @since 1.3
 */
public class RandomWeightedEventSelector extends AbstractEventSelector
{
    private static final String ERR_FORM = "Event successor must be of form: <eventname>,<weighting>[,<delay>]";
    
    private final RandomWeightedSelector<EventSuccessor> selector = new RandomWeightedSelector<EventSuccessor>();

    /**
     * @param registry              registry that contains references to next events
     * @param eventSuccessors       list of events to choose from in form <code>eventname,weighting[,delay]</code>
     */
    public RandomWeightedEventSelector(String name, EventProcessorRegistry registry, List<EventSuccessorInfo> eventSuccessors)
    {
        super(name, registry);
        for(EventSuccessorInfo eventSuccessorInfo : eventSuccessors)
        {
            String eventName = eventSuccessorInfo.getEventName();
            if(eventName == null || eventName.length() == 0)
            {
                throw new RuntimeException(ERR_FORM + ".  Actual: " + eventSuccessorInfo);
            }

            long delay = eventSuccessorInfo.getDelay();
            int weighting = eventSuccessorInfo.getWeighting();
            EventSuccessor eventSuccessor = new EventSuccessor(eventName, weighting, delay);
            selector.add(weighting, eventSuccessor);
        }
    }
    
    /**
     * @param registry              registry that contains references to next events
     * @param eventSuccessors       list of events to choose from in form <code>eventname,weighting[,delay]</code>
     */
    public RandomWeightedEventSelector(EventProcessorRegistry registry, List<EventSuccessorInfo> eventSuccessors)
    {
        this(null, registry, eventSuccessors);
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
