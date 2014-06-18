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
package org.alfresco.bm.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.alfresco.bm.user.UserData.UserCreationState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException.DuplicateKey;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Service providing access to {@link UserData} storage. All {@link UserData} returned from and persisted
 * with this service will be testrun-specific. The testrun-identifier is set in the constructor.
 *
 * @author Frederik Heremans
 * @author Derek Hulley
 * @author steveglover
 * @since 1.1
 */
public class UserDataServiceImpl extends AbstractUserDataService implements InitializingBean
{
    public static final String FIELD_RANDOMIZER = "randomizer";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_CREATION_STATE = "creationState";
    public static final String FIELD_FIRST_NAME = "firstName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_DOMAIN = "domain";

    public static final String FIELD_ID = "id";
    public static final String FIELD_KEY = "key";
    public static final String FIELD_COMPLETE = "complete";
    
    private static Log logger = LogFactory.getLog(UserDataServiceImpl.class);

    private DBCollection collection;
    
    public UserDataServiceImpl(DB db, String collection)
    {
        this.collection = db.getCollection(collection);
    }
    
    @Override
    public void afterPropertiesSet() throws Exception
    {
        checkIndexes();
    }

    /**
     * Ensure that the MongoDB collection has the required indexes associated with
     * this user bean.
     */
    private void checkIndexes()
    {
        collection.setWriteConcern(WriteConcern.SAFE);
        
        DBObject uidxUserName = BasicDBObjectBuilder
                .start(FIELD_USERNAME, 1)
                .get();
        collection.ensureIndex(uidxUserName, "uidx_username", true);

        DBObject uidxEmail = BasicDBObjectBuilder
                .start(FIELD_EMAIL, 1)
                .get();
        collection.ensureIndex(uidxEmail, "uidx_email", true);

        DBObject idxDomain = BasicDBObjectBuilder
                .start(FIELD_DOMAIN, 1)
                .get();
        collection.ensureIndex(idxDomain, "idx_domain", false);
        
        DBObject idxCreated = BasicDBObjectBuilder
                .start(FIELD_CREATION_STATE, 1)
                .add(FIELD_RANDOMIZER, 2)
                .add(FIELD_DOMAIN, 3)
                .get();
        collection.ensureIndex(idxCreated, "idx_creation_state", false);
    }
    
    /**
     * Helper to convert a Mongo DBObject into the API consumable object
     * <p/>
     * Note that <tt>null</tt> is handled as a <tt>null</tt> return.
     */
    private UserData fromDBObject(DBObject userDataObj)
    {
        if (userDataObj == null)
        {
            return null;
        }
        
        UserData userData = new UserData();
        userData.setUsername((String) userDataObj.get(FIELD_USERNAME));
        userData.setPassword((String) userDataObj.get(FIELD_PASSWORD));
        String userCreationStateStr = (String) userDataObj.get(FIELD_CREATION_STATE);
        UserData.UserCreationState creationState = UserCreationState.Unknown;
        try
        {
            creationState = UserCreationState.valueOf(userCreationStateStr);
            userData.setCreationState(creationState);
        }
        catch (Exception  e)
        {
            logger.error("User data has unknown state: " + userData.getUsername() + " - " + userCreationStateStr);
        }
        userData.setFirstName((String) userDataObj.get(FIELD_FIRST_NAME));
        userData.setLastName((String) userDataObj.get(FIELD_LAST_NAME));
        userData.setEmail((String) userDataObj.get(FIELD_EMAIL));
        userData.setDomain((String) userDataObj.get(FIELD_DOMAIN));
        // Done
        return userData;
    }
    
