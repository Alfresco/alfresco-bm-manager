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

import org.alfresco.bm.event.ResultService;

/**
 * Measure the completion ratio based on the number of a specific type of event.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class EventCountCompletionEstimator extends AbstractCompletionEstimator
{
    private final ResultService resultService;
    private final String eventName;
    private final long eventCount;
    
    /**
     * Constructor with required dependencies
     * 
     * @param resultService                 used to count results
     * @param eventName                     the name of the event to count
     * @param eventCount                    the total number of events expected
     */
    public EventCountCompletionEstimator(
            ResultService resultService,
            String eventName, long eventCount)
    {
        this.resultService = resultService;
        this.eventName = eventName;
        this.eventCount = eventCount;
    }

    @Override
    protected long getResultsSuccessImpl()
    {
        return resultService.countResultsBySuccess();
    }

    @Override
    protected long getResultsFailImpl()
    {
        return resultService.countResultsByFailure();
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
        return (double) results/eventCount;
    }
}
