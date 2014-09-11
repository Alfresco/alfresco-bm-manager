package org.alfresco.bm.event.selector;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventProcessor;
import org.alfresco.bm.event.EventProcessorRegistry;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.event.EventWeight;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
        public EventResult processEvent(Event event, StopWatch stopWatch) throws Exception
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
