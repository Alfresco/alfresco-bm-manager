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

import java.util.StringTokenizer;

/**
 * Data representing relative weight for given event names.
 * 
 * @author Steve Glover
 * @author Derek Hulley
 * @since 2.0
 */
public class EventWeight
{
    private final String eventName;
    private final double weight;
    
    /**
     * @see EventWeight#EventWeighting(String, double, String)
     */
    public EventWeight(String eventName, double weight)
    {
        this(eventName, weight, "");
    }

    /**
     * @see EventWeight#EventWeighting(String, double, String)
     */
    public EventWeight(String eventName, String weights)
    {
        this(eventName, -1.0, weights);
    }

    /**
     * @param eventName                     the name of the event being lent some weight
     * @param weight                        an explicit event weight (ignored if less than zero)
     * @param weights                       a comma-separated list of weight values that will be multiplied together e.g. "1.0, 0.5" will give a weighting of "0.5".
     */
    public EventWeight(String eventName, double weight, String weights)
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
    
    @Override
    public String toString()
    {
        return "EventWeight [eventName=" + eventName + ", weight=" + weight + "]";
    }
}
