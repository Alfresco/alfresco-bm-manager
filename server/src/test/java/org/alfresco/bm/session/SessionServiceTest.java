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
package org.alfresco.bm.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @see SessionService
 * 
 * @author Derek Hulley
 * @since 1.4
 */
@RunWith(JUnit4.class)
public class SessionServiceTest
{
    private final static String COLLECTION_BM_USER_DATA_SERVICE = "BenchmarkSessionServiceTest";
    
    private AbstractApplicationContext ctx;
    private SessionService sessionService;

    @Before
    public void setUp()
    {
        Properties props = new Properties();
        props.put("mongoCollection", COLLECTION_BM_USER_DATA_SERVICE);
        
        ctx = new ClassPathXmlApplicationContext(new String[] {"test-MongoSessionServiceTest-context.xml"}, false);
        ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("TestProps", props));
        ctx.refresh();
        ctx.start();
        sessionService = ctx.getBean(SessionService.class);
    }
    
    @After
    public void tearDown()
    {
        ctx.close();
    }
    
    @Test
    public void nullData()
    {
        String sessionId = sessionService.startSession(null);
        assertNull("Null data should be OK for a session. ", sessionService.getSessionData(sessionId));
    }
    
    @Test
    public void simpleLifecycle()
    {
        long sessionAllCountStart = sessionService.getAllSessionsCount();
        assertEquals("Should be no active sessions. ", 0L, sessionService.getActiveSessionsCount());
        
        DBObject data = new BasicDBObject("key", "abc");
        String sessionId = sessionService.startSession(data);
        assertTrue("Start time not set.", sessionService.getSessionStartTime(sessionId) > 0L);
        assertEquals("End time set.", -1L, sessionService.getSessionEndTime(sessionId));
        assertEquals("Elapsed time set.", -1L, sessionService.getSessionElapsedTime(sessionId));
        assertEquals("Data not set.", data, sessionService.getSessionData(sessionId));
        assertEquals("Should be one active session. ", 1L, sessionService.getActiveSessionsCount());
        
        // Update the data
        data = new BasicDBObject("key", "def");
        sessionService.setSessionData(sessionId, data);
        assertEquals("Data not set.", "def", sessionService.getSessionData(sessionId).get("key"));
        
        // End the session
        sessionService.endSession(sessionId);
        assertTrue("End time updated.", sessionService.getSessionEndTime(sessionId) > 0L);
        assertTrue("Elapsed time updated.", sessionService.getSessionElapsedTime(sessionId) > 0L);
        assertEquals("Should be no active sessions. ", 0L, sessionService.getActiveSessionsCount());
        
        assertEquals("Session count did not increase by 1. ", sessionAllCountStart + 1L, sessionService.getAllSessionsCount());
    }
    
    /**
     * Make sure that there are no missing indexes.
     */
    @Test
    public void doMany()
    {
        for (int i = 0; i < 200; i++)
        {
            simpleLifecycle();
        }
        assertEquals(200, sessionService.getAllSessionsCount());
    }
    
    @Test
    public void failureModes()
    {
        String sessionId = "does not exist";
        
        try
        {
            sessionService.getSessionData(sessionId);
            Assert.fail("Invalid SessionId should throw RuntimeException.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        try
        {
            sessionService.getSessionElapsedTime(sessionId);
            Assert.fail("Invalid SessionId should throw RuntimeException.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        try
        {
            sessionService.getSessionEndTime(sessionId);
            Assert.fail("Invalid SessionId should throw RuntimeException.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        try
        {
            sessionService.getSessionStartTime(sessionId);
            Assert.fail("Invalid SessionId should throw RuntimeException.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        try
        {
            sessionService.setSessionData(sessionId, null);
            Assert.fail("Invalid SessionId should throw RuntimeException.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        try
        {
            sessionService.endSession(sessionId);
            Assert.fail("Invalid SessionId should throw RuntimeException.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
    }
}
