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
package org.alfresco.bm.log;

import java.util.Date;

import org.alfresco.bm.test.LifecycleListener;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.CommandFailureException;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Mongo implementation of service providing log message persistence
 *
 * @author Derek Hulley
 * @since 2.0
 */
public class MongoLogService implements LifecycleListener, LogService
{
    public static final String COLLECTION_LOGS = "test.logs";
    
    public static final String FIELD_ID = "_id";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_DRIVER_ID = "d_id";
    public static final String FIELD_TEST = "t";
    public static final String FIELD_TEST_RUN = "tr";
    public static final String FIELD_LEVEL = "level";
    public static final String FIELD_MSG = "msg";
    
    private DBCollection collection;
    private final int ttl;
    
    /**
     * Construct an instance providing the DB and collection name to use
     * 
     * @param db            the database to use
     * @param size          the size (bytes) to cap the log size at or 0 to ignore.
     *                      This must be zero if the TTL is set.
     * @param max           the maximum number of log entries or 0 to ignore.
     *                      This must be zero if the TTL is set.
     * @param ttl           the time to live (seconds) of a log message or 0 to ignore.
     *                      This must be zero or less if the logs are capped by size or max entries.
     */
    public MongoLogService(DB db, long size, int max, int ttl)
    {
        try
        {
            BasicDBObjectBuilder optionsBuilder = BasicDBObjectBuilder.start();
            if (size > 0L)
            {
                optionsBuilder.add("capped", true);
                optionsBuilder.add("size", size);
                if (max > 0L)
                {
                    optionsBuilder.add("max", max);
                }
                if (ttl > 0)
                {
                    throw new IllegalArgumentException("The log collection can only be capped by size, max entries or time to live.");
                }
            }
            else if (max > 0L)
            {
                throw new IllegalArgumentException("The logs must always be capped by size before capping by number.");
            }
            DBObject options = optionsBuilder.get();
            this.collection = db.createCollection(COLLECTION_LOGS, options);
        }
        catch (CommandFailureException e)
        {
            // Double check
            if (!db.collectionExists(COLLECTION_LOGS))
            {
                // The collection is not there so it was some other issue
                throw e;
            }
            this.collection = db.getCollection(COLLECTION_LOGS);
        }
        this.ttl = ttl;
    }
    
    @Override
    public void start() throws Exception
    {
        checkIndexes();
    }

    @Override
    public void stop() throws Exception
    {
    }

    /**
     * Ensure that the MongoDB collection has the required indexes
     */
    private void checkIndexes()
    {
        // Ensure ordering and TTL
        DBObject idxTime = BasicDBObjectBuilder.start()
                .add(FIELD_TIME, -1)
                .get();
        DBObject optTime = BasicDBObjectBuilder.start()
                .add("unique", Boolean.FALSE)
                .get();
        if (ttl > 0)
        {
            optTime.put("expireAfterSeconds", ttl);
        }
        collection.createIndex(idxTime, optTime);

        // Select by server, order by time
        DBObject idxDriverTime = BasicDBObjectBuilder.start()
                .add(FIELD_DRIVER_ID, 1)
                .add(FIELD_TIME, -1)
                .get();
        DBObject optDriverTime = BasicDBObjectBuilder.start()
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxDriverTime, optDriverTime);

        // Select by test, order by time
        DBObject idxTestTime = BasicDBObjectBuilder.start()
                .add(FIELD_TEST, 1)
                .add(FIELD_TIME, -1)
                .get();
        DBObject optTestTime = BasicDBObjectBuilder.start()
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxTestTime, optTestTime);

        // Select by test run, order by time
        DBObject idxTestRunTime = BasicDBObjectBuilder.start()
                .add(FIELD_TEST_RUN, 1)
                .add(FIELD_TIME, -1)
                .get();
        DBObject optTestRunTime = BasicDBObjectBuilder.start()
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxTestRunTime, optTestRunTime);
    }

    @Override
    public void log(String driverId, String test, String testRun, LogLevel level, String msg)
    {
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_TIME, new Date())
                .add(FIELD_LEVEL, level.getLevel())
                .add(FIELD_MSG, msg);
        if (driverId != null)
        {
            insertObjBuilder.add(FIELD_DRIVER_ID, driverId);
        }
        if (test != null)
        {
            insertObjBuilder.add(FIELD_TEST, test);
        }
        if (testRun != null)
        {
            insertObjBuilder.add(FIELD_TEST_RUN, testRun);
        }
        DBObject insertObj = insertObjBuilder.get();
        
        collection.insert(insertObj);
    }

    @Override
    public DBCursor getLogs(String driverId, String test, String testRun, LogLevel level, Long minTime, Long maxTime, int skip, int limit)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (level != null)
        {
            queryObjBuilder.push(FIELD_LEVEL).add("$gte", level.getLevel()).pop();
        }
        if (driverId != null)
        {
            queryObjBuilder.add(FIELD_DRIVER_ID, driverId);
        }
        if (test != null)
        {
            queryObjBuilder.add(FIELD_TEST, test);
        }
        if (testRun != null)
        {
            queryObjBuilder.add(FIELD_TEST_RUN, testRun);
        }
        if (minTime != null || maxTime != null)
        {
            queryObjBuilder.push(FIELD_TIME);
            if (minTime != null)
            {
                queryObjBuilder.add("$gte", new Date(minTime));
            }
            if (maxTime != null)
            {
                queryObjBuilder.add("$lt", new Date(maxTime));
            }
            queryObjBuilder.pop();
        }
        DBObject queryObj = queryObjBuilder.get();
        DBObject sortObj = new BasicDBObject(FIELD_TIME, -1);
        DBObject fieldsObj = BasicDBObjectBuilder.start()
                .add(FIELD_ID, false)
                .add(FIELD_TIME, true)
                .add(FIELD_DRIVER_ID, true)
                .add(FIELD_TEST, true)
                .add(FIELD_TEST_RUN, true)
                .add(FIELD_LEVEL, true)
                .add(FIELD_MSG, true)
                .get();
        return collection.find(queryObj, fieldsObj).sort(sortObj).skip(skip).limit(limit);
    }
}
