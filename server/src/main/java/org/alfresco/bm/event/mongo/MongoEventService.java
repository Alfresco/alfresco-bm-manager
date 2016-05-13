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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
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
    private final String dataOwner;
    /**
     * Data storage for events that are unable to serialize their data to MongoDB storage
     * <p/>
     * TODO: Use a cache that expires entries so that any leaks by a test run are clean up eventually
     */
    private Map<String, Object> runLocalData = Collections.synchronizedMap(new HashMap<String, Object>(1024));

    /**
     * Construct a event service against a Mongo database and given collection name
     */
    public MongoEventService(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
        this.dataOwner = UUID.randomUUID().toString();
    }
    
    @Override
    public void start() throws Exception
    {
        // Initialize indexes
        DBObject idx_NEXT_AVAILABLE_EVENT_V2 = BasicDBObjectBuilder
                .start(Event.FIELD_SCHEDULED_TIME, Integer.valueOf(-1))
                .add(Event.FIELD_LOCK_OWNER, Integer.valueOf(1))
                .add(Event.FIELD_DATA_OWNER, Integer.valueOf(1))
                .add(Event.FIELD_DRIVER, Integer.valueOf(1))
                .get();
        DBObject opt_NEXT_AVAILABLE_EVENT_V2 = BasicDBObjectBuilder
                .start("name", "IDX_NEXT_AVAILABLE_EVENT_V2")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_NEXT_AVAILABLE_EVENT_V2, opt_NEXT_AVAILABLE_EVENT_V2);
        
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
        // If there are still items in the local data, then the test is probably not cleaning up property
        if (runLocalData.size() > 0)
        {
            logger.warn("EventService still has " + runLocalData.size() + " data entries held in memory.");
        }
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
        // Check the event
        if (event.getDataInMemory() && event.getDriver() != null)
        {
            throw new IllegalStateException("Events cannot be assigned a specific driver when they have their data bound in memory: " + event);
        }
        
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder
                .start();
        // Handle the data-key-data-owner link i.e. we store either the object or the key and owner of the key
        insertObjBuilder.add(Event.FIELD_DATA, event.getData());
        insertObjBuilder
                .add(Event.FIELD_LOCK_OWNER, event.getLockOwner())
                .add(Event.FIELD_LOCK_TIME, new Date(event.getLockTime()))
                .add(Event.FIELD_NAME, event.getName())
                .add(Event.FIELD_SCHEDULED_TIME, new Date(event.getScheduledTime()))
                .add(Event.FIELD_SESSION_ID, event.getSessionId())
                .add(Event.FIELD_DRIVER, event.getDriver());
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
    private Event convertDBObject(DBObject obj)
    {
        String id = obj.get(Event.FIELD_ID).toString();
        Object data = obj.get(Event.FIELD_DATA);
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
        String driver = (String) obj.get(Event.FIELD_DRIVER);
        
        // Check to see if we should be getting the data from memory
        if (dataOwner != null)
        {
            if (data != null)
            {
                throw new IllegalStateException("Event should not be stored with data AND a data owner: " + obj);
            }
            // The data was tagged as being held in VM
            data = runLocalData.get(id);
            if (data == null)
            {
                throw new IllegalStateException("Event data is not available in the VM: " + obj);
            }
        }
        
        Event event = new Event(name, scheduledTime, data);
        event.setId(id);
        event.setLockOwner(lockOwner);
        event.setLockTime(lockTime);
        event.setSessionId(sessionId);
        event.setDriver(driver);
        
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

        // Was the event's ID supplied to us
        ObjectId eventIdObj = (ObjectId) insertObj.get(Event.FIELD_ID);
        if (eventIdObj == null)
        {
            // We make up an ID here so that we can record the in-memory object, if necessary
            eventIdObj = new ObjectId();
            insertObj.put(Event.FIELD_ID, eventIdObj);
        }
        String eventId = eventIdObj.toString();
        
        // Replace the data with a key, if necessary
        Object data = event.getData();
        boolean storeInMem = event.getDataInMemory();
        if (storeInMem && data != null)
        {
            // We will only store the data if the insertion works
            insertObj.put(Event.FIELD_DATA_OWNER, dataOwner);
            insertObj.removeField(Event.FIELD_DATA);
            // The data will be removed when the event is plucked for processing
            // If we do an insert before putting this in the map, then it's possible for another
            // thread to pull the event before the local data is even in the map.
            runLocalData.put(eventId, data);
        }
        
        try
        {
            collection.insert(insertObj);
        }
        catch (MongoException e)
        {
            runLocalData.remove(eventId);
            throw new RuntimeException(
                    "Failed to insert event:\n" +
                    "   Event: " + insertObj,
                    e);
        }

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

    @Override
    public List<Event> getEvents(int skip, int limit)
    {
        DBCursor cursor = collection.find().skip(skip).limit(limit);
        List<Event> events = new ArrayList<Event>(limit);
        while (cursor.hasNext())
        {
            DBObject eventObj = cursor.next();
            Event event = convertDBObject(eventObj);
            events.add(event);
        }
        cursor.close();
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Fetched " + events.size() + "events.");
        }
        return events;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event nextEvent(String driverId, long latestScheduledTime)
    {
        // Build query
        BasicDBObjectBuilder qb = BasicDBObjectBuilder
                .start()
                .push(Event.FIELD_SCHEDULED_TIME)                   // Must be scheduled to execute
                    .add("$lte", new Date(latestScheduledTime))
                    .pop()
                .add(Event.FIELD_LOCK_OWNER, null)                  // Must not be locked
                .push(Event.FIELD_DATA_OWNER)                       // We must own the data it or it must be unowned
                    .add("$in", new String[] {dataOwner, null})
                    .pop();
        if (driverId != null)
        {
            qb.push(Event.FIELD_DRIVER)                             // Must be assigned to the given driver or must be unassigned
                .add("$in", new String[] {driverId, null})
                .pop();
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
                    .add(Event.FIELD_LOCK_OWNER, dataOwner)
                    .add(Event.FIELD_LOCK_TIME, new Date(now))
                .pop()
                .get();

        DBObject oldObj = collection.findAndModify(queryObj, sortObj, updateObj);
        // Make sure we return the event, as modified
        Event event = null;
        if (oldObj != null)
        {
            event = convertDBObject(oldObj);
            event.setLockOwner(dataOwner);
            event.setLockTime(now);
        }
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("\n" +
                    "Fetched next event (no lock present): \n" +
                    "   Latest scheduled time:  " + latestScheduledTime + "\n" +
                    "   Driver ID:              " + driverId + "\n" +
                    "   Event: " + event);
        }
        return event;
    }
    
    @Override
    public boolean deleteEvent(Event event)
    {
        String id = event.getId();
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(Event.FIELD_ID, new ObjectId(id))
                .get();
        // Drop any associated memory data
        runLocalData.remove(id);
        
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
    
    @Override
    public boolean clear()
    {
        try
        {
            this.collection.drop();
            return true;
        }
        catch(MongoException mex)
        {
            logger.error("Unable to drop colection '" + this.collection.getName() + "'");
            return false;
        }
    }

}
