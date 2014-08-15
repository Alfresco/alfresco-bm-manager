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
package org.alfresco.bm.report;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.test.TestConstants;
import org.alfresco.bm.test.TestRunServicesCache;
import org.alfresco.bm.test.TestService;
import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract support for event reporting
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public abstract class AbstractEventReporter implements ReportGenerator, TestConstants
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
}
