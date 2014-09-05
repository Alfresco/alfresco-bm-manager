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
package org.alfresco.bm.test;

import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.ResultService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Measure the completion ratio based on the number of a specific type of event.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class EventCountCompletionEstimator extends AbstractCompletionEstimator
{
    private static Log logger = LogFactory.getLog(EventCountCompletionEstimator.class);
    
    private final String eventName;
    private final long eventCount;
    
    /**
     * Constructor with required dependencies
     * 
     * @param eventService                  used to do final checks of event counts
     * @param resultService                 used to count results
     * @param eventName                     the name of the event to count
     * @param eventCount                    the total number of events expected
     */
    public EventCountCompletionEstimator(
            EventService eventService,
            ResultService resultService,
            String eventName, long eventCount)
    {
        super(eventService, resultService);
        this.eventName = eventName;
        this.eventCount = eventCount;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Counts the number of the target event relative to the total expected.
     */
    @Override
    protected double getCompletionImpl()
    {
        if (eventCount <= 0L)
        {
            // Bypass the calculation
            return 1.0;
        }
        long results = resultService.countResultsByEventName(eventName);
        // Check if the event count values were chosen correctly.
        if (results > eventCount)
        {
            logger.warn("The number of results for event '" + eventName + "' exceeds the target: " + results + " exceeds " + eventCount);
            results = eventCount;       // Make sure it's 1.0 again
        }
        // If we have hit the completion phase, switch to the event count
        return (double) results/eventCount;
    }
}
