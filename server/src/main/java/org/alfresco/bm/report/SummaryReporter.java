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
package org.alfresco.bm.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Dumps summary of all results.
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public class SummaryReporter extends AbstractEventReporter
{
    public SummaryReporter(ResultService resultService)
    {
        super(resultService);
    }
    
    /**
     * Results table where each row consists of:
     * <ul>
     *   <li><b>Event name:</b> Name of the event.  These will be listed aphabetically across the rows.</li>
     *   <li><b>Total Count:</b> Total number of events.</li>
     *   <li><b>Success Count:</b> Number of successful events.</li>
     *   <li><b>Failure Count:</b> Number of failed events.</li>
     *   <li><b>Success Rate (%):</b> Percentage of successful events.</li>
     *   <li><b>Min (ms):</b> Minimum event time (successes only).</li>
     *   <li><b>Max (ms):</b> Maximum event time (successes only).</li>
     *   <li><b>Arithmetic Mean (ms):</b> The arithmetic mean of all successful event times.</li>
     *   <li><b>Variance (ms^2):</b> The sample variance of all successful event times.</li>
     * </ul>
     */
    @Override
    public void export(String file, String notes)
    {
        Writer writer = null;
        try
        {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(file)));
            writer = new OutputStreamWriter(os, "UTF8");
            export(writer, notes);
        }
        catch (IOException e)
        {
            log.error("Failed to write summary data to file " + file, e);
        }
        finally
        {
            try { writer.close(); } catch (Throwable e) {}
        }
    }
    
    private TreeMap<String, ResultSummary> collateResults(boolean chartOnly)
    {
        // Do a quick check to see if there are results
        EventRecord firstEvent = resultService.getFirstResult();
        if (firstEvent == null)
        {
            return new TreeMap<String, ResultSummary>();
        }
        EventRecord lastEvent = resultService.getLastResult();

        long oneHour = TimeUnit.HOURS.toMillis(1L);
        long queryWindowStartTime = firstEvent.getStartTime();
        long queryWindowEndTime = queryWindowStartTime + oneHour;
        
        // Prepare recorded data
        TreeMap<String, ResultSummary> results = new TreeMap<String, ResultSummary>();
        int limit = 10000;
        int skip = 0;
        
        while (true)
        {
            List<EventRecord> data = resultService.getResults(queryWindowStartTime, queryWindowEndTime, chartOnly, skip, limit);
            if (data.size() == 0)
            {
                if (queryWindowEndTime > lastEvent.getStartTime())
                {
                    // The query window covered all known events, so we're done
                    break;
                }
                else
                {
                    // Push the window up
                    queryWindowStartTime = queryWindowEndTime;
                    queryWindowEndTime += oneHour;
                    // Requery
                    continue;
                }
            }
            for (EventRecord eventRecord : data)
            {
                skip++;
                // Add the data
                String eventName = eventRecord.getEvent().getName();
                ResultSummary resultSummary = results.get(eventName);
                if (resultSummary == null)
                {
                    resultSummary = new ResultSummary(eventName);
                    results.put(eventName, resultSummary);
                }
                boolean resultSuccess = eventRecord.isSuccess();
                long resultTime = eventRecord.getTime();
                resultSummary.addSample(resultSuccess, resultTime);
            }
        }
        // Done
        return results;
    }

    /**
     * Dump results to an OutputStream, which <b>will not be closed</b>.
     */
    public void export(Writer writer, String notes) throws IOException
    {
        writeTestDetails(writer, notes);
        
        writer.write(",,");
        writer.write(
                "Event Name,Total Count,Success Count,Failure Count,Success Rate (%)," +
                "Min (ms), Max (ms), Arithmetic Mean (ms), Standard Deviation (ms)");
        writer.write(NEW_LINE);
        TreeMap<String, ResultSummary> summaries = collateResults(true);
        for (Map.Entry<String, ResultSummary> entry : summaries.entrySet())
        {
            writer.write(",,");
            String eventName = entry.getKey();
            ResultSummary summary = entry.getValue();
            SummaryStatistics statsSuccess = summary.getStats(true);
            SummaryStatistics statsFail = summary.getStats(false);
            // Event Name
            writer.write(String.format("%s,", eventName));
            // Total Count
            writer.write(String.format("%6d,", summary.getTotalResults()));
            // Success Count
            writer.write(String.format("%6d,", statsSuccess.getN()));
            // Failure Count
            writer.write(String.format("%6d,", statsFail.getN()));
            // Success Rate (%)
            writer.write(String.format("%3.1f,", summary.getSuccessPercentage()));
            // Min (ms)
            writer.write(String.format("%10d,", (long)statsSuccess.getMin()));
            // Max (ms)
            writer.write(String.format("%10d,", (long)statsSuccess.getMax()));
            // Arithmetic Mean (ms)
            writer.write(String.format("%10d,", (long)statsSuccess.getMean()));
            // Standard Deviation (ms)
            writer.write(String.format("%10d%s", (long)statsSuccess.getStandardDeviation(), NEW_LINE));
        }
        // Done
        
    }
}
