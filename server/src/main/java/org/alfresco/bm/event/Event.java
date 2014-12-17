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

import org.bson.types.ObjectId;

import com.mongodb.DBObject;

/**
 * An event that is persisted and retrieved for processing.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class Event
{
    public static final String FIELD_ID = "_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SESSION_ID = "sessionId";
    public static final String FIELD_SCHEDULED_TIME= "scheduledTime";
    public static final String FIELD_LOCK_OWNER = "lockOwner";
    public static final String FIELD_LOCK_TIME = "lockTime";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_DATA_OWNER = "dataOwner";
    
    /** The 'value' that is associated with the data */
    public static final String FIELD_VALUE = "value";

    public static final String EVENT_NAME_START = "start";
    
    public static final String EVENT_ID_START = "000000000000000000000001";
    
    public static final String EVENT_BEAN_PREFIX = "event.";
    
    private String id;
    private final String name;
    private String sessionId;
    private final long scheduledTime;
    private String lockOwner;
    private long lockTime;
    private Object data;            // DBObject, String or Numeric
    private boolean dataInMemory;
    
    /**
     * Construct an event with some data, scheduling it for the current time.
     * <p/>
     * <b>NB: </b> BEWARE OF JAVA AUTOBOXING OF NUMERIC DATA.  This constructor
     *             assumes the event trigger time is <b>now</b>.
     * <p/>
     * Use a {@link DBObject} to allow full MongoDB-friendly persistence, allowing
     * searching for <b>data.X</b>-style searches where the object has key *X*, say.
     * The data, if a {@link String}, will be persisted directly with this event.
     * 
     * @param name                  the event name
     * @param data                  the event data
     */
    public Event(String name, Object data)
    {
        this(
                name,
                System.currentTimeMillis(),
                data,
                false);
    }

    /**
     * Construct a scheduled event with some data.
     * <p/>
     * Use a {@link DBObject} to allow full MongoDB-friendly persistence, allowing
     * searching for <b>data.X</b>-style searches where the object has key *X*, say.
     * The data, if a {@link String}, will be persisted directly with this event.
     * <p/>
     * Events that keep their data in memory <i>cannot</i> be executed by any other
     * running service and will therefore be lost if the server goes down.
     * 
     * @param name                  the event name
     * @param scheduledTime         when the event should be processed
     * @param data                  the event data
     */
    public Event(String name, long scheduledTime, Object data)
    {
        this(name, scheduledTime, data, false);
    }
    
    /**
     * Construct a scheduled event with some data, optionally forcing the data to be stored 
     * in memory.  If the option is selected to force in-memory storage or if the data is
     * not {@link String}, then the data will be stored in local memory and a unique
     * look-up key will be persisted.
     * <p/>
     * Use a {@link DBObject} to allow full MongoDB-friendly persistence, allowing
     * searching for <b>data.X</b>-style searches where the object has key *X*, say.
     * The data, if a {@link String}, will be persisted directly with this event.
     * <p/>
     * Events that keep their data in memory <i>cannot</i> be executed by any other
     * running service and will therefore be lost if the server goes down.
     * 
     * @param name                  the event name
     * @param scheduledTime         when the event should be processed
     * @param data                  the event data
     * @param dataInMemory          prevent the data from being persisted to the event storage.
     *                                      Incompatible data types will be handled automatically,
     *                                      but there may be cases where sensitive data may need to be kept
     *                                      in memory for some reason.
     */
    public Event(String name, long scheduledTime, Object data, boolean dataInMemory)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Event name may not be null.");
        }
        
        this.name = name;
        this.scheduledTime = scheduledTime;
        this.sessionId = null;
        // Record if the object should be forced to stay in memory
        this.data = data;
        this.dataInMemory = dataInMemory;
        
        // Certain event have to be unique within the context of the processing
        if (name.equals(Event.EVENT_NAME_START))
        {
            this.id = Event.EVENT_ID_START;
        }
    }

    @Override
    public String toString()
    {
        String dataStr = (data == null) ? null : data.toString();
        if (dataStr != null && dataStr.length() > 128)
        {
            dataStr = dataStr.substring(0, 127);
        }
        return "Event " +
                "[id=" + id +
                ", name=" + name +
                ", sessionId=" + sessionId +
                ", scheduledTime=" + scheduledTime +
                ", lockOwner=" + lockOwner +
                ", lockTime=" + lockTime +
                ", data=" + dataStr +
                ", dataInMemory=" + dataInMemory +
                "]";
    }

    @Override
    public int hashCode()
    {
        return (id == null) ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Event other = (Event) obj;
        if (id == null)
        {
            if (other.id != null) return false;
        }
        else if (!id.equals(other.id)) return false;
        return true;
    }

    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        if (id == null || !id.equals(id.toLowerCase()) || !ObjectId.isValid(id))
        {
            throw new IllegalArgumentException("Valid ID is 24 characters consisting of [0-9][a-f].");
        }
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public String getSessionId()
    {
        return sessionId;
    }
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public long getScheduledTime()
    {
        return scheduledTime;
    }

    public String getLockOwner()
    {
        return lockOwner;
    }
    public void setLockOwner(String lockOwner)
    {
        this.lockOwner = lockOwner;
    }

    public long getLockTime()
    {
        return lockTime;
    }
    public void setLockTime(long lockTime)
    {
        this.lockTime = lockTime;
    }

    public Object getData()
    {
        return data;
    }
    public boolean getDataInMemory()
    {
        return dataInMemory;
    }
    /**
     * @deprecated      in V2.0.2
     */
    @Deprecated
    public Object getDataObject()
    {
        return data;
    }
}
