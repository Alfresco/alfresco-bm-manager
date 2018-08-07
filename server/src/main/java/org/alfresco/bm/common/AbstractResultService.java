/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

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
            long windowSize,
            long reportPeriod,
            boolean chartOnly)
    {
        /*
         * Keep track of all events' statistics.
         * It is possible to report more frequently than the window size.
         * For each report period in the reporting window, the statistics for the events need to be maintained.
         */
        
        if (handler == null)
        {
            throw new IllegalArgumentException("A result handler must be supplied.");
        }
        if (windowSize <= 0L)
        {
            throw new IllegalArgumentException("'windowSize' must be a non-zero, positive number.");
        }
        if (reportPeriod <= 0L)
        {
            throw new IllegalArgumentException("'reportPeriod' must be a non-zero, positive number.");
        }
        if (reportPeriod > windowSize)
        {
            throw new IllegalArgumentException("'reportPeriod' cannot more than the 'windowSize'.");
        }
        if (windowSize % reportPeriod != 0L)
        {
            throw new IllegalArgumentException("'windowSize' must be a multiple of 'reportPeriod'.");
        }
        
        // We have to keep statistics for each reporting period
        int windowMultiple = (int) (windowSize / reportPeriod);
        
        // Build stats for reporting back
        // Each LinkedList will have 'windowMultiple' entries.
        // The newest statistics will be the last in the linked list; results will be reported from the first entry each time.
        Map<String, LinkedList<DescriptiveStatistics>> statsByEventName = new HashMap<String, LinkedList<DescriptiveStatistics>>(13);
        Map<String, LinkedList<AtomicInteger>> failuresByEventName = new HashMap<String, LinkedList<AtomicInteger>>(13);
        
        // Our even queries use separate windows
        EventRecord firstResult = getFirstResult();
        if (firstResult == null)
        {
            // There is nothing
            return;
        }
        long firstResultStartTime = firstResult.getStartTime();
        EventRecord lastResult = getLastResult();
        long lastResultStartTime = lastResult.getStartTime();

        long queryWindowStartTime = Math.max(firstResultStartTime, startTime);                       // The start time is inclusive
        long queryWindowSize = lastResult.getStartTime() - firstResult.getStartTime();
        if (queryWindowSize < 60000L)
        {
            queryWindowSize = 60000L;                                           // Query window is at least a minute
        }
        else if (queryWindowSize > (60000L * 60L))
        {
            queryWindowSize = 60000L * 60L;                                     // Query window is at most an hour
        }
        long queryWindowEndTime = queryWindowStartTime + queryWindowSize;

        // Rebase the aggregation window to encompasse the first event
        long currentWindowEndTime = (long) Math.floor((firstResultStartTime + reportPeriod) / reportPeriod) * reportPeriod;
        long currentWindowStartTime = currentWindowEndTime - windowSize;
        
        // Iterate over the results
        int skip = 0;
        int limit = 10000;
        boolean stop = false;
        boolean unreportedResults = false;
breakStop:
        while (!stop)
        {
            List<EventRecord> results = getResults(queryWindowStartTime, queryWindowEndTime, chartOnly, skip, limit);
            if (results.size() == 0)
            {
                if (queryWindowEndTime > lastResultStartTime)
                {
                    // The query window has included the last event, so we have extracted all results
                    if (unreportedResults)
                    {
                        // The query window ends in the future, so we are done
                        reportAndCycleStats(statsByEventName, failuresByEventName, currentWindowStartTime, currentWindowEndTime, windowMultiple, handler);
                        unreportedResults = false;
                    }
                    stop = true;
                }
                else
                {
                    // Move the query window up
                    queryWindowStartTime = queryWindowEndTime;
                    queryWindowEndTime+= queryWindowSize;
                    // Reset the skip count as we are in a new query window
                    skip = 0;
                }
                // We continue
                continue;
            }
            // Process each result found in the query window
            for (EventRecord eventRecord : results)
            {
                String eventRecordName = eventRecord.getEvent().getName();
                long eventRecordStartTime = eventRecord.getStartTime();
                long eventRecordTime = eventRecord.getTime();
                boolean eventRecordSuccess = eventRecord.isSuccess();
                
                // If the current event is past the reporting period, then report
                if (eventRecordStartTime >= currentWindowEndTime)
                {
                    // Report the current stats
                    stop = reportAndCycleStats(statsByEventName, failuresByEventName, currentWindowStartTime, currentWindowEndTime, windowMultiple, handler);
                    unreportedResults = false;
                    // Shift the window up by one report period
                    currentWindowStartTime += reportPeriod;
                    currentWindowEndTime += reportPeriod;
                    // Check for stop
                    if (stop)
                    {
                        break breakStop;
                    }
                }
                // Increase the skip with each window result
                skip++;
                
                // Ignore results we don't wish to chart
                if (chartOnly && !eventRecord.isChart())
                {
                    continue;
                }

                // We have to report this result at some point
                unreportedResults = true;
                
                // Get the linked list of stats for the event
                LinkedList<DescriptiveStatistics> eventStatsLL = statsByEventName.get(eventRecordName);
                if (eventStatsLL == null)
                {
                    // Create a LL for the event
                    eventStatsLL = new LinkedList<DescriptiveStatistics>();
                    statsByEventName.put(eventRecordName, eventStatsLL);
                    // We need at least one entry in order to record stats
                    eventStatsLL.add(new DescriptiveStatistics());
                }
                // Write the current event to all the stats for the event
                for (DescriptiveStatistics eventStats : eventStatsLL)
                {
                    eventStats.addValue(eventRecordTime);
                }

                // Get the linked list of failure counts for the event
                LinkedList<AtomicInteger> eventFailuresLL = failuresByEventName.get(eventRecordName);
                if (eventFailuresLL == null)
                {
                    // Create a LL for the event
                    eventFailuresLL = new LinkedList<AtomicInteger>();
                    failuresByEventName.put(eventRecordName, eventFailuresLL);
                    // Need one entry to record failures
                    eventFailuresLL.add(new AtomicInteger(0));
                }
                // Write any failures to all counts for the event
                if (!eventRecordSuccess)
                {
                    for (AtomicInteger eventFailures : eventFailuresLL)
                    {
                        eventFailures.incrementAndGet();
                    }
                }
            }
        }
    }
    
    /**
     * Reports the oldest stats for the events and pops it off the list
     * 
     * @param windowMultiple        the number of reporting entries to hold per event
     * @return                      <tt>true</tt> to stop processing
     */
    private boolean reportAndCycleStats(
            Map<String, LinkedList<DescriptiveStatistics>> statsByEventName,
            Map<String, LinkedList<AtomicInteger>> failuresByEventName,
            long currentWindowStartTime,
            long currentWindowEndTime,
            int windowMultiple,
            ResultHandler handler)
    {
        // Handle stats
        Map<String, DescriptiveStatistics> stats = new HashMap<String, DescriptiveStatistics>(statsByEventName.size() + 7);
        for (Map.Entry<String, LinkedList<DescriptiveStatistics>> entry : statsByEventName.entrySet())
        {
            // Grab the OLDEST stats from the beginning of the list
            String eventName = entry.getKey();
            LinkedList<DescriptiveStatistics> ll = entry.getValue();
            try
            {
                DescriptiveStatistics eventStats = ll.getFirst();
                stats.put(eventName, eventStats);
                if (ll.size() == windowMultiple)
                {
                    // We have enough reporting points for the window, so pop the first and add a new to the end
                    ll.pop();
                }
                ll.add(new DescriptiveStatistics());
            }
            catch (NoSuchElementException e)
            {
                throw new RuntimeException("An event name did not have a result for the reporting period: " + statsByEventName);
            }
        }
        
        // Handle failures
        Map<String, Integer> failures = new HashMap<String, Integer>(statsByEventName.size() + 7);
        for (Map.Entry<String, LinkedList<AtomicInteger>> entry : failuresByEventName.entrySet())
        {
            // Grab the OLDEST stats from the beginning of the list
            String eventName = entry.getKey();
            LinkedList<AtomicInteger> ll = entry.getValue();
            try
            {
                AtomicInteger eventFailures = ll.getFirst();
                failures.put(eventName, Integer.valueOf(eventFailures.get()));
                if (ll.size() == windowMultiple)
                {
                    // We have enough reporting points for the window, so pop the first and add a new to the end
                    ll.pop();
                }
                ll.add(new AtomicInteger());
            }
            catch (NoSuchElementException e)
            {
                throw new RuntimeException("An event name did not have a failure count for the reporting period: " + failuresByEventName);
            }
        }
        
        boolean stop = false;
        try
        {
            boolean go = handler.processResult(currentWindowStartTime, currentWindowEndTime, stats, failures);
            stop = !go;
        }
        catch (Throwable e)
        {
            logger.error("Exception while making callback.", e);
        }
        return stop;
    }
}
