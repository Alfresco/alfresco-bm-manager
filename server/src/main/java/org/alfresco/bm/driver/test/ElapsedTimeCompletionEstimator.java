/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.driver.test;

import org.alfresco.bm.common.EventRecord;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.driver.event.EventService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeUnit;

/**
 * Measure the completion ratio based on the length of time the test has been running.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class ElapsedTimeCompletionEstimator extends AbstractCompletionEstimator
{
    private static Log logger = LogFactory.getLog(ElapsedTimeCompletionEstimator.class);
    
    private final long duration;
    
    /**
     * Constructor with required dependencies
     * 
     * @param resultService                 used find the first event time
     * @param timeUnitStr                   the unit used for the duration e.g. *SECONDS*, *MINUTES*, etc.
     * @param duration                      the duration of the test in whichever units are supplied
     */
    public ElapsedTimeCompletionEstimator(
            EventService eventService,
            ResultService resultService,
            String timeUnitStr,
            long duration)
    {
        super(eventService, resultService);
        // Get the time unit
        try
        {
            TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr.trim().toUpperCase());
            duration = timeUnit.toMillis(duration);
        }
        catch (Exception e)
        {
            // Assume milliseconds
        }
        this.duration = duration;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * @return                          Returns the ration between the elapsed time and the desired duration
     */
    @Override
    protected double getCompletionImpl()
    {
        EventRecord firstResult = resultService.getFirstResult();
        if (firstResult == null)
        {
            return 0.0;
        }
        long firstResultTime = firstResult.getStartTime();
        long elapsedTime = System.currentTimeMillis() - firstResultTime;
        double ratio = (double) elapsedTime / (double) duration;
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Test run is " + String.format("%3.2f", (ratio * 100.0)) + "% complete.");
        }
        return ratio;
    }
}
