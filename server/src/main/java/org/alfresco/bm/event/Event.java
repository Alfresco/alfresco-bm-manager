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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    public static final String FIELD_SCHEDULED_TIME = "scheduledTime";
    public static final String FIELD_LOCK_OWNER = "lockOwner";
    public static final String FIELD_LOCK_TIME = "lockTime";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_DATA_KEY = "dataKey";
    public static final String FIELD_DATA_OWNER = "dataOwner";
    
    /** The 'value' that is associated with the data */
    public static final String FIELD_VALUE = "value";

    public static final String EVENT_NAME_START = "start";
    
    public static final String EVENT_ID_START = "000000000000000000000001";
    
    public static final String EVENT_BEAN_PREFIX = "event.";
    
    /**
     * Data storage for events that are unable to serialize their data to local storage
     */
    private static Map<String, Object> serverLocalData = Collections.synchronizedMap(new HashMap<String, Object>(1024));
    
    private String id;
    private final String name;
    private String sessionId;
    private final long scheduledTime;
    private String lockOwner;
    private long lockTime;
    private Object data;            // Either a DBObject or a String
    private String dataKey;
    private String dataOwner;
    
    /**
     * Construct an event with some data, scheduling it for the current time.
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
     * Construct a scheduled event with some data.  The data, if a {@link String},
     * will be persisted directly with this event.
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
     * Events that keep their data in memory <i>cannot</i> be executed by any other
     * running service and will therefore be lost if the server goes down.
     * 
     * @param name                  the event name
     * @param scheduledTime         when the event should be processed
     * @param data                  the event data
     */
    public Event(String name, long scheduledTime, Object data, boolean forceInMemoryDataStorage)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Event name may not be null.");
        }
        
        this.name = name;
        this.scheduledTime = scheduledTime;
        this.sessionId = null;
        // Need to use Serializable-aware setter
        setDataObject(data, forceInMemoryDataStorage);
        
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
                ", dataKey=" + dataKey +
                "  dataOwner= " + dataOwner +
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

    /**
     * Client-safe method to retrieve data based on in-memory or persisted storage
     */
    public Object getDataObject()
    {
        if (dataKey == null)
        {
            // The data came in and out of persisted storage
            return data;
        }
        else
        {
            Object data = Event.serverLocalData.get(dataKey);
            if (data == null)
            {
                // There is no value.  Check whether we have lost it.
                if (!Event.serverLocalData.containsKey(dataKey))
                {
                    // We have lost the data associated with this
                    throw new IllegalStateException("In-memory data cannot be found for event: " + this);
                }
            }
            return data;
        }
    }
    /**
     * Set the data, taking Serializability or client choice into account
     * 
     * @param data                      the data to store (in memory or persisted)
     * @param forceInMemoryDataStorage  <tt>true</tt> to force in-memory storage
     */
    private void setDataObject(Object data, boolean forceInMemoryDataStorage)
    {
        boolean canPersistData =
                data == null ||
                data instanceof String ||
                data instanceof DBObject ||
                data instanceof Number;
        
        if (forceInMemoryDataStorage || !canPersistData)
        {
            // We have to store the data in memory and just persist a key
            this.data = null;
            this.dataKey = UUID.randomUUID().toString();
            Event.serverLocalData.put(this.dataKey, data);
        }
        else
        {
            // A null, String or DBObject is provided, so we just accept it as is
            this.data = data;
            this.dataKey = null;
        }
    }

    /**
     * @return                      the data key in the event that data is stored in the VM only
     */
    public String getDataKey()
    {
        return dataKey;
    }
    /**
     * Method used by persistence framework ONLY.  DO NOT USE.
     */
    @Deprecated
    public void setDataKey(String dataKey)
    {
        this.dataKey = dataKey;
    }

    /**
     * Ensure that any locally-stored data is cleaned up
     */
    public void cleanData()
    {
        if (dataKey != null)
        {
            Event.serverLocalData.remove(dataKey);
        }
    }

    public String getDataOwner()
    {
        return dataOwner;
    }
    public void setDataOwner(String dataOwner)
    {
        this.dataOwner = dataOwner;
    }
}
