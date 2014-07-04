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

import org.alfresco.bm.test.LifecycleListener;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

/**
 * Mongo implementation of service providing access and management of {@link SessionData}.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public class MongoSessionService extends AbstractSessionService implements LifecycleListener
{
    public static final String FIELD_SESSION_ID = "sessionId";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_END_TIME = "endTime";
    public static final String FIELD_ELAPSED_TIME = "elapsedTime";
    
    private DBCollection collection;
    
    /**
     * Construct an instance providing the DB and collection name to use
     */
    public MongoSessionService(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
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
        collection.setWriteConcern(WriteConcern.SAFE);
        
        // Ensure unique session ID
        DBObject uidxSessionId = BasicDBObjectBuilder
                .start(FIELD_SESSION_ID, 1)
                .get();
        DBObject optSessionId = BasicDBObjectBuilder
                .start("name", "uidx_sessionId")
                .add("unique", Boolean.TRUE)
                .get();
        collection.createIndex(uidxSessionId, optSessionId);

        // Ensure ordering by start time
        DBObject idxSessionIdStartTime = BasicDBObjectBuilder
                .start(FIELD_SESSION_ID, 1)
                .add(FIELD_START_TIME, 2)
                .get();
        DBObject optSessionIdStartTime = BasicDBObjectBuilder
                .start("name", "idx_sessionId_startTime")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxSessionIdStartTime, optSessionIdStartTime);

        // Find unfinished sessions
        DBObject idxEndTime = BasicDBObjectBuilder
                .start(FIELD_END_TIME, 1)
                .add(FIELD_START_TIME, 1)
                .get();
        DBObject optEndTime = BasicDBObjectBuilder
                .start("name", "idx_endTime")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxEndTime, optEndTime);

        // Perhaps find long-running sessions
        DBObject idxElapsedTime = BasicDBObjectBuilder
                .start(FIELD_ELAPSED_TIME, 1)
                .add(FIELD_START_TIME, 1)
                .get();
        DBObject optElapsedTime = BasicDBObjectBuilder
                .start("name", "idx_elapsedTime")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxElapsedTime, optElapsedTime);
    }
    
    private SessionData fromDBObject(DBObject sessionDataObj)
    {
        SessionData sessionData = new SessionData();
        sessionData.setSessionId((String) sessionDataObj.get(FIELD_SESSION_ID));
        sessionData.setData((String) sessionDataObj.get(FIELD_DATA));
        sessionData.setStartTime((Long) sessionDataObj.get(FIELD_START_TIME));
        sessionData.setEndTime((Long) sessionDataObj.get(FIELD_END_TIME));
        sessionData.setElapsedTime((Long) sessionDataObj.get(FIELD_ELAPSED_TIME));
        return sessionData;
    }

    @Override
    protected void saveSessionData(SessionData sessionData)
    {
        DBObject insertObj = BasicDBObjectBuilder.start()
                .add(FIELD_SESSION_ID, sessionData.getSessionId())
                .add(FIELD_DATA, sessionData.getData())
                .add(FIELD_START_TIME, sessionData.getStartTime())
                .add(FIELD_END_TIME, sessionData.getEndTime())
                .add(FIELD_ELAPSED_TIME, sessionData.getElapsedTime())
                .get();
        try
        {
            collection.insert(insertObj);
        }
        catch (MongoException e)
        {
            throw new RuntimeException(
                    "Failed to write new session data: \n" +
                    "   Session: " + sessionData,
                    e);
        }
    }

    @Override
    protected SessionData findSessionData(String sessionId)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_SESSION_ID, sessionId)
                .get();
        DBObject result = collection.findOne(queryObj);
        if (result == null)
        {
            return null;
        }
        else
        {
            return fromDBObject(result);
        }
    }

    @Override
    protected void updateSessionEndTime(String sessionId, long endTime, long elapsedTime)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_SESSION_ID, sessionId)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$set")
                    .add(FIELD_END_TIME, endTime)
                    .add(FIELD_ELAPSED_TIME, elapsedTime)
                .pop()
                .get();
        try
        {
            collection.update(queryObj, updateObj);
        }
        catch (MongoException e)
        {
            throw new RuntimeException(
                    "Failed to update session end time: \n" +
                    "   Session:      " + sessionId + "\n" +
                    "   End Time:     " + endTime + "\n" +
                    "   Elapsed Time: " + elapsedTime,
                    e);
        }
    }

    @Override
    protected void updateSessionData(String sessionId, String data)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_SESSION_ID, sessionId)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$set")
                    .add(FIELD_DATA, data)
                .pop()
                .get();
        try
        {
            collection.update(queryObj, updateObj);
        }
        catch (MongoException e)
        {
            throw new RuntimeException(
                    "Failed to update session data: \n" +
                    "   Session:      " + sessionId + "\n" +
                    "   Data:         " + data,
                    e);
        }
    }
    
    @Override
    public long activeSessionsCount()
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_END_TIME, -1L)
                .get();
        return collection.count(queryObj);
    }
}
