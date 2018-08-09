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
package org.alfresco.bm.manager.report;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Class assisting with the gathering of statistics for an event
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class ResultSummary
{
    private final String name;
    private SummaryStatistics statsSuccess;
    private SummaryStatistics statsFailure;

    public ResultSummary(String name)
    {
        this.name = name;
        this.statsSuccess = new SummaryStatistics();
        this.statsFailure = new SummaryStatistics();
    }
    
    /**
     * Add another sample to the event
     */
    public void addSample(boolean success, long time)
    {
        if (time < 0L)
        {
            throw new IllegalArgumentException("Sample time cannot be negative.");
        }
        if (success)
        {
            statsSuccess.addValue(time);
        }
        else
        {
            statsFailure.addValue(time);
        }
    }
    
    public String getName()
    {
        return name;
    }

    /**
     * Get the statistics for the event
     * 
     * @param success           <tt>true</tt> to return statistics for successs or
     *                          <tt>false</tt> to return failure statistics
     * @return                  the statics for success or failure
     */
    public SummaryStatistics getStats(boolean success)
    {
        if (success)
        {
            return statsSuccess;
        }
        else
        {
            return statsFailure;
        }
    }
    
    /**
     * Get the total number of results (success and failure)
     */
    public long getTotalResults()
    {
        return statsSuccess.getN() + statsFailure.getN();
    }
    
    /**
     * @return          the percentage of successful results or {@link Double#NaN Nan}
     *                  if there were no results
     */
    public double getSuccessPercentage()
    {
        long successes = statsSuccess.getN();
        long total = getTotalResults();
        if (total == 0)
        {
            return Double.NaN;
        }
        else
        {
            double percent = (double)successes / (double)total * 100.0;
            return percent;
        }
    }
}