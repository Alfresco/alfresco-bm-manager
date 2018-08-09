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
package org.alfresco.bm.driver.event.selector;

import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.driver.event.EventProcessor;
import org.alfresco.bm.driver.event.EventProcessorRegistry;
import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.driver.event.EventWeight;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link EventSelector} 
 * 
 * @author Derek Hulley
 * @since 1.4
 */
@RunWith(JUnit4.class)
public class EventSelectorTest
{
    private static CounterTestEventProcessor aProcessor = new CounterTestEventProcessor();
    private static CounterTestEventProcessor bProcessor = new CounterTestEventProcessor();
    private static CounterTestEventProcessor cProcessor = new CounterTestEventProcessor();
    private static EventProcessorRegistry registry;
    private static RandomWeightedEventSelector random1;
    
    @BeforeClass
    public static void setUp()
    {
        registry = new EventProcessorRegistry();
        registry.register("A", aProcessor);
        registry.register("B", bProcessor);
        registry.register("C", cProcessor);
        
        List<EventWeight> list1 = new ArrayList<EventWeight>();
        list1.add(new EventWeight("A", "2,5.0"));
        list1.add(new EventWeight("B", "2.0,5"));
        list1.add(new EventWeight("  C  ", 19.5, "2.0,5.0"));
        
        random1 = new RandomWeightedEventSelector(registry, list1);
    }
    
    /**
     * This does nothing except to confirm that the set up is working
     */
    @Test
    public void testSetUp()
    {
        // 
    }
    
    /**
     * Checks that the weighting is approximately correct
     */
    @Test
    public void testWeighting() throws Exception
    {
        for (int i = 0; i < 1000; i++)
        {
            long now = System.currentTimeMillis();
            Event chosenEvent = random1.nextEvent(null, null);
            Assert.assertNotNull(chosenEvent);
            String chosenEventName = chosenEvent.getName();
            CounterTestEventProcessor chosenProcessor = (CounterTestEventProcessor) registry.getProcessor(chosenEventName);
            chosenProcessor.count++;
            // Check that the time is approximately correct
            Assert.assertEquals("Time not set to 'now'", (double) chosenEvent.getScheduledTime(), (double) now, 100L);
        }
        
        // Check that the distribution is approximately correct
        
        Assert.assertTrue("A was chosen too much: " + aProcessor.count, aProcessor.count < 300);    // ~ 25% of 1000
        Assert.assertTrue("B was chosen too much: " + bProcessor.count, bProcessor.count < 300);    // ~ 25% of 1000
        Assert.assertTrue("C was chosen too little: " + cProcessor.count, cProcessor.count > 400);  // ~ 50% of 1000
    }

    /**
     * Test event processor that allows us to count usage
     * @author Derek Hulley
     * @since 1.4
     */
    private static class CounterTestEventProcessor implements EventProcessor
    {
        public int count = 0;

        @Override
        public String getName()
        {
            return "test";
        }

        @Override
        public long getWarnDelay()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isChart()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventResult processEvent(Event event, StopWatch stopWatch)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAutoPropagateSessionId()
        {
            return true;
        }

        @Override
        public boolean isAutoCloseSessionId()
        {
            return true;
        }
    }
}
