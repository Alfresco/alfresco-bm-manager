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
package org.alfresco.bm.common.util.junit.tools;

import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.common.spring.TestRunServicesCache;
import org.alfresco.bm.manager.Application;
import org.alfresco.bm.manager.report.CSVReporter;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.alfresco.bm.common.TestConstants.PROP_APP_RELEASE;
import static org.alfresco.bm.common.TestConstants.PROP_APP_SCHEMA;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_TEST_DATABASE;
import static org.alfresco.bm.common.TestConstants.PROP_TEST_RUN_MONITOR_PERIOD;
import static org.junit.Assert.assertEquals;
/**
 * @see BMTestRunner
 * 
 * @author Derek Hulley
 * @since 2.0
 */

@RunWith(JUnit4.class)
public class BMTestRunnerTest
{
    public static final String RELEASE = "BMTestRunnerTest";
    public static final int SCHEMA = 0;
    
    private MongoDBForTestsFactory mockDB;

    @Before
    public void setSystemProperties()
    {
        System.setProperty(PROP_APP_RELEASE, RELEASE);
        System.setProperty(PROP_APP_SCHEMA, "" + SCHEMA);
        System.setProperty(PROP_TEST_RUN_MONITOR_PERIOD, "" + 500);
    }

    /**
     * Prevent interference when running all tests in random order
     */
    @After
    public void resetSystemProperties()
    {
        System.clearProperty(PROP_APP_RELEASE);
        System.clearProperty(PROP_APP_SCHEMA);
        System.clearProperty(PROP_TEST_RUN_MONITOR_PERIOD);
    }
    
    @After
    public void shutdownMockMongo() throws Exception
    {
        if (mockDB != null)
        {
            mockDB.destroy();
        }
    }
    
    
    /**
     * Check that time-based limits are applied
     */
    @Test
    public void testWithTimeout() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(2000L);
        BMTestRunnerListener listener = Mockito.mock(BMTestRunnerListener.class);
        runner.addListener(listener);
        try
        {
            runner.run(null, null, null);
        }
        catch (RuntimeException e)
        {
            if (e.getMessage() != null)
            {
                Assert.assertTrue("Message was incorrect: " + e.getMessage(), e.getMessage().startsWith("Test run failed to complete"));
            }
            else
            {
                throw e;
            }
        }
        Mockito.verify(listener, Mockito.times(1)).testReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunStarted(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(0)).testRunFinished(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
    }
    
