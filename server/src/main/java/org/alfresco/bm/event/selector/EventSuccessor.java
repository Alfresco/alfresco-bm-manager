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

/**
 * Information on an event successor, including the event name, relative weighting and delay.
 *  
 * @author Steve Glover
 * @author Derek Hulley
 * @since 1.3
 */
public class EventSuccessor
{
    private final String eventName;
    private final double weight;
    
    public EventSuccessor(String eventName, double weight)
    {
        this.eventName = eventName;
        this.weight = weight;
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
        StringBuilder builder = new StringBuilder();
        builder.append("EventSuccessor [eventName=");
        builder.append(eventName);
        builder.append(", weight=");
        builder.append(weight);
        builder.append("]");
        return builder.toString();
    }
}
