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

/**
 * Service class that retrieves test run results from MongoDB and generates a
 * csv output file from the result.
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public class AllResultsReporter extends AbstractEventReporter
{
    /**
     * {@inheritDoc}
     */
    public AllResultsReporter(ResultService resultService)
    {
        super(resultService);
    }

    @Override
    public void export(String file, String notes)
    {
        // Get the headers we require
        List<String> headers = resultService.getEventNames();
        headers = buildHeaders(headers);
        Map<String, Integer> headerIndexes = new HashMap<String, Integer>();
        int index = 0;
        for (String header : headers)
        {
            headerIndexes.put(header, new Integer(index));
            index++;
        }

        OutputStream outStream = null;
        Writer writer = null;
        try
        {
            outStream = new BufferedOutputStream(new FileOutputStream(new File(file)));
            writer = new OutputStreamWriter(outStream, "UTF8");

            writeTestDetails(writer, notes);
            
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
    private void writeData(Writer writer, Map<String, Integer> headerIndexes) throws IOException
    {
        int skip = 0;
        int limit = LIMIT_VALUE;
        // Get the first set of results
        List<EventRecord> results = resultService.getResults(0L, null, null, skip, limit);

        // Keep looping through results until they've all been read
        while (results.size() > 0)
        {
            writeData(results, writer, headerIndexes);
            // Get next results
            skip += results.size();
            results = resultService.getResults(0L, null, null, skip, limit);
        }
    }

    /**
     * Write recorded data out
     */
    private void writeData(
            List<EventRecord> data,
            Writer writer,
            Map<String, Integer> headerIndexes) throws IOException
    {
        for (EventRecord re : data)
        {
            // What is the column name?
            String columnName = re.isSuccess() ?
                    re.getEvent().getName() :
                        AllResultsReporter.FAILED_LABEL_PREFIX + re.getEvent().getName();
            // What is the column index?
            Integer columnIndex = headerIndexes.get(columnName);
            if (columnIndex == null)
            {
                // It is an unknown event
                continue;
            }

            StringBuilder sb = new StringBuilder();

            String eventId = re.getId();
            sb.append(eventId).append(",");
            long eventTime = re.getStartTime();
            // Write the time
            sb.append(eventTime).append(",");
            // We already have our first two columns
            for (int i = 2; i < columnIndex; i++)
            {
                sb.append(',');
            }
            // Write the time it took
            sb.append(re.getTime()).append(NEW_LINE);
            // Dump
            writer.write(sb.toString());
        }
    }
}
