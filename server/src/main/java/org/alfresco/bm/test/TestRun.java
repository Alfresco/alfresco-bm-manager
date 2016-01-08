/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.alfresco.bm.event.EventProcessor;
import org.alfresco.bm.log.LogService;
import org.alfresco.bm.log.LogService.LogLevel;
import org.alfresco.bm.server.EventController;
import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * Manages the actual execution of a test run.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestRun implements TestConstants
{
    private static Log logger = LogFactory.getLog(TestRun.class);
    
    private final MongoTestDAO testDAO;
    private final LogService logService;
    private final ObjectId id;
    private final ApplicationContext parentCtx;
    private final String driverId;
    private AbstractXmlApplicationContext testRunCtx;       // This will be created when the test run actually starts
    private String test;                                    // Only populated once the test run starts
    private String run;                                     // Only populated once the test run starts
    private String release;                                 // Only populated once the test run starts
    private Integer schema;                                 // Only populated once the test run starts

    /**
     * @param testDAO               data persistence
     * @param logService            logging
     * @param id                    the id of the test that this run controls
     * @param parentCtx             the parent context for all test runs
     * @param driverId              the ID of the driver controlling the test run
     */
    public TestRun(MongoTestDAO testDAO, LogService logService, ObjectId id, ApplicationContext parentCtx, String driverId)
    {
        this.testDAO = testDAO;
        this.logService = logService;
        this.id = id;
        this.parentCtx = parentCtx;
        this.driverId = driverId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TestRun other = (TestRun) obj;
        if (id == null)
        {
            if (other.id != null) return false;
        }
        else if (!id.equals(other.id)) return false;
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("TestRun [id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

    /**
     * @return              the ID of the test run being monitored
     */
    public ObjectId getId()
    {
        return id;
    }
    
    /**
     * @return              the raw DBObject or <tt>null</tt> if the test run is no longer valid
     */
    private DBObject getRunObj(boolean includeProperties)
    {
        DBObject runObj = testDAO.getTestRun(id, includeProperties);
        if (runObj == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("The test run '" + id + "' no longer exists.");
            }
            stop();
            return null;
        }
        else
        {
            return runObj;
        }
    }
    
    /**
     * Get the application context associated with the test run.
     * 
     * @return              the application context or <tt>null</tt> if the application context
     *                      has not been initialized or has already been shut down.
     */
    public synchronized ApplicationContext getCtx()
    {
        return testRunCtx;
    }
    
    /**
     * Called to do a check of the test run state and make any adjustments as necessary.
     * <p/>
     * The initial state is checked and we only accept states that have any chance of
     * progression or execution.
     */
    public synchronized void checkState()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Checking test run state: " + id);
        }
        DBObject runObj = getRunObj(false);
        if (runObj == null)
        {
            return;
        }
        Integer version = (Integer) runObj.get(FIELD_VERSION);
        // Current state
        String stateStr = (String) runObj.get(FIELD_STATE);
        TestRunState state = TestRunState.valueOf(stateStr);

        long now = System.currentTimeMillis();
        // Calculate duration
        Long started = (Long) runObj.get(FIELD_STARTED);
        Long duration = (started == null || started < 0L) ? null : (now - started);
        
        // We update the test run according to its current state and possibly progress it to a new state
        switch (state)
        {
            case NOT_SCHEDULED:
                // Nothing to do
                return;
            case SCHEDULED:
                // Check if the time execution time has been reached
                Long scheduled = (Long) runObj.get(FIELD_SCHEDULED);
                if (scheduled == null)
                {
                    logger.error("Scheduled state has been reached without a scheduled time: " + runObj);
                    testDAO.updateTestRunState(
                            id, version,
                            TestRunState.STOPPED,
                            -1L, -1L, now, -1L, null, null,
                            null, null);
                }
                else if (scheduled < now)
                {
                    // Start the context
                    start();
                    if (testRunCtx == null)
                    {
                        // It failed to start.  Errors will have been reported.
                        // now stop the test (otherwise it will restart again and again ...) - fkb 2015-11-10 
                    	now = System.currentTimeMillis();
                        boolean stopped = testDAO.updateTestRunState(
                                id, version,
                                TestRunState.STOPPED, now, now, now, null, 1L, 0.0D,
                                0L, 0L);
                        if (logger.isDebugEnabled())
                        {
                            if (stopped)
                            {
                                logger.debug("Successfully switched test run to " + TestRunState.STOPPED + ": " + id);
                            }
                            else 
                            {
                                logger.debug("Failed to transition test run to " + TestRunState.STOPPED + ": " + id);
                            }
                        }
                        return;
                    }
                    
                    // It has been scheduled and the time has passed
                    boolean changed = testDAO.updateTestRunState(
                            id, version,
                            TestRunState.STARTED, null, now, null, null, null, 0.0D,
                            0L, 0L);
                    if (!changed)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Failed to transition test run to " + TestRunState.STARTED + ": " + id);
                        }
                        // We bug out until the next check cycle
                        return;
                    }
                    // Lock in all properties
                    runObj = getRunObj(false);
                    if (runObj == null)
                    {
                        return;
                    }
                    ObjectId testObjId = (ObjectId) runObj.get(FIELD_TEST);

                    testDAO.lockProperties(testObjId, id);

                    // Give the EventController the updated list of drivers
                    updateDriverIds();

                    if (logger.isInfoEnabled())
                    {
                        logger.info("Transitioned test run to " + TestRunState.STARTED + ": " + id);
                    }
                }
                return;
            case STARTED:
                start();
                // Monitor progress
                if (testRunCtx != null)
                {
                    // Update the progress
                    CompletionEstimator ce = (CompletionEstimator) testRunCtx.getBean("completionEstimator");
                    double progress = ce.getCompletion();
                    boolean completed = ce.isCompleted();
                    long resultsSuccess = ce.getResultsSuccess();
                    long resultsFail = ce.getResultsFail();
                    if (completed)
                    {
                        testDAO.updateTestRunState(
                                id, version,
                                TestRunState.COMPLETED, null, null, null, now, duration, progress,
                                resultsSuccess, resultsFail);
                        // We do not need another ping.  We can just shut down the context now.
                        stop();
                    }
                    else
                    {
                        testDAO.updateTestRunState(
                                id, version,
                                null, null, null, null, null, duration, progress,
                                resultsSuccess, resultsFail);
                        // Give the EventController the updated list of drivers
                        updateDriverIds();
                    }
                    // There is nothing to do, the context is already available
                    return;
                }
                return;
            case STOPPED:
                stop();
                return;
            case COMPLETED:
                stop();
                return;
        }
    }
    
    /**
     * Helper method to inject the current list of driver IDs into the EventController.
     * The test run must already have started for this to work.
     */
    private synchronized void updateDriverIds()
    {
        // Give the EventController the updated list of drivers
        DBCursor cursor = testDAO.getDrivers(release, schema, true);
        String[] driverIds = new String[cursor.size()];
        int index = 0;
        while (cursor.hasNext())
        {
            ObjectId driverIdObj = (ObjectId) cursor.next().get(FIELD_ID);
            driverIds[index++] = driverIdObj.toString();
        }
        cursor.close();
        // Pass the driver IDs to the event controller
        EventController eventController = testRunCtx.getBean(EventController.class);
        eventController.setDriverIds(driverIds);
    }
    
    /**
     * Called to ensure that the application context is started.
     * <p/>
     * Note that we only pull out the test and test run names at this point so that we don't end up
     * using stale data.
     */
    private synchronized void start()
    {
        DBObject runObj = getRunObj(true);
        if (runObj == null)
        {
            // Nothing much we can do here
            return;
        }
        
        // Check the application context
        if (testRunCtx != null)
        {
            // There is nothing to do, the context is already available
            return;
        }
        
        // INFO logging as this is a critical part of the whole application
        if (logger.isInfoEnabled())
        {
            logger.info("Starting test run application context: " + runObj);
        }

        ObjectId testObjId = (ObjectId) runObj.get(FIELD_TEST);
        ObjectId runObjId = (ObjectId) runObj.get(FIELD_ID);
        String run = (String) runObj.get(FIELD_NAME);
        // We need to build the test run FQN out of the test run details
        DBObject testObj = testDAO.getTest(testObjId, false);
        if (testObj == null)
        {
            logger.warn("The test associated with the test run has been removed: " + runObj);
            logger.warn("The test run will be stopped and deleted: " + id);
            stop();
            testDAO.deleteTestRun(id);
            return;
        }
        String test = (String) testObj.get(FIELD_NAME);
        String release = (String) testObj.get(FIELD_RELEASE);
        Integer schema = (Integer) testObj.get(FIELD_SCHEMA);
        String testRunFqn = test + "." + run;
        
        // Extract the current properties for the run
        Set<String> propsToMask = new HashSet<String>(7);
        Properties testRunProps = new Properties();
        {
            testRunProps.put(PROP_DRIVER_ID, driverId);
            testRunProps.put(PROP_TEST, test);
            testRunProps.put(PROP_TEST_RUN, run);
            testRunProps.put(PROP_TEST_RUN_ID, id.toString());
            testRunProps.put(PROP_TEST_RUN_FQN, testRunFqn);

            BasicDBList propObjs = (BasicDBList) runObj.get(FIELD_PROPERTIES);
            for (Object obj : propObjs)
            {
                DBObject propObj = (DBObject) obj;
                String propName = (String) propObj.get(FIELD_NAME);
                String propDef = (String) propObj.get(FIELD_DEFAULT);
                String propValue = (String) propObj.get(FIELD_VALUE);
                if (propValue == null)
                {
                    propValue = propDef;
                }
                testRunProps.put(propName, propValue);
                // Check on the masking for later reporting
                boolean mask = Boolean.parseBoolean((String) propObj.get(FIELD_MASK));
                if (mask)
                {
                    propsToMask.add(propName);
                }
            }
        }
        
        // Create the child application context WITHOUT AUTOSTART
        // TODO: This is hard coded to "config/spring/test-context.xml".  It should be one of the
        //       test definition properties and have the same as default.
        ClassPathXmlApplicationContext testRunCtx = new ClassPathXmlApplicationContext(
                new String[] {PATH_TEST_CONTEXT},
                false);
        // When running stand-alone, there might not be a parent context
        if (parentCtx != null)
        {
            testRunCtx.setParent(parentCtx);
        }
        // Push cluster properties into the context (must be done AFTER setting parent context)
        ConfigurableEnvironment ctxEnv = testRunCtx.getEnvironment();
        ctxEnv.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        "run-props",
                        testRunProps));
        ctxEnv.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        "system-props",
                        System.getProperties()));
        
        // Complete logging of what is going to be used for the test
        if (logger.isInfoEnabled())
        {
            String nl = "\n";
            StringBuilder sb = new StringBuilder(1024);
            sb.append("Test run application context starting: ").append(nl)
              .append("   Run ID:       ").append(id).append(nl)
              .append("   Test Name:    ").append(test).append(nl)
              .append("   Run Name:     ").append(run).append(nl)
              .append("   Driver ID:    ").append(driverId).append(nl)
              .append("   Release:      ").append(release).append(nl)
              .append("   Schema:       ").append(schema).append(nl)
              .append("   Test Run Properties:   ").append(nl);
            for (Object propNameObj : testRunProps.keySet())
            {
                String propName = (String) propNameObj;
                String propValue = testRunProps.getProperty(propName);
                if (propsToMask.contains(propName) || propName.toLowerCase().contains("username") || propName.toLowerCase().contains("password"))
                {
                    propValue = MASK;
                }
                sb.append("      ").append(propName).append("=").append(propValue).append(nl);
            }
            sb.append("   System Properties:   ").append(nl);
            for (Object propNameObj : System.getProperties().keySet())
            {
                String propName = (String) propNameObj;
                String propValue = System.getProperty(propName);
                if (propsToMask.contains(propName) || propName.toLowerCase().contains("username") || propName.toLowerCase().contains("password"))
                {
                    propValue = MASK;
                }
                sb.append("      ").append(propName).append("=").append(propValue).append(nl);
            }
            logger.info(sb);
        }
        
        // Now refresh (to load beans) and start
        try
        {
            this.testRunCtx = testRunCtx;
            // 2015-08-04 fkbecker: store definitions first - for refresh() or start() may fail, too. 
            this.test = test;
            this.run = run;
            this.release = release;
            this.schema = schema;
            
            testRunCtx.refresh();
            testRunCtx.start();

            // Make sure that the required components are present and of the correct type
            // There may be multiple beans of the type, so we have to use the specific bean name.
            @SuppressWarnings("unused")
            CompletionEstimator estimator = (CompletionEstimator) testRunCtx.getBean("completionEstimator");
            @SuppressWarnings("unused")
            EventProcessor startEventProcessor = (EventProcessor) testRunCtx.getBean("event.start");
            
            // Register the driver with the test run
            testDAO.addTestRunDriver(runObjId, driverId);
            
            // Log the successful startup
            logService.log(driverId, test, run, LogLevel.INFO, "Successful startup of test run '" + testRunFqn + "'.");
        }
        catch (Exception e)
        {
            Throwable root = ExceptionUtils.getRootCause(e);
            if (root != null && (root instanceof MongoException || root instanceof IOException))
            {
                // 2015-08-04 fkbecker IOException also thrown by FTP file service if host not reachable ...
                // FIXME 
                
                String msg1 = "Failed to start test run application '" + testRunFqn + "': " + e.getCause().getMessage();
                String msg2 = "Set the test run property '" + PROP_MONGO_TEST_HOST + "' (<server>:<port>) as required.";
                // We deal with this specifically as it's a simple case of not finding the MongoDB
                logger.error(msg1);
                logger.error(msg2);
                logService.log(driverId, test, run, LogLevel.ERROR, msg1);
                logService.log(driverId, test, run, LogLevel.ERROR, msg2);
            }
            else
            {
                String stack = ExceptionUtils.getStackTrace(e);
                logger.error("Failed to start test run application '" + testRunFqn + "': ", e);
                String error = "Failed to start test run application '" + testRunFqn + ". \r\n" + stack;
                logService.log(driverId, test, run, LogLevel.ERROR, error);
            }
            stop();
        }
    }
    
    /**
     * Called to forcibly stop all executing test runs
     */
    public synchronized void stop()
    {
        // Check the application context
        if (testRunCtx == null)
        {
            // There is nothing to do, the context is not available
            return;
        }

        // INFO logging as this is a critical part of the whole application
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping test run application context: " + id);
        }

        try
        {
            DBObject runObj = getRunObj(false);
            if (runObj != null)
            {
                ObjectId runObjId = (ObjectId) runObj.get(FIELD_ID);
                // Unregister the driver from the test run
                testDAO.removeTestRunDriver(runObjId, driverId);
            }
            
            boolean active = testRunCtx.isActive();
            // Stop the context
            try
            {
                testRunCtx.stop();
            }
            catch (IllegalStateException e)
            {
                // Ignore if we didn't have a functioning context to start with
                if (active)
                {
                    logger.error("Unable to stop test run context: " + e.getMessage());
                }
            }
            // Close down completely
            try
            {
                testRunCtx.close();
            }
            catch (IllegalStateException e)
            {
                // Ignore if we didn't have a functioning context to start with
                if (active)
                {
                    logger.error("Unable to close test run context: " + e.getMessage());
                }
            }
        }
        catch (Exception e)
        {
            // There is little more that we can except report that the child ctx did not shut down
            logger.error("Test run application context did not shut down cleanly: \n" + id, e);
        }
        finally
        {
            testRunCtx = null;
        }
        
        // Log the successful shutdown
        logService.log(driverId, test, run, LogLevel.INFO, "Successful shutdown of test run '" + test + "." + run + "'.");
    }
}
