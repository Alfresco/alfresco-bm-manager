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
package org.alfresco.bm.log;

import org.alfresco.bm.log.LogService.LogLevel;

/**
 * Concrete implementation of a log service that always logs data specific to a given
 * drive-test-testrun combination.  Typically, this instance will not be shared or
 * used generically and should be discarded along with any test or test run context.
 * <p/>
 * This API only provides the ability to write data to the log.
 * 
 * @author Derek Hulley
 * @since 2.0.3
 */
public class TestRunLogService
{
    private final LogService logService;
    private final String driverId;
    private final String test;
    private final String testRun;
    
    /**
     * @param logService            the service that actually does the work
     * @param driverId              the ID of the driver that message will be logged against (may be <tt>null</tt>)
     * @param test                  the name of the test that messages will be logged against (may be <tt>null</tt>)
     * @param testRun               the name of the test run that messages will be logged against (may be <tt>null</tt>)
     */
    public TestRunLogService(LogService logService, String driverId, String test, String testRun)
    {
        this.logService = logService;
        this.driverId = driverId;
        this.test = test;
        this.testRun = testRun;
    }
    
    /**
     * Log a message specific to this instance
     * 
     * @param level                 the severity of the message
     * @param msg                   the log message
     */
    public void log(LogLevel level, String msg)
    {
        logService.log(driverId, test, testRun, level, msg);
    }
}
