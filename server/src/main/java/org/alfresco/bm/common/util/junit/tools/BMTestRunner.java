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

import com.mongodb.MongoClientURI;
import com.mongodb.MongoSocketException;
import org.alfresco.bm.common.PropSetBean;
import org.alfresco.bm.common.TestDetails;
import org.alfresco.bm.common.TestRunDetails;
import org.alfresco.bm.common.TestRunSchedule;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.driver.test.Test;
import org.alfresco.bm.manager.api.v1.ResultsRestAPI;
import org.alfresco.bm.manager.api.v1.TestRestAPI;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.alfresco.bm.common.TestConstants.PATH_APP_CONTEXT;
import static org.alfresco.bm.common.TestConstants.PROP_APP_CONTEXT_PATH;
import static org.alfresco.bm.common.TestConstants.PROP_APP_DIR;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_CONFIG_HOST;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_TEST_HOST;
/**
 * Utility class that kicks off a BM test using the {@link MongoDBForTestsFactory} if MongoDB host has not
 * been specified.
 * <p/>
 * The mock context will create an in-memory MongoDB server that will be used for both the test configuration
 * and the actual test results.<br/>
 * All BM tests must contain a <code>config/spring/test-context.xml</code> that 
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class BMTestRunner
{
    private static final long MAX_TEST_TIME = 300000L;
    
    private static Log logger = LogFactory.getLog(BMTestRunner.class);
    
    private final long maxTestTime;
    private final List<BMTestRunnerListener> listeners;
    
    /**
     * @see #MAX_TEST_TIME
     */
    public BMTestRunner()
    {
        this(MAX_TEST_TIME);
    }
    
    /**
     * @param maxTestTime               the maximum time before we stop the test and fail
     */
    public BMTestRunner(long maxTestTime)
    {
        this.maxTestTime = maxTestTime;
        this.listeners = Collections.synchronizedList(new ArrayList<BMTestRunnerListener>(1));
    }
    
    /**
     * Add a listener.  This must be done before {@link #run(String, String) running} the test
     */
    public void addListener(BMTestRunnerListener listener)
    {
        this.listeners.add(listener);
    }
    
    private static void echoUsage()
    {
        System.out.println("   ");
        System.out.println("Usage of " + BMTestRunner.class.getName() + ":");
        System.out.println("   ");
        System.out.println("   BMTestRunner [mongo-config-host] [mongo-test-host]\n");
        System.out.println("      mongo-config-host:        The host connect to for configuration e.g. '192.168.0.72:27017'");
        System.out.println("                                If absent, an in-memory (transient storage) instance will be created.");
        System.out.println("      mongo-test-host:          The host to connect to for test data (results, etc) storage e.g. '192.168.0.80:27017'");
        System.out.println("                                If absent, the same instance as 'mongo-config-host' will be used.");
        System.out.println("   ");
        System.out.println("   Other values can be changed by settings system properties.");
        System.out.println("   To set the MongoDB connection properties for either the 'config' or 'test' databases:");
        System.out.println("      mongo.<config|test>.host:         Another way of setting the host");
        System.out.println("      mongo.<config|test>.port:         Change the port connection away from default of '27017'");
        System.out.println("      mongo.<config|test>.database:     Connect to a different database from default of 'bm21-config'");
        System.out.println("      mongo.<config|test>.username:     Apply username credential to the database connection");
        System.out.println("      mongo.<config|test>.password:     Supply a password if the username is specified");
        System.out.println("      mongo.<config|test>.uri:          Full URI format as specified by com.mongodb.MongoClientURI");
    }
    
    /**
     * Main method starts up the the application context and provides a callback
     * 
     * @see #echoUsage()
     */
    public static void main(String[] args)
    {
        if (args.length > 2 || (args.length == 1 && args[0].trim().toUpperCase().startsWith("help")))
        {
            echoUsage();
            return;
        }
        // MongoDB config connection
        String mongoConfigHost = null;
        if (args.length > 0)
        {
            mongoConfigHost = args[0];
            // TODO: Check the connection
        }
        // MongoDB test connection
        String mongoTestHost = null;
        if (args.length > 1)
        {
            mongoTestHost = args[1];
            // TODO: Check the connection
        }
        // Too many args
        if (args.length > 2)
        {
            echoUsage();
            return;
        }
        
        try
        {
            BMTestRunner runner = new BMTestRunner(MAX_TEST_TIME);
            runner.run(mongoConfigHost, mongoTestHost, null);
        }
        catch (Exception e)
        {
            logger.error("Failed to execute test with defaults due to internal error.", e);
            echoUsage();
        }
    }

    /**
     * Execute the default test against the given MongoDB or an in-memory instance
     * 
     * @param mongoConfigHost           the MongoDB host to connect to for configuraton data or <tt>null</tt> to use an in-memory version
     * @param mongoTestHost             the MongoDB host to connect to for test data data or <tt>null</tt> to use the same database as the config
     * @param testProperties            any properties to specifically set for the test or <tt>null</tt> if there are none
     */
    public void run(String mongoConfigHost, String mongoTestHost, Properties testProperties) throws Exception
    {
        // Secure the listeners against modification
        List<BMTestRunnerListener> listeners = new ArrayList<BMTestRunnerListener>(this.listeners);
        
        // If no MongoDB URL is provided, then we have to start one
        MongoDBForTestsFactory mongoDBForTestsFactory = null;
        ClassPathXmlApplicationContext ctx = null;
        try
        {
            // Ensure that required system properties are present
            System.setProperty(PROP_APP_CONTEXT_PATH, System.getProperty("user.dir"));
            System.setProperty(PROP_APP_DIR, System.getProperty("user.dir"));
            
            // Create a MongoDB for use if one has not been specified
            if (mongoConfigHost == null)
            {
                mongoDBForTestsFactory = new MongoDBForTestsFactory();
                String uriWithoutDB = mongoDBForTestsFactory.getMongoURIWithoutDB();
                mongoConfigHost = new MongoClientURI(uriWithoutDB).getHosts().get(0);
            }
            // Fill in the URI for the test MongoDB
            if (mongoTestHost == null)
            {
                mongoTestHost = mongoConfigHost;
            }
            
            // Fill in the properties required for the test
            Properties mongoProps = new Properties();
            mongoProps.put(PROP_MONGO_CONFIG_HOST, mongoConfigHost);
            
            // Construct the application context
            ctx = new ClassPathXmlApplicationContext(
                    new String[] {PATH_APP_CONTEXT},
                    false);
            // Push cluster properties into the context (must be done AFTER setting parent context)
            ConfigurableEnvironment ctxEnv = ctx.getEnvironment();
            // Mongo properties come first
            ctxEnv.getPropertySources().addFirst(
                    new PropertiesPropertySource(
                            "mongo-props",
                            mongoProps));
            // Finally, system properties overrule them all
            ctxEnv.getPropertySources().addFirst(
                    new PropertiesPropertySource(
                            "system-props",
                            System.getProperties()));
            
            // Kick it all off
            try
            {
                ctx.refresh();
            }
            catch (Exception e)
            {
                Throwable root = ExceptionUtils.getRootCause(e);
                if (root != null && (root instanceof MongoSocketException || root instanceof UnknownHostException))
                {
                    // We deal with this specifically as it's a simple case of not finding the MongoDB
                    logger.error("Set the configuration property '" + PROP_MONGO_CONFIG_HOST + "' (<server>:<port>) as required.");
                }
                else
                {
                    // Log application start failure because test frameworks might not do so nicely
                    logger.error("Failed to start application.", e);
                }
                throw new RuntimeException("Failed to start application.", e);
            }
            
            // Get the test
            Test test = ctx.getBean(Test.class);
            String release = test.getRelease();
            Integer schema = test.getSchema();
            
            TestRestAPI api = ctx.getBean(TestRestAPI.class);
            
            // Create a new test
            TestDetails testDetails = new TestDetails();
            String testName = "BMTestRunner_" + System.currentTimeMillis();
            testDetails.setName(testName);
            testDetails.setDescription("Test created by BMTestRunner on " + new Date());
            testDetails.setRelease(release);
            testDetails.setSchema(schema);
            api.createTest(testDetails);
            
            // We need to tell the test which MongoDB to write data to
            PropSetBean propSet = new PropSetBean();
            propSet.setValue(mongoTestHost);
            propSet.setVersion(0);
            api.setTestProperty(testName, PROP_MONGO_TEST_HOST, propSet);
            
            // Now set any properties that have been explicitly passed in for the test
            if (testProperties != null)
            {
                for (Map.Entry<Object, Object> entry : testProperties.entrySet())
                {
                    String propKey = (String) entry.getKey();
                    String propVal = (String) entry.getValue();

                    propSet.setValue(propVal);
                    propSet.setVersion(0);
                    api.setTestProperty(testName, propKey, propSet);
                }
            }
            
            // Call listeners: the test has been created
            for (BMTestRunnerListener listener : listeners)
            {
                listener.testReady(ctx, testName);
            }
            
            // Create a new test run
            TestRunDetails testRunDetails = new TestRunDetails();
            String testRunName = "BMTestRunner_" + System.currentTimeMillis();
            testRunDetails.setName(testRunName);
            testRunDetails.setDescription("Test run created by BMTestRunner on " + new Date());
            api.createTestRun(testDetails.getName(), testRunDetails);
            
            // Call listeners: the test run has been created
            for (BMTestRunnerListener listener : listeners)
            {
                listener.testRunReady(ctx, testName, testRunName);
            }
            
            // Get all the test run properties for logging
            String jsonTestRun = api.getTestRun(testName, testRunName);
            
            // Start the test run
            logger.info("Starting test run: " + testRunName + "\n" + jsonTestRun);
            TestRunSchedule testRunSchedule = new TestRunSchedule();
            testRunSchedule.setScheduled(System.currentTimeMillis());
            api.scheduleTestRun(testName, testRunName, testRunSchedule);
            
            // Call listeners: the test run has started
            for (BMTestRunnerListener listener : listeners)
            {
                listener.testRunStarted(ctx, testName, testRunName);
            }
            
            // Wait for the test run to complete
            long timeInit = System.currentTimeMillis();
            long timeLastChange = -1L;
            String jsonLastChange = null;
            String testRunStateStr = api.getTestRunState(testName, testRunName);
            
            // Keep looking until the test run completes
            while (!TestRunState.COMPLETED.toString().equals(testRunStateStr))
            {
                long now = System.currentTimeMillis();
                
                // Check that we have not exceeded the maximum time
                if (now - timeInit > maxTestTime)
                {
                    throw new RuntimeException("Test run failed to complete in " + (int)maxTestTime/1000 + "s.");
                }
                
                testRunStateStr = api.getTestRunState(testName, testRunName);
                
                if (TestRunState.SCHEDULED.toString().equals(testRunStateStr) && (now-timeInit) > 10000L)
                {
                    throw new RuntimeException("Test run failed to start in 10s.");
                }
                
                // Check that there are updates to the test run
                String jsonNow = api.getTestRunSummary(testName, testRunName);
                if (jsonLastChange != null && jsonLastChange.equals(jsonNow))
                {
                    if ((now-timeLastChange) > 60000L)
                    {
                        throw new RuntimeException("Test run has not been updated in the last 60s");
                    }
                }
                // Store values for next iteration
                timeLastChange = now;
                jsonLastChange = jsonNow;

                synchronized (testRunStateStr)
                {
                    try { testRunStateStr.wait(1000L); } catch (InterruptedException e) {}
                }
            }
            // Call listeners: the test run has finished
            for (BMTestRunnerListener listener : listeners)
            {
                listener.testRunFinished(ctx, testName, testRunName);
            }
        }
        finally
        {
            // Close the context
            if (ctx != null)
            {
                try
                {
                    ctx.close();
                }
                catch (Exception e)
                {
                    logger.error("Failed to shut down application context.", e);
                }
            }
            // Close the local Mongo instance
            if (mongoDBForTestsFactory != null)
            {
                try
                {
                    mongoDBForTestsFactory.destroy();
                }
                catch (Exception e)
                {
                    logger.error("Failed to stop in-memory MongoDB instance.", e);
                }
            }
        }
    }
    
    /**
     * Helper method to extract the csv results to a string that can be output in the logs
     */
    public static String getResultsCSV(ResultsRestAPI resultsAPI)
    {
        // Get the summary CSV results for the time period and check some of the values
        StreamingOutput out = resultsAPI.getReportCSV();
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
        return summary;
    }
}
