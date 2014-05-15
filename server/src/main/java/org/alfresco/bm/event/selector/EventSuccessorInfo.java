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
    private String eventName;
    private int weighting;
    private Long delay;
    
    public EventSuccessorInfo(String eventName, Integer weightingOverride)
    {
        this(eventName, null, weightingOverride, null);
    }

    public EventSuccessorInfo(String eventName, String weightingsStr)
    {
        this(eventName, weightingsStr, null, null);
    }

    public EventSuccessorInfo(String eventName, String weightings, Integer weightingOverride)
    {
        this(eventName, weightings, weightingOverride, null);
    }

    public EventSuccessorInfo(String eventName, String weightings, Integer weightingOverride, Long delay)
    {
        super();
        this.eventName = eventName.trim();
        if(weightings != null)
        {
            this.weighting = parseWeightings(weightings);
        }
        if(weightingOverride != null)
        {
            this.weighting += weightingOverride.intValue();
        }
        this.delay = delay;
    }
    
    private int parseWeightings(String weightings)
    {
        int weighting = 1;

        StringTokenizer st = new StringTokenizer(weightings, ",");
        while(st.hasMoreTokens())
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

    public Long getDelay()
    {
        return (delay == null ? 0l : delay);
    }

    @Override
    public String toString()
    {
        return "EventSuccessorInfo [eventName=" + eventName + ", weighting="
                + weighting + ", delay=" + delay + "]";
    }
}
