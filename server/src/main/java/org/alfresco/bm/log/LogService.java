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
package org.alfresco.bm.log;

import java.util.List;

/**
 * Interface for service providing detailed cluster-wide server logs
 * 
 * @author Derek Hulley
 * @since 1.4
 */
public interface LogService
{
    public static final int TRACE = 0;
    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARN = 3;
    public static final int ERROR = 4;
    public static final int FATAL = 4;
    
    /**
     * Log a message
     */
    void log(String serverId, String testRunFQN, int severity, String msg);
    
    /**
     * Retrieve log messages
     * 
     * @param clusterUUID           cluster identifier (mandatory)
     * @param testRunFQN            the name of the test run (mandatory)
     * @param serverId              server ID (optional)
     * @param severity              minimum severity (mandatory)
     * @param minTime               minimum log message time (inclusive)
     * @param maxTime               maximum log message time (exlusive)
     * @param skip                  number of results to skip
     * @param limit                 limit the total number of results
     * @return                      a list of server log messages
     */
    List<LogMessage> getLogs(
            String testRunFQN,
            String serverId,
            int severity,
            Long minTime, Long maxTime,
            int skip, int limit);
}
