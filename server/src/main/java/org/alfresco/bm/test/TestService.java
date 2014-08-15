/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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

import com.mongodb.DBObject;


/**
 * Service to manage test deployments and test runs.
 * <p/>
 * This service provides for features that cannot be simply covered by low-level
 * modifications directly.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public interface TestService extends TestConstants
{
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
    
    /**
     * Exception generated when a test or test run is not found
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    public class NotFoundException extends Exception
    {
        private static final long serialVersionUID = 5931751540252570038L;

        public NotFoundException(String test, String run)
        {
            super(run == null ?
                    "Test not found: " + test :
                    "Test run not found: " + test + "." + run);
        }
    }
    
    /**
     * Exception generated when a modification was not written because the
     * underlying resource has changed
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    public class ConcurrencyException extends Exception
    {
        private static final long serialVersionUID = 3304257961739769579L;
    }
    
    /**
     * Get the complete metadata for a test
     * 
     * @param test                  the name of the test
     * @return                      the test metadata
     * @throws NotFoundException    if the test could not be found
     */
    DBObject getTestMetadata(String test) throws NotFoundException;
    
    /**
     * Retrieve the precise run state for the given test run
     * 
     * @param test                  the name of the test
     * @param run                   the name of the test run
     * @return                      the current state of the test run
     * 
     * @throws NotFoundException    if the test run could not be found
     */
    TestRunState getTestRunState(String test, String run) throws NotFoundException;
    
    /**
     * Get the complete metadata for a test run
     * 
     * @param test                  the name of the test
     * @param run                   the name of the test run
     * @return                      the test run metadata
     * @throws NotFoundException    if the test run could not be found
     */
    DBObject getTestRunMetadata(String test, String run) throws NotFoundException;
    
    /**
     * Schedule a test run.
     * 
     * @param test                  the name of the test
     * @param run                   the name of the test run
     * @param version               the current version of the test run
     * @param scheduled             the time at which the test should start
     * 
     * @throws NotFoundException    if the test run could not be found
     * @throws RunStateException    if the test run cannot be scheduled any more
     * @throws ConcurrencyException if the test run has been modified
     */
    void scheduleTestRun(String test, String run, int version, long scheduled)
            throws NotFoundException, RunStateException, ConcurrencyException;
    
    /**
     * Terminate a test run.
     * 
     * @param test                  the name of the test
     * @param run                   the name of the test run
     * 
     * @throws NotFoundException    if the test run could not be found
     * @throws RunStateException    if the test run cannot be scheduled any more
     * @throws ConcurrencyException if the test run has been modified
     */
    void terminateTestRun(String test, String run)
            throws NotFoundException, RunStateException, ConcurrencyException;
}
