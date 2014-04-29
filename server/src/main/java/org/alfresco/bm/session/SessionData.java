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
package org.alfresco.bm.session;

import org.alfresco.bm.event.Event;

/**
 * Data used to manage and track user scenarios
 * <p/>
 * TODO: https://jira.springsource.org/browse/DATAMONGO-392
 *       Make data data serializable once Spring can deserialize what it writes.
 *       Visit the {@link SessionService} once fixed.
 *
 * @author Derek Hulley
 * @since 1.0
 */
public class SessionData
{
    /** A link to the {@link Event#getSessionId() session} associated with an event chain. */
    private String sessionId;
    /** Any persistable data associated with the session */
    private String data;
    private long startTime;
    private long endTime;
    private long elapsedTime;

    /**
     * Required default constructor for persistence
     */
    public SessionData()
    {
        this.startTime = System.currentTimeMillis();
        this.endTime = -1L;
        this.elapsedTime = -1L;
    }

    /**
     * Convenience constructor for service
     */
    public SessionData(String data)
    {
        this();
        this.data = data;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SessionData [sessionId=");
        builder.append(sessionId);
        builder.append(", data=");
        builder.append(data);
        builder.append(", startTime=");
        builder.append(startTime);
        builder.append(", endTime=");
        builder.append(endTime);
        builder.append(", elapsedTime=");
        builder.append(elapsedTime);
        builder.append("]");
        return builder.toString();
    }

    public String getSessionId()
    {
        return sessionId;
    }
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getData()
    {
        return data;
    }
    public void setData(String data)
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

    public long getElapsedTime()
    {
        return elapsedTime;
    }
    public void setElapsedTime(long elapsedTime)
    {
        this.elapsedTime = elapsedTime;
    }
}
