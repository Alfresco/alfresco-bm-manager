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
package org.alfresco.bm.event.mongo;

import java.util.Date;

import org.alfresco.bm.event.AbstractEventService;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventService;
import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;

/**
 * An {@link EventService} <b>MongoDB</b> collection
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class MongoEventService extends AbstractEventService implements LifecycleListener
{
    private static Log logger = LogFactory.getLog(MongoEventService.class);

    private final DBCollection collection;

    /**
     * Construct a event service against a Mongo database and given collection name
     */
    public MongoEventService(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
    }
    
    @Override
    public void start() throws Exception
    {
        // Initialize indexes
        DBObject idx_NEXT_AVAILABLE_EVENT = BasicDBObjectBuilder
                .start(Event.FIELD_SCHEDULED_TIME, Integer.valueOf(-1))
                .add(Event.FIELD_LOCK_OWNER, Integer.valueOf(1))
                .add(Event.FIELD_DATA_OWNER, Integer.valueOf(1))
                .get();
        DBObject opt_NEXT_AVAILABLE_EVENT = BasicDBObjectBuilder
                .start("name", "IDX_NEXT_AVAILABLE_EVENT")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_NEXT_AVAILABLE_EVENT, opt_NEXT_AVAILABLE_EVENT);
        
        DBObject idx_NAME = BasicDBObjectBuilder
                .start(Event.FIELD_NAME, Integer.valueOf(1))
                .get();
        DBObject opt_NAME = BasicDBObjectBuilder
                .start("name", "IDX_NAME")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_NAME, opt_NAME);
    }

    @Override
    public void stop() throws Exception
    {
    }

    @Override
    public long count()
    {
        return collection.count();
    }

    /**
     * Helper method to convert an {@link Event} into a {@link DBObject persistable object}
     */
    public static DBObject convertEvent(Event event)
    {
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder
                .start();
        // Handle the data-key-data-owner link i.e. we store either the object or the key and owner of the key
        if (event.getDataKey() == null)
        {
            insertObjBuilder.add(Event.FIELD_DATA, event.getDataObject());
        }
        else
        {
            insertObjBuilder.add(Event.FIELD_DATA_KEY, event.getDataKey());
            insertObjBuilder.add(Event.FIELD_DATA_OWNER, event.getDataOwner());
        }
        insertObjBuilder
                .add(Event.FIELD_LOCK_OWNER, event.getLockOwner())
                .add(Event.FIELD_LOCK_TIME, new Date(event.getLockTime()))
                .add(Event.FIELD_NAME, event.getName())
                .add(Event.FIELD_SCHEDULED_TIME, new Date(event.getScheduledTime()))
                .add(Event.FIELD_SESSION_ID, event.getSessionId());
        DBObject insertObj = insertObjBuilder.get();
        // Handle explicit setting of the ID
        if (event.getId() != null)
        {
            insertObj.put(Event.FIELD_ID, new ObjectId(event.getId()));
        }
        return insertObj;
    }
    
    /**
     * Helper method to convert a {@link DBObject persistable object} into an {@link Event}
     */
    @SuppressWarnings("deprecation")
    public static Event convertDBObject(DBObject obj)
    {
        String id = obj.get(Event.FIELD_ID).toString();
        Object data = obj.get(Event.FIELD_DATA);
        String dataKey = (String) obj.get(Event.FIELD_DATA_KEY);
        String dataOwner = (String) obj.get(Event.FIELD_DATA_OWNER);
        String lockOwner = (String) obj.get(Event.FIELD_LOCK_OWNER);
        long lockTime = obj.containsField(Event.FIELD_LOCK_TIME) ?
                ((Date) obj.get(Event.FIELD_LOCK_TIME)).getTime() :
                Long.valueOf(0L);
        String name = (String) obj.get(Event.FIELD_NAME);
        long scheduledTime = obj.containsField(Event.FIELD_SCHEDULED_TIME) ?
                ((Date) obj.get(Event.FIELD_SCHEDULED_TIME)).getTime() :
                Long.valueOf(0L);
        String sessionId = (String) obj.get(Event.FIELD_SESSION_ID);
        
        Event event = new Event(name, scheduledTime, data);
        event.setId(id);
        event.setDataKey(dataKey);
        event.setDataOwner(dataOwner);
        event.setLockOwner(lockOwner);
        event.setLockTime(lockTime);
        event.setSessionId(sessionId);
        // Done
        return event;
    }
    
    @Override
    public String putEvent(Event event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("'event' may not be null.");
        }
        DBObject insertObj = convertEvent(event);
        
        try
        {
            collection.insert(insertObj);
        }
        catch (MongoException e)
        {
            throw new RuntimeException(
                    "Failed to insert event:\n" +
                    "   Event: " + insertObj,
                    e);
        }
        // Extract the ID
        ObjectId eventIdObj = (ObjectId) insertObj.get(Event.FIELD_ID);
        String eventId = eventIdObj.toString();
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Put event: " + insertObj);
        }
        return eventId;
    }

    @Override
    public Event getEvent(String id)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(Event.FIELD_ID, new ObjectId(id))
                .get();
        DBObject eventObj = collection.findOne(queryObj);
        Event event = null;
        if (eventObj != null)
        {
            event = convertDBObject(eventObj);
        }
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Fetched event for ID '" + id + ": " + event);
        }
        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event nextEvent(String serverId, long latestScheduledTime, boolean localDataOnly)
    {
        if (serverId == null)
        {
            throw new IllegalArgumentException("'serverId' must be supplied.");
        }
        
        // Build query
        QueryBuilder qb = QueryBuilder
                .start()
                .and(Event.FIELD_SCHEDULED_TIME).lessThanEquals(new Date(latestScheduledTime))
                .and(Event.FIELD_LOCK_OWNER).is(null);
        if (localDataOnly)
        {
            qb.and(Event.FIELD_DATA_OWNER).is(serverId);
        }
        else
        {
            qb.or(
                    BasicDBObjectBuilder.start().add(Event.FIELD_DATA_OWNER, serverId).get(),
                    BasicDBObjectBuilder.start().add(Event.FIELD_DATA_OWNER, null).get());
        }
        DBObject queryObj = qb.get();
        // Build sort
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(Event.FIELD_SCHEDULED_TIME, Integer.valueOf(1))
                .get();
        // Build update
        long now = System.currentTimeMillis();
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .push("$set")
                    .add(Event.FIELD_LOCK_OWNER, serverId)
                    .add(Event.FIELD_LOCK_TIME, now)
                .pop()
                .get();

        DBObject oldObj = collection.findAndModify(queryObj, sortObj, updateObj);
        // Make sure we return the event, as modified
        Event event = null;
        if (oldObj != null)
        {
            event = convertDBObject(oldObj);
            event.setLockOwner(serverId);
            event.setLockTime(now);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("\n" +
                    "Fetched next event (no lock present): \n" +
                    "   Latest scheduled time:  " + latestScheduledTime + "\n" +
                    "   Server ID:              " + serverId + "\n" +
                    "   Event: " + event);
        }
        return event;
    }
    
    @Override
    public boolean deleteEvent(Event event)
    {
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(Event.FIELD_ID, new ObjectId(event.getId()))
                .get();
        
        WriteResult wr = collection.remove(queryObj);
        if (wr.getN() != 1)
        {
            // Done
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Failed to removed event: \n" +
                        "   Event:  " + event + "\n" +
                        "   Result: " + wr);
            }
            return false;
        }
        else
        {
            // Done
            if (logger.isDebugEnabled())
            {
                logger.debug("Removed event: " + event);
            }
            return true;
        }
    }
}
