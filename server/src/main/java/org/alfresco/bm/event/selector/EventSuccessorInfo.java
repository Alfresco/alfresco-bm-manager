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
 *  
 * @author Steve Glover
 * @since 1.3
 */
public class EventSuccessorInfo
{
    private final String eventName;
    private final int weighting;
    private final long delay;
    
    public EventSuccessorInfo(String eventName, int weightingOverride)
    {
        this(eventName, "", weightingOverride, 0L);
    }

    public EventSuccessorInfo(String eventName, String weightingsStr)
    {
        this(eventName, weightingsStr, -1, 0L);
    }

    public EventSuccessorInfo(String eventName, String weightings, int weightingOverride)
    {
        this(eventName, weightings, weightingOverride, 0L);
    }

    /**
     * @param eventName                     the name of the event being lent some weight
     * @param weightings                    a comma-separated list of weighting values that will be multiplied together e.g. "1, 5" will give a weighting of 5.
     * @param weightingOverride             a value to override the weightings (ignored if less than zero)
     * @param delay                         the delay before the next event
     */
    public EventSuccessorInfo(String eventName, String weightings, int weightingOverride, long delay)
    {
        this.eventName = eventName.trim();
        if (weightingOverride >= 0)
        {
            this.weighting = weightingOverride;
        }
        else if (weightings != null)
        {
            this.weighting = parseWeightings(weightings);
        }
        else
        {
            this.weighting = 1;
        }
        this.delay = delay;
    }
    
    private int parseWeightings(String weightings)
    {
        int weighting = 1;

        StringTokenizer st = new StringTokenizer(weightings, ",");
        while (st.hasMoreTokens())
        {
            String weighingStr = st.nextToken().trim();
            weighting *= Integer.parseInt(weighingStr);
        }

        return weighting;
    }
    
    public String getEventName()
    {
        return eventName;
    }
    
    public int getWeighting()
    {
        return weighting;
    }
    
    public long getDelay()
    {
        return delay;
    }
    
    @Override
    public String toString()
    {
        return "EventSuccessorInfo [eventName=" + eventName + ", weighting="
                + weighting + ", delay=" + delay + "]";
    }
}
