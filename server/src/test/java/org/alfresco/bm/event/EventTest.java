/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

/**
 * @see Event
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class EventTest
{
    @Test
    public void setId()
    {
        Event event = new Event("A", null);
        // Check that the ID validation is done
        try
        {
            event.setId("abcd");
            fail("Incompatible ID not detected.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            event.setId("1234567890abcdefabcdef.a");
            fail("Incompatible ID not detected.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            event.setId("1234567890abcdefabcdefgh");
            fail("Incompatible ID not detected.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            event.setId("1234567890abcdefABCDEFab");
            fail("Incompatible ID not detected: uppercase not allowed.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        // Now successful ones
        event.setId("1234567890abcdefabcdefab");
        event.setId("000000000000000000000000");
        event.setId("aaaaaaaaaaaaaaaaaaaaaaaa");
        event.setId("ffffffffffffffffffffffff");
    }
    
    @Test
    public void implictIds()
    {
        Event event = null;
        event = new Event(Event.EVENT_NAME_START, null);
        assertEquals(Event.EVENT_ID_START, event.getId());
    }
    
    @Test
    public void nullName()
    {
        try
        {
            new Event(null, null);
            fail("Expected null event name to be caught.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    @Test
    public void constructor01()
    {
        long now = System.currentTimeMillis();
        
        Event event = new Event("A", null);
        assertNull(event.getDataKey());
        assertNull(event.getDataObject());
        assertNull(event.getDataOwner());
        assertNull(event.getId());
        assertNull(event.getLockOwner());
        assertEquals(0L, event.getLockTime());
        assertEquals("A", event.getName());
        assertTrue(event.getScheduledTime() >= (now-10L));
        assertNull(event.getSessionId());
    }
    
    @Test
    public void constructor02()
    {
        long now = System.currentTimeMillis();
        
        Event event = new Event("A", now+100L, null);
        assertTrue(event.getScheduledTime() >= (now+90L));
    }
    
    @Test
    public void constructor03()
    {
        long now = System.currentTimeMillis();
        
        Event event = new Event("A", now, null, true);
        assertNull(event.getDataObject());
        assertNotNull(event.getDataKey());
    }
    
    @Test
    public void nullData()
    {
        Event event = new Event("A", 0L, null);
        assertNull(event.getDataKey());
        assertNull(event.getDataObject());
    }
    
    @Test
    public void nullDataInMem()
    {
        Event event = new Event("A", 0L, null, true);
        assertNotNull(event.getDataKey());
        assertNull(event.getDataObject());
    }
    
    @Test
    public void stringData()
    {
        Event event = new Event("A", 0L, "BOB");
        assertNull(event.getDataKey());
        assertEquals("BOB", event.getDataObject());
    }
    
    @Test
    public void stringDataInMem()
    {
        Event event = new Event("A", 0L, "BOB", true);
        assertNotNull(event.getDataKey());
        assertEquals("BOB", event.getDataObject());
    }
    
    @Test
    public void integerData()
    {
        Event event = new Event("A", 0L, Integer.MAX_VALUE);
        assertNull(event.getDataKey());
        assertEquals(Integer.MAX_VALUE, event.getDataObject());
    }
    
    @Test
    public void integerDataInMem()
    {
        Event event = new Event("A", 0L, Integer.MAX_VALUE, true);
        assertNotNull(event.getDataKey());
        assertEquals(Integer.MAX_VALUE, event.getDataObject());
    }
    
    @Test
    public void dbObjectData()
    {
        DBObject bobTheObj = BasicDBObjectBuilder.start().add("BOB", Integer.MAX_VALUE).get();
        
        Event event = new Event("A", 0L, bobTheObj);
        assertNull(event.getDataKey());
        assertEquals(bobTheObj, event.getDataObject());
    }
    
    @Test
    public void dbObjectDataInMem()
    {
        DBObject bobTheObj = BasicDBObjectBuilder.start().add("BOB", Integer.MAX_VALUE).get();
        
        Event event = new Event("A", 0L, bobTheObj, true);
        assertNotNull(event.getDataKey());
        assertEquals(bobTheObj, event.getDataObject());
    }
    
    @Test
    public void someObjectData()
    {
        Event event = new Event("A", 0L, this);
        assertNotNull(event.getDataKey());
        assertEquals(this, event.getDataObject());
    }
    
    @Test
    public void someObjectDataInMem()
    {
        Event event = new Event("A", 0L, this, true);
        assertNotNull(event.getDataKey());
        assertEquals(this, event.getDataObject());
    }
}
