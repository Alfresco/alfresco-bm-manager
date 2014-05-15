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
import java.util.List;

import org.alfresco.bm.event.AbstractResultService;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;

/**
 * A Mongo-based implementation of the results for benchmark test runs.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class MongoResultService extends AbstractResultService implements LifecycleListener
{
    private static Log logger = LogFactory.getLog(MongoResultService.class);

    private final DBCollection collection;

    /**
     * Construct a test result provider against a Mongo database and given collection name
     */
    public MongoResultService(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
    }
    
    @Override
    public void start() throws Exception
    {
        // Initialize indexes
        EventRecord.checkIndexes(this.collection);
    }

    @Override
    public void stop() throws Exception
    {
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
        
        String serverId = (String) eventRecordObj.get(EventRecord.FIELD_SERVER_ID);
        boolean success = eventRecordObj.containsField(EventRecord.FIELD_SUCCESS) ?
                (Boolean) eventRecordObj.get(EventRecord.FIELD_SUCCESS) :
                false;
        long startTime = eventRecordObj.containsField(EventRecord.FIELD_START_TIME) ?
                (Long) eventRecordObj.get(EventRecord.FIELD_START_TIME) :
                -1L;
        long time = eventRecordObj.containsField(EventRecord.FIELD_TIME) ?
                (Long) eventRecordObj.get(EventRecord.FIELD_TIME) :
                -1L;
        Object data = eventRecordObj.get(EventRecord.FIELD_DATA);
        String id = (String) eventRecordObj.get(EventRecord.FIELD_ID).toString();
        String warning = (String) eventRecordObj.get(EventRecord.FIELD_WARNING);
        boolean chart = eventRecordObj.containsField(EventRecord.FIELD_CHART) ?
                (Boolean) eventRecordObj.get(EventRecord.FIELD_CHART) :
                false;
        long startDelay = eventRecordObj.containsField(EventRecord.FIELD_START_DELAY) ?
                (Long) eventRecordObj.get(EventRecord.FIELD_START_DELAY) :
                -1L;
        
        // Extract the event
        DBObject eventObj = (DBObject) eventRecordObj.get(EventRecord.FIELD_EVENT);
        if (eventObj == null)
        {
            throw new IllegalArgumentException("DBObject for EventRecord does not contain Event data: " + eventRecordObj);
        }
        Event event = convertToEvent(eventObj);
                
        EventRecord eventRecord = new EventRecord(serverId, success, startTime, time, data, event);
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
    @SuppressWarnings("deprecation")
    private Event convertToEvent(DBObject eventObj)
    {
        String name = (String) eventObj.get(Event.FIELD_NAME);
        long scheduledTime = eventObj.containsField(Event.FIELD_SCHEDULED_TIME) ?
                (Long) eventObj.get(Event.FIELD_SCHEDULED_TIME) :
                -1L;
        Object data = eventObj.get(Event.FIELD_DATA);
        String dataKey = (String) eventObj.get(Event.FIELD_DATA_KEY);
        String dataOwner = (String) eventObj.get(Event.FIELD_DATA_OWNER);
        String lockOwner = (String) eventObj.get(Event.FIELD_LOCK_OWNER);
        String sessionId = (String) eventObj.get(Event.FIELD_SESSION_ID);
        
        Event event = new Event(name, scheduledTime, data, false);
        event.setDataKey(dataKey);
        event.setDataOwner(dataOwner);
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
        DBObject insertObj = BasicDBObjectBuilder
                .start()
                .add(EventRecord.FIELD_CHART, result.isChart())
                .add(EventRecord.FIELD_DATA, result.getData())
                .add(EventRecord.FIELD_SERVER_ID, result.getServerId())
                .add(EventRecord.FIELD_START_DELAY, result.getStartDelay())
                .add(EventRecord.FIELD_START_TIME, result.getStartTime())
                .add(EventRecord.FIELD_SUCCESS, result.isSuccess())
                .add(EventRecord.FIELD_TIME, result.getTime())
                .add(EventRecord.FIELD_WARNING, result.getWarning())
                .add(EventRecord.FIELD_EVENT, eventObj)
                .get();
        
        WriteResult wr = collection.insert(insertObj);
        if (wr.getError() != null)
        {
            throw new RuntimeException(
                    "Failed to insert event result:\n" +
                    "   Result: " + insertObj + "\n" +
                    "   Error:  " + wr);
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
                .add(EventRecord.FIELD_START_TIME, 1)
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
                .add(EventRecord.FIELD_START_TIME, -1)
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
                .add(EventRecord.FIELD_START_TIME, 1)
                .get();
        
        DBCursor cursor = collection.find(queryObj);
        cursor.sort(sortObj);
        cursor.skip(skip);
        cursor.limit(limit);
        
        // Get all the results and convert them
        int size = cursor.size();
        List<EventRecord> results = new ArrayList<EventRecord>(size);
        while (cursor.hasNext())
        {
            DBObject obj = cursor.next();
            EventRecord eventRecord = convertToEventRecord(obj);
            results.add(eventRecord);
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
            String eventName,
            Boolean successOrFail,
            int skip, int limit)
    {
        QueryBuilder queryBuilder = QueryBuilder
                .start()
                .and(EventRecord.FIELD_START_TIME).greaterThanEquals(startTime);
        if (eventName != null)
        {
            queryBuilder.and(EventRecord.FIELD_EVENT_NAME).is(eventName);
        }
        if (successOrFail != null)
        {
            queryBuilder.and(EventRecord.FIELD_SUCCESS).is(successOrFail);
        }
        DBObject queryObj = queryBuilder.get();
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(EventRecord.FIELD_START_TIME, 1)
                .get();
        
        DBCursor cursor = collection.find(queryObj);
        cursor.sort(sortObj);
        cursor.skip(skip);
        cursor.limit(limit);
        
        // Get all the results and convert them
        int size = cursor.size();
        List<EventRecord> results = new ArrayList<EventRecord>(size);
        while (cursor.hasNext())
        {
            DBObject obj = cursor.next();
            EventRecord eventRecord = convertToEventRecord(obj);
            results.add(eventRecord);
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
}
