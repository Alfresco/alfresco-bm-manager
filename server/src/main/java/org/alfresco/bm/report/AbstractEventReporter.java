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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract support for event reporting
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public abstract class AbstractEventReporter implements EventReporter
{
    protected static String ID_EVENT_LABEL = "Event Result Id";
    protected static String TIME_EVENT_LABEL = "Time";
    protected static String FAILED_LABEL_PREFIX = "Failed:";

    protected static final String NEW_LINE = "\n";
    protected static final int LIMIT_VALUE = 1000;
    
    protected Log log = LogFactory.getLog(this.getClass());

    protected final ResultService resultService;

    /**
     * @param resultService     service providing access to results
     */
    protected AbstractEventReporter(ResultService resultService)
    {
        this.resultService = resultService;
    }

    /**
     * Helper method to build header labels of csv file.
     * The head labels also include failed results.
     * 
     * @param headers       collection of event names to use as labels for header
     * @return collection   with a given order where first 2 labels are time and start
     */
    public static List<String> buildHeaders(final List<String> headers)
    {
        ArrayList<String> tableHead = new ArrayList<String>();
        ArrayList<String> failedHeaderLabel = new ArrayList<String>();
        tableHead.add(ID_EVENT_LABEL);
        tableHead.add(TIME_EVENT_LABEL);
        tableHead.add(Event.EVENT_NAME_START);
        for (String header : headers)
        {
            if (!Event.EVENT_NAME_START.equals(header))
            {
                tableHead.add(header);
                failedHeaderLabel.add(FAILED_LABEL_PREFIX + header);
            }
        }
        tableHead.addAll(failedHeaderLabel);
        failedHeaderLabel = null;
        return tableHead;
    }
    
    /**
     * Dump summary data for a test
     */
    protected void writeTestDetails(Writer writer, String notes) throws IOException
    {
        // Get the test result times
        EventRecord firstResult = resultService.getFirstResult();
        long firstEventTime = firstResult == null ? System.currentTimeMillis() : firstResult.getStartTime();
        Date firstEventDate = new Date(firstEventTime);
        EventRecord lastResult = resultService.getLastResult();
        long lastEventTime = lastResult == null ? System.currentTimeMillis() : lastResult.getStartTime();
        Date lastEventDate = new Date(lastEventTime);
        String durationStr = DurationFormatUtils.formatDurationHMS(lastEventTime - firstEventTime);
        
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
}
