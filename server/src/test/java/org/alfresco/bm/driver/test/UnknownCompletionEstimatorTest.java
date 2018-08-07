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
 * @see UnknownCompletionEstimator
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class UnknownCompletionEstimatorTest
{
    private EventService eventService = Mockito.mock(EventService.class);
    private ResultService resultService = Mockito.mock(ResultService.class);
    private UnknownCompletionEstimator estimator;
    
    @Before
    public void beforeTest()
    {
        estimator = new UnknownCompletionEstimator(eventService, resultService);
        // Ensure that we only hit the underlying services once per test
        estimator.setCheckPeriod(120000L);
        // Reset Mockito
        Mockito.reset(resultService);
    }
    
    @Test
    public void testNotStarted()
    {
        Mockito.when(resultService.countResults()).thenReturn(0L);
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(0L);
        Mockito.when(resultService.countResultsByFailure()).thenReturn(0L);
        Mockito.when(eventService.count()).thenReturn(0L);
        Assert.assertTrue(estimator.getCompletion() <= 0.0);
        Mockito.verify(resultService, Mockito.times(1)).countResults();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(1)).count();

        // Make sure that we cache correctly
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).countResults();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(1)).count();
    }
    
    @Test
    public void testInProgress01()
    {
        Mockito.when(resultService.countResults()).thenReturn(16L);
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(14L);
        Mockito.when(resultService.countResultsByFailure()).thenReturn(2L);
        Mockito.when(eventService.count()).thenReturn(4L);
        Assert.assertTrue(estimator.getCompletion() <= 0.0);
        Mockito.verify(resultService, Mockito.times(1)).countResults();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(2)).count();

        // Make sure that we cache correctly
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).countResults();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(2)).count();
    }
    
    @Test
    public void testDoneWithAllEvents()
    {
        Mockito.when(resultService.countResults()).thenReturn(20L);
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(18L);
        Mockito.when(resultService.countResultsByFailure()).thenReturn(2L);
        Mockito.when(eventService.count()).thenReturn(0L);
        Assert.assertTrue(estimator.getCompletion() >= 1.0);
        Mockito.verify(resultService, Mockito.times(1)).countResults();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(1)).count();

        // Make sure that we cache correctly
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).countResults();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(eventService, Mockito.times(1)).count();
    }
}
