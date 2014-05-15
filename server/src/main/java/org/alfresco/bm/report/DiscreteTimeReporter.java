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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.bm.chart.ResultChart;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.event.ResultService.ResultHandler;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Event reporter that dumps averaged results at discrete time intervals.
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public class DiscreteTimeReporter extends AbstractEventReporter
{
    public static final int DEFAULT_NUMBER_OF_RESULTS = 1000;
    
    private int numberOfResults = DEFAULT_NUMBER_OF_RESULTS;
    
    /**
     * {@inheritDoc}
     * @since 1.3
     */
    public DiscreteTimeReporter(ResultService resultService)
    {
        super(resultService);
    }

    /**
     * Change the {@link #DEFAULT_NUMBER_OF_RESULTS default} number of results
     * 
     * @param numberOfResults           the approximate number of results to export
     */
    public void setNumberOfResults(int numberOfResults)
    {
        this.numberOfResults = numberOfResults;
    }

    @Override
    public void export(String file, String notes)
    {
        // Get the headers we require
        List<String> headers = resultService.getEventNames();
        headers = buildHeaders(headers);
        Map<String, Integer> headerIndexes = new HashMap<String, Integer>();
        int index = 0;                          // Skip 3 columns before actual results
        for (String header : headers)
        {
            headerIndexes.put(header, Integer.valueOf(index));
            index++;
        }

        OutputStream outStream = null;
        Writer writer = null;
        try
        {
            outStream = new BufferedOutputStream(new FileOutputStream(new File(file)));
            writer = new OutputStreamWriter(outStream, "UTF8");

            writeTestDetails(writer, notes);
            
            writer.write(NEW_LINE);
            writer.write(ReportUtil.formatHeaders(headers.toString()));
            writer.write(NEW_LINE);
            // Start writing data
            writeData(writer, headerIndexes);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
                if (outStream != null)
                {
                    outStream.close();
                }
            }
            catch (IOException e)
            {
                log.error(e);
            }
            outStream = null;
            writer = null;
        }
    }

    /**
     * Perform paged data queries and write it out
     */
    private void writeData(final Writer writer, final Map<String, Integer> headerIndexes) throws IOException
    {
        // Get the test result times
        EventRecord firstResult = resultService.getFirstResult();
        long firstEventTime = firstResult == null ? System.currentTimeMillis() : firstResult.getStartTime();
        EventRecord lastResult = resultService.getFirstResult();
        long lastEventTime = lastResult == null ? System.currentTimeMillis() : lastResult.getStartTime();

        final long windowSize = ResultChart.getWindowSize(firstEventTime, lastEventTime, numberOfResults);
        
        ResultHandler handler = new ResultHandler()
        {
            @Override
            public boolean processResult(
                    long fromTime, long toTime,
                    Map<String, DescriptiveStatistics> statsByEventName)
                    throws Throwable
            {
                writeTimeEntry(
                        toTime,
                        statsByEventName,
                        writer,
                        headerIndexes);
                return false;
            }

            @Override
            public long getWaitTime()
            {
                return -1L;
            }
        };
        resultService.getResults(handler, 0L, null, Boolean.TRUE, windowSize, false);
    }
    
    /**
     * Write recorded data out
     * 
     * @param printTime             the time for the events
     * @param stats                 the averages for the events, if there are samples for the time window
     */
    private void writeTimeEntry(
            long time,
            Map<String, DescriptiveStatistics> stats,
            Writer writer,
            Map<String, Integer> headerIndexes) throws IOException
    {
        double[] statsRow = new double[headerIndexes.size()];
        for (Map.Entry<String, DescriptiveStatistics> statsEntry : stats.entrySet())
        {
            String columnName = statsEntry.getKey();
            // What is the column index?
            Integer columnIndex = headerIndexes.get(columnName);
            if (columnIndex == null)
            {
                // It is an unknown event
                continue;
            }
            DescriptiveStatistics columnStats = statsEntry.getValue();
            if (columnStats != null)
            {
                statsRow[columnIndex] = columnStats.getMean();
            }
            else
            {
                statsRow[columnIndex] = -1.0;
            }
        }
        
        // Put output time in correct place
        statsRow[1] = time;
        // We have a double[] of values to write out
        StringBuilder sb = new StringBuilder();
        for (double statsEntry : statsRow)
        {
            sb.append(statsEntry >= 0 ? statsEntry : "");
            sb.append(",");
        }
        sb.append(NEW_LINE);
        writer.write(sb.toString());
    }
}
