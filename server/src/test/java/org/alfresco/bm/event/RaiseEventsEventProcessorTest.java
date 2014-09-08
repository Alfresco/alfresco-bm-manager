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
package org.alfresco.bm.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @see RaiseEventsEventProcessor
 * 
 * @author Derek Hulley
 * @since 1.4
 */
@RunWith(JUnit4.class)
public class RaiseEventsEventProcessorTest
{
    private static final String EVENT_NAME_GO = "go";
    private RaiseEventsEventProcessor processor = new TestRaiseEventsEventProcessorOverride(EVENT_NAME_GO, 100L, 20);
    
    @BeforeClass
    public static void setUp()
    {
    }
    
    @AfterClass
    public static void tearDown()
    {
    }
    
    @Test
    public void testSingleBatch() throws Exception
    {
        Event event = new Event("", null);  // No input
        processor.setBatchSize(1000);       // More than the number required
        EventResult result = processor.processEvent(event);
        Assert.assertEquals(20, result.getNextEvents().size());
    }
    
    @Test
    public void testSingleBatchOnEdge() throws Exception
    {
        Event event = new Event("", null);  // No input
        processor.setBatchSize(20);        // Exactly the number required
        
        EventResult result = processor.processEvent(event);
        Assert.assertEquals(20, result.getNextEvents().size());
        Assert.assertEquals(new BasicDBObject("sound", "WOOF"), result.getNextEvents().get(0).getDataObject());
    }
    
    @Test
    public void testBatch() throws Exception
    {
        Event event = new Event("", null);  // No input
        processor.setBatchSize(10);         // Small batch
        
        List<Event> allEvents = new ArrayList<Event>(50);
        
        EventResult result = processor.processEvent(event);
        Assert.assertEquals(11, result.getNextEvents().size());
        allEvents.addAll(result.getNextEvents());
        
        event = new Event("", result.getNextEvents().get(10).getDataObject());
        result = processor.processEvent(event);
        Assert.assertEquals(10, result.getNextEvents().size());
        allEvents.addAll(result.getNextEvents());
        
        // Now check that all the GO events are correctly spaces
        long lastEventScheduledTime = 0L;
        long allEventScheduledTime = 0L;
        for (Event nextEvent : allEvents)
        {
            allEventScheduledTime = nextEvent.getScheduledTime();
            Assert.assertTrue("Event schedule time must never be 0", allEventScheduledTime > 0);
            if (lastEventScheduledTime == 0L)
            {
                lastEventScheduledTime = allEventScheduledTime;
                continue;
            }
            // The next event delay must be correct
            long delta = allEventScheduledTime - lastEventScheduledTime;
            if (nextEvent.getName().equals("raiseEvents"))
            {
                // The rescheduling must be at the same time as the last GO event
                Assert.assertEquals("Event delay not correct.", 0L, delta);
                continue;
            }
            else
            {
                // The next event delta must increase
                Assert.assertEquals("Event delay not correct.", 100L, delta);
                // Check the data
                Assert.assertEquals(new BasicDBObject("sound", "WOOF"), nextEvent.getDataObject());
            }
            lastEventScheduledTime = allEventScheduledTime;
        }
    }

    /**
     * Test class to demonstrate override capabilities
     * 
     * @author Derek Hulley
     */
    private static class TestRaiseEventsEventProcessorOverride extends RaiseEventsEventProcessor
    {
        public TestRaiseEventsEventProcessorOverride(
                String outputEventName,
                long timeBetweenEvents,
                int outputEventCount)
        {
            super(outputEventName, timeBetweenEvents, outputEventCount);
        }

        @Override
        protected DBObject getNextEventData()
        {
            return new BasicDBObject("sound", "WOOF");
        }
    }
}
