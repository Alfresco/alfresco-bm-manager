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
package org.alfresco.bm.manager.api.v1;

import static org.alfresco.bm.common.TestConstants.FIELD_RELEASE;
import static org.alfresco.bm.common.TestConstants.FIELD_SCHEMA;
import static org.alfresco.bm.common.TestConstants.FIELD_VERSION;
import static org.alfresco.bm.common.TestConstants.RUN_NAME_REGEX;
import static org.alfresco.bm.common.TestConstants.TEST_NAME_REGEX;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.bm.common.PropSetBean;
import org.alfresco.bm.common.TestDetails;
import org.alfresco.bm.common.TestRunDetails;
import org.alfresco.bm.common.TestRunSchedule;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.common.TestService;
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.common.spring.TestRunServicesCache;
import org.alfresco.bm.common.util.exception.ConcurrencyException;
import org.alfresco.bm.common.util.exception.NotFoundException;
import org.alfresco.bm.common.util.exception.ObjectNotFoundException;
import org.alfresco.bm.common.util.exception.RunStateException;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.alfresco.bm.manager.api.AbstractRestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.util.JSON;

/**
 * <b>REST API V1</b><br/>
 * <p>
 * The URL pattern:
 * <ul>
 * <li>&lt;API URL&gt;/v1/tests
 * </pre>
 * </li>
 * </ul>
 * </p>
 * Delegate the request to service layer and responds with JSON.
 * 
 * @author Michael Suzuki
 * @author Derek Hulley
 * @author Frank Becker
 * @since 2.0
 */

@RestController
@RequestMapping(path = "api/v1/tests")
public class TestRestAPI extends AbstractRestResource
{
    @Autowired
    private final MongoTestDAO testDAO;
    @Autowired
    private final TestService testService;
    @Autowired
    private final LogService logService;
    @Autowired
    private final TestRunServicesCache testRunServices;

    /**
     * @param testDAO
     *            low-level data service for tests
     * @param testService
     *            test service for retrieving calculated data
     * @param logService
     *            service to log basic crud for end user record
     * @param testRunServices
     *            factory providing access to test run services
     */
    public TestRestAPI(MongoTestDAO testDAO, TestService testService, LogService logService, TestRunServicesCache testRunServices)
    {
        this.testDAO = testDAO;
        this.testService = testService;
        this.logService = logService;
        this.testRunServices = testRunServices;
    }