    /**
     * Turn a cursor into an array of API-friendly objects
     */
    private List<UserData> fromDBCursor(DBCursor cursor)
    {
        int count = cursor.count();
        try
        {
            List<UserData> userDatas = new ArrayList<UserData>(count);
            while (cursor.hasNext())
            {
                DBObject userDataObj = cursor.next();
                UserData userData = fromDBObject(userDataObj);
                userDatas.add(userData);
            }
            // Done
            return userDatas;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createNewUser(UserData data)
    {
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_RANDOMIZER, data.getRandomizer())
                .add(FIELD_USERNAME, data.getUsername())
                .add(FIELD_PASSWORD, data.getPassword())
                .add(FIELD_CREATION_STATE, data.getCreationState().toString())
                .add(FIELD_FIRST_NAME, data.getFirstName())
                .add(FIELD_LAST_NAME, data.getLastName())
                .add(FIELD_EMAIL, data.getEmail())
                .add(FIELD_DOMAIN, data.getDomain());
        DBObject insertObj = insertObjBuilder.get();
        
        try
        {
            WriteResult result = collection.insert(insertObj);
            if (result.getError() != null)
            {
                throw new RuntimeException(
                        "Failed to insert user data: \n" +
                        "   User Data: " + data + "\n" +
                        "   Result:    " + result);
            }
        }
        catch (DuplicateKey e)
        {
            // We just rethrow as per the API
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserPassword(String username, String password)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_USERNAME, username)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$set")
                    .add(FIELD_PASSWORD, password)
                .pop()
                .get();
        WriteResult result = collection.update(queryObj, updateObj);
        if (result.getError() != null || result.getN() != 1)
        {
            throw new RuntimeException(
                    "Failed to update user ticket: \n" +
                    "   Username: " + username + "\n" +
                    "   Password: " + password + "\n" +
                    "   Result:   " + result);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserCreationState(String username, UserCreationState creationState)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_USERNAME, username)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$set")
                    .add(FIELD_CREATION_STATE, creationState.toString())
                .pop()
                .get();
        WriteResult result = collection.update(queryObj, updateObj);
        if (result.getError() != null || result.getN() != 1)
        {
            throw new RuntimeException(
                    "Failed to update user creation state: \n" +
                    "   Username:       " + username + "\n" +
                    "   Creation State: " + creationState + "\n" +
                    "   Result:         " + result);
        }
    }
    
    @Override
    public long countUsers(String domain, UserCreationState creationState)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (domain != null)
        {
            queryObjBuilder.add(FIELD_DOMAIN, domain);
        }
        if (creationState != null)
        {
            queryObjBuilder.add(FIELD_CREATION_STATE, creationState.toString());
        }
        DBObject queryObj = queryObjBuilder.get();
        
        return collection.count(queryObj);
    }

    @Override
    public long deleteUsers(UserCreationState creationState)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (creationState != null)
        {
            queryObjBuilder.add(FIELD_CREATION_STATE, creationState.toString());
        }
        DBObject queryObj = queryObjBuilder.get();
        
