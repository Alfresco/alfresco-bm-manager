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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.alfresco.bm.test.prop.TestProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Implementation of service managing and querying the server instances
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class Test implements
        LifecycleListener, TestConstants,
        ApplicationContextAware
{
    /** default test run monitor time */
    private static final long DEFAULT_TEST_RUN_MONITOR_PERIOD = 5000L;
    
    private static Log logger = LogFactory.getLog(Test.class);
    
    private ApplicationContext ctx;
    
    private final MongoTestDAO testDAO;

    private final String release;
    private final Integer schema;
    private final String description;
    private final String contextPath;
    private final TestDefaults defaults;
    private InetAddress inetAddress;
    private Set<String> systemCapabilities;
    
    /** The task that keeps the server details fresh in the database */
    private final TestDriverPingTask refreshRegistrationTask;
    private volatile String driverId;
    
    /** The task that creates monitors for test runs */
    private final TestRunPingTask testRunPingTask;
    /** The time between test run monitor runs */
    private long testRunMonitorPeriod = DEFAULT_TEST_RUN_MONITOR_PERIOD;
    
    /**
     * @param testDAO               data persistence
     * @param release               the software release name of this test
     * @param schema                the property schema version
     * @param description           the test description
     * @param contextPath           the context under which the application was launched
     * @param defaults              provider of all the test defaults
     */
    public Test(MongoTestDAO testDAO, String release, Integer schema, String description, String contextPath, TestDefaults defaults)
    {
        this.testDAO = testDAO;
        this.release = release;
        this.schema = schema;
        this.description = description;
        this.contextPath = contextPath;
        this.defaults = defaults;
        this.systemCapabilities = Collections.singleton(CAPABILITY_JAVA6);
        this.refreshRegistrationTask = new TestDriverPingTask();
        this.testRunPingTask = new TestRunPingTask();
        
        // This will only be valid if driver registration succeeds
        this.driverId = null;
    }

    /**
     * Override the {@link #DEFAULT_TEST_RUN_MONITOR_PERIOD default} test run monitor period.
     */
    public void setTestRunMonitorPeriod(long testRunMonitorPeriod)
    {
        this.testRunMonitorPeriod = testRunMonitorPeriod;
    }

    /**
     * Keep track of the parent application context, which will act as a parent context for all test runs.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.ctx = applicationContext;
    }

    /**
     * Set a comma-separated string of capabilities.  These are tags and can be or represent anything.
     */
    public void setSystemCapabilities(String capabilitiesStr)
    {
        if (capabilitiesStr == null)
        {
            throw new IllegalArgumentException("Capabilities string cannot be null.");
        }
        Set<String> capabilitiesTmp = new HashSet<String>(5);
        StringTokenizer st = new StringTokenizer(capabilitiesStr, ",");
        while (st.hasMoreTokens())
        {
            String capability = st.nextToken().trim().toLowerCase();
            capabilitiesTmp.add(capability);
        }
        // Prevent later modification
        this.systemCapabilities = Collections.unmodifiableSet(capabilitiesTmp);
    }
    
    /**
     * @return              the build-supplied release name
     */
    public String getRelease()
    {
        return release;
    }

    /**
     * @return              the build-supplied schema number
     */
    public Integer getSchema()
    {
        return schema;
    }

    /**
     * Timer callback that ensures the test driver details are updated and kept alive
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    private class TestDriverPingTask extends TimerTask
    {
        /** 1 minute registration timeout */
        private static final long PING_TIMEOUT = 1L*60L*1000L;
        
        private boolean active = true;                      // Start off active
        
        public synchronized void setActive(boolean active)
        {
            this.active = active;
        }
        
        @Override
        public synchronized void run()
        {
            if (active && driverId != null)
            {
                // Add the ping
                long expiryTime = System.currentTimeMillis() + PING_TIMEOUT;
                testDAO.refreshDriver(driverId, expiryTime);
            }
            else
            {
                // Cancel internally
                super.cancel();
            }
        }
    }
    
    /**
     * Timer callback that generates test run monitors
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    private class TestRunPingTask extends TimerTask
    {
        private boolean active = true;                      // Start off active
        private final Map<ObjectId, TestRun> testRuns;
        
        /**
         * Internal constructor for containing class only
         */
        private TestRunPingTask()
        {
            testRuns = new HashMap<ObjectId, TestRun>(23);
        }
        
        /**
         * Deactivate the ping task, which stops all associated test runs
         */
        public synchronized void deactivate()
        {
            this.active = false;
            // Call all test runs and remove them
            for (TestRun testRun : testRuns.values())
            {
                testRun.stop();
            }
        }
        
        /**
         * Get the object managing the specific test run
         * 
         * @param testRunId             the ID of the test run
         * @return                      the test run wrapper or <tt>null</tt> if not found
         */
        private synchronized TestRun getTestRun(ObjectId testRunId)
        {
            return testRuns.get(testRunId);
        }
        
        @Override
        public synchronized void run()
        {
            if (!active)
            {
                // Cancel internally
                super.cancel();
                return;
            }
            
            // Keep track of all test runs so that we can remove redundant instances
            Set<ObjectId> redundantTestRunIds = new HashSet<ObjectId>(testRuns.keySet());
            
            int testsSkip = 0;
            DBCursor testsCursor = testDAO.getTests(release, schema, testsSkip, 100);
            testsSkip += testsCursor.count();
            while (testsCursor.hasNext())
            {
                DBObject testObj = testsCursor.next();
                String test = (String) testObj.get(FIELD_NAME);
                // For each test (matching the app we're in) get the runs
                int testRunsSkip = 0;
                DBCursor testRunsCursor = testDAO.getTestRuns(test, testRunsSkip, 10, TestRunState.SCHEDULED, TestRunState.STARTED);
                testRunsSkip += testRunsCursor.count();
                while (testRunsCursor.hasNext())
                {
                    DBObject testRunObj = testRunsCursor.next();
                    ObjectId testRunId = (ObjectId) testRunObj.get(FIELD_ID);
                    if (testRuns.containsKey(testRunId))
                    {
                        // Remove it from the redundant list
                        redundantTestRunIds.remove(testRunId);
                        // We have already created a test run for this
                        continue;
                    }
                    // Build a test run
                    TestRun testRun = new TestRun(testDAO, testRunId, ctx, driverId);
                    testRuns.put(testRunId, testRun);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Created TestRun monitor: " + testRunObj);
                    }
                }
                // Close the cursor.  Note that the use of a finally is not necessary as Mongo has protection against leaked cursors.
                testRunsCursor.close();
            }
            
            // Remove redundant monitors
            for (ObjectId testRunId : redundantTestRunIds)
            {
                // We make one last call to the test run we are dropping so that it can do a final cleanup
                TestRun testRun = testRuns.get(testRunId);
                testRun.checkState();
                // Now clean it out
                testRuns.remove(testRunId);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Removed redundant TestRun monitor: " + testRunId);
                }
            }
            
            // Call each monitor to get it to check itself
            for (TestRun testRun : testRuns.values())
            {
                testRun.checkState();
            }
        }
    }
    
    /**
     * Get the object managing the specific test run
     * 
     * @param testRunId             the ID of the test run
     * @return                      the test run wrapper or <tt>null</tt> if not found
     */
    public TestRun getTestRun(ObjectId testRunId)
    {
        if (testRunPingTask == null)
        {
            return null;            // Probably not started
        }
        else
        {
            return testRunPingTask.getTestRun(testRunId);
        }
    }
    
    @Override
    public void start() throws Exception
    {
        initNetworkDetails();
        
        // The core BM Server application does NOT driver anything
        boolean isDriver = !release.toLowerCase().startsWith("alfresco-benchmark-server-");
        if (isDriver)
        {
            // Ensure that there is a representation of the test in the DB
            initTestDefaults();
            
            // Register this driver
            registerDriver();
            
            // Store server details
            refreshRegistrationTask.run();
            // Make sure we keep it refreshed
            Timer timer = new Timer("TestDriverPing-" + release + "-" + schema, true);
            timer.schedule(refreshRegistrationTask, 0L, TestDriverPingTask.PING_TIMEOUT/2);
            
            // Create monitors for all test runs of interest
            testRunPingTask.run();
            // Keep it refreshed
            timer = new Timer("TestRunPing-" + release + "-" + schema, true);
            timer.schedule(testRunPingTask, testRunMonitorPeriod, testRunMonitorPeriod);
        }
        else
        {
            logger.debug("Not registering driver details: " + release + ", " + schema + ", " + systemCapabilities);
        }
    }
    
    /**
     * <b>TEST ONLY:</b> Force the driver ping task to execute on the current thread.
     */
    public void forcePing()
    {
        testRunPingTask.run();
    }
    
    @Override
    public void stop() throws Exception
    {
        // Kill the ping
        refreshRegistrationTask.setActive(false);
        if (driverId != null)
        {
            // Remove driver entry
            testDAO.unregisterDriver(driverId);
        }
        // Kill monitoring
        testRunPingTask.deactivate();
    }

    /**
     * Initialize IP address and hostname
     */
    private void initNetworkDetails() throws Exception
    {
        // We have some preferences about what to use
        InetAddress ipv4 = null;
        InetAddress multicast = null;
        InetAddress ipv6 = null;
        
        // Get an IP address and host name
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        while (nis.hasMoreElements())
        {
            NetworkInterface ni = nis.nextElement();
            // Check each network interface
            Enumeration<InetAddress> ias = ni.getInetAddresses();
            while (ias.hasMoreElements())
            {
                InetAddress ia = ias.nextElement();
                if (ipv4 == null && ia instanceof Inet4Address)
                {
                    // Our first choice
                    ipv4 = ia;
                }
                else if (multicast != null && ia.isMulticastAddress())
                {
                    multicast = ia;
                }
                else if (ipv6 != null && ia instanceof Inet6Address)
                {
                    ipv6 = ia;
                }
            }
        }
        // Now go by preference
        if (ipv4 != null)
        {
            inetAddress = ipv4;
        }
        else if (multicast != null)
        {
            inetAddress = multicast;
        }
        else if (ipv6 != null)
        {
            inetAddress = ipv6;
        }
        else if (inetAddress == null)
        {
            inetAddress = InetAddress.getLocalHost();
        }
    }
    
    /**
     * Write test defaults into the persitent storage, taking care that existing values
     * are not modified.
     */
    private void initTestDefaults() throws Exception
    {
        // Check the test defaults
        if (defaults == null)
        {
            throw new RuntimeException("No test defaults provided.");
        }
        if (release == null || release.length() == 0)
        {
            throw new RuntimeException("Test defaults did not provide a release string.");
        }
        if (schema == null || schema.intValue() < 0)
        {
            throw new RuntimeException("Test defaults did not provide a schema number.");
        }
        
        List<TestProperty> properties = defaults.getPropertiesList();
        boolean written = testDAO.writeTestDef(release, schema, description, properties);
        if (!written)
        {
            logger.info("Test definition already written: " + release + ":" + schema);
        }
    }
    
    /**
     * Ensure that this instance registers as a driver
     */
    private void registerDriver() throws Exception
    {
        String hostname = inetAddress.getHostName();
        String ipAddress = inetAddress.getHostAddress();
        
        driverId = testDAO.registerDriver(
                release,
                schema,
                ipAddress,
                hostname,
                contextPath,
                systemCapabilities);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Registered test driver details: " + release + ", " + ipAddress + ", " + systemCapabilities);
        }
    }
}
