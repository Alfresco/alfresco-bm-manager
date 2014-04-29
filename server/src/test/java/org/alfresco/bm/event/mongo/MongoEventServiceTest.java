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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.ResultService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import de.flapdoodle.embedmongo.distribution.Version;
import de.flapdoodle.embedmongo.tests.MongodForTestsFactory;

/**
 * @see MongoEventService
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class MongoEventServiceTest
{
    private static MongodForTestsFactory mongoFactory;
    private MongoEventService eventService;
    private DB db;
    private DBCollection es;
    
    @BeforeClass
    public static void setUpMongo() throws Exception
    {
        mongoFactory = MongodForTestsFactory.with(Version.Main.V2_2);
    }
    
    @AfterClass
    public static void tearDownMongo()
    {
        mongoFactory.shutdown();
    }
    
    @Before
    public void setUpService() throws Exception
    {
        final Mongo mongo = mongoFactory.newMongo();
        db = mongoFactory.newDB(mongo);
        eventService = new MongoEventService(db, "es");
        eventService.start();
        es = db.getCollection("es");
    }
    
    @After
    public void tearDownDao() throws Exception
    {
        eventService.stop();
        db.dropDatabase();
    }
    
    @Test
    public void basic()
    {
        assertNotNull(db);
        assertNotNull(es);
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("system.indexes");
        collectionNames.add("es");
        assertEquals(collectionNames, db.getCollectionNames());
        
        // Check indexes (includes implicit '_id_' index)
        List<DBObject> indexes = es.getIndexInfo();
        assertEquals("Incorrect indexes: " + indexes, 3, indexes.size());
    }
    
    @Test
    public void empty()
    {
        assertEquals(0, eventService.count());
        assertNull(eventService.nextEvent("SERVER01", Long.MAX_VALUE, false));
    }
    
    /**
     * Attempt to record bad results
     */
    @Test
    public void putBadEvents()
    {
        Event event = null;
        // Null event
        try
        {
            eventService.putEvent(event);
            fail("Null should be checked.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    public static final Event createEvent()
    {
        long now = System.currentTimeMillis();
        String name = "E-" + (int)(Math.random() * 10);
        
        DBObject data = BasicDBObjectBuilder.start().get();
        for (int i = 0; i < 10; i++)
        {
            String dataKey = "D-" + i;
            Integer dataValue = i;
            data.put(dataKey, dataValue);
        }
        Event event = new Event(name, now, data);
        // Done
        return event;
    }
    
    /**
     * Push N number of event records into the {@link ResultService}
     */
    private void pumpEvents(int n)
    {
        for (int i = 0; i < n; i++)
        {
            Event event = createEvent();
            eventService.putEvent(event);
        }
    }
    
    @Test
    public void countEvents()
    {
        pumpEvents(100);
        assertEquals(100, eventService.count());
    }
    
    @Test
    public void nextEvent()
    {
        pumpEvents(100);
        
        long lastScheduledTime = 0L;
        for (int i = 0; i < 100; i++)
        {
            long now = System.currentTimeMillis();
            Event event = eventService.nextEvent("SERVER01", now, false);
            assertNotNull("Did not get a next event.", event);
            assertTrue("Scheduled time must be increasing. ", event.getScheduledTime() >= lastScheduledTime);
            lastScheduledTime = event.getScheduledTime();
            assertNotNull("Event must have an ID", event.getId());
            assertNotNull("Data payload not present on Event", event.getDataObject());
            // Delete the event
            assertTrue(
                    "Failed to delete event: " + event,
                    eventService.deleteEvent(event));
        }
        // There should be exactly zero, now
        assertEquals(0, eventService.count());
    }
    
    @Test
    public void lockEventManual()
    {
        Event event = createEvent();
        event.setId("12345678901234567890aaaa");
        // There should be no issue with the first one
        String eventId = eventService.putEvent(event);
        assertEquals("Event ID should have been explicitly set", event.getId(), eventId);
        event.setId(eventId);       // For next comparison check
        // Retrieve it by ID
        Event eventCheck = eventService.getEvent(eventId);
        assertEquals("Event put and retrieved must be exact matches. ", event.toString(), eventCheck.toString());
        
        // Ensure that we cannot put the same event
        Event dupEvent = createEvent();
        dupEvent.setId(event.getId());
        try
        {
            eventService.putEvent(dupEvent);
            fail("Should not have been able to use the same lock");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
    }
    
    @Test
    public synchronized void lockEventAutomatic() throws Exception
    {
        Event event = createEvent();
        String eventId = eventService.putEvent(event);
        event = eventService.getEvent(eventId);
        // Check that the event is not locked
        assertNotNull(event.getId());
        assertNull(event.getLockOwner());
        assertEquals(0L, event.getLockTime());
        // Check that the scheduled time has passed
        this.wait(10L);
        long now = System.currentTimeMillis();
        assertTrue(now > event.getScheduledTime());
        
        // Put an event in the future
        Event futureEvent = new Event("lockEventAutomatic", now + 10000L, null);
        @SuppressWarnings("unused")
        String futureEventId = eventService.putEvent(futureEvent);
        
        // Get 'next' event with incorrect server ID
        assertNull("Next event not filtered out by invalid server ID", eventService.nextEvent("FROG", now, true));
        
        // Get 'next' event with incorrect scheduled time
        assertNull("Next event not filtered out by scheduled time", eventService.nextEvent("FROG", now - 20000L, false));
        
        // Get the 'next' event, which should NOT be the future event
        Event nextEvent = eventService.nextEvent("FROG", now, false);
        assertNotNull("Next event was not fetched for non-local data with correct time.");
        assertEquals("Incorrect event found", eventId, nextEvent.getId());
        
        // Exactly the same search should give nothing
        assertNull("Should not be able to get the same event twice", eventService.nextEvent("FROG", now, false));
    }
}
