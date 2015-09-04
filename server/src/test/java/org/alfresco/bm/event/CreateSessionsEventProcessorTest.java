/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.alfresco.bm.session.SessionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @see CreateSessionsEventProcessor
 * 
 * @author Derek Hulley
 * @since 2.0.10
 */
@RunWith(JUnit4.class)
public class CreateSessionsEventProcessorTest
{
    private static final String EVENT_NAME_CREATE_SESSIONS = "createSessions";
    private static final String EVENT_NAME_START_SESSION = "startSession";
    private SessionService sessionService;
    private CreateSessionsEventProcessor processor;
    
    @Before
    public void setUp()
    {
        sessionService = Mockito.mock(SessionService.class);
        processor = new TestCreateSessionsEventProcessorOverride(sessionService, EVENT_NAME_START_SESSION, 10, 20);
        processor.setEventNameCreateSessions(EVENT_NAME_CREATE_SESSIONS);
        processor.setTimeBetweenSessions(10L);
        processor.setCheckPeriod(100L);
    }
    
    @After
    public void tearDown()
    {
    }
    
    @Test
    public void testMultiplePasses() throws Exception
    {
        long now = System.currentTimeMillis();
        EventResult result;
        List<Event> events;
        Event event = new Event("start", null);
        
        // First pass
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(0L);
        Mockito.when(sessionService.getActiveSessionsCount()).thenReturn(0L);
        now = System.currentTimeMillis();
        result = processor.processEvent(event);
        events = result.getNextEvents();
        assertTrue(result.isSuccess());
        assertEquals(11, events.size());
        assertEquals(now, events.get(0).getScheduledTime(), 20L);
        assertEquals(EVENT_NAME_START_SESSION, events.get(0).getName());
        assertEquals(now, events.get(1).getScheduledTime(), 30L);
        assertEquals(EVENT_NAME_START_SESSION, events.get(1).getName());
        assertEquals(now, events.get(10).getScheduledTime(), 110L);
        assertEquals(EVENT_NAME_CREATE_SESSIONS, events.get(10).getName());
        
        // Second pass without any sessions complete but some active ones
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(0L);
        Mockito.when(sessionService.getActiveSessionsCount()).thenReturn(5L);
        event = new Event(EVENT_NAME_CREATE_SESSIONS, events.get(10).getData());
        now = System.currentTimeMillis();
        result = processor.processEvent(event);
        events = result.getNextEvents();
        assertTrue(result.isSuccess());
        assertEquals(1, events.size());
        assertEquals(now, events.get(0).getScheduledTime(), 110L);
        assertEquals(EVENT_NAME_CREATE_SESSIONS, events.get(0).getName());
        
        // Third pass: some sessions have finished
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(5L);
        Mockito.when(sessionService.getActiveSessionsCount()).thenReturn(5L);
        event = new Event(EVENT_NAME_CREATE_SESSIONS, events.get(0).getData());
        now = System.currentTimeMillis();
        result = processor.processEvent(event);
        events = result.getNextEvents();
        assertTrue(result.isSuccess());
        assertEquals(6, events.size());
        assertEquals(now, events.get(0).getScheduledTime(), 30L);
        assertEquals(EVENT_NAME_START_SESSION, events.get(0).getName());
        assertEquals(now, events.get(1).getScheduledTime(), 50L);
        assertEquals(EVENT_NAME_START_SESSION, events.get(1).getName());
        assertEquals(now, events.get(5).getScheduledTime(), 110L);
        assertEquals(EVENT_NAME_CREATE_SESSIONS, events.get(5).getName());
        
        // Fourth pass: more sessions have finished
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(19L);
        Mockito.when(sessionService.getActiveSessionsCount()).thenReturn(1L);
        event = new Event(EVENT_NAME_CREATE_SESSIONS, events.get(5).getData());
        now = System.currentTimeMillis();
        result = processor.processEvent(event);
        events = result.getNextEvents();
        assertTrue(result.isSuccess());
        assertEquals(5, events.size());     // 14 finished but only need 5 more to get to 20
        assertEquals(now, events.get(0).getScheduledTime(), 30L);
        assertEquals(EVENT_NAME_START_SESSION, events.get(0).getName());
        assertEquals(now, events.get(1).getScheduledTime(), 50L);
        assertEquals(EVENT_NAME_START_SESSION, events.get(1).getName());
    }

    /**
     * Test class to demonstrate override capabilities
     * 
     * @author Derek Hulley
     */
    private static class TestCreateSessionsEventProcessorOverride extends CreateSessionsEventProcessor
    {
        public TestCreateSessionsEventProcessorOverride(
                SessionService sessionService,
                String outputEventName,
                int concurrentSessions,
                int totalSessions)
        {
            super(sessionService, outputEventName, concurrentSessions, totalSessions);
        }

        @Override
        protected DBObject getNextEventData()
        {
            return new BasicDBObject("sound", "WOOF");
        }
    }
}
