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
package org.alfresco.bm.driver.test;

import org.alfresco.bm.driver.event.EventService;
import org.alfresco.bm.common.ResultService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @see EventCountCompletionEstimator
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class EventCountCompletionEstimatorTest
{
    private static final String EVENT = "xEVENTx";
    private static final long EVENT_TOTAL = 31L;
    
    private EventService eventService = Mockito.mock(EventService.class);
    private ResultService resultService = Mockito.mock(ResultService.class);
    private EventCountCompletionEstimator estimator;
    
    @Before
    public void beforeTest()
    {
        estimator = new EventCountCompletionEstimator(eventService, resultService, EVENT, EVENT_TOTAL);
        // Ensure that we only hit the underlying services once per test
        estimator.setCheckPeriod(120000L);
        // Reset Mockito
        Mockito.reset(eventService);
        Mockito.reset(resultService);
    }
    
    @Test
    public void testNotStarted()
    {
        Mockito.when(eventService.count()).thenReturn(0L);                                      //  0 events
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(0L);                     //  0 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(0L);                     //  0 failed results
        Mockito.when(resultService.countResultsByEventName(EVENT)).thenReturn(0L);              //  0 specific results
        double completion = estimator.getCompletion();
        Assert.assertTrue("Completion was: " + completion, completion <= 0.0);
        Assert.assertEquals(0L, estimator.getResultsSuccess());
        Assert.assertEquals(0L, estimator.getResultsFail());
        Mockito.verify(eventService, Mockito.times(1)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(eventService, Mockito.times(1)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
    }
    
    @Test
    public void testInProgress01()
    {
        Mockito.when(eventService.count()).thenReturn(23L);                                 //  some events still in queue
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(76L);                //  76 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(23L);                //  23 failed results
        Mockito.when(resultService.countResultsByEventName(EVENT)).thenReturn(5L);          //  5 specific results
        double completion = estimator.getCompletion();
        Assert.assertTrue("Completion was: " + completion, completion > 0.15 && completion < 0.17);
        Assert.assertEquals(76L, estimator.getResultsSuccess());
        Assert.assertEquals(23L, estimator.getResultsFail());
        Mockito.verify(eventService, Mockito.times(0)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(eventService, Mockito.times(0)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
    }
    
    @Test
    public void testDoneWithAllMonitoredEvents()
    {
        Mockito.when(eventService.count()).thenReturn(1L);                                      //  1 other event
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(103L);                   //  103 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(89L);                    //  89 failed results
        Mockito.when(resultService.countResultsByEventName(EVENT)).thenReturn(EVENT_TOTAL);     //  31 specific results
        double completion = estimator.getCompletion();
        Assert.assertEquals("Completion was: " + completion, 1.0, completion, 0.05);
        Assert.assertEquals(103L, estimator.getResultsSuccess());
        Assert.assertEquals(89L, estimator.getResultsFail());
        Mockito.verify(eventService, Mockito.times(0)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(eventService, Mockito.times(0)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
    }
    
    @Test
    public void testDoneWithAllEvents()
    {
        Mockito.when(eventService.count()).thenReturn(0L);                                      //  no other event
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(109L);                   //  109 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(91L);                    //  91 failed results
        Mockito.when(resultService.countResultsByEventName(EVENT)).thenReturn(EVENT_TOTAL);     //  31 specific results
        double completion = estimator.getCompletion();
        Assert.assertTrue("Completion was: " + completion, completion >= 1.0);
        Assert.assertEquals(109L, estimator.getResultsSuccess());
        Assert.assertEquals(91L, estimator.getResultsFail());
        Mockito.verify(eventService, Mockito.times(0)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(eventService, Mockito.times(0)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
    }
    
    @Test
    public void testDoneWithoutTotalReached()
    {
        Mockito.when(eventService.count()).thenReturn(0L);                                      //  no other event
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(1L);                     //  1 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(0L);                     //  0 failed results
        Mockito.when(resultService.countResultsByEventName(EVENT)).thenReturn(0L);              //  2 specific results
        double completion = estimator.getCompletion();
        Assert.assertTrue("Completion was: " + completion, completion >= 1.0);
        Assert.assertEquals(1L, estimator.getResultsSuccess());
        Assert.assertEquals(0L, estimator.getResultsFail());
        Mockito.verify(eventService, Mockito.times(1)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(eventService, Mockito.times(1)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByEventName(EVENT);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
    }
}
