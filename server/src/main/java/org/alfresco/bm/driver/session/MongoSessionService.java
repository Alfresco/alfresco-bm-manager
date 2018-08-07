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
package org.alfresco.bm.driver.session;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.alfresco.bm.common.spring.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

/**
 * Mongo implementation of service providing access and management of {@link SessionData}.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public class MongoSessionService extends AbstractSessionService implements LifecycleListener
{
    private static Log logger = LogFactory.getLog(MongoSessionService.class);
    
    public static final String FIELD_ID = "_id";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_END_TIME = "endTime";
    
    private DBCollection collection;
    
    /**
     * Construct an instance providing the DB and collection name to use
     */
    public MongoSessionService(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
    }
    
    @Override
    public void start()
    {
        checkIndexes();
    }

    @Override
    public void stop()
    {
    }

    /**
     * Ensure that the MongoDB collection has the required indexes
     */
    private void checkIndexes()
    {
        collection.setWriteConcern(WriteConcern.SAFE);
        
        // Ensure unique session ID
        // This is guaranteed by MongoDB

        // Ensure ordering by start time
        DBObject idxSessionIdStartTime = BasicDBObjectBuilder
                .start(FIELD_START_TIME, 1)
                .get();
        DBObject optSessionIdStartTime = BasicDBObjectBuilder
                .start("name", "idxSessionIdStartTime")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxSessionIdStartTime, optSessionIdStartTime);

        // Find unfinished sessions
        DBObject idxEndTimeStartTime = BasicDBObjectBuilder
                .start(FIELD_END_TIME, 1)
                .add(FIELD_START_TIME, 1)
                .get();
        DBObject optEndTimeStartTime = BasicDBObjectBuilder
                .start("name", "idxEndTimeStartTime")
                .add("unique", Boolean.FALSE)
                .get();
        collection.createIndex(idxEndTimeStartTime, optEndTimeStartTime);
    }
    
    private SessionData fromDBObject(DBObject sessionDataObj)
    {
        SessionData sessionData = new SessionData();
        sessionData.setId(sessionDataObj.get(FIELD_ID).toString());
        sessionData.setStartTime((Long) sessionDataObj.get(FIELD_START_TIME));
        sessionData.setEndTime((Long) sessionDataObj.get(FIELD_END_TIME));
        sessionData.setData((DBObject) sessionDataObj.get(FIELD_DATA));
        return sessionData;
    }

    @Override
    protected String newSession(SessionData sessionData)
    {
        ObjectId id = new ObjectId();
        DBObject insertObj = BasicDBObjectBuilder.start()
                .add(FIELD_ID, id)
                .add(FIELD_START_TIME, sessionData.getStartTime())
                .add(FIELD_END_TIME, sessionData.getEndTime())
                .add(FIELD_DATA, sessionData.getData())
                .get();
        try
        {
            collection.insert(insertObj);
            return id.toString();
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
                .add(FIELD_ID, new ObjectId(sessionId))
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
    protected void updateSessionEndTime(String sessionId, long endTime)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_ID, new ObjectId(sessionId))
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$set")
                    .add(FIELD_END_TIME, endTime)
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
                    "   End Time:     " + endTime,
                    e);
        }
    }

    @Override
    protected boolean updateSessionData(String sessionId, DBObject data)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_ID, new ObjectId(sessionId))
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$set")
                    .add(FIELD_DATA, data)
                .pop()
                .get();
        try
        {
            WriteResult wr = collection.update(queryObj, updateObj);
            return wr.getN() > 0;
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
    public long getActiveSessionsCount()
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_END_TIME, -1L)
                .get();
        return collection.count(queryObj);
    }
    
    @Override
    public long getCompletedSessionsCount()
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .push(FIELD_END_TIME)
                    .append("$gt", 0)
                .pop()
                .get();
        return collection.count(queryObj);
    }

    @Override
    public long getAllSessionsCount()
    {
        return collection.count();
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
