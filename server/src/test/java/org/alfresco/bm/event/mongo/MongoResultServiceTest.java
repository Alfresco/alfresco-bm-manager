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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.mongodb.BasicDBObject;
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
        assertEquals(0, resultService.getResults(0L, Long.MAX_VALUE, false, 0, 5).size());
        resultService.getResults(
                new ResultHandler()
                {
                    @Override
                    public boolean processResult(
                            long fromTime, long toTime,
                            Map<String, DescriptiveStatistics> statsByEventName,
                            Map<String, Integer> failuresByEventName) throws Throwable
                    {
                        fail("Should not have any results");
                        return true;
                    }
                },
                0L, 1000L, 100L, false);
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
    public static final EventRecord createEventRecord(long eventStartTime)
    {
        Event event = MongoEventServiceTest.createEvent();

        String server = "SERVER" + (int)(Math.random() * 10);
        boolean success = Math.random() >= 0.5;
        long startTime = eventStartTime;
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
        // Record a random processor
        eventRecord.setProcessedBy("someTestProcessor");
        // Done
        return eventRecord;
    }
    
    @Test
    public synchronized void persistenceAndSearchOfDBObject() throws Exception
    {
        EventRecord eventRecord = createEventRecord(System.currentTimeMillis());
        resultService.recordResult(eventRecord);
        // Check that we can find it using the data keys
        DBObject findObj = new BasicDBObject("data.D-1", Integer.valueOf(1));
        DBObject foundObj = rs.findOne(findObj);
        assertNotNull("Did not find result with DBObject data.", foundObj);
        // Check that we can find it using the data keys
        findObj = new BasicDBObject("event.data.D-1", Integer.valueOf(1));
        foundObj = rs.findOne(findObj);
        assertNotNull("Did not find result with DBObject data.", foundObj);
    }
    
    /**
     * Push N number of event records into the {@link ResultService}.
     * The first event will be now and there will be one every 10ms.
     */
    private void pumpRecords(int n)
    {
        long testStartTime = System.currentTimeMillis();
        for (int i = 0; i < n; i++)
        {
            EventRecord eventRecord = createEventRecord(testStartTime + i * 10L);
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
                    // Check that we record and retrieve the 'processedBy' field
                    assertEquals("someTestProcessor", record.getProcessedBy());
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
        long before = System.currentTimeMillis() - 10L;         // The start time is set back when the record is created
        pumpRecords(100);
        long after = before + 100 * 10L + 10L;                  // Extra 10ms to Adjust for JVM accuracy
        
        int totalA = 0;         // before, future, all
        int totalB = 0;         // after, future, all
        int totalC = 0;         // before, future, chart-only
        int limit = 5;
        for (int skip = 0; skip < 100 ; skip += limit)
        {
            totalA += resultService.getResults(before, Long.MAX_VALUE, false, skip, limit).size();
            totalB += resultService.getResults(after, Long.MAX_VALUE, false, skip, limit).size();
            totalC += resultService.getResults(before, Long.MAX_VALUE, false, skip, limit).size();
        }
        assertEquals(100, totalA);
        assertEquals(000, totalB);
        assertEquals(100, totalC);
    }
    
    /**
     * Create some results but then search for something that does not match
     */
    @Test
    public void getZeroResultsUsingHandler()
    {
        pumpRecords(10);
        long after = resultService.getLastResult().getStartTime() + TimeUnit.HOURS.toMillis(1);     // Make sure the query window is out of range
        
        final AtomicInteger count = new AtomicInteger();
        resultService.getResults(
                new ResultHandler()
                {
                    @Override
                    public boolean processResult(
                            long fromTime, long toTime,
                            Map<String, DescriptiveStatistics> statsByEventName,
                            Map<String, Integer> failuresByEventName) throws Throwable
                    {
                        // Check that we have a failure count for each event
                        if (failuresByEventName.size() != statsByEventName.size())
                        {
                            throw new RuntimeException("Didn't have a failure count matching stats count.");
                        }
                        // Increment
                        count.incrementAndGet();
                        return true;
                    }
                },
                after, 20L, 10L, false);
        
        // Check
        assertEquals(0, count.get());
    }
    
    /**
     * Test the case where the reporting period is smaller than the stats window
     */
    @Test
    public void getResultsUsingHandler()
    {
        pumpRecords(10);
        final long firstEventTime = resultService.getFirstResult().getStartTime();
        final long lastEventTime = resultService.getLastResult().getStartTime();
        
        final AtomicInteger count = new AtomicInteger();
        final Set<String> names = new HashSet<String>(17);
        
        resultService.getResults(
                new ResultHandler()
                {
                    @Override
                    public boolean processResult(
                            long fromTime, long toTime,
                            Map<String, DescriptiveStatistics> statsByEventName,
                            Map<String, Integer> failuresByEventName) throws Throwable
                    {
                        if (toTime <= firstEventTime)
                        {
                            fail("The window is before the first event.");
                        }
                        if (fromTime > lastEventTime)
                        {
                            fail("The window is past the last event.");
                        }
                        assertEquals("Window not rebased. ", 0L, fromTime % 10L);       // Rebased on reporting period
                        assertEquals("Window size incorrect", 20L, toTime - fromTime);

                        // Record all the event names we got
                        names.addAll(statsByEventName.keySet());
                        
                        // Increment
                        count.incrementAndGet();
                        
                        return true;
                    }
                },
                0L, 20L, 10L, false);
        
        // Check
        assertEquals(10, count.get());
        assertEquals(resultService.getEventNames().size(), names.size());
    }
    
    /**
     * Test the case where the reporting period is smaller than the stats window
     */
    @Test
    public void getCheckedResultsUsingHandler()
    {
        pumpRecords(10);
        
        final AtomicInteger count = new AtomicInteger();
        final Map<String, DescriptiveStatistics> lastStatsByEventName = new HashMap<String, DescriptiveStatistics>(17);
        
        resultService.getResults(
                new ResultHandler()
                {
                    @Override
                    public boolean processResult(
                            long fromTime, long toTime,
                            Map<String, DescriptiveStatistics> statsByEventName,
                            Map<String, Integer> failuresByEventName) throws Throwable
                    {
                        // Always keep the last stats
                        lastStatsByEventName.clear();
                        lastStatsByEventName.putAll(statsByEventName);
                        
                        count.incrementAndGet();
                        return true;
                    }
                },
                0L, 200L, 10L, false);
        // Check
        assertEquals(10, count.get());
        
        // Now go through the last stats received
        // Check it against the last window size
        List<String> names = resultService.getEventNames();
        for (String eventName : names)
        {
            List<EventRecord> eventResults = resultService.getResults(eventName, 0, 1000);
            DescriptiveStatistics eventStats = new DescriptiveStatistics();
            for (EventRecord eventRecord : eventResults)
            {
                eventStats.addValue(eventRecord.getTime());
            }
            DescriptiveStatistics lastEventStats = lastStatsByEventName.get(eventName);
            assertNotNull("No last report for event '" + eventName  + "'.", lastEventStats);
            // Now check that this matched the last report exactly
            assertEquals(
                    "Mean for '" + eventName + "' was not correct. ",
                    (long) Math.floor(eventStats.getMean()), (long) Math.floor(lastStatsByEventName.get(eventName).getMean()));
        }
    }
}