    /**
     * Check that the mock MongoDB is created for us when nothing is provided
     */
    @Test
    public void testWithMockMongo() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(60000L);
        BMTestRunnerListener listener = Mockito.mock(BMTestRunnerListener.class);
        runner.addListener(listener);
        runner.run(null, null, null);
        Mockito.verify(listener, Mockito.times(1)).testReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunStarted(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunFinished(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
    }
    
    /**
     * Run the {@link BMTestRunner} with an instance of Mongo available for config
     */
    @Test
    public void testWithRunningConfigMongo() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(60000L);
        BMTestRunnerListener listener = Mockito.mock(BMTestRunnerListener.class);
        runner.addListener(listener);

        MongoDBForTestsFactory mongoDBForTestsFactory = new MongoDBForTestsFactory();
        try
        {
            // Extract the Mongo DB details
            ServerAddress serverAddress = mongoDBForTestsFactory.getServerAddress();
            String host = serverAddress.getHost() + ":" + serverAddress.getPort();
            runner.run(host, null, null);
        }
        finally
        {
            try { mongoDBForTestsFactory.destroy(); } catch (Exception e) {}
        }
        Mockito.verify(listener, Mockito.times(1)).testReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunStarted(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunFinished(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
    }
    
    /**
     * Run the {@link BMTestRunner} with an instance of Mongo available for config
     */
    @Test
    public void testWithRunningDataMongo() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(60000L);
        BMTestRunnerListener listener = Mockito.mock(BMTestRunnerListener.class);
        runner.addListener(listener);

        MongoDBForTestsFactory mongoDBForTestsFactory = new MongoDBForTestsFactory();
        try
        {
            // Extract the Mongo DB details
            ServerAddress serverAddress = mongoDBForTestsFactory.getServerAddress();
            String host = serverAddress.getHost() + ":" + serverAddress.getPort();
            runner.run(null, host, null);
        }
        finally
        {
            try { mongoDBForTestsFactory.destroy(); } catch (Exception e) {}
        }
        Mockito.verify(listener, Mockito.times(1)).testReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunReady(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunStarted(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(listener, Mockito.times(1)).testRunFinished(Mockito.any(ClassPathXmlApplicationContext.class), Mockito.any(String.class), Mockito.any(String.class));
    }
    
    /**
     * Ensure that the results can be accessed from the callbacks
     */
    @Test
    public void testAccessToResults() throws Exception
    {
        BMTestRunnerListener listener = new BMTestRunnerListener()
        {
            private ResultService rs;
            @Override
            public void testReady(ApplicationContext testCtx, String test)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                Assert.assertNotNull(services);
            }
            @Override
            public void testRunReady(ApplicationContext testCtx, String test, String run)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                rs = services.getResultService(test, run);
                Assert.assertNotNull(rs);
                Assert.assertEquals("Should not be any events", 0, rs.getEventNames().size());
            }
            @Override
            public void testRunStarted(ApplicationContext testCtx, String test, String run)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                ResultService rsCheck = services.getResultService(test, run);
                Assert.assertTrue("Expected a cached instance of the ResultService.", rsCheck == rs);
            }
            @Override
            public void testRunFinished(ApplicationContext testCtx, String test, String run)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                ResultService rsCheck = services.getResultService(test, run);
                Assert.assertTrue("Expected a cached instance of the ResultService.", rsCheck == rs);
                Assert.assertEquals("Incorrect number of results.", 21, rs.countResults());
                
                // Access the summary results
                CSVReporter summaryReporter = new CSVReporter(services, test, run);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
                String summary = "";
                try
                {
                    summaryReporter.export(bos);
                    summary = new String(bos.toByteArray(), "UTF-8");
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                Assert.assertTrue(summary.contains("Standard Deviation (ms)"));
                
                // Check the log messages for the test run
                LogService logService = testCtx.getBean(LogService.class);
                DBCursor cursor = logService.getLogs(null, test, run, LogLevel.TRACE, null, null, 0, Integer.MAX_VALUE);
                try
                {
                    String logs = cursor.size() > 0 ? JSON.serialize(cursor) : "[]";
                    // The test run context start/stop gives 2 messages; the EventController start/stop gives another 2
                    assertEquals("The number of log messages needs to be exact: " + cursor + "\n " + logs, 6, cursor.size());
                }
                finally
                {
                    cursor.close();
                }
            }
        };
        
        BMTestRunner runner = new BMTestRunner(60000L);
        runner.addListener(listener);

        MongoDBForTestsFactory mongoDBForTestsFactory = new MongoDBForTestsFactory();
        try
        {
            // Extract the Mongo DB details
            ServerAddress serverAddress = mongoDBForTestsFactory.getServerAddress();
            String host = serverAddress.getHost() + ":" + serverAddress.getPort();
            runner.run(null, host, null);
        }
        finally
        {
            try { mongoDBForTestsFactory.destroy(); } catch (Exception e) {}
        }
    }
    
    @Test
    public void testWithProperties() throws Throwable
    {
        // Override the Mongo configuration host to something that fails
        final Properties testProperties = new Properties();
        testProperties.setProperty(PROP_MONGO_TEST_DATABASE, "testWithProperties");
        
        /**
         * A listener that ensures that we've pushed the test run results into the correct database
         */
        BMTestRunnerListener listener = new BMTestRunnerListenerAdaptor()
        {
            @Override
            public void testRunStarted(ApplicationContext testCtx, String test, String run)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                ResultService rsCheck = services.getResultService(test, run);
                String expectedDataLocation = testProperties.get(PROP_MONGO_TEST_DATABASE) + "." + test + "." + run + ".results";
                Assert.assertEquals("Data location was not changed for test.", expectedDataLocation, rsCheck.getDataLocation());
            }
        };
        BMTestRunner runner = new BMTestRunner(60000L);
        runner.addListener(listener);
        runner.run(null, null, testProperties);
    }
}