    @GetMapping(produces = { "application/json" })
    public String getTests(@RequestParam(value = "release", required = false) String release, @RequestParam(value = "schema", required = false) Integer schema,
            @RequestParam(value = "skip", defaultValue = "0") int skip, @RequestParam(value = "count", defaultValue = "50") int count)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[release:" + release + ",schema:" + schema + ",skip:" + skip + ",count:" + count + "]");
        }
        DBCursor cursor = null;
        try
        {
            String json = "[]";
            cursor = testDAO.getTests(release, schema, skip, count);
            if (cursor.count() > 0)
            {
                json = JSON.serialize(cursor);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        finally
        {
            if (cursor != null)
            {
                try
                {
                    cursor.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public String createTest(@RequestBody TestDetails testDetails)
    {
        String name = testDetails.getName();
        String description = testDetails.getDescription();
        String release = testDetails.getRelease();
        Integer schema = testDetails.getSchema();
        // When copying
        String copyOf = testDetails.getCopyOf();
        int version = testDetails.getVersion();

        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[name:" + name + ",release:" + release + ",schema:" + schema + ",description:" + description + ",copyOf:"
                    + copyOf + ",version" + version + "]");
        }

        // Check the name of the test
        if (name == null || name.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name supplied.");
        }

        Pattern pattern = Pattern.compile(TEST_NAME_REGEX);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches())
        {

            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "The test name '" + name + "' is invalid.  "
                    + "Test names must start with a letter and contain only letters, numbers or underscores e.g 'TEST_01'.");
        }

        // Checks specific to copying or creating anew
        boolean copy = copyOf != null;
        if (!copy)
        {
            // Get the definition and make sure that we have details to play with
            // Note that it will throw an exception if the definition does not exist
            DBObject testDefObj = testDAO.getTestDef(release, schema);
            if (testDefObj == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Test definition not found for " + release + " schema " + schema + ".");
            }
        }

        try
        {
            boolean written = false;
            if (copy)
            {
                // This is a copy
                written = testDAO.copyTest(name, release, schema, copyOf, version);
                if (!written)
                {
                    DBObject copyOfObj = testDAO.getTest(copyOf, false);
                    Integer copyOfVersion = copyOfObj == null ? null : (Integer) copyOfObj.get(FIELD_VERSION);
                    if (copyOfVersion != null && copyOfVersion.equals(version))
                    {
                        throw new HttpClientErrorException(HttpStatus.CONFLICT, "A test with name '" + name + "' already exists.");
                        
                    }
                    else
                    {
                        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test to copy was not found: " + copyOf + "(V" + version + ")");
                    }
                }
            }
            else
            {
                // This is a create
                written = testDAO.createTest(name, description, release, schema);
                if (!written)
                {
                    throw new HttpClientErrorException(HttpStatus.CONFLICT, "A test with name '" + name + "' already exists.");
                }
            }
            // Now fetch the full test definition
            DBObject dbObject = testDAO.getTest(name, true);
            if (dbObject == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The newly create test '" + name + "' could not be found.");
            }
            dbObject = AbstractRestResource.maskValues(dbObject);
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            logService.log(null, name, null, LogLevel.INFO, "New test + '" + name + "' using " + release + " schema " + schema);
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {   
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(path = "/{test}", produces = { "application/json" })
    public String getTest(@PathVariable("test") String test)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + "]");
        }
        try
        {
            DBObject dbObject = testDAO.getTest(test, true);
            if (dbObject == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test '" + test + "' does not exist.");
            }
            dbObject = AbstractRestResource.maskValues(dbObject);
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
       
    }

    @PutMapping(produces = { "application/json" }, consumes = { "application/json" })
    public String updateTest(@RequestBody TestDetails testDetails)
    {
        String name = testDetails.getName();
        String oldName = testDetails.getOldName();
        Integer version = testDetails.getVersion();
        String description = testDetails.getDescription();
        String release = testDetails.getRelease();
        Integer schema = testDetails.getSchema();

        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[name:" + name + ",oldName:" + oldName + ",version:" + version + ",release:" + release + ",schema:" + schema
                    + ",name:" + name + ",description:" + description + "]");
        }

        // Check if the test definition is going to change
        if (release != null || schema != null)
        {
            // Get the definition and make sure that we have details to play
            // with
            DBObject testDef = testDAO.getTestDef(release, schema);
            if (testDef == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Test definition not found for " + release + " schema " + schema + ".");
            }
        }

        if (oldName == null || version == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "A 'version' must be applied with the 'oldName'");
        }

        if (name == null || name.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name supplied.");
        }

        // Check the name of the test
        Pattern pattern = Pattern.compile(TEST_NAME_REGEX);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches())
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "The test name '" + name + "' is invalid.  "
                    + "Test names must start with a letter and contain only letters, numbers or underscores e.g 'TEST_01'.");
        }

        try
        {
            // This is an update
            boolean written = testDAO.updateTest(oldName, version, name, description, release, schema);
            if (!written)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Could not update test '" + oldName + "'.");
            }
            // Now fetch the full test definition
            DBObject dbObject = testDAO.getTest(name, true);
            if (dbObject == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The updated test '" + name + "' could not be found.");
            }
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch (HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Delete a test and optionally clean up all related test data
     * 
     * @param test
     *            the name of the test
     * @param clean
     *            <tt>true</tt> to remove all related test runs as well
     * 
     */
    @DeleteMapping("/{test}")
    public void deleteTest(@PathVariable("test") String test, @RequestParam(value = "clean", defaultValue = "true") boolean clean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", clean:" + clean + "]");
        }
        try
        {
            // Clean up all test runs and related data
            if (clean)
            {
                // Get all the test runs for the test
                List<String> runs = testDAO.getTestRunNames(test);
                // Delete each one, in turn, which will clean up associated
                // collections and data
                for (String run : runs)
                {
                    deleteTestRun(test, run, true);
                }
            }
            // Delete the test configuration
            boolean deleted = testDAO.deleteTest(test);
            if (!deleted)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test '" + test + "' was not deleted.");
            }
            logService.log(null, test, null, LogLevel.INFO, "Deleted test + '" + test + "'.");
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(path = "/{test}/drivers", produces = { "application/json" })
    public String getTestDrivers(@PathVariable("test") String test, @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", activeOnly:" + activeOnly + "]");
        }
        String exampleUrl = "/tests/MYTEST/drivers";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }

        DBCursor cursor = null;
        try
        {
            DBObject dbObject = testDAO.getTest(test, false);
            if (dbObject == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test '" + test + "' does not exist.");
            }
            String release = (String) dbObject.get(FIELD_RELEASE);
            Integer schema = (Integer) dbObject.get(FIELD_SCHEMA);

            String json = "[]";
            cursor = testDAO.getDrivers(release, schema, activeOnly);
            if (cursor.count() > 0)
            {
                json = JSON.serialize(cursor);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        finally
        {
            if (cursor != null)
            {
                try
                {
                    cursor.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    @GetMapping(path = "/{test}/props/{property:.+}", produces = { "application/json" })
    public String getTestProperty(@PathVariable("test") String test, @PathVariable("property") String property)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", property: " + property + "]");
        }
        String exampleUrl = "/tests/MYTEST/props/MYPROP";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (property == null || property.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property name supplied: " + exampleUrl);
        }

        try
        {
            DBObject dbObject = testDAO.getProperty(test, null, property);
            if (dbObject == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The property '" + test + "." + property + "' does not exist.");
            }
            dbObject = AbstractRestResource.maskValues(dbObject);
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PutMapping(path = "/{test}/props/{property:.+}", produces = { "application/json" }, consumes = { "application/json" })
    public String setTestProperty(@PathVariable("test") String test, @PathVariable("property") String property, @RequestBody PropSetBean propBean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", property: " + property + ", json:" + propBean + "]");
        }
        String exampleUrl = "/tests/MYTEST/props/MYPROP";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (property == null || property.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property name supplied: " + exampleUrl);
        }
        String exampleJSON = "{\"version\":\"0\", \"value\":\"someNewValue\"}";
        if (propBean == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property JSON provided: " + exampleJSON);
        }
        if (propBean.getVersion() == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid property version.  Example body JSON: " + exampleJSON);
        }

        String value = propBean.getValue();
        Integer version = propBean.getVersion();

        boolean written = testDAO.setPropertyOverride(test, null, property, version, value);
        if (!written)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND,
                    "Property '" + property + "' was not updated.  The version number was not found: " + version);
        }

        // Retrieve the property
        String json = getTestProperty(test, property);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Outbound: " + json);
        }
        return json;
    }

    @DeleteMapping(path = "/{test}/props/{property:.+}", produces = { "application/json" }, consumes = { "application/json" })
    public String unsetTestProperty(@PathVariable("test") String test, @PathVariable("property") String property, @RequestBody PropSetBean propBean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", property: " + property + ", json:" + propBean + "]");
        }
        String exampleUrl = "/tests/MYTEST/props/MYPROP";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (property == null || property.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property name supplied: " + exampleUrl);
        }
        String exampleJSON = "{\"version\":\"0\"}";
        if (propBean == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property JSON provided: " + exampleJSON);
        }
        if (propBean.getVersion() == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid property version.  Example body JSON: " + exampleJSON);
        }

        Integer version = propBean.getVersion();

        boolean written = testDAO.setPropertyOverride(test, null, property, version, null);
        if (!written)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND,
                    "Property '" + property + "' was not reset.  The version number was not found: " + version);
        }

        // Retrieve the property
        String json = getTestProperty(test, property);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Outbound: " + json);
        }
        return json;
    }

    @GetMapping(path = "/{test}/runs", produces = { "application/json" })
    public String getTestRuns(@PathVariable("test") String test, @RequestParam(value = "skip", defaultValue = "0") int skip,
            @RequestParam(value = "count", defaultValue = "50") int count, @RequestParam(value = "state", defaultValue = "") String stateStr)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ",skip:" + skip + ",count:" + count + ",state:" + stateStr + "]");
        }
        String exampleUrl = "/tests/MYTEST/run";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }

        // Handle the 'all' test
        if (test.equals("-"))
        {
            test = null;
        }

        // Check the state values
        TestRunState[] states = null;
        if (stateStr.length() == 0)
        {
            states = new TestRunState[0];
        }
        else
        {
            try
            {
                states = new TestRunState[] { TestRunState.valueOf(stateStr) };
            }
            catch (IllegalArgumentException e)
            {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Test run state '" + stateStr + "' is not a valid value.");
            }
        }

        DBCursor cursor = null;
        try
        {
            String json = "[]";
            cursor = testDAO.getTestRuns(test, skip, count, states);
            if (cursor.count() > 0)
            {
                json = JSON.serialize(cursor);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        finally
        {
            if (cursor != null)
            {
                try
                {
                    cursor.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    @PostMapping(path = "/{test}/runs", produces = { "application/json" }, consumes = { "application/json" })
    public String createTestRun(@PathVariable("test") String test, @RequestBody TestRunDetails testRunDetails)
    {
        String name = testRunDetails.getName();
        String description = testRunDetails.getDescription();
        // When copying
        String copyOf = testRunDetails.getCopyOf();
        int version = testRunDetails.getVersion();

        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ",name:" + name + ",description:" + description + ",copyOf:" + copyOf + ",version" + version
                    + "]");
        }

        // Get the definition and make sure that we have details to play with
        // Note that it will throw an exception if the definition does not exist
        DBObject testObj = testDAO.getTest(test, false);
        if (testObj == null)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Test not found: " + test + ".");
        }

        // Check the name of the test run
        if (name == null || name.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No run name supplied.");
        }

        Pattern pattern = Pattern.compile(RUN_NAME_REGEX);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches())
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "The run name '" + name + "' is invalid.  " + "Run names may contain only letters, numbers or underscores e.g 'RUN_01'.");
        }

        // Checks specific to copying or creating anew
        boolean copy = copyOf != null;

        try
        {
            boolean written = false;
            if (copy)
            {
                // This is a copy
                written = testDAO.copyTestRun(test, name, copyOf, version);
                if (!written)
                {
                    DBObject copyOfObj;
                    try
                    {
                        copyOfObj = testDAO.getTestRun(test, copyOf, false);
                    }
                    catch (ObjectNotFoundException onfe)
                    {
                        copyOfObj = null;
                    }
                    Integer copyOfVersion = copyOfObj == null ? null : (Integer) copyOfObj.get(FIELD_VERSION);
                    if (copyOfVersion != null && copyOfVersion.equals(version))
                    {
                        throw new HttpClientErrorException(HttpStatus.CONFLICT, "A test run with name '" + test + "." + name + "' already exists.");
                    }
                    else
                    {
                        throw new HttpClientErrorException(HttpStatus.NOT_FOUND,
                                "The test run to copy was not found: " + test + "." + copyOf + "(V" + version + ")");
                    }
                }
            }
            else
            {
                // This is a create
                written = testDAO.createTestRun(test, name, description);
                if (!written)
                {
                    throw new HttpClientErrorException(HttpStatus.CONFLICT, "A test run with name '" + test + "." + name + "' already exists.");
                }
            }
            // Now fetch the full run definition
            DBObject dbObject;
            try
            {
                dbObject = testDAO.getTestRun(test, name, true);
            }
            catch (ObjectNotFoundException onfe)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The newly create run '" + name + "' could not be found.");
            }
            dbObject = AbstractRestResource.maskValues(dbObject);
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            logService.log(null, test, name, LogLevel.INFO, "Created test run + '" + name + "' in test '" + test + "'.");
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(path = "/{test}/runs/{run}", produces = { "application/json" })
    public String getTestRun(@PathVariable("test") String test, @PathVariable("run") String run)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run:" + run + "]");
        }
        try
        {
            DBObject dbObject = null;
            try
            {
                dbObject = testDAO.getTestRun(test, run, true);
            }
            catch (ObjectNotFoundException onfe)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test run '" + test + "." + run + "' does not exist.");
            }
            dbObject = AbstractRestResource.maskValues(dbObject);
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PutMapping(path = "/{test}/runs", consumes = { "application/json" }, produces = { "application/json" })
    public String updateTestRun(@PathVariable("test") String test, @RequestBody TestRunDetails testRunDetails)
    {
        String name = testRunDetails.getName();
        String oldName = testRunDetails.getOldName();
        Integer version = testRunDetails.getVersion();
        String description = testRunDetails.getDescription();

        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ",oldName:" + oldName + ",version:" + version + ",name:" + name + ",description:"
                    + description + "]");
        }

        if (test == null || test.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name supplied.");
        }

        /*
         * Dead code ... version can never be null: testRunDetails.getVersion()
         * returns int if (version == null) { throw new
         * HttpServerErrorException(HttpStatus.BAD_REQUEST,
         * "A 'version' must be supplied to update a test run."); }
         */
        if (oldName == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Test run 'oldName' must be supplied.");
        }

        // Check the name of the test
        Pattern pattern = Pattern.compile(RUN_NAME_REGEX);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches())
        {
            throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
                    "The test name '" + name + "' is invalid.  " + "Test run names must contain only letters, numbers or underscores e.g 'RUN_01'.");
        }

        try
        {
            // This is an update
            boolean written = testDAO.updateTestRun(test, oldName, version, name, description);
            if (!written)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Could not update test run '" + test + "." + oldName + "'.");
            }

            // Now fetch the full test run
            DBObject dbObject = null;
            try
            {
                dbObject = testDAO.getTestRun(test, name, true);
            }
            catch (ObjectNotFoundException onfe)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test for test run '" + test + "." + name + "' could not be found.");
            }
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e){
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Delete a test run and optionally clean up all related test data
     * 
     * @param test
     *            the name of the test
     * @param run
     *            the name of the test run
     * @param clean
     *            <tt>true</tt> to remove all related test run data as well
     * 
     */
    @DeleteMapping(path = "/{test}/runs/{run}")
    public void deleteTestRun(@PathVariable("test") String test, @PathVariable("run") String run,
            @RequestParam(value = "clean", defaultValue = "true") boolean clean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ",run:" + run + ",clean:" + clean + "]");
        }
        try
        {
            // delete the test run data
            if (clean)
            {
                if (null != this.testRunServices)
                {
                    this.testRunServices.deleteTestRun(test, run);
                }
                else
                {
                    // clean must always be true ...
                    throw new HttpClientErrorException(HttpStatus.NOT_FOUND,
                            "The test run data collections '" + test + "." + run + "' were not deleted.");
                }
            }
            else
            {
                logger.warn("Test run data of test '" + test + "', run '" + run + "' not removed!");
            }

            // Delete the test run and all associated configuration
            boolean deleted = testDAO.deleteTestRun(test, run);
            if (!deleted)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test run '" + test + "." + run + "' was not deleted.");
            }
            logService.log(null, test, run, LogLevel.INFO, "Deleted test run + '" + run + "' in test '" + test + "'.");
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /*
     * Helper method to retrieve the property value in play for a particular
     * test run
     * 
     * @return the property value or <tt>null</tt> if not available
     *
     * never used private String getTestRunPropertyString(String test, String
     * run, String propertyName) { DBObject propertyObj =
     * testDAO.getProperty(test, run, propertyName);
     * 
     * if (propertyObj == null) { return null; }
     * 
     * String propertyValue = (String) propertyObj.get(FIELD_DEFAULT); if
     * (propertyObj.get(FIELD_VALUE) != null) { propertyValue = (String)
     * propertyObj.get(FIELD_VALUE); } return propertyValue; }
     */

    @GetMapping(path = "/{test}/runs/{run}/props/{property:.+}", produces = { "application/json" })
    public String getTestRunProperty(@PathVariable("test") String test, @PathVariable("run") String run, @PathVariable("property") String property)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run: " + run + ", property: " + property + "]");
        }
        String exampleUrl = "/tests/MYTEST/runs/RUN01/props/MYPROP";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (run == null || run.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No run name provided: " + exampleUrl);
        }
        if (property == null || property.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No run property name supplied: " + exampleUrl);
        }

        try
        {
            DBObject dbObject = testDAO.getProperty(test, run, property);
            if (dbObject == null)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The property '" + test + "." + run + "." + property + "' does not exist.");
            }
            dbObject = AbstractRestResource.maskValues(dbObject);
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PutMapping(path = "/{test}/runs/{run}/props/{property:.+}", produces = { "application/json" }, consumes = { "application/json" })
    public String setTestRunProperty(@PathVariable("test") String test, @PathVariable("run") String run, @PathVariable("property") String property,
            @RequestBody PropSetBean propBean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run: " + run + ", property: " + property + ", json:" + propBean + "]");
        }
        String exampleUrl = "/tests/MYTEST/runs/RUN01/props/MYPROP";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (run == null || run.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No run name provided: " + exampleUrl);
        }
        if (property == null || property.length() == 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property name supplied: " + exampleUrl);
        }
        String exampleJSON = "{\"version\":\"0\", \"value\":\"someNewValue\"}";
        if (propBean == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test property JSON provided: " + exampleJSON);
        }
        if (propBean.getVersion() == null)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid property version.  Example body JSON: " + exampleJSON);
        }

        String value = propBean.getValue();
        Integer version = propBean.getVersion();

        try
        {
            boolean written = testDAO.setPropertyOverride(test, run, property, version, value);
            if (!written)
            {
                throw new HttpClientErrorException(HttpStatus.CONFLICT,
                        "Property '" + property + "' (" + version + ") was not updated for " + test + "." + run);
            }
        }
        catch (DuplicateKeyException e)
        {
            throw new HttpClientErrorException(HttpStatus.CONFLICT,
                    "Property '" + property + "' (" + version + ") was not updated for " + test + "." + run);
        }
        catch (IllegalStateException e)
        {
            String msg = test + "." + run;
            try
            {
                DBObject runObj = testDAO.getTestRun(test, run, false);
                msg = runObj.toString();
            }
            catch (ObjectNotFoundException e1)
            {
                logger.debug("Test '" + test + "', run '" + run + "' not found!", e1);
            }
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Properties cannot be changed once a test has started: " + msg);
        }

        // Retrieve the property
        String json = getTestRunProperty(test, run, property);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Outbound: " + json);
        }
        return json;
    }

    @GetMapping(path = "/{test}/runs/{run}/summary", produces = { "application/json" })
    public String getTestRunSummary(@PathVariable("test") String test, @PathVariable("run") String run)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run:" + run + "]");
        }
        try
        {
            DBObject dbObject = null;
            try
            {
                dbObject = testDAO.getTestRun(test, run, false);
            }
            catch (ObjectNotFoundException onfe)
            {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test run '" + test + "." + run + "' does not exist.");
            }
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(path = "/{test}/runs/{run}/state", produces = { "application/json" })
    public String getTestRunState(@PathVariable("test") String test, @PathVariable("run") String run)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run:" + run + "]");
        }
        try
        {
            TestRunState state = testService.getTestRunState(test, run);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + state);
            }
            return state.toString();
        }
        catch (NotFoundException e)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test run '" + test + "." + run + "' does not exist.");
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping(path = "/{test}/runs/{run}/schedule", produces = { "application/json" }, consumes = { "application/json" })
    public String scheduleTestRun(@PathVariable("test") String test, @PathVariable("run") String run, @RequestBody TestRunSchedule schedule)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run: " + run + ", json:" + schedule + "]");
        }

        int version = schedule.getVersion();
        long scheduled = schedule.getScheduled();

        String exampleUrl = "/tests/MYTEST/runs/RUN01";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (run == null || run.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No run name provided: " + exampleUrl);
        }
        String exampleJSON = "{\"version\":\"0\", \"scheduled\":\"0000000\", \"duration\":\"120000\"}";
        if (version < 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid property 'version'.  Example body JSON: " + exampleJSON);
        }
        if (scheduled < 0)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid property 'scheduled'.  Example body JSON: " + exampleJSON);
        }

        try
        {
            testService.scheduleTestRun(test, run, version, scheduled);
        }
        catch (ConcurrencyException e)
        {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "Test run '" + test + "." + run + "' was not updated (" + schedule + ").");
        }
        catch (NotFoundException e)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Test run '" + test + "." + run + "' was not found");
        }
        catch (RunStateException e)
        {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, e.getMessage()); // Assume
                                                                                     // the
                                                                                     // state
                                                                                     // moved
                                                                                     // underneath
                                                                                     // the
                                                                                     // client
        }

        // Retrieve the test run
        String json = getTestRun(test, run);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Outbound: " + json);
        }
        logService.log(null, test, run, LogLevel.INFO, "Test run '" + test + "." + run + "' scheduled for " + new Date(scheduled));
        return json;
    }

    @PostMapping(path = "/{test}/runs/{run}/terminate", produces = { "application/json" }, consumes = { "application/json" })
    public String terminateTestRun(@PathVariable("test") String test, @PathVariable("run") String run)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run: " + run + "]");
        }

        String exampleUrl = "/tests/MYTEST/runs/RUN01/terminate";
        if (test == null || test.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No test name provided: " + exampleUrl);
        }
        if (run == null || run.length() < 1)
        {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "No run name provided: " + exampleUrl);
        }

        try
        {
            testService.terminateTestRun(test, run);
        }
        catch (ConcurrencyException e)
        {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "Test run '" + test + "." + run + "' was not terminated.");
        }
        catch (NotFoundException e)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Test run '" + test + "." + run + "' was not found");
        }
        catch (RunStateException e)
        {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, e.getMessage()); // Assume
                                                                                     // the
                                                                                     // state
                                                                                     // moved
                                                                                     // underneath
                                                                                     // the
                                                                                     // client
        }

        // Retrieve the test run
        String json = getTestRun(test, run);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Outbound: " + json);
        }
        logService.log(null, test, run, LogLevel.WARN, "Test run terminated: " + test + "." + run);
        return json;
    }

    @GetMapping(path = "/{test}/runs/{run}/exportProps", produces = { "application/json" })
    public String exportProps(@PathVariable("test") String test, @PathVariable("run") String run)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run:" + run + "]");
        }
        try
        {
            DBObject dbObject = testDAO.exportTestRun(test, run);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch (ObjectNotFoundException onfe)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test run '" + test + "." + run + "' does not exist.");
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @PostMapping(path = "/{test}/runs/{run}/importProps", produces = { "application/json" }, consumes = { "application/json" })
    public String importProps(@PathVariable("test") String test, @PathVariable("run") String run, @RequestBody PropSetBean propBean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + ", run:" + run + "]");
        }
        try
        {
            DBObject dbImportObject = (DBObject) JSON.parse(propBean.getValue());
            DBObject dbObject = testDAO.importTestRun(test, run, dbImportObject);

            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(path = "/{test}/exportProps", produces = { "application/json" })
    public String exportTestProps(@PathVariable("test") String test)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[test:" + test + "]");
        }
        try
        {
            DBObject dbObject = testDAO.exportTest(test);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch (ObjectNotFoundException onfe)
        {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The test '" + test + "' doesn't exist.");
        }
        catch(HttpClientErrorException e) 
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping(path = "/{test}/importProps", produces = { "application/json" }, consumes = { "application/json" })
    public String importTestProps(@PathVariable("test") String test, @RequestBody PropSetBean propBean)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: [test:" + test + "]");
        }
        try
        {
            DBObject dbImportObject = (DBObject) JSON.parse(propBean.getValue());
            DBObject dbObject = testDAO.importTest(test, dbImportObject);

            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
