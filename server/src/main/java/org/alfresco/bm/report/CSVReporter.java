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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.test.TestRunServicesCache;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.mongodb.DBObject;

/**
 * Dumps summary of all results.
 * <p/>
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
 *   <li><b>Standard Deviation:</b> The standard deviation of all successful event times.</li>
 * </ul>
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public class CSVReporter extends AbstractEventReporter
{
    public CSVReporter(TestRunServicesCache services, String test, String run)
    {
        super(services, test, run);
    }

    /**
     * Dump summary data for a test
     */
    protected void writeTestDetails(Writer writer, String notes) throws Exception
    {
        ResultService resultService = getResultService();
        // Get the test result times
        EventRecord firstResult = resultService.getFirstResult();
        long firstEventTime = firstResult == null ? System.currentTimeMillis() : firstResult.getStartTime();
        Date firstEventDate = new Date(firstEventTime);
        EventRecord lastResult = resultService.getLastResult();
        long lastEventTime = lastResult == null ? System.currentTimeMillis() : lastResult.getStartTime();
        Date lastEventDate = new Date(lastEventTime);
        String durationStr = DurationFormatUtils.formatDurationHMS(lastEventTime - firstEventTime);
        
        DBObject testRunObj = getTestService().getTestRunMetadata(test, run);
        
        writer.write("Name:,");
        writer.write(test + "." + run);
        writer.write(NEW_LINE);
        writer.write("Description:,");
        if (testRunObj.get(FIELD_DESCRIPTION) != null)
        {
            writer.write((String) testRunObj.get(FIELD_DESCRIPTION));
        }
        writer.write(NEW_LINE);
        writer.write("Data:,");
        writer.write(resultService.getDataLocation());
        writer.write(NEW_LINE);
        writer.write("Started:,");
        writer.write(firstEventDate.toString());
        writer.write(NEW_LINE);
        writer.write("Finished:,");
        writer.write(lastEventDate.toString());
        writer.write(NEW_LINE);
        writer.write("Duration:,");
        writer.write("'" + durationStr);            // ' is needed for Excel
        writer.write(NEW_LINE);
        writer.write(NEW_LINE);

        writer.write("Notes:");
        writer.write(NEW_LINE);
        writer.write(notes.replace(',', ' '));
        writer.write(NEW_LINE);
        writer.write(NEW_LINE);
    }
    
    @Override
    public void export(OutputStream os)
    {
        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter(os, "UTF8");
            export(writer);
        }
        catch (Exception e)
        {
            log.error("Failed to write summary csv data: " + this, e);
        }
        finally
        {
            try { writer.close(); } catch (Throwable e) {}
        }
    }
    
    protected void export(Writer writer) throws Exception
    {
        // TODO: Get any nodes
        String notes = "";
        
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
