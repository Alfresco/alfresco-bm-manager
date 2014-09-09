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

import java.util.StringTokenizer;

/**
 * Data representing relative weight for given event names.
 * 
 * @author Steve Glover
 * @author Derek Hulley
 * @since 1.3
 */
public class EventWeight
{
    private final String eventName;
    private final double weight;
    private final long delay;
    
    /**
     * @see EventWeight#EventWeighting(String, double, String, long)
     */
    public EventWeight(String eventName, double weight)
    {
        this(eventName, weight, "", 0L);
    }

    /**
     * @see EventWeight#EventWeighting(String, double, String, long)
     */
    public EventWeight(String eventName, String weights)
    {
        this(eventName, -1.0, weights, 0L);
    }

    /**
     * @see EventWeight#EventWeighting(String, double, String, long)
     */
    public EventWeight(String eventName, double weight, String weights)
    {
        this(eventName, weight, weights, 0L);
    }

    /**
     * @param eventName                     the name of the event being lent some weight
     * @param weight                        an explicit event weight (ignored if less than zero)
     * @param weights                       a comma-separated list of weight values that will be multiplied together e.g. "1.0, 0.5" will give a weighting of "0.5".
     * @param delay                         the delay before the next event
     */
    public EventWeight(String eventName, double weight, String weights, long delay)
    {
        this.eventName = eventName.trim();
        if (weight >= 0)
        {
            this.weight = weight;
        }
        else if (weights != null)
        {
            this.weight = parseWeights(weights);
        }
        else
        {
            this.weight = 1;
        }
        this.delay = delay;
    }
    
    private double parseWeights(String weightings)
    {
        double weighting = 1.0;

        StringTokenizer st = new StringTokenizer(weightings, ",");
        while (st.hasMoreTokens())
        {
            String weighingStr = st.nextToken().trim();
            weighting *= Double.parseDouble(weighingStr);
        }

        return weighting;
    }
    
    public String getEventName()
    {
        return eventName;
    }
    
    public double getWeight()
    {
        return weight;
    }
    
    public long getDelay()
    {
        return delay;
    }
    
    @Override
    public String toString()
    {
        return "EventSuccessorInfo [eventName=" + eventName + ", weight="
                + weight + ", delay=" + delay + "]";
    }
}
