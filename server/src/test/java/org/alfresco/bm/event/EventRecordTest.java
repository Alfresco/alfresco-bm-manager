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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

/**
 * @see EventRecord
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class EventRecordTest
{
    private static Event event = new Event("A", "EVENT DATA");

    @Test
    public void nullEvent()
    {
        try
        {
            new EventRecord("HOST", true, 0L, 0L, "BOB", null);
            fail("Expected null event to be caught.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    @Test
    public void nullServerId()
    {
        try
        {
            new EventRecord(null, true, 0L, 0L, "BOB", event);
            fail("Expected null server ID to be caught.");
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
        
        EventRecord eventRecord = new EventRecord("Server01", true, now, 20L, "BOB", event);
        eventRecord.setChart(false);
        eventRecord.setId("ID1");
        eventRecord.setStartDelay(25L);
        eventRecord.setWarning("Took too long");
        
        assertEquals("BOB", eventRecord.getData());
        assertEquals(event, eventRecord.getEvent());
        assertEquals("ID1", eventRecord.getId());
        assertEquals("Server01", eventRecord.getServerId());
        assertEquals(25L, eventRecord.getStartDelay());
        assertEquals(now, eventRecord.getStartTime());
        assertEquals(20L, eventRecord.getTime());
        assertEquals("Took too long", eventRecord.getWarning());
    }
    
    @Test
    public void nullData()
    {
        EventRecord eventRecord = new EventRecord("A", false, 0L, 0L, null, event);
        assertNull(eventRecord.getData());
    }
    
    @Test
    public void stringData()
    {
        EventRecord eventRecord = new EventRecord("A", false, 0L, 0L, "BOB", event);
        assertEquals("BOB", eventRecord.getData());
    }
    
    @Test
    public void integerData()
    {
        EventRecord eventRecord = new EventRecord("A", false, 0L, 0L, Integer.MAX_VALUE, event);
        assertEquals(Integer.MAX_VALUE, eventRecord.getData());
    }
    
    @Test
    public void dbObjectData()
    {
        DBObject bobTheObj = BasicDBObjectBuilder.start().add("BOB", Integer.MAX_VALUE).get();
        
        EventRecord eventRecord = new EventRecord("A", false, 0L, 0L, bobTheObj, event);
        assertEquals(bobTheObj, eventRecord.getData());
    }
    
    @Test
    public void someObjectData()
    {
        try
        {
            new EventRecord("A", false, 0L, 0L, this, event);
            fail("EventRecord cannot store arbitrary values.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
}