        WriteResult result = collection.remove(queryObj);
        return result.getN();
    }

    /**
     * Find a user by username
     * 
     * @return                          the {@link UserData} found otherwise <tt>null</tt.
     */
    @Override
    public UserData findUserByUsername(String username)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_USERNAME, username)
                .get();
        DBObject userDataObj = collection.findOne(queryObj);
        return fromDBObject(userDataObj);
    }
    
    @Override
    public UserData findUserByEmail(String email)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_EMAIL, email)
                .get();
        DBObject userDataObj = collection.findOne(queryObj);
        return fromDBObject(userDataObj);
    }
    
    @Override
    public List<UserData> getUsersByCreationState(UserCreationState creationState, int startIndex, int count)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CREATION_STATE, creationState.toString())
                .get();
        DBCursor cursor = collection.find(queryObj).skip(startIndex).limit(count);
        return fromDBCursor(cursor);
    }

    /**
     * @return              the maximum value of the randomizer
     */
    private int getMaxRandomizer()
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CREATION_STATE, UserCreationState.Created.toString())
                .get();
        DBObject sortObj = BasicDBObjectBuilder.start()
                .add(FIELD_RANDOMIZER, -1)
                .get();
        DBObject fieldsObj = BasicDBObjectBuilder.start()
                .add(FIELD_RANDOMIZER, Boolean.TRUE)
                .get();
        
        DBObject resultObj = collection.findOne(queryObj, fieldsObj, sortObj);
        if (resultObj == null)
        {
            return 0;
        }
        else
        {
            int randomizer = (Integer) resultObj.get(FIELD_RANDOMIZER);
            return randomizer;
        }
    }

    @Override
    public UserData getRandomUser()
    {
        int upper = getMaxRandomizer();             // The upper limit will be exclusive
        int random = (int) (Math.random() * (double) upper);
        
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_CREATION_STATE, UserCreationState.Created.toString())
                .push(FIELD_RANDOMIZER)
                    .add("$gte", Integer.valueOf(random))
                .pop()
                .get();
        
        DBObject userDataObj = collection.findOne(queryObj);
        return fromDBObject(userDataObj);
    }
    
    /*
     * USER DOMAIN SERVICES
     */

    @Override
    public List<UserData> getUsersInDomain(String domain, int startIndex, int count)
    {
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(FIELD_DOMAIN, domain)
                .add(FIELD_CREATION_STATE, UserCreationState.Created.toString())
                .get();
        DBCursor cursor = collection.find(queryObj).skip(startIndex).limit(count);
        
        return fromDBCursor(cursor);
    }
    
    @Override
    public Iterator<String> getDomainsIterator()
    {
        @SuppressWarnings("unchecked")
        List<String> domains = (List<String>) collection.distinct(FIELD_DOMAIN);
        return domains.iterator();
    }

    @Override
    public UserData getRandomUserFromDomain(String domain)
    {
        List<String> domains = Collections.singletonList(domain);
        return getRandomUserFromDomains(domains);
    }
    
    private Range getRandomizerRange(List<String> domains)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_CREATION_STATE, UserCreationState.Created.toString());
        if (domains.size() > 0)
        {
            queryObjBuilder
                .push(FIELD_DOMAIN)
                    .add("$in", domains)
                .pop();
        }
        DBObject queryObj = queryObjBuilder.get();

        DBObject fieldsObj = BasicDBObjectBuilder.start()
                .add(FIELD_RANDOMIZER, Boolean.TRUE)
                .get();
        
        DBObject sortObj = BasicDBObjectBuilder.start()
                .add(FIELD_RANDOMIZER, -1)
                .get();
        
        // Find max
        DBObject resultObj = collection.findOne(queryObj, fieldsObj, sortObj);
        int maxRandomizer = resultObj == null ? 0 : (Integer) resultObj.get(FIELD_RANDOMIZER);
        
        // Find min
        sortObj.put(FIELD_RANDOMIZER, +1);
        resultObj = collection.findOne(queryObj, fieldsObj, sortObj);
        int minRandomizer = resultObj == null ? 0 : (Integer) resultObj.get(FIELD_RANDOMIZER);
        
        return new Range(minRandomizer, maxRandomizer);
    }
    
    @Override
    public UserData getRandomUserFromDomains(List<String> domains)
    {
        Range range = getRandomizerRange(domains);
        int upper = range.getMax();
        int lower = range.getMin();
        int random = lower + (int) (Math.random() * (double) (upper - lower));

        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start()
                .add(FIELD_CREATION_STATE, UserCreationState.Created.toString())
                .push(FIELD_RANDOMIZER)
                    .add("$gte", random)
                .pop();
        if (domains.size() > 0)
        {
            queryObjBuilder
                .push(FIELD_DOMAIN)
                    .add("$in", domains)
                .pop();
        }
        DBObject queryObj = queryObjBuilder.get();
        
        DBObject userDataObj = collection.findOne(queryObj);
        return fromDBObject(userDataObj);
    }
    
    public static class Range
    {
        private int min;
        private int max;
        
        public Range(int min, int max)
        {
            super();
            this.min = min;
            this.max = max;
        }

        public int getMin()
        {
            return min;
        }

        public int getMax()
        {
            return max;
        }
    }
}
