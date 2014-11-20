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

import com.mongodb.DBCursor;

/**
 * Interface for service providing detailed system-wide logging
 * 
 * @author Derek Hulley
 * @since 2.0.3
 */
public interface LogService
{
    /**
     * Log levels for the benchmark {@link LogService}
     * @author Derek Hulley
     * @since 2.0.3
     */
    public static enum LogLevel
    {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL;
        
        /**
         * Get the severity level
         * 
         * @return          Returns the {@link #ordinal() ordinal} unless there is a specific alternative
         */
        public int getLevel()
        {
            return ordinal();
        }
    }
    
    /**
     * Log a message
     * 
     * @param driverId              the driver ID from which the message originated (optional)
     * @param test                  the name of the test (optional)
     * @param testRun               the name of the test run (optional)
     * @param level                 the severity of the message
     * @param msg                   the log message
     */
    void log(String driverId, String test, String testRun, LogLevel level, String msg);
    
    /**
     * Retrieve log messages in a cursor
     * 
     * @param driverId              driver ID (optional)
     * @param test                  the name of the test (optional)
     * @param testRun               the name of the test run (optional)
     * @param level                 minimum severity (optional)
     * @param minTime               minimum log message time (inclusive)
     * @param maxTime               maximum log message time (exlusive)
     * @param skip                  number of results to skip
     * @param limit                 limit the total number of results
     * @return                      a results cursor that must be closed
     */
    DBCursor getLogs(
            String driverId, String test, String testRun,
            LogLevel level,
            Long minTime, Long maxTime,
            int skip, int limit);
}
