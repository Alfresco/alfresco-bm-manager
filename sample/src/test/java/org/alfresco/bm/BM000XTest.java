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
package org.alfresco.bm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.StreamingOutput;

import org.alfresco.bm.api.v1.ResultsRestAPI;
import org.alfresco.bm.api.v1.TestRestAPI;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.process.ScheduleProcesses;
import org.alfresco.bm.test.TestRunServicesCache;
import org.alfresco.bm.test.TestService;
import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.alfresco.bm.tools.BMTestRunner;
import org.alfresco.bm.tools.BMTestRunnerListener;
import org.alfresco.bm.tools.BMTestRunnerListenerAdaptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.ApplicationContext;

/**
 * Sample on how to run your test against a local Mongo instance.
 * This does not replace running the test in the full BM environment,
 * but allows initial debugging to take place.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
@RunWith(JUnit4.class)
public class BM000XTest extends BMTestRunnerListenerAdaptor
{
    private static Log logger = LogFactory.getLog(BM000XTest.class);
    
    @Test
    public void runSample() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(60000L);         // Should be done in 60s
        runner.addListener(this);
        runner.run(null, null, null);
    }

    /**
     * A listener method that allows the test to check results <b>before</b> the in-memory MongoDB instance
     * is discarded.
     * <p/>
     * Check that the exact number of results are available, as expected
     * 
     * @see BMTestRunnerListener
     */
    @Override
    public void testRunFinished(ApplicationContext testCtx, String test, String run)
    {
        TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
        MongoTestDAO testDAO = services.getTestDAO(test, run);
        TestService testService = services.getTestService(test, run);
        ResultService resultService = services.getResultService(test, run);
        Assert.assertNotNull(resultService);
        // Let's check the results before the DB gets thrown away (we didn't make it ourselves)
        
        // One successful START event
        Assert.assertEquals("Incorrect number of start events.", 1, resultService.countResultsByEventName(Event.EVENT_NAME_START));
        List<EventRecord> results = resultService.getResults(0L, Long.MAX_VALUE, Event.EVENT_NAME_START, Boolean.TRUE, false, 0, 1);
        if (results.size() != 1)
        {
            Assert.fail(Event.EVENT_NAME_START + " failed: \n" + results.toString());
        }
        
        /*
         * Start = 1 result
         * Scheduling = 2 results
         * Processing = 200 results
         * Successful processing generates a No-op for each 
         */
        List<String> eventNames = resultService.getEventNames();
        Assert.assertEquals("Incorrect number of event names: " + eventNames, 4, eventNames.size());
        Assert.assertEquals(
                "Incorrect number of events: " + ScheduleProcesses.EVENT_NAME_PROCESS,
                200, resultService.countResultsByEventName(ScheduleProcesses.EVENT_NAME_PROCESS));
        long failures = resultService.countResultsByFailure();
        
        // 202 events in total
        Assert.assertEquals("Incorrect number of results.", (403-failures), resultService.countResults());
        
        // Test the charting API
        TestRestAPI testAPI = new TestRestAPI(testDAO, testService, services);
        ResultsRestAPI resultsAPI = testAPI.getTestRunResultsAPI(test, run);
        
        // Get the summary CSV results for the time period and check some of the values
        StreamingOutput out = resultsAPI.getResultsSummaryCSV();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
        String summary = "";
        try
        {
            out.write(bos);
            summary = bos.toString("UTF-8");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try { bos.close(); } catch (Exception e) {}
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("BM000X summary report: \n" + summary);
        }
        Assert.assertTrue(summary.contains("scheduleProcesses"));
        Assert.assertTrue(summary.contains("process"));
        
        // Get the chart results and check
        String chartData = resultsAPI.getResults(0L, "seconds", 10, 2, true);
        if (logger.isDebugEnabled())
        {
            logger.debug("BM000X chart data: \n" + chartData);
        }
    }
}
