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
package org.alfresco.bm.data;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * Sample POJO for demonstration.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ProcessData
{
    public static final String FIELD_ID = "id";
    public static final String FIELD_PROCESS_NAME = "processName";

    private String id;
    private String processName;
    private boolean done;

    /**
     * Ensure that the MongoDB collection has the required indexes associated with
     * this user bean.
     * 
     * @param collection                the DB collection
     */
    public static void checkIndexes(DBCollection collection)
    {
        DBObject idx_PROCNAME = BasicDBObjectBuilder
                .start(FIELD_PROCESS_NAME, 1)
                .get();
        DBObject opt_PROCNAME = BasicDBObjectBuilder
                .start("name", "IDX_PROCNAME")
                .add("unique", true)
                .get();
        collection.createIndex(idx_PROCNAME, opt_PROCNAME);
    }
    
    /**
     * Utility method to create a new process
     * 
     * @param collection            DB collection containing data
     * @param processName           the name of the process to find
     * @return                      <tt>true</tt> if the insert was successful
     */
    public static boolean insertProcess(DBCollection collection, String processName)
    {
        DBObject insertObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_PROCESS_NAME, processName)
                .get();
        try
        {
            collection.insert(insertObj);
            return true;
        }
        catch (MongoException e)
        {
            // Log and rethrow
            return false;
        }
    }
    
    /**
     * Utility method to find a user by process name
     * 
     * @param collection            DB collection containing data
     * @param processName           the name of the process to find
     * @return                      Returns the data or <tt>null</tt> if not found
     */
    public static ProcessData findProcessByName(DBCollection collection, String processName)
    {
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_PROCESS_NAME, processName)
                .get();
        DBObject resultObj = collection.findOne(queryObj);
        if (resultObj == null)
        {
            return null;
        }
        else
        {
            ProcessData result = new ProcessData();
            result.setProcessName((String) resultObj.get(FIELD_PROCESS_NAME));
            return result;
        }
    }
    
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public boolean isDone()
    {
        return done;
    }

    public void setDone(boolean done)
    {
        this.done = done;
    }
}
