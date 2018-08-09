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

import org.alfresco.bm.common.EventRecord;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.common.TestService;
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.common.spring.TestRunServicesCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Abstract support for event reporting
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public abstract class AbstractEventReporter implements ReportGenerator
{
    protected static String ID_EVENT_LABEL = "Event Result Id";
    protected static String TIME_EVENT_LABEL = "Time";
    protected static String FAILED_LABEL_PREFIX = "Failed:";

    protected static final String NEW_LINE = "\n";
    protected static final int LIMIT_VALUE = 1000;
    
    protected Log log = LogFactory.getLog(this.getClass());

    protected final TestRunServicesCache services;
    protected final String test;
    protected final String run;

    /**
     * @param services          services for a test run
     * @param test              the name of the test for this report
     * @param run               the name of the test run for this report
     */
    protected AbstractEventReporter(TestRunServicesCache services, String test, String run)
    {
        this.services = services;
        this.test = test;
        this.run = run;
    }
    
    protected ResultService getResultService()
    {
        return services.getResultService(test, run);
    }

    protected TestService getTestService()
    {
        return services.getTestService();
    }

    protected MongoTestDAO getTestDAO()
    {
        return services.getTestDAO();
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + " [test=" + test + ", run=" + run + "]";
    }

    protected TreeMap<String, ResultSummary> collateResults(boolean chartOnly)
    {
        ResultService resultService = getResultService();
        // Do a quick check to see if there are results
        EventRecord firstEvent = resultService.getFirstResult();
        if (firstEvent == null)
        {
            return new TreeMap<String, ResultSummary>();
        }
        EventRecord lastEvent = resultService.getLastResult();

        long oneHour = TimeUnit.HOURS.toMillis(1L);
        long queryWindowStartTime = firstEvent.getStartTime();
        long queryWindowEndTime = queryWindowStartTime + oneHour;
        
        // Prepare recorded data
        TreeMap<String, ResultSummary> results = new TreeMap<String, ResultSummary>();
        int limit = 10000;
        int skip = 0;
        
        while (true)
        {
            List<EventRecord> data = resultService.getResults(queryWindowStartTime, queryWindowEndTime, chartOnly, skip, limit);
            if (data.size() == 0)
            {
                if (queryWindowEndTime > lastEvent.getStartTime())
                {
                    // The query window covered all known events, so we're done
                    break;
                }
                else
                {
                    // Push the window up
                    queryWindowStartTime = queryWindowEndTime;
                    queryWindowEndTime += oneHour;
                    skip = 0;
                    // Requery
                    continue;
                }
            }
            for (EventRecord eventRecord : data)
            {
                skip++;
                // Add the data
                String eventName = eventRecord.getEvent().getName();
                ResultSummary resultSummary = results.get(eventName);
                if (resultSummary == null)
                {
                    resultSummary = new ResultSummary(eventName);
                    results.put(eventName, resultSummary);
                }
                boolean resultSuccess = eventRecord.isSuccess();
                long resultTime = eventRecord.getTime();
                resultSummary.addSample(resultSuccess, resultTime);
            }
        }
        // Done
        return results;
    }
    
    /**
     * Stardard window sizes
     */
    public static final long[] WINDOW_SIZES = new long[]
                                                     {
        1L, 5L, 10L, 100L,                                          // < 1 second
        1000L, 5000L, 10000L, 30000L,                               // < 1 minute
        60000L, 120000L, 300000L, 600000L, 1800000L,                // < 1 hour
        360000L,                                                    // < 1 day
        24*360000L
                                                     };
    /**
     * Helper method to calculate a window size (ms) such that the approximate number
     * of results is retrieved across the given time range.
     * 
     * @param startTime             beginning of results
     * @param endTime               end of results
     * @param windowCount           number of windows to fit in
     * @return                      the size of a window in milliseconds
     */
    public static long getWindowSize(long startTime, long endTime, int windowCount)
    {
        long delta = endTime - startTime;
        // Try all the windows out
        for (int i = 0; i < WINDOW_SIZES.length; i++)
        {
            long windowSize = WINDOW_SIZES[i];
            double actualWindowCount = (double) delta / (double) windowSize;
            if (actualWindowCount < windowCount)
            {
                // We have too few results
                return windowSize;
            }
        }
        // Didn't find one big enough.  Go with the biggest.
        return WINDOW_SIZES[WINDOW_SIZES.length - 1];
    }
}
