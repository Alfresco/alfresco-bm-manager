package org.alfresco.bm.event.selector;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventProcessor;
import org.alfresco.bm.event.EventProcessorRegistry;
import org.alfresco.bm.event.EventResult;
import org.apache.commons.lang3.time.StopWatch;
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
        
        List<EventSuccessorInfo> list1 = new ArrayList<EventSuccessorInfo>();
        list1.add(new EventSuccessorInfo("A", "2,5"));
        list1.add(new EventSuccessorInfo("B", "2,5"));
        list1.add(new EventSuccessorInfo("  C  ", "2,5", 80, 1000L));
        
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
            if (chosenEventName.equals("C"))
            {
                Assert.assertTrue("Time delay not added", chosenEvent.getScheduledTime() > (now + 500L));
            }
        }
        
        // Check that the distribution is approximately correct
        
        Assert.assertTrue("A was chosen too much.", aProcessor.count < 200);    // Looking up to 20% of 1000
        Assert.assertTrue("B was chosen too much.", bProcessor.count < 200);    // Looking up to 20% of 1000
        Assert.assertTrue("C was chosen too little.", cProcessor.count > 600);  // Looking down to 60% of 1000
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
        public void propagateSessionId(Event event, Event nextEvent)
        {
            throw new UnsupportedOperationException();
        }
    }
}
