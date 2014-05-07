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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.event.ResultService.ResultHandler;
import org.alfresco.mongo.MongoDBForTestsFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @see MongoResultService
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class MongoResultServiceTest
{
    private MongoDBForTestsFactory mongoFactory;
    private MongoResultService resultService;
    private DB db;
    private DBCollection rs;
    
    @Before
    public void setUp() throws Exception
    {
        mongoFactory = new MongoDBForTestsFactory();
        db = mongoFactory.getObject();
        resultService = new MongoResultService(db, "rs");
        resultService.start();
        rs = db.getCollection("rs");
    }
    
    @After
    public void tearDown() throws Exception
    {
        resultService.stop();
        mongoFactory.destroy();
    }
    
    @Test
    public void basic()
    {
        assertNotNull(db);
        assertNotNull(rs);
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("system.indexes");
        collectionNames.add("rs");
        assertEquals(collectionNames, db.getCollectionNames());
        
        // Check indexes (includes implicit '_id_' index)
        List<DBObject> indexes = rs.getIndexInfo();
        assertEquals("Incorrect indexes: " + indexes, 5, indexes.size());
    }
    
    @Test
    public void empty()
    {
        assertEquals(0, resultService.countResults());
        assertEquals(0, resultService.countResultsByFailure());
        assertEquals(0, resultService.countResultsBySuccess());
        assertEquals(0, resultService.countResultsByEventName("e.1"));
        
        assertEquals(0, resultService.getResults("e.1", 0, 5).size());
        assertEquals(0, resultService.getResults(0L, "e.1", Boolean.TRUE, 0, 5).size());
        resultService.getResults(
                new ResultHandler()
                {
                    @Override
                    public boolean processResult(long fromTime, long toTime, Map<String, DescriptiveStatistics> statsByEventName) throws Throwable
                    {
                        fail("Should not have any results");
                        return true;
                    }
                    @Override
                    public long getWaitTime()
                    {
                        return 0L;
                    }
                },
                0L, "e.1", Boolean.TRUE, 1000L, false);
    }
    
    /**
     * Attempt to record bad results
     */
    @Test
    public void recordBadResults()
    {
        EventRecord record = null;
        // Null record
        try
        {
            resultService.recordResult(record);
            fail("Null should be checked.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    /**
     * Create a sample set of results
     */
    public static final EventRecord createEventRecord()
    {
        Event event = MongoEventServiceTest.createEvent();

        String server = "SERVER" + (int)(Math.random() * 10);
        boolean success = Math.random() >= 0.5;
        long startTime = System.currentTimeMillis() - (long)(Math.random() * 1000.0);
        long time = (long)(Math.random() * 1000.0);
        boolean chart = Math.random() > 0.5;
        long startDelay = (long)(Math.random() * 100.0);
        boolean warn = time > 900L;
        
        DBObject data = BasicDBObjectBuilder.start().get();
        for (int i = 0; i < 10; i++)
        {
            String dataKey = "D-" + i;
            Integer dataValue = i;
            data.put(dataKey, dataValue);
        }
        EventRecord eventRecord = new EventRecord(server, success, startTime, time, data, event);
        eventRecord.setChart(chart);
        eventRecord.setStartDelay(startDelay);
        if (warn)
        {
            eventRecord.setWarning("Execution time was greater than 900ms");
        }
        // Done
        return eventRecord;
    }
    
    /**
     * Push N number of event records into the {@link ResultService}
     */
    private void pumpRecords(int n)
    {
        for (int i = 0; i < n; i++)
        {
            EventRecord eventRecord = createEventRecord();
            resultService.recordResult(eventRecord);
        }
    }
    
    @Test
    public void countEventsBySuccessAndFailure()
    {
        pumpRecords(100);
        assertEquals(100, resultService.countResults());
        assertEquals(100, resultService.countResultsByFailure() + resultService.countResultsBySuccess());
    }
    
    @Test
    public void countEventsByName()
    {
        pumpRecords(100);
        List<String> names = resultService.getEventNames();
        long total = 0L;
        for (String name : names)
        {
            long count = resultService.countResultsByEventName(name);
            total += count;
        }
        assertEquals(100, total);
    }
    
    @Test
    public void getResultsPagedAll()
    {
        pumpRecords(100);
        long total = 0L;
        int skip = 0;
        int limit = 5;
        while (true)
        {
            List<EventRecord> records = resultService.getResults(null, skip, limit);
            for (EventRecord record : records)
            {
                assertTrue(record.getEvent().getDataObject() instanceof DBObject);
                assertTrue(record.getData() instanceof DBObject);
                if (record.getTime() > 900L)
                {
                    assertTrue("Warning not tracked", record.getWarning() != null);
                }
                else
                {
                    assertTrue("Unexpected warning", record.getWarning() == null);
                }
                assertNotNull(record.getId());
            }
            total += records.size();
            // Do we keep paging?
            if (records.size() < limit)
            {
                break;
            }
            else
            {
                skip += limit;
            }
        }
        assertEquals(100, total);
    }
    
    @Test
    public void getResultsPagedByEventName()
    {
        pumpRecords(100);
        List<String> names = resultService.getEventNames();
        long total = 0L;
        for (String name : names)
        {
            int skip = 0;
            int limit = 5;
            while (true)
            {
                List<EventRecord> records = resultService.getResults(name, skip, limit);
                for (EventRecord record : records)
                {
                    assertEquals("Incorrect event name", name, record.getEvent().getName());
                }
                total += records.size();
                // Do we keep paging?
                if (records.size() < limit)
                {
                    break;
                }
                else
                {
                    skip += limit;
                }
            }
        }
        assertEquals(100, total);
        
        // Check that invalid event name returns nothing
        assertEquals(0, resultService.getResults("BOB", 0, 1).size());
    }
    
    @Test
    public void getResultsAfterTime()
    {
        long before = System.currentTimeMillis() - 1000L;       // The start time is set back when the record is created
        pumpRecords(100);
        long after = System.currentTimeMillis() + 10L;          // Adjust for JVM accuracy
        
        int totalA = 0;         // before, null, null
        int totalB = 0;         // after, null, null
        int totalC = 0;         // before, null, true
        int totalD = 0;         // after, null, false
        int limit = 5;
        for (int skip = 0; skip < 100 ; skip += limit)
        {
            totalA += resultService.getResults(before, null, null, skip, limit).size();
            totalB += resultService.getResults(after, null, null, skip, limit).size();
            totalC += resultService.getResults(before, null, Boolean.TRUE, skip, limit).size();
            totalD += resultService.getResults(before, null, Boolean.FALSE, skip, limit).size();
        }
        assertEquals(100, totalA);
        assertEquals(000, totalB);
        assertEquals(100, totalC + totalD);
    }
    
    @Test
    public void getResultsUsingHandler()
    {
        pumpRecords(100);
        
        final AtomicLong total = new AtomicLong(0L);

        resultService.getResults(
                new ResultHandler()
                {
                    @Override
                    public boolean processResult(long fromTime, long toTime, Map<String, DescriptiveStatistics> statsByEventName) throws Throwable
                    {
                        // Count each of the events in the time window
                        for (DescriptiveStatistics stats : statsByEventName.values())
                        {
                            total.addAndGet(stats.getN());
                        }
                        return true;
                    }
                    @Override
                    public long getWaitTime()
                    {
                        return 0L;
                    }
                },
                0L, null, null, 1000L, false);
        
        // Check
        assertEquals(100L, total.get());
    }
}
