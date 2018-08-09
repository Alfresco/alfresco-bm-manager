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
package org.alfresco.bm.common.util.log;

import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Concrete implementation of a log service that always logs data specific to a given drive-test-testrun combination.
 * Typically, this instance will not be shared or used generically and should be discarded along with any test or test
 * run context.
 * <p/>
 * This API only provides the ability to write data to the log.
 * 
 * @author Derek Hulley
 * @since 2.0.3
 * @deprecated Use {@link org.alfresco.bm.driver.test.TestRunService} instead
 */
@Deprecated
public class TestRunLogService
{ 
    /** logger */
    protected Log logger = LogFactory.getLog(TestRunLogService.class);
    
    /** Stores the log service to use */
    private final LogService logService;
    
    /** Driver ID that triggers the log message */
    private final String driverId;
    
    /** Test instance name */
    private final String test;

    /** Test run to log message for */
    private final String testRun;

    /**
     * @param logService
     *            the service that actually does the work
     * @param driverId
     *            the ID of the driver that message will be logged against (may be <tt>null</tt>)
     * @param test
     *            the name of the test that messages will be logged against (may be <tt>null</tt>)
     * @param testRun
     *            the name of the test run that messages will be logged against (may be <tt>null</tt>)
     */
    public TestRunLogService(LogService logService, String driverId, String test, String testRun)
    {
        this.logService = logService;
        this.driverId = driverId;
        this.test = test;
        this.testRun = testRun;
        
        if (logger.isDebugEnabled())
        {
            logger.debug("DriverId: '" + driverId + "', Test Name: '" + test + ", Test Run Name: '" + testRun + "'.");
        }
    }

    /**
     * Log a message specific to this instance
     * 
     * @param level
     *            the severity of the message
     * @param msg
     *            the log message
     */
    public void log(LogLevel level, String msg)
    {
        if (null != msg && !msg.isEmpty())
        {
            logService.log(driverId, test, testRun, level, msg);
        }
    }

    /**
     * Logs a TRACE message.
     * 
     * @param msg
     *            (String) message to log.
     * 
     * @since 2.0.10
     */
    public void logTrace(String msg)
    {
        log(LogLevel.TRACE, msg);
    }

    /**
     * Logs a DEBUG message.
     * 
     * @param msg
     *            (String) message to log.
     * 
     * @since 2.0.10
     */
    public void logDebug(String msg)
    {
        log(LogLevel.DEBUG, msg);
    }

    /**
     * Logs an INFO message.
     * 
     * @param msg
     *            (String) message to log.
     * 
     * @since 2.0.10
     */
    public void logInfo(String msg)
    {
        log(LogLevel.INFO, msg);
    }

    /**
     * Logs a WARN message.
     * 
     * @param msg
     *            (String) message to log.
     * 
     * @since 2.0.10
     */
    public void logWarn(String msg)
    {
        log(LogLevel.WARN, msg);
    }

    /**
     * Logs an ERROR message.
     * 
     * @param msg
     *            (String) message to log.
     * 
     * @since 2.0.10
     */
    public void logError(String msg)
    {
        log(LogLevel.ERROR, msg);
    }

    /**
     * Logs a FATAL message.
     * 
     * @param msg
     *            (String) message to log.
     * 
     * @since 2.0.10
     */
    public void logFatal(String msg)
    {
        log(LogLevel.FATAL, msg);
    }
    
    /**
     * @return (String) test name
     * 
     * @since 2.0.10
     */
    public String getTestName()
    {
        return this.test;
    }
    
    /**
     * @return (String) test run name
     * 
     * @since 2.0.10
     */
    public String getTestRunName()
    {
        return this.testRun;
    }
    
    /**
     * @return (String) driver ID
     * 
     * @since 2.0.10
     */
    public String getDriverId()
    {
        return this.driverId;
    }
}
