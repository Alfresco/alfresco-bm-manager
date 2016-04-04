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
package org.alfresco.bm.test;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.report.DataReportService;
import org.alfresco.bm.result.ResultDataService;
import org.alfresco.bm.session.SessionService;
import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.alfresco.bm.util.ArgumentCheck;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.MongoSocketException;

/**
 * Helper class for instantiating and holding service instances for specific test runs.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestRunServicesCache implements LifecycleListener, TestConstants
{
    /** The time to hold a context open since last access */
    private static final long CONTEXT_ACCESS_TIMEOUT = 120000L;

    private static final Log logger = LogFactory.getLog(TestRunServicesCache.class);

    private final MongoTestDAO dao;
    private final TestService testService;
    private final Map<String, ClassPathXmlApplicationContext> contexts;
    private final Map<String, Long> contextAccessTimes;
    private final ContextCleanerTask contextCleanerTask;
    private final ReentrantReadWriteLock lock;

    /**
     * @param dao
     *            provides access to the state of a specific test run
     */
    public TestRunServicesCache(MongoTestDAO dao)
    {
        this.dao = dao;
        this.testService = new TestServiceImpl(dao);

        this.contexts = new HashMap<String, ClassPathXmlApplicationContext>(13);
        this.contextAccessTimes = Collections.synchronizedMap(new HashMap<String, Long>(13));
        this.contextCleanerTask = new ContextCleanerTask();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void start() throws Exception
    {
        Timer timer = new Timer("TestServicesCache", true);
        timer.schedule(contextCleanerTask, 0L, CONTEXT_ACCESS_TIMEOUT);
    }

    @Override
    public void stop() throws Exception
    {
        // Stop the timer
        contextCleanerTask.cancel();
        // Shut down all current service instances
        contextCleanerTask.run();
        // Remove contexts to prevent accidental processing by cleaner
        contexts.clear();
    }

    /**
     * Create an application context holding the services for the given test run
     */
    private ClassPathXmlApplicationContext createContext(String test, String run)
    {
        String testRunFqn = test + "." + run;
        DBObject runObj = dao.getTestRun(test, run, true);
        if (runObj == null)
        {
            return null;
        }
        // Dig the properties out of the test run
        Properties testRunProps = new Properties();
        {
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
            }
        }
        // Construct the properties
        ClassPathXmlApplicationContext testRunCtx = new ClassPathXmlApplicationContext(
                new String[] { PATH_TEST_SERVICES_CONTEXT }, false);
        ConfigurableEnvironment ctxEnv = testRunCtx.getEnvironment();
        ctxEnv.getPropertySources().addFirst(new PropertiesPropertySource("run-props", testRunProps));
        // Bind to shutdown
        testRunCtx.registerShutdownHook();

        // Attempt to start the context
        try
        {
            testRunCtx.refresh();
            testRunCtx.start();
            // Make sure that the required components are present
            testRunCtx.getBean(EventService.class);
            testRunCtx.getBean(ResultService.class);
            testRunCtx.getBean(SessionService.class);
        }
        catch (Exception e)
        {
            Throwable root = ExceptionUtils.getRootCause(e);
            if (root != null && root instanceof MongoSocketException)
            {
                // We deal with this specifically as it's a simple case of not finding the MongoDB
                logger.error("Failed to start test run services context '" + testRunFqn + "': "
                        + e.getCause().getMessage());
                logger.error("Set the test run property '" + PROP_MONGO_TEST_HOST + "' (<server>:<port>) as required.");
            }
            else if (root != null && root instanceof UnknownHostException)
            {
                // We deal with this specifically as it's a simple case of not finding the MongoDB
                logger.error("Failed to start test run services context '" + testRunFqn + "': "
                        + e.getCause().getCause().getMessage());
                logger.error("Set the test run property '" + PROP_MONGO_TEST_HOST + "' (<server>:<port>) as required.");
            }
            else
            {
                logger.error("Failed to start test run services context '" + testRunFqn + "': ", e);
            }
            testRunCtx = null;
        }
        // Done
        if (testRunCtx == null)
        {
            logger.warn("Failed to start test run services context: " + testRunFqn);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Started test run services context: " + testRunFqn);
        }
        return testRunCtx;
    }

    /**
     * Get the {@link ResultService} for the given test run
     * 
     * @param test
     *            the name of the test
     * @param run
     *            the name of the run
     * @return the service to access the results or <tt>null</tt> if not available
     */
    private ClassPathXmlApplicationContext getContext(String test, String run)
    {
        String testRunFqn = test + "." + run;
        Long now = System.currentTimeMillis();

        lock.readLock().lock();
        try
        {
            ClassPathXmlApplicationContext ctx = contexts.get(testRunFqn);
            if (ctx != null)
            {
                // Record when we last required the context
                contextAccessTimes.put(testRunFqn, now);
                return ctx;
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
        // We didn't have a context
        lock.writeLock().lock();
        try
        {
            ClassPathXmlApplicationContext ctx = createContext(test, run);
            if (ctx == null)
            {
                // An error will already have been logged
                return null;
            }
            // Store it for later
            contexts.put(testRunFqn, ctx);
            // Record when we last required the context
            contextAccessTimes.put(testRunFqn, now);
            // Done
            return ctx;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get the {@link MongoTestDAO} for the given test run
     * 
     * @return the DAO for accessing the low-level test config data
     */
    public MongoTestDAO getTestDAO()
    {
        return dao;
    }

    /**
     * Get the {@link TestService} for the given test run
     * 
     * @return the service for accessing test data
     */
    public TestService getTestService()
    {
        return testService;
    }

    /**
     * Get the {@link ResultService} for the given test run
     * 
     * @return the service or <tt>null</tt> if it could not be created or accessed
     */
    public ResultService getResultService(String test, String run)
    {
        ApplicationContext ctx = getContext(test, run);
        if (ctx == null)
        {
            return null;
        }
        return ctx.getBean(ResultService.class);
    }

    /**
     * Get the {@link EventService} for the given test run
     * 
     * @return the service or <tt>null</tt> if it could not be created or accessed
     */
    public EventService getEventService(String test, String run)
    {
        ApplicationContext ctx = getContext(test, run);
        if (ctx == null)
        {
            return null;
        }
        return ctx.getBean(EventService.class);
    }

    /**
     * Get the {@link SessionService} for the given test run
     * 
     * @return the service or <tt>null</tt> if it could not be created or accessed
     */
    public SessionService getSessionService(String test, String run)
    {
        ApplicationContext ctx = getContext(test, run);
        if (ctx == null)
        {
            return null;
        }
        return ctx.getBean(SessionService.class);
    }

    /*
     * Returns the result data service or null if no service is configured.
     * 
     * @param test
     * (String) test name
     * 
     * @param run
     * (String) test run name
     * 
     * @return (ResultDataService or null)
     */
    public ResultDataService getResultDataService(String test, String run)
    {
        ArgumentCheck.checkMandatoryString(test, "test");
        ArgumentCheck.checkMandatoryString(run, "run");

        try
        {
            ApplicationContext ctx = getContext(test, run);
            if (null != ctx)
            {
                return ctx.getBean(ResultDataService.class);
            }
        }
        catch (Exception e)
        {
            logger.debug("No result data service available!", e);
        }
        return null;
    }

    /**
     * Returns the data report service or null if no data report service is configured.
     * 
     * @param test
     *            (String) test name
     * @param run
     *            (String) test run name
     * @return (DataReportService or null)
     */
    public DataReportService getDataReportService(String test, String run)
    {
        ArgumentCheck.checkMandatoryString(test, "test");
        ArgumentCheck.checkMandatoryString(run, "run");

        try
        {
            ApplicationContext ctx = getContext(test, run);
            if (null != ctx)
            {
                return ctx.getBean(DataReportService.class);
            }
        }
        catch (Exception e)
        {
            logger.debug("No data report service available!", e);
        }
        return null;
    }

    /**
     * Checks the access times for all contexts and shuts down and removes any that have not been accessed since it last
     * checked.
     * 
     * @see TestServicesCache#CONTEXT_ACCESS_TIMEOUT
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    private class ContextCleanerTask extends TimerTask
    {
        @Override
        public void run()
        {
            long now = System.currentTimeMillis();
            // Copy the keys to avoid concurrent modification while iterating
            Set<String> testRunFqns = new TreeSet<String>(contexts.keySet());
            for (String testRunFqn : testRunFqns)
            {
                Long lastAccess = contextAccessTimes.get(testRunFqn);
                boolean expired = lastAccess == null || lastAccess < (now - CONTEXT_ACCESS_TIMEOUT);
                if (!expired)
                {
                    continue;
                }
                // The context has expired
                if (logger.isDebugEnabled())
                {
                    logger.debug("Cleaning up unused test services context: " + testRunFqn);
                }
                lock.writeLock().lock();
                try
                {
                    ClassPathXmlApplicationContext ctx = contexts.get(testRunFqn);
                    if (ctx == null)
                    {
                        logger.error("Expected to remove unused test services context but didn't find the context: "
                                + testRunFqn);
                        continue;
                    }
                    // First remove it
                    contexts.remove(testRunFqn);
                    contextAccessTimes.remove(testRunFqn);
                    // Then shut it down
                    ctx.stop();
                    ctx.close();
                }
                catch (Exception e)
                {
                    // Can't fail so just report
                    logger.error("Failed to clean up unused test services context: " + testRunFqn, e);
                }
                finally
                {
                    lock.writeLock().unlock();
                }
            }
        }
    }
}
