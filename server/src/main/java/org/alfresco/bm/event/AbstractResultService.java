/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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
package org.alfresco.bm.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Common implementation around event results.
 * 
 * @author Derek Hulley
 * @since 1.3
 */
public abstract class AbstractResultService implements ResultService
{
    protected Log logger = LogFactory.getLog(this.getClass());
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getResults(
            ResultHandler handler,
            long startTime,
            String eventName,
            Boolean successOrFail,
            long windowSize,
            boolean chartOnly)
    {
        if (handler == null)
        {
            throw new IllegalArgumentException("A result handler must be supplied.");
        }
        if (windowSize <= 0)
        {
            throw new IllegalArgumentException("'windowSize' must be a non-zero, positive number.");
        }
        
        // Build stats for reporting back
        Map<String, DescriptiveStatistics> statsByEventName = new HashMap<String, DescriptiveStatistics>(7);
        
        // Keep track of the start and end times for the current window
        long currentWindowStartTime = (long) Math.floor(startTime / windowSize) * windowSize;
        long currentWindowEndTime = currentWindowStartTime + windowSize - 1L;    // It must be inclusive

        // Iterate over the results
        int windowNumber = -1;
        int skip = 0;
        boolean stop = false;
breakStop:
        while (!stop)
        {
            List<EventRecord> results = getResults(currentWindowStartTime, eventName, successOrFail, skip, 100);
            // Simulate iteration over results
            for (EventRecord eventRecord : results)
            {
                String eventRecordName = eventRecord.getEvent().getName();
                long eventRecordStartTime = eventRecord.getStartTime();
                long eventRecordTime = eventRecord.getTime();
                
                // Move the window up to enclose the first event
                // If we have enclosed the first event, then just keep moving the window up in incrementally
                while (eventRecordStartTime > currentWindowEndTime)
                {
                    if (windowNumber < 0)
                    {
                        // There is nothing to report
                        // Rebase the window to encompasse the first event
                        currentWindowStartTime = (long) Math.floor(eventRecordStartTime / windowSize) * windowSize;
                        currentWindowEndTime = currentWindowStartTime + windowSize - 1L;
                        // We are still in the first window
                    }
                    else
                    {
                        // Report the previous window
                        stop = reportCurrentStats(statsByEventName, currentWindowStartTime, currentWindowEndTime, handler);
                        // Shift the window up
                        currentWindowStartTime += windowSize;
                        currentWindowEndTime += windowSize;
                        // Increment the window count
                        windowNumber++;
                        // Check for stop
                        if (stop)
                        {
                            break breakStop;
                        }
                    }
                    // Reset skip for the new window
                    skip = 0;
                }
                // We are in the first window, at least
                if (windowNumber < 0)
                {
                    windowNumber = 0;
                }
                // Increase the skip with each window result
                skip++;
                
                // Ignore results we don't wish to chart
                if (chartOnly && !eventRecord.isChart())
                {
                    continue;
                }

                // Get the stats for the current event
                DescriptiveStatistics eventRecordStats = statsByEventName.get(eventRecordName);
                if (eventRecordStats == null)
                {
                    // The window size is unlimited; we don't know how many results will occur
                    // within the window
                    eventRecordStats = new DescriptiveStatistics(DescriptiveStatistics.INFINITE_WINDOW);
                    statsByEventName.put(eventRecordName, eventRecordStats);
                }
                
                // Update the stats
                eventRecordStats.addValue(eventRecordTime);
            }
            // If there are no more results, find out if we should wait or quit
            if (results.size() == 0)
            {
                long waitTime = handler.getWaitTime();
                if (waitTime > 0L)
                {
                    synchronized (statsByEventName)
                    {
                        try { statsByEventName.wait(waitTime); } catch (InterruptedException e) {}
                    }
                }
                else
                {
                    reportCurrentStats(statsByEventName, currentWindowStartTime, currentWindowEndTime, handler);
                    stop = true;
                }
            }
        }
    }
    
    /**
     * @return                      <tt>true</tt> to stop processing
     */
    private boolean reportCurrentStats(
            Map<String, DescriptiveStatistics> statsByEventName,
            long currentWindowStartTime,
            long currentWindowEndTime,
            ResultHandler handler)
    {
        boolean stop = false;
        try
        {
            boolean go = handler.processResult(currentWindowStartTime, currentWindowEndTime, statsByEventName);
            stop = !go;
        }
        catch (Throwable e)
        {
            logger.error("Exception while making callback.", e);
        }
        finally
        {
            // Reset the statistics for the event
            statsByEventName.clear();
        }
        return stop;
    }
}
