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

import java.util.Collections;
import java.util.List;

import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.ResultService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Uses the highest estimate from a list of {@link CompletionEstimator estimations}.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class CompoundCompletionEstimator extends AbstractCompletionEstimator
{
    private static Log logger = LogFactory.getLog(CompoundCompletionEstimator.class);
    
    private final List<CompletionEstimator> estimators;
    
    /**
     * Constructor with required dependencies
     * 
     * @param eventService                  used to count remaining events
     * @param resultService                 used to count results
     * @param estimators                    the estimators to which the work will be delegated
     */
    public CompoundCompletionEstimator(EventService eventService, ResultService resultService, List<CompletionEstimator> estimators)
    {
        super(eventService, resultService);
        if (estimators.size() == 0)
        {
            throw new IllegalArgumentException("No estimators provided.");
        }
        this.estimators = Collections.unmodifiableList(estimators);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The first estimator to declare the run finish will, in effect, stop the run.
     * 
     * @return                  the maximum estimate as provided by the list of
     *                          {@link CompoundCompletionEstimator#CompoundCompletionEstimator(EventService, ResultService, List) estimators}
     *                          provided
     */
    @Override
    protected double getCompletionImpl()
    {
        double max = 0.0;
        for (CompletionEstimator estimator : estimators)
        {
            double nextEstimate = estimator.getCompletion();
            max = (nextEstimate > max) ? nextEstimate : max;
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Test run is " + String.format("%3.2f", (max * 100.0)) + "% complete.");
        }
        return max;
    }
}
