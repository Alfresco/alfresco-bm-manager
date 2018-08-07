/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.driver.test;

import org.alfresco.bm.driver.event.EventService;
import org.alfresco.bm.common.ResultService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

/**
 * @see CompoundCompletionEstimator
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class CompoundCompletionEstimatorTest
{
    private EventService eventService = Mockito.mock(EventService.class);
    private ResultService resultService = Mockito.mock(ResultService.class);
    private CompletionEstimator estimator1 = Mockito.mock(CompletionEstimator.class);
    private CompletionEstimator estimator2 = Mockito.mock(CompletionEstimator.class);
    private CompletionEstimator estimator3 = Mockito.mock(CompletionEstimator.class);
    private CompoundCompletionEstimator estimator;
    
    @Before
    public void beforeTest()
    {
        List<CompletionEstimator> estimators = Arrays.asList(estimator1, estimator2, estimator3);
        
        estimator = new CompoundCompletionEstimator(eventService, resultService, estimators);
        // Ensure that we only hit the underlying services once per test
        estimator.setCheckPeriod(120000L);
        // Reset Mockito
        Mockito.reset(resultService);
    }
    
    @Test
    public void testNotStarted()
    {
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(0L);
        Mockito.when(resultService.countResultsByFailure()).thenReturn(0L);
        Mockito.when(estimator1.getCompletion()).thenReturn(0.0);
        Mockito.when(estimator2.getCompletion()).thenReturn(0.0);
        Mockito.when(estimator3.getCompletion()).thenReturn(0.0);
        Assert.assertEquals(0.0, estimator.getCompletion(), 0.05);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(estimator1, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator2, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator3, Mockito.times(1)).getCompletion();

        // Make sure that we cache correctly
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(estimator1, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator2, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator3, Mockito.times(1)).getCompletion();
    }
    
    @Test
    public void testInProgress01()
    {
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(9L);
        Mockito.when(resultService.countResultsByFailure()).thenReturn(1L);
        Mockito.when(estimator1.getCompletion()).thenReturn(0.1);
        Mockito.when(estimator2.getCompletion()).thenReturn(0.7);
        Mockito.when(estimator3.getCompletion()).thenReturn(0.5);
        Assert.assertEquals(0.7, estimator.getCompletion(), 0.05);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(estimator1, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator2, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator3, Mockito.times(1)).getCompletion();

        // Make sure that we cache correctly
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(estimator1, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator2, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator3, Mockito.times(1)).getCompletion();
    }
    
    @Test
    public void testDoneWithAllEvents()
    {
        Mockito.when(resultService.countResultsBySuccess()).thenReturn(18L);
        Mockito.when(resultService.countResultsByFailure()).thenReturn(2L);
        Mockito.when(estimator1.getCompletion()).thenReturn(0.2);
        Mockito.when(estimator2.getCompletion()).thenReturn(0.8);
        Mockito.when(estimator3.getCompletion()).thenReturn(1.1);
        Assert.assertEquals(1.0, estimator.getCompletion(), 0.05);
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(estimator1, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator2, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator3, Mockito.times(1)).getCompletion();

        // Make sure that we cache correctly
        estimator.getCompletion();
        Mockito.verify(resultService, Mockito.times(1)).countResultsBySuccess();
        Mockito.verify(resultService, Mockito.times(1)).countResultsByFailure();
        Mockito.verify(estimator1, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator2, Mockito.times(1)).getCompletion();
        Mockito.verify(estimator3, Mockito.times(1)).getCompletion();
    }
}
