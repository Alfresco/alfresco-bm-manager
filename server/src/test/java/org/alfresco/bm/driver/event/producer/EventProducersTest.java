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
package org.alfresco.bm.driver.event.producer;

import org.alfresco.bm.driver.event.Event;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @see EventProducer
 * @see EventProducerRegistry
 * @see TerminateEventProducer
 * @see RedirectEventProducer
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class EventProducersTest
{
    private ClassPathXmlApplicationContext ctx;
    private EventProducerRegistry registry;
    private Event eventDataKey;
    private Event eventInMem;
    
    @Before
    public void setUp()
    {
        ctx = new ClassPathXmlApplicationContext("test-EventProducerTest-context.xml");
        ctx.start();
        registry = ctx.getBean(EventProducerRegistry.class);
        
        eventDataKey = new Event("a", "Some data");
        eventDataKey.setId("123456789012345678901234"); 
        eventDataKey.setSessionId("sessionA");
        eventInMem = new Event("a", this);
    }
    
    @After
    public void tearDown()
    {
        if (ctx != null)
        {
            ctx.close();
        }
    }
    
    @Test
    public void testBeansRegistered()
    {
        Assert.assertNotNull(registry.getProducer("redirectToB"));
        Assert.assertNotNull(registry.getProducer("redirectToC"));
        Assert.assertNotNull(registry.getProducer("random"));
        Assert.assertNotNull(registry.getProducer("terminate"));
    }
    
    @Test
    public void testRedirectEventProducer_DataKey()
    {
        Event eventOriginal = eventDataKey;
        EventProducer producer = registry.getProducer("redirectToB");
        List<Event> events = producer.getNextEvents(eventOriginal);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEquals("b", event.getName());
        assertEquals(eventOriginal.getId(), event.getId());
        assertEquals(eventOriginal.getSessionId(), event.getSessionId());
        assertEquals((double) (System.currentTimeMillis()+10000L), event.getScheduledTime(), 200L);
        assertTrue(eventOriginal.getData() == event.getData());
    }
    
    @Test
    public void testRedirectEventProducer_InMem()
    {
        Event eventOriginal = eventInMem;
        EventProducer producer = registry.getProducer("redirectToC");
        List<Event> events = producer.getNextEvents(eventOriginal);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEquals("c", event.getName());
        assertEquals(eventOriginal.getId(), event.getId());
        assertEquals(eventOriginal.getSessionId(), event.getSessionId());
        assertEquals((double) (System.currentTimeMillis()), event.getScheduledTime(), 200L);
        assertTrue(eventOriginal.getData() == event.getData());
    }
    
    @Test
    public void testRandomRedirectEventProducer()
    {
        Event eventOriginal = eventInMem;
        EventProducer producer = registry.getProducer("random");
        int b = 0;
        int c = 0;
        for (int i = 0; i < 100; i++)
        {
            List<Event> events = producer.getNextEvents(eventOriginal);
            assertEquals(1, events.size());
            Event event = events.get(0);
            if (event.getName().equals("b"))
            {
                b++;
            }
            else if (event.getName().equals("c"))
            {
                c++;
            }
            else
            {
                fail("Unknown redirect: " + event);
            }
        }
        assertEquals(50.0, (double) b, 15.0);
        assertEquals(50.0, (double) c, 15.0);
    }
}
