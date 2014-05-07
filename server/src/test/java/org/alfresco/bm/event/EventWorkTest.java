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

import org.alfresco.bm.event.mongo.MongoEventService;
import org.alfresco.bm.event.mongo.MongoResultService;
import org.alfresco.mongo.MongoDBForTestsFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import com.mongodb.DB;

/**
 * @see EventWork
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class EventWorkTest
{
    private static final String SERVER_ID = "testserver";
    private static final String TEST_RUN_FQN = "EventWorkTest.X";
    private static final String EVENT_NAME = "x.y";
    
    private static MongoDBForTestsFactory mongoFactory;
    private MongoEventService eventService;
    private MongoResultService resultService;
    private DB db;
    private Event event;
    private List<Event> nextEvents;
    private EventProcessor processor;
    private EventWork work;

    
    @Before
    public void setUp() throws Exception
    {
        mongoFactory = new MongoDBForTestsFactory();
        db = mongoFactory.getObject();
        eventService = new MongoEventService(db, "es");
        eventService.start();
        resultService = new MongoResultService(db, "rs");
        resultService.start();

        event = new Event(EVENT_NAME, "SOME_DATA");
        event.setId(eventService.putEvent(event));
        nextEvents = new ArrayList<Event>(2);
        processor = Mockito.mock(EventProcessor.class);
        work = new EventWork(
                SERVER_ID, TEST_RUN_FQN, event,
                new TestEventProcessor(),
                eventService, resultService);
    }
    
    @After
    public void tearDown() throws Exception
    {
        mongoFactory.destroy();
        eventService.stop();
        resultService.stop();
    }
    
    private class TestEventProcessor extends AbstractEventProcessor
    {
        @Override
        protected EventResult processEvent(Event event) throws Exception
        {
            StopWatch stopWatch = new StopWatch();
            // We call a mock here so the input values are irrelevant
            return processor.processEvent(event, stopWatch);
        }
    }
    
    
    /**
     * A class used to demonstrate how unserializable data is transfered in events
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    private static final class DataThatWillNotSerialize
    {
    }
    
    @Test
    public void BasicSingleEventSuccess() throws Exception
    {
        nextEvents.add(new Event(EVENT_NAME, "MORE_DATA"));
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
        
        Assert.assertEquals(1, resultService.countResultsBySuccess());
        Assert.assertEquals(1, eventService.count());
        Assert.assertNull(eventService.nextEvent(SERVER_ID, Long.MAX_VALUE, true));
        
        Assert.assertNotNull(eventService.nextEvent(SERVER_ID, Long.MAX_VALUE, false));
        // Must be locked against further retrieval
        Assert.assertNull(eventService.nextEvent(SERVER_ID, Long.MAX_VALUE, false));
    }
    
    @Test
    public void BasicMultipleEventSuccess() throws Exception
    {
        nextEvents.add(new Event(EVENT_NAME, "MORE_DATA"));
        nextEvents.add(new Event(EVENT_NAME, "YET_MORE_DATA"));
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
        
        Assert.assertEquals(1, resultService.countResultsBySuccess());
        Assert.assertEquals(2, eventService.count());
        Assert.assertNull(eventService.nextEvent(SERVER_ID, Long.MAX_VALUE, true));
        Assert.assertNotNull(eventService.nextEvent(SERVER_ID, Long.MAX_VALUE, false));
    }
    
    @Test
    public void BasicSerializableEventData() throws Exception
    {
        Object dataThatWillNotSerialize = new DataThatWillNotSerialize();
        nextEvents.add(new Event(EVENT_NAME, dataThatWillNotSerialize));
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
        
        Assert.assertEquals(1, resultService.countResultsBySuccess());
        Assert.assertEquals(1, eventService.count());
        Assert.assertNull("The local data should not be available for other servers.", eventService.nextEvent("RANDOM_SERVER", Long.MAX_VALUE, false));
        Event nextEvent = eventService.nextEvent(SERVER_ID, Long.MAX_VALUE, false);
        Assert.assertNotNull(nextEvent);
        
        // Check that we can get hold of the next event's data
        Assert.assertTrue("Expect exactly the original event data.", dataThatWillNotSerialize == nextEvent.getDataObject());
    }
}
