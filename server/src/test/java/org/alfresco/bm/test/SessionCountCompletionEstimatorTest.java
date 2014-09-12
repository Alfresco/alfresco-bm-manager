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
package org.alfresco.bm.test;

import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.session.SessionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @see SessionCountCompletionEstimator
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class SessionCountCompletionEstimatorTest
{
    private static final long SESSION_TOTAL = 20L;
    
    private EventService eventService = Mockito.mock(EventService.class);
    private ResultService resultService = Mockito.mock(ResultService.class);
    private SessionService sessionService = Mockito.mock(SessionService.class);
    private SessionCountCompletionEstimator estimator;
    
    @Before
    public void beforeTest()
    {
        estimator = new SessionCountCompletionEstimator(eventService, resultService, sessionService, SESSION_TOTAL);
        // Ensure that we only hit the underlying services once per test
        estimator.setCheckPeriod(120000L);
        // Reset Mockito
        Mockito.reset(eventService);
        Mockito.reset(resultService);
        Mockito.reset(sessionService);
    }
    
    @Test
    public void testNotStarted() throws Exception
    {
        Mockito.when(eventService.count()).thenReturn(0L);                                      //  0 events
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(0L);                     //  0 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(0L);                     //  0 failed results
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(0L);                //  0 completed sessions
        double completion = estimator.getCompletion();
        Assert.assertTrue("Completion was: " + completion, completion <= 0.0);
        Assert.assertEquals(0L, estimator.getResultsSuccess());
        Assert.assertEquals(0L, estimator.getResultsFail());
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();
    }
    
    @Test
    public void testInProgress01() throws Exception
    {
        Mockito.when(eventService.count()).thenReturn(10L);                                     //  10 events
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(8L);                     //  8 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(2L);                     //  2 failed results
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(0L);                //  0 completed sessions
        double completion = estimator.getCompletion();
        Assert.assertTrue("Completion was: " + completion, completion <= 0.0);
        Assert.assertEquals(8L, estimator.getResultsSuccess());
        Assert.assertEquals(2L, estimator.getResultsFail());
        Mockito.verify(eventService, Mockito.times(1)).count();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();
    }
    
    @Test
    public void testInProgress02() throws Exception
    {
        Mockito.when(eventService.count()).thenReturn(10L);                                     //  20 events
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(14L);                    //  14 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(6L);                     //  6 failed results
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(10L);               //  10 completed sessions
        double completion = estimator.getCompletion();
        Assert.assertEquals("Completion was: " + completion, 0.5, completion, 0.01);
        Assert.assertEquals(14L, estimator.getResultsSuccess());
        Assert.assertEquals(6L, estimator.getResultsFail());
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();
    }
    
    @Test
    public void testComplete() throws Exception
    {
        Mockito.when(eventService.count()).thenReturn(32L);                                     //  32 events
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(24L);                    //  24 successful results
        Mockito.when(resultService.countResultsByFailure()).thenReturn(8L);                     //  8 failed results
        Mockito.when(sessionService.getCompletedSessionsCount()).thenReturn(SESSION_TOTAL);     //  SESSION_TOTAL completed sessions
        double completion = estimator.getCompletion();
        Assert.assertEquals("Completion was: " + completion, 1.0, completion, 0.01);
        Assert.assertEquals(24L, estimator.getResultsSuccess());
        Assert.assertEquals(8L, estimator.getResultsFail());
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();

        // Make sure that we cache correctly
        estimator.getCompletion();
        estimator.getResultsSuccess();
        estimator.getResultsFail();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(sessionService, Mockito.times(1)).getCompletedSessionsCount();
    }
}
