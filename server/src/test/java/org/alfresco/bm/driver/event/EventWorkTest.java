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
package org.alfresco.bm.driver.event;

import com.mongodb.DB;
import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.common.mongo.MongoEventService;
import org.alfresco.bm.common.mongo.MongoResultService;
import org.alfresco.bm.driver.event.producer.EventProducerRegistry;
import org.alfresco.bm.common.session.MongoSessionService;
import org.alfresco.bm.common.util.junit.tools.MongoDBForTestsFactory;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.MongoLogService;
import org.alfresco.bm.common.util.log.TestRunLogService;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @see EventWork
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class EventWorkTest
{
    private static Log logger = LogFactory.getLog(EventWorkTest.class);
    
    private static final String DRIVER_ID = "testdriver";
    private static final String TEST_NAME = "EventWorkTest";
    private static final String TEST_RUN_NAME = "X";
    private static final String TEST_RUN_FQN = TEST_NAME + "." + TEST_RUN_NAME;
    private static final String EVENT_NAME = "x.y";
    
    private static MongoDBForTestsFactory mongoFactory;
    private MongoEventService eventService;
    private MongoResultService resultService;
    private MongoSessionService sessionService;
    private TestRunLogService logService;
    private DB db;
    private Event event;
    private List<Event> nextEvents;
    private EventProcessor processor;
    private EventProducerRegistry eventProducers;
    private EventWork work;

    
    @Before
    public void setUp() throws Exception
    {
        mongoFactory = new MongoDBForTestsFactory();
        String uri = mongoFactory.getMongoURI();
        logger.debug("Mongo URI: " + uri);
        db = mongoFactory.getObject();
        eventService = new MongoEventService(db, "es");
        eventService.start();
        resultService = new MongoResultService(db, "rs");
        resultService.start();
        sessionService = new MongoSessionService(db, "ss");
        sessionService.start();
        LogService mongoLogService = new MongoLogService(db, Integer.MAX_VALUE, 1000, 0);
        logService = new TestRunLogService(mongoLogService, DRIVER_ID, TEST_NAME, TEST_RUN_NAME);

        event = new Event(EVENT_NAME, "SOME_DATA");
        event.setId(eventService.putEvent(event));
        nextEvents = new ArrayList<Event>(2);
        processor = Mockito.mock(EventProcessor.class);
        eventProducers = new EventProducerRegistry();
        work = new EventWork(
                DRIVER_ID, TEST_RUN_FQN, event,
                new String[] {DRIVER_ID},
                new TestEventProcessor(),
                eventProducers,
                eventService, resultService, sessionService,
                logService);
    }
    
    @After
    public void tearDown() throws Exception
    {
        eventService.stop();
        resultService.stop();
        sessionService.stop();
        mongoFactory.destroy();
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
    public void emptyDriverIdList() throws Exception
    {
        work = new EventWork(
                DRIVER_ID, TEST_RUN_FQN, event,
                new String[] {},
                new TestEventProcessor(),
                eventProducers,
                eventService, resultService, sessionService,
                logService);
        nextEvents.add(new Event(EVENT_NAME, "DATA1"));
        nextEvents.add(new Event(EVENT_NAME, "DATA2"));
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
    }
    
    @Test
    public void testBasicSingleEventSuccess() throws Exception
    {
        EventService anotherEventService = new MongoEventService(db, "es");

        Event event1 = new Event(EVENT_NAME, "DATA1");
        Event event2 = new Event(EVENT_NAME, "DATA2");
        
        nextEvents.add(event1);
        nextEvents.add(event2);
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
        
        assertEquals(1, resultService.countResultsBySuccess());
        assertEquals(2, eventService.count());
        
        // TODO: Check that a driver ID is inserted and enforced correctly
        List<Event> events = eventService.getEvents(0, 2);
        assertEquals(2, events.size());
        Event event = events.get(0);
        assertEquals(DRIVER_ID, event.getDriver());
        
        // The other drivers cannot see anything when respecting driver assignment
        assertNull(anotherEventService.nextEvent("SOME_DRIVER", Long.MAX_VALUE));
        // We cannot see anything if the time is not right
        assertNull(eventService.nextEvent(null, 0L));
        // We look only for things that belong to a specific driver
        assertNotNull(eventService.nextEvent(DRIVER_ID, Long.MAX_VALUE));
        // We look for anything
        assertNotNull(anotherEventService.nextEvent(null, Long.MAX_VALUE));
        // There should be no more
        assertNull(eventService.nextEvent(null, Long.MAX_VALUE));
        assertNull(eventService.nextEvent(null, Long.MAX_VALUE));
    }
    
    @Test
    public void testMultipleEventServiceAccess() throws Exception
    {
        EventService anotherEventService = new MongoEventService(db, "es");

        nextEvents.add(new Event(EVENT_NAME, "MORE_DATA"));
        nextEvents.add(new Event(EVENT_NAME, "YET_MORE_DATA"));
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
        
        assertEquals(1, resultService.countResultsBySuccess());
        assertEquals(2, eventService.count());
        // Any EventService should be able to get the transportable events
        assertNotNull(eventService.nextEvent(null, Long.MAX_VALUE));
        assertNotNull(anotherEventService.nextEvent(null, Long.MAX_VALUE));
    }
    
    @Test
    public void testBasicSerializableEventData() throws Exception
    {
        // Ensure that other EventService instances cannot grab our in-memory data
        EventService anotherEventService = new MongoEventService(db, "es");
        
        Object dataThatWillNotSerialize = new DataThatWillNotSerialize();
        nextEvents.add(new Event(EVENT_NAME, dataThatWillNotSerialize));
        EventResult result = new EventResult(nextEvents);
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        
        work.run();
        
        assertEquals(1, resultService.countResultsBySuccess());
        assertEquals(1, eventService.count());
        // Check that we have memory-stored data and no assigned driver
        Event checkEvent = eventService.getEvents(0, 1).get(0);
        assertTrue("Data should be stored in memory automatically", checkEvent.getDataInMemory());
        assertNull("No driver should be assigned.", checkEvent.getDriver());
        
        assertNull("The local data should not be available for other drivers.", anotherEventService.nextEvent(null, Long.MAX_VALUE));
        Event nextEvent = eventService.nextEvent(null, Long.MAX_VALUE);
        assertNotNull(nextEvent);
        
        // Check that we can get hold of the next event's data
        assertTrue("Expect exactly the original event data.", dataThatWillNotSerialize == nextEvent.getData());
    }
    
    @Test
    public void testSessionPropagation_PropagateWhenOneNextEvent() throws Exception
    {
        String sessionId = sessionService.startSession(null);
        event.setSessionId(sessionId);
        
        nextEvents.add(new Event(EVENT_NAME, null));
        EventResult result = new EventResult(nextEvents);
        
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        Mockito.when(processor.isAutoPropagateSessionId()).thenReturn(true);
        
        work.run();
        
        assertEquals(1L, sessionService.getActiveSessionsCount());
        assertEquals(0L, sessionService.getCompletedSessionsCount());
        assertEquals(-1L, sessionService.getSessionEndTime(sessionId));
    }
    
    @Test
    public void testSessionPropagation_CloseWhenNoNextEvent() throws Exception
    {
        String sessionId = sessionService.startSession(null);
        event.setSessionId(sessionId);
        
        EventResult result = new EventResult(nextEvents);
        
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        Mockito.when(processor.isAutoPropagateSessionId()).thenReturn(true);
        
        work.run();
        
        assertEquals(0L, sessionService.getActiveSessionsCount());
        assertEquals(1L, sessionService.getCompletedSessionsCount());
        assertEquals(1L, sessionService.getAllSessionsCount());
        assertTrue(sessionService.getSessionEndTime(sessionId) > 0L);
    }
    
    @Test
    public void testSessionPropagation_CloseWhenMultipleNextEvents() throws Exception
    {
        String sessionId = sessionService.startSession(null);
        event.setSessionId(sessionId);
        
        nextEvents.add(new Event(EVENT_NAME, null));
        nextEvents.add(new Event(EVENT_NAME, null));
        EventResult result = new EventResult(nextEvents);
        
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenReturn(result);
        Mockito.when(processor.isAutoPropagateSessionId()).thenReturn(true);
        
        work.run();
        
        assertEquals(0L, sessionService.getActiveSessionsCount());
        assertEquals(1L, sessionService.getCompletedSessionsCount());
        assertEquals(1L, sessionService.getAllSessionsCount());
        assertTrue(sessionService.getSessionEndTime(sessionId) > 0L);
    }
    
    @Test
    public void testSessionPropagation_CloseWhenException() throws Exception
    {
        String sessionId = sessionService.startSession(null);
        event.setSessionId(sessionId);
        
        nextEvents.add(new Event(EVENT_NAME, null));
        
        Mockito.when(processor.processEvent(Mockito.any(Event.class), Mockito.any(StopWatch.class))).thenThrow(new RuntimeException());
        Mockito.when(processor.isAutoPropagateSessionId()).thenReturn(true);
        
        work.run();
        
        assertEquals(0L, sessionService.getActiveSessionsCount());
        assertEquals(1L, sessionService.getAllSessionsCount());
        assertTrue(sessionService.getSessionEndTime(sessionId) > 0L);
    }
}
