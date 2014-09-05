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

/**
 * General support and controls for a {@link CompletionEstimator} implementation.
 * <p/>
 * This class ensures that checks are done periodically, requiring that implementations
 * supply results for the abstract methods only.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public abstract class AbstractCompletionEstimator implements CompletionEstimator
{
    /**
     * Default time between live completion checks
     */
    private static final long DEFAULT_CHECK_PERIOD = 5000L;

    protected final EventService eventService;
    protected final ResultService resultService;
    private long checkPeriod = DEFAULT_CHECK_PERIOD;
    private long lastCheck = 0L;                        // Never checked
    private double completion = 0.0;
    private long resultsSuccess = 0L;
    private long resultsFail = 0L;
    
    /**
     * @param eventService              used to do the final check when completion reachers 1.0
     * @param resultService             generally-useful service for estimation
     */
    protected AbstractCompletionEstimator(EventService eventService, ResultService resultService)
    {
        this.eventService = eventService;
        this.resultService = resultService;
    }

    /**
     * @return              the minimum time between real completion checks
     */
    public final long getCheckPeriod()
    {
        return checkPeriod;
    }

    /**
     * Override the {@link #DEFAULT_CHECK_PERIOD default} time between calls to the underlying
     * data providers
     */
    public final void setCheckPeriod(long checkPeriod)
    {
        this.checkPeriod = checkPeriod;
    }
    
    /**
     * Cache values and record the time at which this occurs
     */
    private void cacheData()
    {
        long now = System.currentTimeMillis();
        if (now - lastCheck < checkPeriod)
        {
            // We checked quite recently
            return;
        }
        // Keep track of the last completion
        double lastCompletion = this.completion;
        
        this.lastCheck = now;
        this.resultsSuccess = getResultsSuccessImpl();
        this.resultsFail = getResultsFailImpl();
        this.completion = getCompletionImpl();
        
        // Double check when  the completion reaches 1.0
        if (completion >= 1.0)
        {
            completion = 1.0;
        }
        else if (lastCompletion >= this.completion)
        {
            long eventCount = eventService.count();
            boolean haveResults = resultsSuccess > 0L || resultsFail > 0L;
            // We have not advanced.  Check against the event count
            if (eventCount == 0L && haveResults)
            {
                // We're done
                completion = 1.0;
            }
        }
    }

    @Override
    public final synchronized double getCompletion()
    {
        cacheData();
        return completion;
    }
    
    @Override
    public final synchronized long getResultsSuccess()
    {
        cacheData();
        return resultsSuccess;
    }

    @Override
    public final synchronized long getResultsFail()
    {
        cacheData();
        return resultsFail;
    }

    @Override
    public final synchronized boolean isStarted()
    {
        cacheData();
        return resultsSuccess > 0 || resultsFail > 0;
    }

    @Override
    public final synchronized boolean isCompleted()
    {
        cacheData();
        return completion >= 1.0;
    }

    /**
     * Implementation of a fetch to fetch the number of successful results
     * 
     * @return              the number of successful event results
     */
    protected long getResultsSuccessImpl()
    {
        return resultService.countResultsBySuccess();
    }

    /**
     * Implementation of a fetch to fetch the number of failed results
     * 
     * @return              the number of failed event results
     */
    protected long getResultsFailImpl()
    {
        return resultService.countResultsByFailure();
    }

    /**
     * Implementation of a fetch to get the completion state.
     * Once the estimate gets to or exceeds <tt>1.0</tt>, the test will be flagged as
     * {@link TestRunState#COMPLETED complete}.
     * 
     * @return              the data-led completion done as accurately as possible
     */
    protected abstract double getCompletionImpl();
}
