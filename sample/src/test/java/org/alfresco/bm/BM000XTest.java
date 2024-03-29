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
package org.alfresco.bm;

import static org.alfresco.bm.common.TestConstants.PROP_APP_RELEASE;
import static org.alfresco.bm.common.TestConstants.PROP_APP_SCHEMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.alfresco.bm.common.EventRecord;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.common.TestService;
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.common.session.SessionService;
import org.alfresco.bm.common.spring.TestRunServicesCache;
import org.alfresco.bm.common.util.junit.tools.BMTestRunner;
import org.alfresco.bm.common.util.junit.tools.BMTestRunnerListener;
import org.alfresco.bm.common.util.junit.tools.BMTestRunnerListenerAdaptor;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.driver.event.EventService;
import org.alfresco.bm.manager.api.v1.ResultsRestAPI;
import org.alfresco.bm.manager.api.v1.TestRestAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.ApplicationContext;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

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

    /*
     * App.release and app.schema properties are read from the sample driver properties file and set as env variables.
     * For some reason the app.properties is not loaded when running the junits,
     * but it is loaded when running the spring boot app.
     */
    @Before
    public void setSystemProperties() throws IOException
    {
        // Manually load properties from app.properties file
        Properties properties = new Properties();
        InputStream input = getClass().getResourceAsStream("/config/startup/app.properties");
        properties.load(input);

        System.setProperty(PROP_APP_RELEASE, properties.getProperty(PROP_APP_RELEASE));
        System.setProperty(PROP_APP_SCHEMA, properties.getProperty(PROP_APP_SCHEMA));
    }

    /**
     * Prevent interference when running all tests in random order
     */
    @After
    public void resetSystemProperties()
    {
        System.clearProperty(PROP_APP_RELEASE);
        System.clearProperty(PROP_APP_SCHEMA);
    }

    @Test
    public void runQuick() throws Exception
    {
        Properties props = new Properties();
        props.put("test.duration", "1");                        // By the first check, it should say it's overdue a stop
        BMTestRunner runner = new BMTestRunner(20000L);
        runner.addListener(new BMTestRunnerListenerAdaptor()
        {
            @Override
            public void testRunFinished(ApplicationContext testCtx, String test, String run)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                EventService eventService = services.getEventService(test, run);
                // Check that there are still events in the event queue, which will show that we terminated by time
                assertNotEquals("There should be events in the event queue. ", 0L, eventService.count());
            }
        });
        runner.run(null, null, props);
    }
    
    @Test
    public void runComplete() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(300000L);         // Should be done in 60s
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
        LogService logService = testCtx.getBean(LogService.class);
        MongoTestDAO testDAO = services.getTestDAO();
        TestService testService = services.getTestService();
        ResultService resultService = services.getResultService(test, run);
        SessionService sessionService = services.getSessionService(test, run);
        assertNotNull(resultService);
        TestRestAPI testAPI = new TestRestAPI(testDAO, testService, logService, services);
        ResultsRestAPI resultsAPI = new ResultsRestAPI(services);
        // Let's check the results before the DB gets thrown away (we didn't make it ourselves)

        // Dump one of each type of event for information
        Set<String> eventNames = new TreeSet<String>(resultService.getEventNames());
        logger.info("Showing 1 of each type of event:");
        for (String eventName : eventNames)
        {
            List<EventRecord> eventRecord = resultService.getResults(eventName, 0, 1);
            logger.info("   " + eventRecord);
            assertFalse(
                    "An event was created that has no available processor or producer: " + eventRecord + ".  Use the TerminateEventProducer to absorb events.",
                    eventRecord.toString().contains("processedBy=unknown"));
        }

        // One successful START event
        assertEquals("Incorrect number of start events.", 1, resultService.countResultsByEventName(Event.EVENT_NAME_START));
        List<EventRecord> results = resultService.getResults(0L, Long.MAX_VALUE, false, 0, 1);
        if (results.size() != 1 || !results.get(0).getEvent().getName().equals(Event.EVENT_NAME_START))
        {
            Assert.fail(Event.EVENT_NAME_START + " failed: \n" + results.toString());
        }
        
        /*
         * 'start' = 1 result
         * 'scheduleProcesses' = 2 results
         * 'executeProcess' = 200 results
         * Sessions = 200
         */
        assertEquals("Incorrect number of event names: " + eventNames, 3, eventNames.size());
        assertEquals(
                "Incorrect number of events: " + "scheduleProcesses",
                2, resultService.countResultsByEventName("scheduleProcesses"));
        assertEquals(
                "Incorrect number of events: " + "executeProcess",
                200, resultService.countResultsByEventName("executeProcess"));
        // 203 events in total
        assertEquals("Incorrect number of results.", 203, resultService.countResults());
        // Check that we got the failure rate correct ~30%
        long failures = resultService.countResultsByFailure();
        assertEquals("Failure rate out of bounds. ", 60.0, (double) failures, 15.0);
        
        // Get the summary CSV results for the time period and check some of the values
        String summary = BMTestRunner.getResultsCSV(resultsAPI, test, run);
        logger.info(summary);
        assertTrue(summary.contains(",,scheduleProcesses,     2,"));
        assertTrue(summary.contains(",,executeProcess,   200,"));
        
        // Get the chart results and check
        String chartData = resultsAPI.getTimeSeriesResults(test, run, 0L, "seconds", 1, 10, true);
        if (logger.isDebugEnabled())
        {
            logger.debug("BM000X chart data: \n" + chartData);
        }
        // Check that we have 10.0 processes per second; across 10s, we should get 100 events
        assertTrue("Expected 10 processes per second.", chartData.contains("\"num\" : 100 , \"numPerSec\" : 10.0"));
        
        // Check the session data
        assertEquals("All sessions should be closed: ", 0L, sessionService.getActiveSessionsCount());
        assertEquals("All sessions should be used: ", 200L, sessionService.getAllSessionsCount());
        
        // Check the log messages
        DBCursor logs = logService.getLogs(null, test, run, LogLevel.INFO, null, null, 0, 500);
        try
        {
            assertEquals("Incorrect number of log messages for this test run. ", 6, logs.size());
            logger.debug("Log messages for " + test + "." + "." + run);
            while (logs.hasNext())
            {
                DBObject log = logs.next();
                Date time = (Date) log.get("time");
                String msg = (String) log.get("msg");
                logger.debug("   " + " >>> " + time + " >>> " + msg);
            }
        }
        finally
        {
            logs.close();
        }
    }
}
