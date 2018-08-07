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

import org.alfresco.bm.common.EventRecord;
import org.alfresco.bm.driver.event.EventService;
import org.alfresco.bm.common.ResultService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @see ElapsedTimeCompletionEstimator
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class ElapsedTimeCompletionEstimatorTest
{
    private EventService eventService = Mockito.mock(EventService.class);
    private ResultService resultService = Mockito.mock(ResultService.class);
    private EventRecord firstResult = Mockito.mock(EventRecord.class);
    private ElapsedTimeCompletionEstimator estimator;
    
    @Before
    public void beforeTest()
    {
        estimator = new ElapsedTimeCompletionEstimator(eventService, resultService, "SECONDS", 60L);
        // Ensure that we only hit the underlying services once per test
        estimator.setCheckPeriod(120000L);
        // Reset Mockito
        Mockito.reset(resultService);
    }
    
    @Test
    public void testNotStarted()
    {
        Mockito.when(resultService.getFirstResult()).thenReturn(null);
        Assert.assertTrue(estimator.getCompletion() <= 0.0);
        Mockito.verify(resultService, Mockito.times(1)).getFirstResult();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(1)).count();
        
        // Check caching
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).getFirstResult();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(1)).count();
    }
    
    @Test
    public void testInProgress()
    {
        long testStart = System.currentTimeMillis() - (30 * 1000L);
        Mockito.when(resultService.getFirstResult()).thenReturn(firstResult);
        Mockito.when(firstResult.getStartTime()).thenReturn(testStart);
        Assert.assertEquals(0.5, estimator.getCompletion(), 0.1);
        Mockito.verify(resultService, Mockito.times(1)).getFirstResult();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(0)).count();
        
        // Check caching
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).getFirstResult();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(0)).count();
    }
    
    @Test
    public void testDone()
    {
        long testStart = System.currentTimeMillis() - (61 * 1000L);
        Mockito.when(resultService.getFirstResult()).thenReturn(firstResult);
        Mockito.when(firstResult.getStartTime()).thenReturn(testStart);
        Assert.assertEquals(1.0, estimator.getCompletion(), 0.05);
        Mockito.verify(resultService, Mockito.times(1)).getFirstResult();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(0)).count();
        
        // Check caching
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).getFirstResult();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(0)).count();
    }
}
