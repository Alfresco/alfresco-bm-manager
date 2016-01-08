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
import java.util.Date;
import java.util.List;

import org.alfresco.bm.api.v1.EventDetails;
import org.alfresco.bm.api.v1.EventResultFilter;
import org.alfresco.bm.event.AbstractResultService;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.QueryBuilder;

/**
 * A Mongo-based implementation of the results for benchmark test runs.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class MongoResultService extends AbstractResultService implements LifecycleListener
{
    private static Log logger = LogFactory.getLog(MongoResultService.class);

    private DBCollection collection;
    private boolean checkIndexes = false;

    /**
     * Construct a test result provider against a Mongo database and given collection name
     */
    public MongoResultService(DB db, String collection)
    {
        try
        {
            this.collection = db.createCollection(collection, new BasicDBObject());
            checkIndexes = true;
        }
        catch (MongoCommandException e)
        {
            if (!db.collectionExists(collection))
            {
                // The collection is really not there
                throw e;
            }
            // Someone else created it
            this.collection = db.getCollection(collection);
            this.checkIndexes = false;
        }
    }
    
    @Override
    public void start() throws Exception
    {
        checkIndexes();
    }
    
    private void checkIndexes() throws Exception
    {
        if (!checkIndexes)
        {
            return;
        }
        
        // Initialize indexes
        DBObject idx_EVENT_NAME_START = BasicDBObjectBuilder
                .start(EventRecord.FIELD_EVENT_NAME, Integer.valueOf(1))
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();
        DBObject opt_EVENT_NAME_START = BasicDBObjectBuilder
                .start("name", "IDX_EVENT_NAME_START")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_EVENT_NAME_START, opt_EVENT_NAME_START);
        
        DBObject idx_SUCCESS_START = BasicDBObjectBuilder
                .start(EventRecord.FIELD_SUCCESS, Integer.valueOf(1))
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();
        DBObject opt_SUCCESS_START = BasicDBObjectBuilder
                .start("name", "IDX_SUCCESS_START")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_SUCCESS_START, opt_SUCCESS_START);
        
        DBObject idx_START = BasicDBObjectBuilder
                .start(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();
        DBObject opt_START = BasicDBObjectBuilder
                .start("name", "IDX_START")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_START, opt_START);
        
        DBObject idx_SESSION_START = BasicDBObjectBuilder
                .start(EventRecord.FIELD_EVENT_SESSION_ID, Integer.valueOf(1))
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();
        DBObject opt_SESSION_START = BasicDBObjectBuilder
                .start("name", "IDX_SESSION_START")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idx_SESSION_START, opt_SESSION_START);
    }

    @Override
    public void stop() throws Exception
    {
    }

    /**
     * Creates an {@see EventDetails} object from the MongoDB record
     * 
     * @param eventDetailsObj (DBObject)
     * 
     * @return EventDetails
     */
    private EventDetails convertToEventDetails(DBObject eventDetailsObj)
    {
        // get event date
        Date startTime = eventDetailsObj.containsField(EventRecord.FIELD_START_TIME) ?
                (Date) eventDetailsObj.get(EventRecord.FIELD_START_TIME) :
                new Date(0L);

        // get event success 
        boolean success = eventDetailsObj.containsField(EventRecord.FIELD_SUCCESS) ?
                (Boolean) eventDetailsObj.get(EventRecord.FIELD_SUCCESS) :
                false;
                
        // get inputData
        Object eventData = eventDetailsObj.get(EventRecord.FIELD_DATA);       
        
        // get Event
        DBObject eventObj = (DBObject) eventDetailsObj.get(EventRecord.FIELD_EVENT);
        if (eventObj == null)
        {
            throw new IllegalArgumentException("DBObject for EventDetails does not contain Event data: " + eventDetailsObj);
        }
        Event event = convertToEvent(eventObj);        
        
        // get event name
        String name = event.getName();
        
        // get Event.Data
        Object inputData = event.getData();
        
        return new EventDetails(startTime, name, success, inputData, eventData);
    }
    
    /**
     * Helper to convert Mongo-persisted object to a client-visible {@link EventRecord}
     */
    private EventRecord convertToEventRecord(DBObject eventRecordObj)
    {
        if (eventRecordObj == null)
        {
            return null;
        }
        
        String processedBy = (String) eventRecordObj.get(EventRecord.FIELD_PROCESSED_BY);
        String driverId = (String) eventRecordObj.get(EventRecord.FIELD_DRIVER_ID);
        if (driverId == null)
        {
           // data model is too old
            throw new IllegalArgumentException("DBObject for EventRecord doesn't contain a driver ID. The data model may be too old!");
        }
        boolean success = eventRecordObj.containsField(EventRecord.FIELD_SUCCESS) ?
                (Boolean) eventRecordObj.get(EventRecord.FIELD_SUCCESS) :
                false;
        long startTime = eventRecordObj.containsField(EventRecord.FIELD_START_TIME) ?
                ((Date) eventRecordObj.get(EventRecord.FIELD_START_TIME)).getTime() :
                Long.valueOf(0L);
        long time = eventRecordObj.containsField(EventRecord.FIELD_TIME) ?
                (Long) eventRecordObj.get(EventRecord.FIELD_TIME) :
                Long.valueOf(-1L);
        Object data = eventRecordObj.get(EventRecord.FIELD_DATA);
        String id = (String) eventRecordObj.get(EventRecord.FIELD_ID).toString();
        String warning = (String) eventRecordObj.get(EventRecord.FIELD_WARNING);
        boolean chart = eventRecordObj.containsField(EventRecord.FIELD_CHART) ?
                (Boolean) eventRecordObj.get(EventRecord.FIELD_CHART) :
                false;
        long startDelay = eventRecordObj.containsField(EventRecord.FIELD_START_DELAY) ?
                (Long) eventRecordObj.get(EventRecord.FIELD_START_DELAY) :
                Long.valueOf(-1L);
        
        // Extract the event
        DBObject eventObj = (DBObject) eventRecordObj.get(EventRecord.FIELD_EVENT);
        if (eventObj == null)
        {
            throw new IllegalArgumentException("DBObject for EventRecord does not contain Event data: " + eventRecordObj);
        }
        Event event = convertToEvent(eventObj);
                
        EventRecord eventRecord = new EventRecord(driverId, success, startTime, time, data, event);
        eventRecord.setProcessedBy(processedBy);
        eventRecord.setId(id);
        eventRecord.setWarning(warning);
        eventRecord.setChart(chart);
        eventRecord.setStartDelay(startDelay);
        // Done
        if (logger.isTraceEnabled())
        {
            logger.trace("Converted EventRecord: \n" +
                    "   In:  " + eventRecordObj + "\n" +
                    "   Out: " + eventRecord);
        }
        return eventRecord;
    }
    
    /**
     * Helper to convert Mongo-persisted object to a client-visible {@link Event}
     */
    private Event convertToEvent(DBObject eventObj)
    {
        String name = (String) eventObj.get(Event.FIELD_NAME);
        long scheduledTime = eventObj.containsField(Event.FIELD_SCHEDULED_TIME) ?
                ((Date) eventObj.get(Event.FIELD_SCHEDULED_TIME)).getTime() :
                -1L;
        Object data = eventObj.get(Event.FIELD_DATA);
        String lockOwner = (String) eventObj.get(Event.FIELD_LOCK_OWNER);
        String sessionId = (String) eventObj.get(Event.FIELD_SESSION_ID);
        
        Event event = new Event(name, scheduledTime, data, false);
        event.setLockOwner(lockOwner);
        event.setSessionId(sessionId);
        // Done
        if (logger.isTraceEnabled())
        {
            logger.trace("Converted Event: \n" +
                    "   In:  " + eventObj + "\n" +
                    "   Out: " + event);
        }
        return event;
    }

    @Override
    public void recordResult(EventRecord result)
    {
        if (result == null)
        {
            throw new IllegalArgumentException("EventRecord may not be null.");
        }
        
        Event event = result.getEvent();
        if (event == null)
        {
            throw new IllegalArgumentException("EventRecord must contain an Event.");
        }
        
        DBObject eventObj = MongoEventService.convertEvent(event);
        // Remove the event data if it is not persistable
        if (event.getDataInMemory())
        {
            eventObj.removeField(Event.FIELD_DATA);
        }
        // Remove data that is captured in the result
        eventObj.removeField(Event.FIELD_DATA_OWNER);           // This is covered by the processedBy
        eventObj.removeField(Event.FIELD_ID);                   // Internal and not required
        eventObj.removeField(Event.FIELD_SCHEDULED_TIME);       // This is the (startTime - startDelay)
        eventObj.removeField(Event.FIELD_LOCK_TIME);            // Locking was an internal function
        eventObj.removeField(Event.FIELD_LOCK_OWNER);           // Locking was an internal function
        
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder
                .start()
                .add(EventRecord.FIELD_PROCESSED_BY, result.getProcessedBy())
                .add(EventRecord.FIELD_CHART, result.isChart())
                .add(EventRecord.FIELD_DATA, result.getData())
                .add(EventRecord.FIELD_DRIVER_ID, result.getDriverId())
                .add(EventRecord.FIELD_START_DELAY, result.getStartDelay())
                .add(EventRecord.FIELD_START_TIME, new Date(result.getStartTime()))
                .add(EventRecord.FIELD_SUCCESS, result.isSuccess())
                .add(EventRecord.FIELD_TIME, result.getTime())
                .add(EventRecord.FIELD_EVENT, eventObj);
        if (result.getWarning() != null)
        {
            insertObjBuilder.add(EventRecord.FIELD_WARNING, result.getWarning());
        }
        DBObject insertObj = insertObjBuilder.get();
        
        try
        {
            collection.insert(insertObj);
        }
        catch (MongoException e)
        {
            throw new RuntimeException(
                    "Failed to insert event result:\n" +
                    "   Result: " + insertObj,
                    e);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Recorded result: " + insertObj);
        }
    }
    
    @Override
    public String getDataLocation()
    {
        return collection.getFullName();
    }

    @Override
    public EventRecord getFirstResult()
    {
        DBObject sortObj = BasicDBObjectBuilder.start()
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();

        DBObject resultObj = collection.findOne(null, null, sortObj);
        EventRecord result = convertToEventRecord(resultObj);

        if (logger.isDebugEnabled())
        {
            logger.debug("Found first result: " + result);
        }
        return result;
    }
    
    @Override
    public EventRecord getLastResult()
    {
        DBObject sortObj = BasicDBObjectBuilder.start()
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(-1))
                .get();

        DBObject resultObj = collection.findOne(null, null, sortObj);
        EventRecord result = convertToEventRecord(resultObj);

        if (logger.isDebugEnabled())
        {
            logger.debug("Found last result: " + result);
        }
        return result;
    }

    @Override
    public List<EventRecord> getResults(String eventName, int skip, int limit)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .get();
        if (eventName != null)
        {
            queryObj.put(EventRecord.FIELD_EVENT_NAME, eventName);
        }
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();
        
        DBCursor cursor = collection.find(queryObj);
        cursor.sort(sortObj);
        cursor.skip(skip);
        cursor.limit(limit);
        
        // Get all the results and convert them
        int size = cursor.size();
        List<EventRecord> results = new ArrayList<EventRecord>(size);
        try
        {
            while (cursor.hasNext())
            {
                DBObject obj = cursor.next();
                EventRecord eventRecord = convertToEventRecord(obj);
                results.add(eventRecord);
            }
        }
        finally
        {
            cursor.close();
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("\n" +
                    "Found results: \n" +
                    "   Query:          " + queryObj + "\n" +
                    "   Skip:           " + skip + "\n" +
                    "   Limit:          " + limit + "\n" +
                    "   Results:        " + size);
        }
        return results;
    }
    
    @Override
    public List<EventRecord> getResults(
            long startTime,
            long endTime,
            boolean chartOnly,
            int skip, int limit)
    {
        QueryBuilder queryBuilder = QueryBuilder
                .start()
                .and(EventRecord.FIELD_START_TIME).greaterThanEquals(new Date(startTime))
                .and(EventRecord.FIELD_START_TIME).lessThan(new Date(endTime));
        if (chartOnly)
        {
            queryBuilder.and(EventRecord.FIELD_CHART).is(true);
        }
        DBObject queryObj = queryBuilder.get();
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(1))
                .get();
        
        DBCursor cursor = collection.find(queryObj);
        cursor.sort(sortObj);
        cursor.skip(skip);
        cursor.limit(limit);
        
        // Get all the results and convert them
        int size = cursor.size();
        List<EventRecord> results = new ArrayList<EventRecord>(size);
        try
        {
            while (cursor.hasNext())
            {
                DBObject obj = cursor.next();
                EventRecord eventRecord = convertToEventRecord(obj);
                results.add(eventRecord);
            }
        }
        finally
        {
            cursor.close();
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("\n" +
                    "Found results: \n" +
                    "   Query:          " + queryObj + "\n" +
                    "   Skip:           " + skip + "\n" +
                    "   Limit:          " + limit + "\n" +
                    "   Results:        " + size);
        }
        return results;
    }

    @Override
    public List<String> getEventNames()
    {
        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) collection.distinct(EventRecord.FIELD_EVENT_NAME);
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("\n" +
                    "Distinct event names: \n" +
                    "   Results:        " + results);
        }
        return results;
    }

    @Override
    public long countResults()
    {
        long count = collection.count();

        // Done
        if(logger.isDebugEnabled())
        {
            logger.debug("Counted " + count + " results.");
        }
        return count;
    }

    @Override
    public long countResultsByEventName(String name)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(EventRecord.FIELD_EVENT_NAME).is(name)
                .get();
        
        long count = collection.count(queryObj);

        // Done
        if(logger.isDebugEnabled())
        {
            logger.debug("Counted " + count + " results for event name: " + name);
        }
        return count;
    }

    @Override
    public long countResultsBySuccess()
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(EventRecord.FIELD_SUCCESS).is(true)
                .get();
        
        long count = collection.count(queryObj);

        // Done
        if(logger.isDebugEnabled())
        {
            logger.debug("Counted " + count + " results for success: " + true);
        }
        return count;
    }

    @Override
    public long countResultsByFailure()
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(EventRecord.FIELD_SUCCESS).is(false)
                .get();
        
        long count = collection.count(queryObj);

        // Done
        if(logger.isDebugEnabled())
        {
            logger.debug("Counted " + count + " results for success: " + false);
        }
        return count;
    }

    @Override
    public List<EventDetails> getEventDetails(EventResultFilter filter, String filterEventName, int skip, int limit)
    {
        QueryBuilder queryBuilder = QueryBuilder.start();
        
        // apply filter
        switch (filter)
        {
            case Failed:
                queryBuilder.and(EventRecord.FIELD_SUCCESS).is(false);
                break;
            
            case Success:
                queryBuilder.and(EventRecord.FIELD_SUCCESS).is(true);
                break;
            default:
                break;
        }
        
        //apply event name filter
        if (null != filterEventName && !filterEventName.isEmpty())
        {
            queryBuilder.and(EventRecord.FIELD_EVENT_NAME).is(filterEventName);
        }
        
        DBObject queryObj = queryBuilder.get();
        // sort descending to get the newest values first
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(EventRecord.FIELD_START_TIME, Integer.valueOf(-1))
                .get();
        DBCursor cursor = collection.find(queryObj);
        cursor.sort(sortObj);
        cursor.skip(skip);
        cursor.limit(limit);
        
        // Get all the results and convert them
        int size = cursor.size();
        List<EventDetails> results = new ArrayList<EventDetails>(size);
        try
        {
            while (cursor.hasNext())
            {
                DBObject obj = cursor.next();
                EventDetails eventDetails = convertToEventDetails(obj);
                results.add(eventDetails);
            }
        }
        finally
        {
            cursor.close();
        }
        
        return results;
    }
}
