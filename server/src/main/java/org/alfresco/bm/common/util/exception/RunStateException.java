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
package org.alfresco.bm.common.util.exception;

import org.alfresco.bm.common.TestRunState;

/**
 * Exception generated when an attempt is made to change the state of a test run
 * in a way that is not supported.
 *
 * @author Derek Hulley
 * @since 2.0
 */
public class RunStateException extends Exception
{
    private static final long serialVersionUID = 6589647065852440835L;
    private final String test;
    private final String run;
    private final TestRunState previousState;
    private final TestRunState newState;
    public RunStateException(String test, String run, TestRunState previousState, TestRunState newState)
    {
        super("The test run '" + test + "." + run + "' state cannot be change from " + previousState + " to " + newState);
        this.test = test;
        this.run = run;
        this.previousState = previousState;
        this.newState = newState;
    }
    public String getTest()
    {
        return test;
    }
    public String getRun()
    {
        return run;
    }
    public TestRunState getPreviousState()
    {
        return previousState;
    }
    public TestRunState getNewState()
    {
        return newState;
    }
}
