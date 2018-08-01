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
package org.alfresco.bm.driver.event;

import org.alfresco.bm.common.EventResult;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * <b>TEST ONLY:</b> A processor that simply delays for random times and sometimes fails as well.
 * 
 * @author Derek Hulley
 * @since 2.0.4
 */
public class DelayingSampleEventProcessor extends AbstractEventProcessor
{
    private NormalDistribution normalDistribution;
    private final String outputEventName;
    private final long minTime;
    private final long maxTime;
    private final long failurePercent;
    
    /**
     * @param outputEventName       the output event name
     * @param minTime               the minimum apparent execution time
     * @param maxTime               the maximum apparent execution time
     * @param failurePercent        the percentage of events that must fail
     */
    public DelayingSampleEventProcessor(String outputEventName, long minTime, long maxTime, int failurePercent)
    {
        this.outputEventName = outputEventName;
        this.normalDistribution = new NormalDistribution();
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.failurePercent = failurePercent;
    }
    
    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        boolean fail = (Math.random() * 100) < failurePercent;
        long time = getValue(minTime, maxTime);
        
        Long mutex = new Long(0);
        synchronized(mutex)
        {
            mutex.wait(time);
        }
        
        if (fail)
        {
            return new EventResult("Sample must fail " + failurePercent + "% of the time.", false);
        }
        else
        {
            Event outputEvent = new Event(outputEventName, null);
            return new EventResult("Sample must succeed " + (100-failurePercent) + "% of the time.", outputEvent);
        }
    }
    
    private long getValue(long min, long max)
    {
        double sample = normalDistribution.sample();
        if (sample < -1.0)
        {
            sample = -1.0;
        }
        else if (sample > 1.0)
        {
            sample = 1.0;
        }
        long halfRange = (max - min)/2L;
        long mean = min + halfRange;
        long ret = mean + (long) (halfRange * sample);
        // Done
        return ret;
    }
}
