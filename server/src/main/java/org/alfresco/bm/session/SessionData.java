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
package org.alfresco.bm.session;

import org.alfresco.bm.event.Event;

import com.mongodb.DBObject;

/**
 * Data used to manage and track user scenarios
 *
 * @author Derek Hulley
 * @since 1.0
 */
public class SessionData
{
    /** A link to the {@link Event#getSessionId() session} associated with an event chain. */
    private String id;
    private DBObject data;
    private long startTime;
    private long endTime;

    /**
     * Required default constructor for persistence
     */
    public SessionData()
    {
        this.startTime = System.currentTimeMillis();
        this.endTime = -1L;
        this.data = null;
    }

    /**
     * Convenience constructor for service
     */
    public SessionData(DBObject data)
    {
        this();
        this.data = data;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SessionData [id=");
        builder.append(id);
        builder.append(", data=");
        builder.append(data);
        builder.append(", startTime=");
        builder.append(startTime);
        builder.append(", endTime=");
        builder.append(endTime);
        builder.append("]");
        return builder.toString();
    }

    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }

    public DBObject getData()
    {
        return data;
    }
    public void setData(DBObject data)
    {
        this.data = data;
    }

    public long getStartTime()
    {
        return startTime;
    }
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }
    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }
}
