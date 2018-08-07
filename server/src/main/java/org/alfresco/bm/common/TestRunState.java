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
package org.alfresco.bm.common;


/**
 * Class that controls the state transitions for the test runs
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public enum TestRunState
{
    NOT_SCHEDULED
    {
        @Override
        public TestRunState transition(TestRunState next)
        {
            switch (next)
            {
                case NOT_SCHEDULED:
                case SCHEDULED:
                    return next;
                default:
                    // Fall through
            }
            return super.transition(next);
        }
    },
    SCHEDULED
    {
        @Override
        public TestRunState transition(TestRunState next)
        {
            switch (next)
            {
                case NOT_SCHEDULED:
                case SCHEDULED:
                case STARTED:
                case STOPPED:   // 2015-11-10 allowed STOPPED as transition after scheduled. Allow to stop a BM run with no driver as well as config issues in server connection that now stop the run
                    return next;
                default:
                    // Fall through
            }
            return super.transition(next);
        }
    },
    STARTED
    {
        @Override
        public TestRunState transition(TestRunState next)
        {
            switch (next)
            {
                case STARTED:
                case STOPPED:
                case COMPLETED:
                    return next;
                default:
                    // Fall through
            }
            return super.transition(next);
        }
    },
    STOPPED
    {
        @Override
        public TestRunState transition(TestRunState next)
        {
            switch (next)
            {
                case STOPPED:
                    return next;
                default:
                    // Fall through
            }
            return super.transition(next);
        }
    },
    COMPLETED
    {
        @Override
        public TestRunState transition(TestRunState next)
        {
            switch (next)
            {
                case COMPLETED:
                    return next;
                default:
                    // Fall through
            }
            return super.transition(next);
        }
    };
    
    TestRunState()
    {
    }
    
    /**
     * Checks that a transition is allowed
     * 
     * @param next          the state to transition to
     * @return              the new state
     */
    public TestRunState transition(TestRunState next)
    {
        throw new IllegalArgumentException("Transition to state " + next + " is not allowed.");
    }
}
