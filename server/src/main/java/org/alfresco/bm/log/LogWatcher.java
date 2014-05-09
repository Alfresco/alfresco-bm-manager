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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Service to watch logs
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class LogWatcher implements LifecycleListener
{
    private static Log logger = LogFactory.getLog(LogWatcher.class);
    
    private final File logDir;
    
    public LogWatcher(String logDir)
    {
        this.logDir = new File(logDir);
    }

    /**
     * Locates all available logs files and notes them for later fetch
     */
    @Override
    public void start() throws Exception
    {
        // Check log directory
        if (!logDir.exists())
        {
            throw new RuntimeException("Log directory does not exist: " +logDir);
        }
    }

    /**
     * No op
     */
    @Override
    public void stop() throws Exception
    {
    }
    
    public List<String> getLogFilenames()
    {
        // Get all the local log files
        FilenameFilter logFilter = new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".log");
            }
        };
        File[] logFiles = logDir.listFiles(logFilter);
        List<String> logFilenames = new ArrayList<String>(logFiles.length);
        for (File logFile : logFiles)
        {
            logFilenames.add(logFile.getName());
        }
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Found log files: " + logFiles);
        }
        return logFilenames;
    }
}
