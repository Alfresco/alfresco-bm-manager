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
package org.alfresco.bm.driver.test;

import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.driver.event.EventService;

/**
 * Only able to know if the test has started and assumes the test has finished
 * when there are no more events available.
 * <p/>
 * This estimator is likely to disappear when the test kickstart becomes more dynamic,
 * so look to use a more positive estimate in your tests.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class UnknownCompletionEstimator extends AbstractCompletionEstimator
{
    /**
     * Constructor with required dependencies
     * 
     * @param eventService                  used to count remaining events
     * @param resultService                 used to count results
     */
    public UnknownCompletionEstimator(EventService eventService, ResultService resultService)
    {
        super(eventService, resultService);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * @return                  0.0 always
     */
    @Override
    protected double getCompletionImpl()
    {
        long results = resultService.countResults();
        if (results == 0L)
        {
            return 0.0;
        }
        // We have some results.  Check if there are any events remaining.
        long events = eventService.count();
        return events == 0 ? 1.0 : 0.0;
    }
}
