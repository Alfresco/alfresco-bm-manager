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

import java.util.Date;

import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import com.mongodb.DBObject;

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
        DBObject testRunObj = testDAO.getTestRun(test, run, false);
        if (testRunObj == null)
        {
            throw new NotFoundException(test, null);
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
        DBObject testRunObj = testDAO.getTestRun(test, run, false);
        if (testRunObj == null)
        {
            throw new NotFoundException(test, null);
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
        DBObject testRunObj = testDAO.getTestRun(test, run, false);
        if (testRunObj == null)
        {
            throw new NotFoundException(test, run);
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
        DBObject testRunObj = testDAO.getTestRun(test, run, false);
        if (testRunObj == null)
        {
            throw new NotFoundException(test, run);
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
