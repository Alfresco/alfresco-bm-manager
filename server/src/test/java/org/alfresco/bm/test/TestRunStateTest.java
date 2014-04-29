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

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @see TestRunState
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class TestRunStateTest implements TestConstants
{
    @Test
    public void illegalValue()
    {
        // Check that illegal types are excluded
        try
        {
            TestRunState.valueOf("Fail");
            fail("Expected IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    private void transition(TestRunState state, TestRunState nextState, boolean expectSuccess)
    {
        if (expectSuccess)
        {
            state.transition(nextState);
        }
        else
        {
            try
            {
                state.transition(nextState);
                fail("Expected failure from " + state + " to " + nextState);
            }
            catch (IllegalArgumentException e)
            {
                // Expected
            }
        }
    }
    
    @Test
    public void transitionNotScheduled()
    {
        transition(TestRunState.NOT_SCHEDULED, TestRunState.NOT_SCHEDULED, true);
        transition(TestRunState.NOT_SCHEDULED, TestRunState.SCHEDULED, true);
        transition(TestRunState.NOT_SCHEDULED, TestRunState.STARTED, false);
        transition(TestRunState.NOT_SCHEDULED, TestRunState.STOPPED, false);
        transition(TestRunState.NOT_SCHEDULED, TestRunState.COMPLETED, false);
    }
    
    @Test
    public void transitionScheduled()
    {
        transition(TestRunState.SCHEDULED, TestRunState.NOT_SCHEDULED, true);
        transition(TestRunState.SCHEDULED, TestRunState.SCHEDULED, true);
        transition(TestRunState.SCHEDULED, TestRunState.STARTED, true);
        transition(TestRunState.SCHEDULED, TestRunState.STOPPED, false);
        transition(TestRunState.SCHEDULED, TestRunState.COMPLETED, false);
    }
    
    @Test
    public void transitionStarted()
    {
        transition(TestRunState.STARTED, TestRunState.NOT_SCHEDULED, false);
        transition(TestRunState.STARTED, TestRunState.SCHEDULED, false);
        transition(TestRunState.STARTED, TestRunState.STARTED, true);
        transition(TestRunState.STARTED, TestRunState.STOPPED, true);
        transition(TestRunState.STARTED, TestRunState.COMPLETED, true);
    }
    
    @Test
    public void transitionStopped()
    {
        transition(TestRunState.STOPPED, TestRunState.NOT_SCHEDULED, false);
        transition(TestRunState.STOPPED, TestRunState.SCHEDULED, false);
        transition(TestRunState.STOPPED, TestRunState.STARTED, false);
        transition(TestRunState.STOPPED, TestRunState.STOPPED, true);
        transition(TestRunState.STOPPED, TestRunState.COMPLETED, false);
    }
    
    @Test
    public void transitionCompleted()
    {
        transition(TestRunState.COMPLETED, TestRunState.NOT_SCHEDULED, false);
        transition(TestRunState.COMPLETED, TestRunState.SCHEDULED, false);
        transition(TestRunState.COMPLETED, TestRunState.STARTED, false);
        transition(TestRunState.COMPLETED, TestRunState.STOPPED, false);
        transition(TestRunState.COMPLETED, TestRunState.COMPLETED, true);
    }
}
