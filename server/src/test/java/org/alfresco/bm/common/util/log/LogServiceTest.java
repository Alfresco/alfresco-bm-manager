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
package org.alfresco.bm.common.util.log;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.alfresco.bm.common.util.junit.tools.MongoDBForTestsFactory;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @see LogService
 * 
 * @author Derek Hulley
 * @since 2.0.3
 */
@RunWith(JUnit4.class)
public class LogServiceTest
{
    private static MongoDBForTestsFactory mongoFactory;
    private MongoLogService logService;
    private DB db;
    private DBCollection ls;
    
    @Before
    public void setUp() throws Exception
    {
        mongoFactory = new MongoDBForTestsFactory();
        db = mongoFactory.getObject();
        logService = new MongoLogService(db, 8196  , 20, 0);
        logService.start();
        ls = db.getCollection(MongoLogService.COLLECTION_LOGS);
    }

    /**
     * make sure the system name is NOT contained as from 3.2 on
     * 
     * @param collection
     *        (Set<String>) collection to check
     * @return
     */
    private Set<String> removeSystemValues(Set<String> collection)
    {
        if (null != collection)
        {
            // make sure the system name is NOT contained as from 3.2 on
            collection.remove("system.indexes");
        }
        return collection;
    }

    @After
    public void tearDown() throws Exception
    {
        logService.stop();
        mongoFactory.destroy();
    }
    
    @Test
    public void basic()
    {
        assertNotNull(db);
        assertNotNull(ls);
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add(MongoLogService.COLLECTION_LOGS);
        assertEquals(collectionNames, removeSystemValues(
                db.getCollectionNames()));

        // Check indexes (includes implicit '_id_' index)
        List<DBObject> indexes = ls.getIndexInfo();
        assertEquals("Incorrect indexes: " + indexes, 5, indexes.size());
    }
    
    @Test
    public void cappingOptions()
    {
        // Capping it is find as long as the TTL does not apply
        new MongoLogService(db, 1024^2, 20, 0);
        // Putting a TTL on is fine as long as there is no cap
        new MongoLogService(db, 0, 0, 180);
        try
        {
            new MongoLogService(db, 0, 20, 0);
            fail("Should detect max being set without size.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            new MongoLogService(db, 1024^2, 0, 180);
            fail("Should detect capping and TTL");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    @Test
    public void filterByLevel()
    {
        logService.log(null, null, null, LogLevel.INFO, "INFO 1");
        logService.log(null, null, null, LogLevel.INFO, "INFO 2");
        logService.log(null, null, null, LogLevel.WARN, "WARN 1");
        logService.log(null, null, null, LogLevel.ERROR, "ERROR 1");
        
        assertEquals(4, logService.getLogs(null, null, null, LogLevel.TRACE, null, null, 0, 5).size());
        assertEquals(1, logService.getLogs(null, null, null, LogLevel.TRACE, null, null, 3, 5).size());
        assertEquals(1, logService.getLogs(null, null, null, LogLevel.TRACE, null, null, 0, 1).size());
        assertEquals(2, logService.getLogs(null, null, null, LogLevel.WARN, null, null, 0, 5).size());
        assertEquals(1, logService.getLogs(null, null, null, LogLevel.ERROR, null, null, 0, 5).size());
        assertEquals(0, logService.getLogs(null, null, null, LogLevel.FATAL, null, null, 0, 5).size());
    }
    
    @Test
    public synchronized void filterByTime() throws Exception
    {
        long before = System.currentTimeMillis();
        this.wait(50L);
        {
            logService.log(null, null, null, LogLevel.INFO, "INFO 1");
            logService.log(null, null, null, LogLevel.INFO, "INFO 2");
            logService.log(null, null, null, LogLevel.WARN, "WARN 1");
            logService.log(null, null, null, LogLevel.ERROR, "ERROR 1");
        }
        this.wait(50L);
        long after = System.currentTimeMillis();
        
        assertEquals(0, logService.getLogs(null, null, null, null, before, before, 0, 5).size());
        assertEquals(0, logService.getLogs(null, null, null, null, after, after, 0, 5).size());
        assertEquals(4, logService.getLogs(null, null, null, null, before, after, 0, 5).size());
    }
    
    @Test
    public void filterByTest()
    {
        logService.log(null, "A", null, LogLevel.INFO, "INFO 1");
        logService.log(null, "A", null, LogLevel.INFO, "INFO 2");
        logService.log(null, "B", null, LogLevel.WARN, "WARN 1");
        logService.log(null, "B", null, LogLevel.ERROR, "ERROR 1");
        
        assertEquals(0, logService.getLogs(null, "X", null, null, null, null, 0, 5).size());
        assertEquals(2, logService.getLogs(null, "A", null, null, null, null, 0, 5).size());
        assertEquals(2, logService.getLogs(null, "B", null, null, null, null, 0, 5).size());
    }
    
    @Test
    public void filterByRun()
    {
        logService.log(null, null, "A", LogLevel.INFO, "INFO 1");
        logService.log(null, null, "A", LogLevel.INFO, "INFO 2");
        logService.log(null, null, "B", LogLevel.WARN, "WARN 1");
        logService.log(null, null, "B", LogLevel.ERROR, "ERROR 1");
        
        assertEquals(0, logService.getLogs(null, null, "X", null, null, null, 0, 5).size());
        assertEquals(2, logService.getLogs(null, null, "A", null, null, null, 0, 5).size());
        assertEquals(2, logService.getLogs(null, null, "B", null, null, null, 0, 5).size());
    }
    
    @Test
    public void filterByDriverId()
    {
        logService.log("D1", null, null, LogLevel.INFO, "INFO 1");
        logService.log("D1", null, null, LogLevel.INFO, "INFO 2");
        logService.log("D2", null, null, LogLevel.WARN, "WARN 1");
        logService.log("D2", null, null, LogLevel.ERROR, "ERROR 1");
        
        assertEquals(2, logService.getLogs("D1", null, null, null, null, null, 0, 5).size());
        assertEquals(1, logService.getLogs("D2", null, null, null, null, null, 1, 5).size());
    }
}
