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
    
    /**
     * Stardard window sizes
     */
    public static final long[] WINDOW_SIZES = new long[]
                                                     {
        1L, 5L, 10L, 100L,                                          // < 1 second
        1000L, 5000L, 10000L, 30000L,                               // < 1 minute
        60000L, 120000L, 300000L, 600000L, 1800000L,                // < 1 hour
        360000L,                                                    // < 1 day
        24*360000L
                                                     };
    /**
     * Helper method to calculate a window size (ms) such that the approximate number
     * of results is retrieved across the given time range.
     * 
     * @param startTime             beginning of results
     * @param endTime               end of results
     * @param windowCount           number of windows to fit in
     * @return                      the size of a window in milliseconds
     */
    public static long getWindowSize(long startTime, long endTime, int windowCount)
    {
        long delta = endTime - startTime;
        // Try all the windows out
        for (int i = 0; i < WINDOW_SIZES.length; i++)
        {
            long windowSize = WINDOW_SIZES[i];
            double actualWindowCount = (double) delta / (double) windowSize;
            if (actualWindowCount < windowCount)
            {
                // We have too few results
                return windowSize;
            }
        }
        // Didn't find one big enough.  Go with the biggest.
        return WINDOW_SIZES[WINDOW_SIZES.length - 1];
    }

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

        final long reportPeriod = DiscreteTimeReporter.getWindowSize(firstEventTime, lastEventTime, numberOfResults);
        final long windowSize = reportPeriod;
        
        ResultHandler handler = new ResultHandler()
        {
            @Override
            public boolean processResult(
                    long fromTime, long toTime,
                    Map<String, DescriptiveStatistics> statsByEventName,
                    Map<String, Integer> failuresByEventName)
                    throws Throwable
            {
                writeTimeEntry(
                        toTime,
                        statsByEventName,
                        writer,
                        headerIndexes);
                return false;
            }
        };
        resultService.getResults(handler, 0L, windowSize, reportPeriod, false);
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
