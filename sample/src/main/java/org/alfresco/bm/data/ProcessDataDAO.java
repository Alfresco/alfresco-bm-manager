/*
 * #%L
 * Alfresco Benchmark Manager
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.bm.data;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * Sample DAO for demonstration.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ProcessDataDAO
{
    private final DBCollection collection;

    /**
     * @param db                    MongoDB
     * @param collection            name of DB collection containing process data
     */
    public ProcessDataDAO(DB db, String collection)
    {
        super();
        this.collection = db.getCollection(collection);
        
        // Initialize indexes
        DBObject idx_PROCNAME = BasicDBObjectBuilder
                .start(ProcessData.FIELD_NAME, 1)
                .get();
        DBObject opt_PROCNAME = BasicDBObjectBuilder
                .start("name", "IDX_PROCNAME")
                .add("unique", true)
                .get();
        this.collection.createIndex(idx_PROCNAME, opt_PROCNAME);
    }

    /**
     * Create a new process
     * 
     * @return                      <tt>true</tt> if the insert was successful
     */
    public boolean createProcess(String processName)
    {
        DBObject insertObj = BasicDBObjectBuilder
                .start()
                .add(ProcessData.FIELD_NAME, processName)
                .add(ProcessData.FIELD_STATE, DataCreationState.NotScheduled.toString())
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
     * Find a process by unique name
     * 
     * @param processName           the name of the process to find
     * @return                      Returns the data or <tt>null</tt> if not found
     */
    public ProcessData findProcessByName(String processName)
    {
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(ProcessData.FIELD_NAME, processName)
                .get();
        DBObject resultObj = collection.findOne(queryObj);
        if (resultObj == null)
        {
            return null;
        }
        else
        {
            ProcessData result = new ProcessData();
            String stateStr = (String) resultObj.get(ProcessData.FIELD_STATE);
            DataCreationState state = DataCreationState.valueOf(stateStr);
            result.setState(state);
            result.setName( (String) resultObj.get(ProcessData.FIELD_NAME));
            return result;
        }
    }
    
    public boolean updateProcessState(String processName, DataCreationState state)
    {
        DBObject findObj = new BasicDBObject()
                .append(ProcessData.FIELD_NAME, processName);
        DBObject setObj = BasicDBObjectBuilder
                .start()
                .push("$set")
                    .append(ProcessData.FIELD_STATE, state.toString())
                 .pop()
                 .get();
        DBObject foundObj = collection.findAndModify(findObj, setObj);
        return foundObj != null;
    }
}
