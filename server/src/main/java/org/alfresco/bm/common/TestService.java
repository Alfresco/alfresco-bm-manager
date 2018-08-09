/*
 * #%L
 * Alfresco Benchmark Manager
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

import com.mongodb.DBObject;
import org.alfresco.bm.common.util.exception.ConcurrencyException;
import org.alfresco.bm.common.util.exception.NotFoundException;
import org.alfresco.bm.common.util.exception.RunStateException;

/**
 * Service to manage test deployments and test runs.
 * <p/>
 * This service provides for features that cannot be simply covered by low-level
 * modifications directly.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public interface TestService
{

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
