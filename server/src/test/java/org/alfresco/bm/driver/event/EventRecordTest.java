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

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.alfresco.bm.common.EventRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
    public void nullDriverId()
    {
        try
        {
            new EventRecord(null, true, 0L, 0L, "BOB", event);
            fail("Expected null driver ID to be caught.");
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
        
        EventRecord eventRecord = new EventRecord("Driver01", true, now, 20L, "BOB", event);
        eventRecord.setChart(false);
        eventRecord.setId("ID1");
        eventRecord.setStartDelay(25L);
        eventRecord.setWarning("Took too long");
        
        assertEquals("BOB", eventRecord.getData());
        assertEquals(event, eventRecord.getEvent());
        assertEquals("ID1", eventRecord.getId());
        assertEquals("Driver01", eventRecord.getDriverId());
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
