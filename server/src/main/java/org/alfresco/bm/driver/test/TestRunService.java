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
package org.alfresco.bm.driver.test;

import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.common.util.ArgumentCheck;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test run services for logging and result report
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class TestRunService
{
    /** logger */
    protected Log logger = LogFactory.getLog(TestRunService.class);

    /** Stores the log service */
    private final LogService logService;

    /** stores the test DAO */
    private final MongoTestDAO testDAO;

    /** Stores the driver ID */
    private String driverId;

    /** Stores test name */
    private String testName;

    /** Stores the test run name */
    private String testRunName;

    /**
     * Constructor
     * 
     * @param logService
     *        (LogService, mandatory) the global log service to use
     * @param testDAO
     *        (MongoTestDAO, mandatory) the MongoTestDAO
     * @param driverId
     *        (String, mandatory) driver ID (${driverId})
     * @param testName
     *        (String, mandatory) test name (${test})
     * @param testRunName
     *        (String, mandatory) test run name (${testRun})
     * 
     * @throws BenchmarkResultException
     */
    public TestRunService(
            LogService logService,
            MongoTestDAO testDAO,
            String driverId,
            String testName,
            String testRunName) 
    {
        ArgumentCheck.checkMandatoryObject(logService, "logService");
        ArgumentCheck.checkMandatoryObject(testDAO, "testDAO");
        ArgumentCheck.checkMandatoryString(driverId, "driverId");
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");

        this.logService = logService;
        this.testDAO = testDAO;
        this.driverId = driverId;
        this.testName = testName;
        this.testRunName = testRunName;

        if (logger.isDebugEnabled())
        {
            logger.debug("[TestRunService] initialized with driver ID: '" + driverId + "', test name: '" + testName
                    + ", test run name: '" + testRunName + "'.");
        }
    }

    /**
     * Log a message specific to this instance
     * 
     * @param level
     *        the severity of the message
     * @param msg
     *        the log message
     */
    public void log(LogLevel level, String msg)
    {
        if (null != msg && !msg.isEmpty())
        {
            logService.log(driverId, testName, testRunName, level, msg);
        }
    }

    /**
     * Logs a TRACE message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logTrace(String msg)
    {
        log(LogLevel.TRACE, msg);
    }

    /**
     * Logs a DEBUG message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logDebug(String msg)
    {
        log(LogLevel.DEBUG, msg);
    }

    /**
     * Logs an INFO message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logInfo(String msg)
    {
        log(LogLevel.INFO, msg);
    }

    /**
     * Logs a WARN message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logWarn(String msg)
    {
        log(LogLevel.WARN, msg);
    }

    /**
     * Logs an ERROR message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logError(String msg)
    {
        log(LogLevel.ERROR, msg);
    }

    /**
     * Logs a FATAL message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logFatal(String msg)
    {
        log(LogLevel.FATAL, msg);
    }

    /**
     * @return (String) test name
     */
    public String getTestName()
    {
        return this.testName;
    }

    /**
     * @return (String) test run name
     */
    public String getTestRunName()
    {
        return this.testRunName;
    }

    /**
     * @return (String) driver ID
     */
    public String getDriverId()
    {
        return this.driverId;
    }

    /**
     * @return (MongoTestDAO) test DAO
     */
    public MongoTestDAO getTestDAO()
    {
        return testDAO;
    }
}
