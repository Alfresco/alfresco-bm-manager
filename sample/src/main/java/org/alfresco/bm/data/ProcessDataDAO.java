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

import com.mongodb.DB;
import com.mongodb.DBCollection;

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
     * @param processCount          number of unfinished processes to create
     */
    public ProcessDataDAO(DB db, String collection)
    {
        super();
        this.collection = db.getCollection(collection);
        
        // Initialize indexes
        ProcessData.checkIndexes(this.collection);
    }

    /**
     * Create a new process
     * 
     * @param collection            DB collection containing data
     * @param processName           the name of the process to find
     * @return                      <tt>true</tt> if the insert was successful
     */
    public boolean createProcess(String processName)
    {
        return ProcessData.insertProcess(collection, processName);
    }
    
    /**
     * Find a process by unique name
     * 
     * @param processName           the name of the process to find
     * @return                      Returns the data or <tt>null</tt> if not found
     */
    public ProcessData findProcessByName(String processName)
    {
        return ProcessData.findProcessByName(collection, processName);
    }
}
