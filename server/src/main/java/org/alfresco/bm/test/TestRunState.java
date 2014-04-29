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

/**
 * Class that controls the state transitions for the test runs
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public enum TestRunState implements TestConstants
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
    
    private TestRunState()
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
