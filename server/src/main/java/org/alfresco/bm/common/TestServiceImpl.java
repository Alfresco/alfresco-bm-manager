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
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.common.util.exception.ConcurrencyException;
import org.alfresco.bm.common.util.exception.NotFoundException;
import org.alfresco.bm.common.util.exception.ObjectNotFoundException;
import org.alfresco.bm.common.util.exception.RunStateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import java.util.Date;

import static org.alfresco.bm.common.TestConstants.FIELD_ID;
import static org.alfresco.bm.common.TestConstants.FIELD_STARTED;
import static org.alfresco.bm.common.TestConstants.FIELD_STATE;
import static org.alfresco.bm.common.TestConstants.FIELD_VERSION;

/**
 * Provide support to manipulate and control tests
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class TestServiceImpl implements TestService
{
    private static Log logger = LogFactory.getLog(TestServiceImpl.class);

    private final MongoTestDAO testDAO;
    
    public TestServiceImpl(final MongoTestDAO testDAO)
    {
        this.testDAO = testDAO;
    }
    
    @Override
    public DBObject getTestMetadata(String test) throws NotFoundException
    {
        DBObject testObj = testDAO.getTest(test, false);
        if (testObj == null)
        {
            throw new NotFoundException(test, null);
        }
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Retreived test metadata for '" + test + ".");
        }
        return testObj;
    }
    
    @Override
    public TestRunState getTestRunState(String test, String run) throws NotFoundException
    {
        DBObject testRunObj;
        try
        {
            testRunObj = testDAO.getTestRun(test, run, false);
        }
        catch (ObjectNotFoundException e)
        {
            throw new NotFoundException(test, null, e);
        }
        
        // Check the state transition
        String stateStr = (String) testRunObj.get(FIELD_STATE);
        TestRunState state = TestRunState.valueOf(stateStr);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Retreived test run state for '" + test + "." + run + "': " + state);
        }
        return state;
    }
    
    @Override
    public DBObject getTestRunMetadata(String test, String run) throws NotFoundException
    {
        DBObject testRunObj;
        try
        {
            testRunObj = testDAO.getTestRun(test, run, false);
        }
        catch (ObjectNotFoundException e)
        {
            throw new NotFoundException(test, null, e);
        }
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Retreived test run metadata for '" + test + "." + run + "'. ");
        }
        return testRunObj;
    }

    @Override
    public void scheduleTestRun(String test, String run, int version, long scheduled)
            throws NotFoundException, RunStateException, ConcurrencyException
    {
        // Get the current state of play
        DBObject testRunObj;
        try
        {
            testRunObj = testDAO.getTestRun(test, run, false);
        }
        catch (ObjectNotFoundException e1)
        {
            throw new NotFoundException(test, run, e1);
        }
        
        // Check the state transition
        String oldStateStr = (String) testRunObj.get(FIELD_STATE);
        TestRunState oldState = TestRunState.valueOf(oldStateStr);
        try
        {
            oldState.transition(TestRunState.SCHEDULED);
        }
        catch (IllegalArgumentException e)
        {
            logger.warn("Test run '" + test + "." + run + "' cannot be (re)scheduled as it has already started running.");
            throw new RunStateException(test, run, oldState, TestRunState.SCHEDULED);
        }
        
        ObjectId testRunId = (ObjectId) testRunObj.get(FIELD_ID);
        boolean written = testDAO.updateTestRunState(
                testRunId, version,
                TestRunState.SCHEDULED, scheduled, null, null, null, null, null,
                null, null);
        if (!written)
        {
            throw new ConcurrencyException();
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Scheduled test run '" + test + "." + run + "' for " + new Date(scheduled));
        }
    }
    
    @Override
    public void terminateTestRun(String test, String run)
            throws NotFoundException, RunStateException, ConcurrencyException
    {
        // Get the current state of play
        DBObject testRunObj;
        try
        {
            testRunObj = testDAO.getTestRun(test, run, false);
        }
        catch (ObjectNotFoundException e1)
        {
            throw new NotFoundException(test, run, e1);
        }
        Integer version = (Integer) testRunObj.get(FIELD_VERSION);

        // Check the state transition
        String oldStateStr = (String) testRunObj.get(FIELD_STATE);
        TestRunState oldState = TestRunState.valueOf(oldStateStr);
        try
        {
            oldState.transition(TestRunState.STOPPED);
        }
        catch (IllegalArgumentException e)
        {
            logger.warn("Test run '" + test + "." + run + "' cannot be terminated.");
            throw new RunStateException(test, run, oldState, TestRunState.STOPPED);
        }
        
        ObjectId testRunId = (ObjectId) testRunObj.get(FIELD_ID);
        Long now = System.currentTimeMillis();
        // Calculate duration
        Long started = (Long) testRunObj.get(FIELD_STARTED);
        Long duration = (started == null || started < 0L) ? null : (now - started);

        boolean written = testDAO.updateTestRunState(
                testRunId, version,
                TestRunState.STOPPED, null, null, now, null, duration, null,
                null, null);
        if (!written)
        {
            throw new ConcurrencyException();
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Terminated test run '" + test + "." + run + "' for " + new Date(now));
        }
    }
}
