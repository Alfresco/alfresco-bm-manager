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
package org.alfresco.bm.event;

import java.util.Date;

import com.mongodb.DBObject;

/**
 * A representation of the data for an event result
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class EventRecord
{
    public static final String FIELD_ID = "_id";
    public static final String FIELD_DRIVER_ID = "driverId";
    public static final String FIELD_SUCCESS = "success";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_START_DELAY = "startDelay";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_WARNING = "warning";
    public static final String FIELD_CHART = "chart";
    public static final String FIELD_PROCESSED_BY = "processedBy";
    
    public static final String FIELD_EVENT = "event";
    public static final String FIELD_EVENT_DATA = "event." + Event.FIELD_DATA;
    public static final String FIELD_EVENT_DATA_VALUE = "event." + Event.FIELD_DATA + "." + Event.FIELD_VALUE;
    public static final String FIELD_EVENT_DATA_OWNER = "event." + Event.FIELD_DATA_OWNER;
    public static final String FIELD_EVENT_LOCK_OWNER = "event." + Event.FIELD_LOCK_OWNER;
    public static final String FIELD_EVENT_LOCK_TIME = "event." + Event.FIELD_LOCK_TIME;
    public static final String FIELD_EVENT_NAME = "event." + Event.FIELD_NAME;
    public static final String FIELD_EVENT_SCHEDULED_TIME = "event." + Event.FIELD_SCHEDULED_TIME;
    public static final String FIELD_EVENT_SESSION_ID = "event." + Event.FIELD_SESSION_ID;

    private final String driverId;
    private String id;
    private final boolean success;
    private final long startTime;
    private long startDelay;
    private final long time;
    private final Event event;
    private final Object data;
    private String warning;
    private boolean chart = true;
    private String processedBy = "unknown";

    /**
     * @param driverId          the driver ID
     * @param success           <tt>true</tt> if this represents a successful event
     *                          otherwise <tt>false</tt>
     * @param startTime         the time when processing started
     * @param time              the time it took to process the event
     * @param data              any additional data that should be recorded with this event
     * @param event             the event that was processed
     */
    public EventRecord(String driverId, boolean success, long startTime, long time, Object data, Event event)
    {
        if (driverId == null)
        {
            throw new IllegalArgumentException("A driver ID is mandatory");
        }
        if (event == null)
        {
            throw new IllegalArgumentException("An Event is mandatory");
        }
        
        this.driverId = driverId;
        this.success = success;
        this.startTime = startTime;
        this.startDelay = (event.getScheduledTime() == 0) ? 0L : (startTime - event.getScheduledTime());
        this.time = time;
        this.event = event;
        // Check and store the data
        boolean canPersistData =
                data == null ||
                data instanceof String ||
                data instanceof DBObject ||
                data instanceof Number;
        
        if (!canPersistData)
        {
            throw new IllegalArgumentException("EventRecord data must be a 'null', 'String', 'DBObject' or 'Number'.");
        }
        else
        {
            // A null, String or DBObject is provided, so we just accept it as is
            this.data = data;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("EventRecord ");
        builder.append("[ driverId=").append(driverId);
        builder.append(", processedBy=").append(processedBy);
        builder.append(", success=").append(success);
        builder.append(", startTime=").append(new Date(startTime));
        builder.append(", startDelay=").append(String.format("%.1fs", startDelay/1000.0));
        builder.append(", time=").append(time).append("ms");
        builder.append(", data=").append(data);
        builder.append(", event=").append(event);
        builder.append("]");
        return builder.toString();
    }

    public String getDriverId()
    {
        return driverId;
    }

    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getStartDelay()
    {
        return startDelay;
    }
    public void setStartDelay(long startDelay)
    {
        this.startDelay = startDelay;
    }

    public long getTime()
    {
        return time;
    }

    public Event getEvent()
    {
        return event;
    }

    public Object getData()
    {
        return data;
    }

    public String getWarning()
    {
        return warning;
    }
    public void setWarning(String warning)
    {
        this.warning = warning;
    }

    public boolean isChart()
    {
        return chart;
    }
    public void setChart(boolean chart)
    {
        this.chart = chart;
    }

    public String getProcessedBy()
    {
        return processedBy;
    }
    public void setProcessedBy(String processedBy)
    {
        this.processedBy = processedBy;
    }
}
