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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.bm.manager.api.v1;

import com.google.gson.Gson;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import org.alfresco.bm.common.PropSetBean;
import org.alfresco.bm.common.TestDetails;
import org.alfresco.bm.common.TestRunDetails;
import org.alfresco.bm.common.TestRunSchedule;
import org.alfresco.bm.driver.test.TestRun;
import org.alfresco.bm.common.spring.TestRunServicesCache;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.common.TestServiceImpl;
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.manager.report.DataReportService;
import org.alfresco.bm.common.mongo.MongoClientFactory;
import org.alfresco.bm.common.mongo.MongoDBFactory;
import org.alfresco.bm.common.util.junit.tools.MongoDBForTestsFactory;
import org.alfresco.bm.common.util.exception.ObjectNotFoundException;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.LogWatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import static org.alfresco.bm.common.TestConstants.FIELD_COMPLETED;
import static org.alfresco.bm.common.TestConstants.FIELD_DEFAULT;
import static org.alfresco.bm.common.TestConstants.FIELD_DURATION;
import static org.alfresco.bm.common.TestConstants.FIELD_ID;
import static org.alfresco.bm.common.TestConstants.FIELD_PROGRESS;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_FAIL;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_SUCCESS;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_TOTAL;
import static org.alfresco.bm.common.TestConstants.FIELD_SCHEDULED;
import static org.alfresco.bm.common.TestConstants.FIELD_STARTED;
import static org.alfresco.bm.common.TestConstants.FIELD_STATE;
import static org.alfresco.bm.common.TestConstants.FIELD_STOPPED;
import static org.alfresco.bm.common.TestConstants.FIELD_SUCCESS_RATE;
import static org.alfresco.bm.common.TestConstants.FIELD_VALUE;
import static org.alfresco.bm.common.TestConstants.MASK;
import static org.alfresco.bm.common.TestConstants.MONGO_PREFIX;
import static org.alfresco.bm.common.TestConstants.PATH_APP_CONTEXT;
import static org.alfresco.bm.common.TestConstants.PROP_APP_CONTEXT_PATH;
import static org.alfresco.bm.common.TestConstants.PROP_APP_DIR;
import static org.alfresco.bm.common.TestConstants.PROP_APP_INHERITANCE;
import static org.alfresco.bm.common.TestConstants.PROP_APP_RELEASE;
import static org.alfresco.bm.common.TestConstants.PROP_APP_SCHEMA;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_CONFIG_DATABASE;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_CONFIG_HOST;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_TEST_DATABASE;
import static org.alfresco.bm.common.TestConstants.PROP_MONGO_TEST_HOST;
import static org.alfresco.bm.common.TestConstants.PROP_SYSTEM_CAPABILITIES;
import static org.alfresco.bm.common.TestConstants.PROP_TEST_RUN_MONITOR_PERIOD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @see TestRestAPI V1
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class RestAPITest
{
    public static final String RELEASE = "RestAPITest";
    public static final int SCHEMA = 0;
    public static final String MONGO_TEST_DATABASE = "bm21-data";

    private static Log logger = LogFactory.getLog(RestAPITest.class);

    private Gson gson = new Gson();
    private MongoDBForTestsFactory mockDBFactory;
    private ClassPathXmlApplicationContext ctx;

    private MongoTestDAO dao;
    private TestRestAPI api;
    private TestRunServicesCache testRunServicesCache;
    private DB appDB;
    private String mongoHost;

    @Before
    public void setUp() throws Exception
    {
        gson = new Gson();

        // Create a mock DB to connect to
        mockDBFactory = new MongoDBForTestsFactory();
        mongoHost = mockDBFactory.getMongoHost();
        String database = mockDBFactory.getObject().getName();

        // Create an application context (don't start, yet)
        ctx = new ClassPathXmlApplicationContext(
                new String[] {
                        PATH_APP_CONTEXT
                },
                false);

        // Pass our properties to the new context
        Properties ctxProperties = new Properties();
        {
            ctxProperties.put(PROP_MONGO_CONFIG_HOST, mongoHost);
            ctxProperties.put(PROP_MONGO_CONFIG_DATABASE, database);
            ctxProperties.put(PROP_MONGO_TEST_DATABASE, MONGO_TEST_DATABASE);
            ctxProperties.put(PROP_APP_RELEASE, RELEASE);
            ctxProperties.put(PROP_APP_SCHEMA, "" + SCHEMA);
            ctxProperties.put(PROP_APP_INHERITANCE, "COMMON, TEST");
            ctxProperties.put(PROP_TEST_RUN_MONITOR_PERIOD, "" + 20000);
            ctxProperties.put(PROP_SYSTEM_CAPABILITIES, "Java");
            ctxProperties.put(PROP_APP_CONTEXT_PATH, "/");
            ctxProperties.put(PROP_APP_DIR, ".");
        }
        ConfigurableEnvironment ctxEnv = ctx.getEnvironment();
        ctxEnv.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        "RestAPITest",
                        ctxProperties));
        // Override all properties with system properties
        ctxEnv.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        "system",
                        System.getProperties()));
        // Start the context now
        ctx.refresh();

        // Get beans
        dao = ctx.getBean(MongoTestDAO.class);
        appDB = dao.getDb();
        TestServiceImpl testService = ctx.getBean(TestServiceImpl.class);
        LogService logService = ctx.getBean(LogService.class);
        this.testRunServicesCache = ctx.getBean(TestRunServicesCache.class);
        api = new TestRestAPI(dao, testService, logService, testRunServicesCache);
    }

    @After
    public void tearDown()
    {
        if (ctx != null)
        {
            try
            {
                ctx.close();
            }
            catch (Exception e)
            {
            }
        }
        if (mockDBFactory != null)
        {
            try
            {
                mockDBFactory.destroy();
            }
            catch (Exception e)
            {
            }
        }
    }

    @Test
    public void basic()
    {
        assertNotNull(appDB);
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("test.drivers");
        collectionNames.add("test.defs");
        collectionNames.add("tests");
        collectionNames.add("test.runs");
        collectionNames.add("test.props");

        Set<String> dbColNames = removeSystemValues(appDB.getCollectionNames());
        assertEquals(collectionNames, dbColNames);
    }

    /**
     * make sure the system name is NOT contained as from 3.2 on
     * 
     * @param collection
     *        (Set<String>) collection to check
     * @return
     */
    private Set<String> removeSystemValues(Set<String> collection)
    {
        if (null != collection)
        {
            // make sure the system name is NOT contained as from 3.2 on
            collection.remove("system.indexes");
        }
        return collection;
    }

    @Test
    public void connectToMongoDB()
    {
        DB db = dao.getDb();
        String dbName = db.getName();
        ServerAddress mongoAddress = db.getMongo().getAddress();

        // Now connect a real mongo client to it
        MongoClient mongoClient = new MongoClient(mongoAddress);
        // won't be removed in V3.x of MongoDB .... new code uses new API ....
        @SuppressWarnings("deprecation")
        DB dbCheck = mongoClient.getDB(dbName);

        // See if we can add/remove stuff and that it works
        assertTrue(dao.createTest("TESTconnectToMongoDB", "connectToMongoDB", "R1", 1));

        MongoTestDAO daoCheck = new MongoTestDAO(dbCheck);
        DBObject testCheckObj = daoCheck.getTest("TESTconnectToMongoDB", false);
        assertNotNull(testCheckObj);

        // clean-up
        mongoClient.close();
    }

    /**
     * Helper to convert Json to map for quick testing of values
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json)
    {
        Map<String, Object> map = (HashMap<String, Object>) gson.fromJson(json, HashMap.class);
        return map;
    }

    private void checkLongValue(String json, String field, Long value)
    {
        Map<String, Object> map = fromJson(json);
        Long actual = Double.valueOf(map.get(field).toString()).longValue();
        assertEquals("Incorrect json for field  '" + field + "': \n" + json + "\n", value, actual);
    }

    private void checkTimeValue(String json, String field, long time)
    {
        Map<String, Object> map = fromJson(json);
        long actual = Double.valueOf(map.get(field).toString()).longValue();
        long startTime = time - 30000L;
        long endTime = time + 30000L;
        assertTrue(
                "Incorrect time range for field '" + field + "' around " + time + ": \n" + json,
                startTime <= actual && actual <= endTime);
    }

    private void checkDoubleValue(String json, String field, Double value)
    {
        Map<String, Object> map = fromJson(json);
        Double actual = Double.valueOf(map.get(field).toString());
        assertEquals("Incorrect json for field  '" + field + "': \n" + json + "\n", value, actual);
    }

    /**
     * Helper method to check that a test run is in a particular state
     */
    private void checkTestRunState(String test, String run, TestRunState state, boolean expected)
    {
        long now = System.currentTimeMillis();
        // Do a direct check
        String checkStateStr = api.getTestRunState(test, run);
        TestRunState checkState = TestRunState.valueOf(checkStateStr);
        if (expected)
        {
            assertEquals("State was incorrect for " + test + "." + run + ". ", state, checkState);
        }
        else
        {
            assertNotSame("State was incorrect for " + test + "." + run + ". ", state, checkState);
        }
        // Now check by querying for the state
        assertEquals(expected,
                api.getTestRuns(
                        test, 0, 2,
                        state.toString()).contains("\"name\" : \"" + run + "\""));
        // Check that the appropriate fields have been set for the given state
        String checkJson = api.getTestRunSummary(test, run);
        switch (checkState)
        {
            case NOT_SCHEDULED:
                checkTimeValue(checkJson, FIELD_SCHEDULED, -1L);
                checkTimeValue(checkJson, FIELD_STARTED, -1L);
                checkTimeValue(checkJson, FIELD_STOPPED, -1L);
                checkTimeValue(checkJson, FIELD_COMPLETED, -1L);
                checkLongValue(checkJson, FIELD_DURATION, 0L);
                checkDoubleValue(checkJson, FIELD_PROGRESS, 0.0);
                checkLongValue(checkJson, FIELD_RESULTS_SUCCESS, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_FAIL, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_TOTAL, 0L);
                checkDoubleValue(checkJson, FIELD_SUCCESS_RATE, 1.0);
                break;
            case SCHEDULED:
                checkTimeValue(checkJson, FIELD_SCHEDULED, now);
                checkTimeValue(checkJson, FIELD_STARTED, -1L);
                checkTimeValue(checkJson, FIELD_STOPPED, -1L);
                checkTimeValue(checkJson, FIELD_COMPLETED, -1L);
                checkLongValue(checkJson, FIELD_DURATION, 0L);
                checkDoubleValue(checkJson, FIELD_PROGRESS, 0.0);
                checkLongValue(checkJson, FIELD_RESULTS_SUCCESS, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_FAIL, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_TOTAL, 0L);
                checkDoubleValue(checkJson, FIELD_SUCCESS_RATE, 1.0);
                break;
            case STARTED:
                checkTimeValue(checkJson, FIELD_SCHEDULED, now);
                checkTimeValue(checkJson, FIELD_STARTED, now);
                checkTimeValue(checkJson, FIELD_STOPPED, -1L);
                checkTimeValue(checkJson, FIELD_COMPLETED, -1L);
                checkLongValue(checkJson, FIELD_RESULTS_FAIL, 0L);
                checkDoubleValue(checkJson, FIELD_SUCCESS_RATE, 1.0);
                break;
            case COMPLETED:
                checkTimeValue(checkJson, FIELD_SCHEDULED, now);
                checkTimeValue(checkJson, FIELD_STARTED, now);
                checkTimeValue(checkJson, FIELD_STOPPED, -1L);
                checkTimeValue(checkJson, FIELD_COMPLETED, now);
                checkTimeValue(checkJson, FIELD_DURATION, 30001L);      // The
                                                                        // range
                                                                        // is +-
                                                                        // 30s,
                                                                        // so we
                                                                        // check
                                                                        // it's
                                                                        // in
                                                                        // range
                                                                        // 1ms-60001ms
                checkDoubleValue(checkJson, FIELD_PROGRESS, 1.0);
                checkLongValue(checkJson, FIELD_RESULTS_SUCCESS, 21L);
                checkLongValue(checkJson, FIELD_RESULTS_FAIL, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_TOTAL, 21L);
                checkDoubleValue(checkJson, FIELD_SUCCESS_RATE, 1.0);
                break;
            case STOPPED:
                checkTimeValue(checkJson, FIELD_SCHEDULED, now);
                checkTimeValue(checkJson, FIELD_STARTED, now);
                checkTimeValue(checkJson, FIELD_STOPPED, now);
                checkTimeValue(checkJson, FIELD_COMPLETED, -1L);
                checkTimeValue(checkJson, FIELD_DURATION, 30001L);      // The
                                                                        // range
                                                                        // is +-
                                                                        // 30s,
                                                                        // so we
                                                                        // check
                                                                        // it's
                                                                        // in
                                                                        // range
                                                                        // 1ms-60001ms
                checkDoubleValue(checkJson, FIELD_PROGRESS, 0.0);
                checkLongValue(checkJson, FIELD_RESULTS_SUCCESS, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_FAIL, 0L);
                checkLongValue(checkJson, FIELD_RESULTS_TOTAL, 0L);
                checkDoubleValue(checkJson, FIELD_SUCCESS_RATE, 1.0);
                break;
        }
    }

    /**
     * Basic CRUD of a test run
     */
    @Test
    public void testScenario01()
    {
        DBObject dbObject;
        String json;
        TestDetails testDetails;
        TestRunDetails testRunDetails;
        PropSetBean propSetBean;

        // Check the response when there are no tests
        json = api.getTests("A", 4, 0, 10);
        assertEquals("Empty Json results must still be an array", "[]", json);

        testDetails = new TestDetails();
        testDetails.setName("T01");
        testDetails.setRelease(RELEASE);
        testDetails.setSchema(SCHEMA);
        testDetails.setDescription("A test for scenario 01.");
        json = api.createTest(testDetails);
        assertFalse(json, json.indexOf("\"default\" : \"NEVER-LEAVES-THE-SERVER\"") >= 0);
        assertTrue(json, json.indexOf(MASK) >= 0);
        assertTrue(json, json.indexOf("\"description\" : \"The password for the user executing the process.\"") >= 0);
        assertTrue(json, json.indexOf("A test for scenario 01.") >= 0);

        assertEquals("Expect exact match from createTest and getTest", json, api.getTest("T01"));

        assertTrue(api.getTests(RELEASE, SCHEMA, 0, 2).contains("\"name\" : \"T01\""));

        // Update test
        testDetails = new TestDetails();
        testDetails.setOldName("T01");
        testDetails.setName("TEST01");
        testDetails.setVersion(0);
        testDetails.setDescription("The test for scenario 01.");
        json = api.updateTest(testDetails);
        assertTrue(json, json.indexOf("The test for scenario 01.") >= 0);

        // Set 'proc.pwd' at the test level
        propSetBean = new PropSetBean();
        propSetBean.setVersion(0);
        propSetBean.setValue("pwd1");
        json = api.setTestProperty("TEST01", "proc.pwd", propSetBean);
        assertFalse(json, json.indexOf("pwd1") >= 0);
        assertTrue(json, json.indexOf(MASK) >= 0);
        assertTrue(json, json.indexOf("\"description\" : \"The password for the user executing the process.\"") >= 0);
        dbObject = dao.getProperty("TEST01", null, "proc.pwd");
        assertEquals("NEVER-LEAVES-THE-SERVER", dbObject.get(FIELD_DEFAULT));
        assertEquals("pwd1", dbObject.get(FIELD_VALUE));

        // Set 'processCount' for test 'T01'
        propSetBean = new PropSetBean();
        propSetBean.setVersion(0);
        propSetBean.setValue("500");
        json = api.setTestProperty("TEST01", "processCount", propSetBean);
        assertTrue(json, json.indexOf("\"default\" : \"200\"") >= 0);
        assertTrue(json, json.indexOf("\"value\" : \"500\"") >= 0);
        assertTrue(json, json.indexOf("\"title\" : \"Process Count\"") >= 0);
        dbObject = dao.getProperty("TEST01", null, "processCount");
        assertEquals("200", dbObject.get(FIELD_DEFAULT));
        assertEquals("500", dbObject.get(FIELD_VALUE));

        // Check the response when there are no test runs
        json = api.getTestRuns("TEST01", 0, 10, "");
        assertEquals("Empty Json results must still be an array", "[]", json);

        // Create run 01
        testRunDetails = new TestRunDetails();
        testRunDetails.setName("01");
        testRunDetails.setDescription("Scenario 01 - Run 01");
        json = api.createTestRun("TEST01", testRunDetails);
        assertTrue(json, json.indexOf("\"description\" : \"Scenario 01 - Run 01\"") >= 0);
        assertFalse(json, json.indexOf("\"default\" : \"NEVER-LEAVES-THE-SERVER\"") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"admin\"") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"" + MASK) >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"500\"") >= 0);

        assertEquals("Expect exact match from createTestRun and getTestRun", json, api.getTestRun("TEST01", "01"));

        assertTrue(api.getTestRuns("TEST01", 0, 2, "").contains("\"name\" : \"01\""));

        // Update test run
        testRunDetails = new TestRunDetails();
        testRunDetails.setOldName("01");
        testRunDetails.setName("001");
        testRunDetails.setVersion(0);
        testRunDetails.setDescription("The test run 001.");
        json = api.updateTestRun("TEST01", testRunDetails);
        assertTrue(json, json.indexOf("The test run 001.") >= 0);
        assertTrue(json, json.indexOf("\"progress\" : 0.0") >= 0);

        // Set 'processCount' for run '01'
        propSetBean = new PropSetBean();
        propSetBean.setVersion(0);
        propSetBean.setValue("125");
        json = api.setTestRunProperty("TEST01", "001", "processCount", propSetBean);
        assertTrue(json, json.indexOf("\"version\" : 1") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"500\"") >= 0);
        assertTrue(json, json.indexOf("\"value\" : \"125\"") >= 0);
        assertTrue(json, json.indexOf("\"title\" : \"Process Count\"") >= 0);
        dbObject = dao.getProperty("TEST01", "001", "processCount");
        assertEquals("500", dbObject.get(FIELD_DEFAULT));
        assertEquals("125", dbObject.get(FIELD_VALUE));

        // Get the test run by its state
        checkTestRunState("TEST01", "001", TestRunState.NOT_SCHEDULED, true);
        checkTestRunState("TEST01", "001", TestRunState.SCHEDULED, false);

        // Delete the test run
        api.deleteTestRun("TEST01", "001", true);
        try
        {
            api.getTestRun("TEST01", "001");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    /**
     * Copy test run
     */
    @Test
    public void testScenario02()
    {
        DBObject dbObject;
        String json;
        TestDetails testDetails;
        TestRunDetails testRunDetails;
        PropSetBean propSetBean;

        testDetails = new TestDetails();
        testDetails.setName("T02");
        testDetails.setRelease(RELEASE);
        testDetails.setSchema(SCHEMA);
        testDetails.setDescription("A test for scenario 02.");
        json = api.createTest(testDetails);

        // Set 'proc.pwd' at the test level
        propSetBean = new PropSetBean();
        propSetBean.setVersion(0);
        propSetBean.setValue("pwd1");
        json = api.setTestProperty("T02", "proc.pwd", propSetBean);

        // Copy the test
        testDetails.setName("T02_CP");
        testDetails.setRelease(null);
        testDetails.setSchema(null);
        testDetails.setDescription(null);
        testDetails.setCopyOf("T02");
        testDetails.setVersion(0);
        json = api.createTest(testDetails);

        assertTrue(json, json.indexOf("T02_CP") >= 0);
        assertTrue(json, json.indexOf("A test for scenario 02.") >= 0);

        assertEquals("Expect exact match from createTest and getTest", json, api.getTest("T02_CP"));

        assertTrue(api.getTests(RELEASE, SCHEMA, 0, 2).contains("\"name\" : \"T02\""));
        assertTrue(api.getTests(RELEASE, SCHEMA, 0, 2).contains("\"name\" : \"T02_CP\""));

        json = api.getTestProperty("T02_CP", "proc.pwd");
        assertFalse(json, json.indexOf("pwd1") >= 0);
        assertTrue(json, json.indexOf(MASK) >= 0);
        assertTrue(json, json.indexOf("\"description\" : \"The password for the user executing the process.\"") >= 0);
        dbObject = dao.getProperty("T02_CP", null, "proc.pwd");
        assertEquals("NEVER-LEAVES-THE-SERVER", dbObject.get(FIELD_DEFAULT));
        assertEquals("pwd1", dbObject.get(FIELD_VALUE));

        // Create run 01
        testRunDetails = new TestRunDetails();
        testRunDetails.setName("01");
        testRunDetails.setDescription("Scenario 02 - Run 01");
        json = api.createTestRun("T02_CP", testRunDetails);
        assertTrue(json, json.indexOf("\"description\" : \"Scenario 02 - Run 01\"") >= 0);
        assertFalse(json, json.indexOf("\"default\" : \"NEVER-LEAVES-THE-SERVER\"") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"admin\"") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"" + MASK) >= 0);

        // Copy-create run 02
        testRunDetails.setDescription(null);
        testRunDetails.setName("02");
        testRunDetails.setCopyOf("01");
        testRunDetails.setVersion(0);
        json = api.createTestRun("T02_CP", testRunDetails);
        assertTrue(json, json.indexOf("\"description\" : \"Scenario 02 - Run 01\"") >= 0);
        assertFalse(json, json.indexOf("\"default\" : \"NEVER-LEAVES-THE-SERVER\"") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"admin\"") >= 0);
        assertTrue(json, json.indexOf("\"default\" : \"" + MASK) >= 0);
    }

    /**
     * Schedule a test run
     */
    @Test
    public void testScenario03()
    {
        String json;
        TestDetails testDetails;
        TestRunDetails testRunDetails;
        PropSetBean propSetBean;

        testDetails = new TestDetails();
        testDetails.setName("T03");
        testDetails.setRelease(RELEASE);
        testDetails.setSchema(SCHEMA);
        testDetails.setDescription("A test for scenario 03.");
        json = api.createTest(testDetails);

        // Set 'proc.pwd' at the test level
        propSetBean = new PropSetBean();
        propSetBean.setVersion(0);
        propSetBean.setValue("pwd1");
        json = api.setTestProperty("T03", "proc.pwd", propSetBean);

        // Create run 01
        testRunDetails = new TestRunDetails();
        testRunDetails.setName("01");
        testRunDetails.setDescription("Scenario 03 - Run 01");
        json = api.createTestRun("T03", testRunDetails);

        // Set 'processCount' for run '01'
        propSetBean = new PropSetBean();
        propSetBean.setVersion(0);
        propSetBean.setValue("125");
        json = api.setTestRunProperty("T03", "01", "processCount", propSetBean);

        // Schedule the run
        TestRunSchedule testRunSchedule = new TestRunSchedule();
        testRunSchedule.setVersion(5);          // Wrong version
        testRunSchedule.setScheduled(System.currentTimeMillis());
        try
        {
            api.scheduleTestRun("T03", "01", testRunSchedule);
            fail("Version number not checked for test run scheduling.");
        }
        catch (WebApplicationException e)
        {
            // Expected
        }
        testRunSchedule.setVersion(0);          // Correct
        json = api.scheduleTestRun("T03", "01", testRunSchedule);
        assertTrue("'scheduled' not set", json.contains("" + testRunSchedule.getScheduled()));

        // Get the test run by its state
        checkTestRunState("T03", "01", TestRunState.NOT_SCHEDULED, false);
        checkTestRunState("T03", "01", TestRunState.SCHEDULED, true);
        checkTestRunState("T03", "01", TestRunState.STARTED, false);
    }

    /**
     * Helper method to create a test run
     */
    private String createTestRun(String test, String testDescription, String run, String runDescription)
    {
        TestDetails testDetails;
        TestRunDetails testRunDetails;

        testDetails = new TestDetails();
        testDetails.setName(test);
        testDetails.setRelease(RELEASE);
        testDetails.setSchema(SCHEMA);
        testDetails.setDescription(testDescription);
        try
        {
            api.createTest(testDetails);
        }
        catch (WebApplicationException e)
        {
            if (e.getResponse().getStatus() == Status.CONFLICT.getStatusCode())
            {
                // Test already exists
            }
            else
            {
                throw e;
            }
        }

        // Create run 01
        testRunDetails = new TestRunDetails();
        testRunDetails.setName(run);
        testRunDetails.setDescription(runDescription);
        return api.createTestRun(test, testRunDetails);
    }

    /**
     * Locking of properties and state change protection
     */
    @Test
    public synchronized void testScenario04()
    {
        String json = createTestRun("T04", "A test for scenario 04.", "01", "Scenario 04 - Run 01");

        // First check that we get the correct response for a missing test run
        try
        {
            api.getTestRunSummary("Fred", "01");
            fail("Missing test must throw NOT FOUND.");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
        try
        {
            api.getTestRunSummary("T04", "Fred");
            fail("Missing test run must throw NOT FOUND.");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

        // It should not have started
        json = api.getTestRunSummary("T04", "01");
        assertTrue(json.contains("\"started\" : -1"));

        // Attempt to override the test run properties
        json = api.getTestRunProperty("T04", "01", "proc.pwd");
        PropSetBean propSetBean = new PropSetBean();
        propSetBean.setValue("NEW VALUE BEFORE START");
        propSetBean.setVersion(0);
        api.setTestRunProperty("T04", "01", "proc.pwd", propSetBean);
        try
        {
            // Using an incorrect version
            api.setTestRunProperty("T04", "01", "proc.pwd", propSetBean);
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }

        // Point to the correct MongoDB
        propSetBean.setValue(mongoHost);
        propSetBean.setVersion(0);
        api.setTestRunProperty("T04", "01", PROP_MONGO_TEST_HOST, propSetBean);

        // Monitor the test. We do this here so that we don't have to wait a
        // long time for the monitor to kick in.
        // The defaults here do not matter as the test definition was written in
        // the @Before phase
        org.alfresco.bm.driver.test.Test test = ctx.getBean(org.alfresco.bm.driver.test.Test.class);

        // Extract the test run wrapper
        DBObject testRunObj = null;
        try
        {
            testRunObj = dao.getTestRun("T04", "01", false);
        }
        catch (ObjectNotFoundException e1)
        {
            fail("Should find 'T04.01'!");
        }
        ObjectId testRunId = (ObjectId) testRunObj.get(FIELD_ID);
        TestRun testRun = test.getTestRun(testRunId);

        // The test run should not even be on the menu
        assertNull("Should not even be considering the test run", testRun);

        // Now schedule it
        TestRunSchedule schedule = new TestRunSchedule();
        schedule.setScheduled(System.currentTimeMillis() + 20000L);         // It
                                                                            // is
                                                                            // scheduled
                                                                            // for
                                                                            // 20s
                                                                            // from
                                                                            // now
        schedule.setVersion(0);
        json = api.scheduleTestRun("T04", "01", schedule);

        // Poke the test and check that the test run's new state brings it into
        // view
        test.forcePing();
        testRun = test.getTestRun(testRunId);
        assertNotNull("The test should have found the test run as it is now scheduled.", testRun);
        assertNull("The test run has not hit scheduled time so it must have started its context.", testRun.getCtx());
        // The test run should not have started as the scheduled time will not
        // have passed
        checkTestRunState("T04", "01", TestRunState.SCHEDULED, true);
        checkTestRunState("T04", "01", TestRunState.STARTED, false);

        // Now update the schedule to make it runnable immediately
        schedule.setScheduled(System.currentTimeMillis() - 20000L);             // Overdue
        schedule.setVersion(1);
        json = api.scheduleTestRun("T04", "01", schedule);
        test.forcePing();
        assertNotNull("Should be a test run context", testRun.getCtx());
        checkTestRunState("T04", "01", TestRunState.SCHEDULED, false);
        checkTestRunState("T04", "01", TestRunState.STARTED, true);

        // Attempt to override the test run properties
        json = api.getTestRunProperty("T04", "01", "proc.pwd");
        propSetBean = new PropSetBean();
        propSetBean.setValue("NEW VALUE AFTER START");
        propSetBean.setVersion(1);
        try
        {
            api.setTestRunProperty("T04", "01", "proc.pwd", propSetBean);
            fail("Should not be able to override properties of a running test.");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.FORBIDDEN.getStatusCode(), e.getResponse().getStatus());
        }

        // Terminate
        checkTestRunState("T04", "01", TestRunState.STARTED, true);
        json = api.terminateTestRun("T04", "01");
        checkTestRunState("T04", "01", TestRunState.STOPPED, true);
        String nowStr = "" + System.currentTimeMillis();
        assertTrue(json.contains("\"stopped\" : " + nowStr.substring(0, 5)));

        // Must still not be able to edit any properties
        try
        {
            api.setTestRunProperty("T04", "01", "proc.pwd", propSetBean);
            fail("Should not be able to override properties of a terminated test.");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.FORBIDDEN.getStatusCode(), e.getResponse().getStatus());
        }

        // The test run should have stopped
        test.forcePing();
        assertNull("Should NOT be a test run context after termination", testRun.getCtx());

        // Make sure that the test drops the test run from consideration
        test.forcePing();
        assertNull("Test must drop a completed test run", test.getTestRun(testRunId));

        try
        {
            test.stop();
        }
        catch (Exception e)
        {
            fail("Test should not fail on stop!");
        }
    }

    /**
     * Check that progress updates are made automatically and that the test run
     * automatically
     * stops and closes down.
     */
    @Test
    public synchronized void testScenario05() throws Exception
    {
        createTestRun("T05", "A test for scenario 05.", "01", "Scenario 05 - Run 01");

        // Monitor the test. We do this here so that we don't have to wait a
        // long time for the monitor to kick in.
        // The defaults here do not matter as the test definition was written in
        // the @Before phase
        org.alfresco.bm.driver.test.Test test = ctx.getBean(org.alfresco.bm.driver.test.Test.class);

        // Force an immediate ping update
        test.forcePing();

        // Extract the test run wrapper
        DBObject testRunObj = null;
        try
        {
            testRunObj = dao.getTestRun("T05", "01", false);
        }
        catch (ObjectNotFoundException e1)
        {
            fail("Should find 'T05.01'!");
        }
        ObjectId testRunId = (ObjectId) testRunObj.get(FIELD_ID);
        TestRun testRun = test.getTestRun(testRunId);
        assertNull("The test run should not be instantiated until it has been scheduled.", testRun);

        // Now schedule it
        TestRunSchedule schedule = new TestRunSchedule();
        schedule.setScheduled(System.currentTimeMillis());
        schedule.setVersion(0);
        api.scheduleTestRun("T05", "01", schedule);

        // Point to the correct MongoDB
        PropSetBean propSetBean = new PropSetBean();
        propSetBean.setValue(mongoHost);
        propSetBean.setVersion(0);
        api.setTestRunProperty("T05", "01", PROP_MONGO_TEST_HOST, propSetBean);

        // Force another ping, which will activate the test run
        test.forcePing();
        testRun = test.getTestRun(testRunId);
        assertNotNull("The test run shouldbe instantiated once it has been scheduled.", testRun);

        // Loop a bit to allow the test run to complete
        boolean completed = false;
        for (int i = 0; i < 10; i++)
        {
            this.wait(1000L);
            // Need to keep checking progress
            test.forcePing();
            String testRunStateJson = api.getTestRunSummary("T05", "01");
            if (testRunStateJson.contains(TestRunState.COMPLETED.toString()))
            {
                completed = true;
                break;              // This is what we were looking for
            }
        }
        checkTestRunState("T05", "01", TestRunState.COMPLETED, true);

        // Check that it's completed
        assertTrue("Test run did not progress to completion: " + api.getTestRunSummary("T05", "01"), completed);

        // Force another ping, which will deactivate the test run
        test.forcePing();

        // Terminate
        try
        {
            api.terminateTestRun("T05", "01");
            fail("Cannot terminate a test run that has completed.");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }

        // The test run should have completed
        assertNull("Should NOT be a test run context after completion", testRun.getCtx());
    }

    /**
     * Point the test at a missing MongoDB
     */
    @Test
    public synchronized void testScenario06() throws Exception
    {
        createTestRun("T06", "A test for scenario 06.", "01", "Scenario 06 - Run 01");

        // Set an incorrect MongoDB host
        PropSetBean propSetBean = new PropSetBean();
        propSetBean.setValue("localhostFAIL");
        propSetBean.setVersion(0);
        api.setTestRunProperty("T06", "01", PROP_MONGO_TEST_HOST, propSetBean);

        // Direct monitor
        org.alfresco.bm.driver.test.Test test = ctx.getBean(org.alfresco.bm.driver.test.Test.class);

        // Now schedule it
        TestRunSchedule schedule = new TestRunSchedule();
        schedule.setScheduled(System.currentTimeMillis());
        schedule.setVersion(0);
        api.scheduleTestRun("T06", "01", schedule);

        // Poke the test and check that the test run's new state brings it into
        // view
        test.forcePing();
        // The test run should not have started as the mongo host is invalid
        // from server 2.1 on the test would be STOPPED if incorrect server
        // connection - because otherwise it will restart over and over again.
        // depending on the test check speed the status would be scheduled or
        // stopped - but must never be started!
        checkTestRunState("T06", "01", TestRunState.STARTED, false);

        // Now check that the logs reflect the situation
        LogWatcher logWatcher = ctx.getBean(LogWatcher.class);
        boolean found = false;
        for (String logFilename : logWatcher.getLogFilenames())
        {
            File logFile = new File(logFilename);
            Scanner scanner = new Scanner(logFile);
            try
            {
                String scanned = scanner.findWithinHorizon("localhostFAIL", 0);
                if (scanned != null)
                {
                    found = true;
                    break;
                }
            }
            finally
            {
                scanner.close();
            }
        }
        assertTrue("Failed to find error message for MongoDB host.", found);

        test.stop();
    }

    /**
     * Utility method to create and execute a test run
     */
    private void executeTestRun(String testName, String testDescription, String runName, String runDescription)
            throws Exception
    {
        createTestRun(testName, testDescription, runName, runDescription);
        org.alfresco.bm.driver.test.Test test = ctx.getBean(org.alfresco.bm.driver.test.Test.class);
        test.forcePing();

        // Now schedule it
        TestRunSchedule schedule = new TestRunSchedule();
        schedule.setScheduled(System.currentTimeMillis());
        schedule.setVersion(0);
        api.scheduleTestRun(testName, runName, schedule);

        // Point to the correct MongoDB
        String testHost = api.getTestRunProperty(testName, runName, PROP_MONGO_TEST_HOST);
        PropSetBean propSetBean = new PropSetBean();
        propSetBean.setValue(mongoHost);
        propSetBean.setVersion(0);
        api.setTestRunProperty(testName, runName, PROP_MONGO_TEST_HOST, propSetBean);

        // Force another ping, which will activate the test run
        test.forcePing();

        // Loop a bit to allow the test run to complete
        boolean completed = false;
        for (int i = 0; i < 10; i++)
        {
            this.wait(1000L);
            // Need to keep checking progress
            test.forcePing();
            String testRunSummaryJson = api.getTestRunSummary(testName, runName);
            Map<String, Object> testRunSummaryMap = fromJson(testRunSummaryJson);
            if (TestRunState.COMPLETED.name().equals(testRunSummaryMap.get(FIELD_STATE)))
            {
                completed = true;
                break;              // This is what we were looking for
            }
        }
        checkTestRunState(testName, runName, TestRunState.COMPLETED, true);

        // Check that it's completed
        assertTrue("Test run did not progress to completion: " + api.getTestRunSummary(testName, runName), completed);

        // Force another ping, which will deactivate the test run
        test.forcePing();

    }

    /**
     * Checks the test application APIs for retrieving test results.
     */
    @Test
    public synchronized void testScenario07() throws Exception
    {
        /*
         * In-memory byte[] are used because we know the size of the documents
         * will be manageable.
         */

        executeTestRun("T07", "A test for scenario 07.", "01", "Scenario 07 - Run 01");

        // Get report for test run that does not exist
        try
        {
            ResultsRestAPI resultsAPI = api.getTestRunResultsAPI("T07MISSING", "01");
            resultsAPI.getReportCSV();
            Assert.fail("Expected exception regarding missing test.");
        }
        catch (WebApplicationException e)
        {
            assertEquals(Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

        ResultsRestAPI resultsAPI = api.getTestRunResultsAPI("T07", "01");

        // Get the results CSV
        StreamingOutput csvOutput = resultsAPI.getReportCSV();
        ByteArrayOutputStream csvBos = new ByteArrayOutputStream();
        csvBos.close();
        csvOutput.write(csvBos);
        String csvResults = new String(csvBos.toByteArray());
        // Check
        assertTrue(csvResults.contains("Data:,bm21-data.T07.01.results"));
        assertTrue(csvResults.contains("Started"));
        assertTrue(csvResults.contains("Finished"));
        assertTrue(csvResults.contains("Duration"));

        // Get the JSON results
        String chartJson = resultsAPI.getTimeSeriesResults(0L, "seconds", 1, 5, false);
        assertTrue(chartJson.startsWith("[ { \"time\" : "));
        assertTrue(chartJson.contains("[ { \"time\" : "));
        assertTrue(chartJson.contains("000 , \"name\" : \"start\" , \"mean\" : "));
        assertTrue(chartJson.endsWith(" , \"fail\" : 0 , \"failPerSec\" : 0.0}]"));

        // Get the XLSX report
        StreamingOutput xlsxOutput = resultsAPI.getReportXLSX();
        ByteArrayOutputStream xlsxBos = new ByteArrayOutputStream();
        xlsxOutput.write(xlsxBos);
        xlsxBos.close();
        ByteArrayInputStream xlsxBis = new ByteArrayInputStream(xlsxBos.toByteArray());
        XSSFWorkbook xlsxWorkbook = new XSSFWorkbook(xlsxBis);
        xlsxBis.close();
        // Write it to a temp file just for fun
        File xlsxFile = File.createTempFile(UUID.randomUUID().toString(), ".xlsx");
        FileOutputStream xlsxFos = new FileOutputStream(xlsxFile);
        xlsxWorkbook.write(xlsxFos);
        xlsxFos.close();
        logger.info("XLSX report found here: " + xlsxFile);

        // Check
        POIXMLProperties xlsxProperties = xlsxWorkbook.getProperties();
        CoreProperties xlsxCoreProperties = xlsxProperties.getCoreProperties();
        Assert.assertNotNull("No XLSX description: " + xlsxFile, xlsxCoreProperties.getDescription());
        Assert.assertTrue("Title in XLSX must contain test run FQN: " + xlsxFile,
                xlsxCoreProperties.getTitle().contains("T07.01"));
        Assert.assertTrue("Description in XLSX must contain test name and description: " + xlsxFile,
                xlsxCoreProperties.getDescription().contains("A test for scenario 07."));
        Assert.assertTrue("Description in XLSX must contain test name and description." + xlsxFile,
                xlsxCoreProperties.getDescription().contains("Scenario 07 - Run 01"));
        xlsxWorkbook.close();
    }

    /**
     * Checks that test run deletion removes the associated collections
     */
    @Test
    public synchronized void testScenario08() throws Exception
    {
        MongoClient testMongoClient = new MongoClientFactory(new MongoClientURI(MONGO_PREFIX + mongoHost), null, null)
                .getObject();
        DB testMongoDB = new MongoDBFactory(testMongoClient, MONGO_TEST_DATABASE).getObject();
        Set<String> testRunCollections = removeSystemValues(testMongoDB.getCollectionNames());
        Assert.assertEquals("Unexpected number of collections in results: " + testRunCollections, 0,
                testRunCollections.size());

        executeTestRun("T08", "A test for scenario 08.", "01", "Scenario 08 - Run 01");

        testRunCollections = removeSystemValues(testMongoDB.getCollectionNames());
        Assert.assertEquals("Unexpected number of collections in results: " + testRunCollections, 5,
                testRunCollections.size());
        Assert.assertTrue(testRunCollections.contains("T08.01.events"));
        Assert.assertTrue(testRunCollections.contains("T08.01.results"));
        Assert.assertTrue(testRunCollections.contains("T08.01.sessions"));

        // Delete the test run
        api.deleteTestRun("T08", "01", true);
        testRunCollections = removeSystemValues(testMongoDB.getCollectionNames());
        Assert.assertEquals("Unexpected number of collections in results: " + testRunCollections, 2,
                testRunCollections.size());
    }

    /**
     * Checks that test deletion removes the associated collections
     */
    @Test
    public synchronized void testScenario09() throws Exception
    {
        MongoClient testMongoClient = new MongoClientFactory(new MongoClientURI(MONGO_PREFIX + mongoHost), null, null)
                .getObject();
        DB testMongoDB = new MongoDBFactory(testMongoClient, MONGO_TEST_DATABASE).getObject();
        Set<String> testRunCollections = removeSystemValues(testMongoDB.getCollectionNames());
        Assert.assertEquals("Unexpected number of collections in results: " + testRunCollections, 0,
                testRunCollections.size());

        executeTestRun("T09", "A test for scenario 09.", "01", "Scenario 09 - Run 01");
        executeTestRun("T09", "A test for scenario 09.", "02", "Scenario 09 - Run 02");

        testRunCollections = removeSystemValues(testMongoDB.getCollectionNames());
        Assert.assertEquals("Unexpected number of collections in results: " + testRunCollections, 8,
                testRunCollections.size());
        Assert.assertTrue(testRunCollections.contains("T09.01.events"));
        Assert.assertTrue(testRunCollections.contains("T09.01.results"));
        Assert.assertTrue(testRunCollections.contains("T09.01.sessions"));
        Assert.assertTrue(testRunCollections.contains("T09.02.events"));
        Assert.assertTrue(testRunCollections.contains("T09.02.results"));
        Assert.assertTrue(testRunCollections.contains("T09.02.sessions"));

        // Delete the test run
        api.deleteTest("T09", true);
        testRunCollections = removeSystemValues(testMongoDB.getCollectionNames());
        Assert.assertEquals("Unexpected number of collections in results: " + testRunCollections, 2,
                testRunCollections.size());
    }

    @Test
    public synchronized void testScenario10() throws Exception
    {
        // Create the test
        executeTestRun("T10", "A test for scenario 10.", "01", "Scenario 10 - Run 01");
        // Now get the registered drivers
        String json = api.getTestDrivers("T10", true);
        assertTrue("Driver not found for test: " + json, json.contains("\"expires\" : { \"$date\" : \"20"));
    }

    /**
     * Extra data creation and export of XSLX Workbook
     * 
     * @throws Exception
     */
    @Test
    public synchronized void testScenario11() throws Exception
    {
        // test definitions and extra data sheets
        String test = "T11";
        String run = "01";
        String driver = UUID.randomUUID().toString();
        String sheet1 = "extra.data.sheet1";
        String sheet2 = "extra.data.sheet2";
        String sheet3 = "extra.data.sheet3";
        String[] fieldNames = { "tf1", "tf2", "tf3", "tf4", "tf5" };
        String[] descriptions = { "Test Field 1", "Test Field 2", "Test Field 3", "Test Field 4", "Test Field 5" };
        String[] values = new String[5];

        executeTestRun(test, "A test for scenario 11.", run, "Scenario 11 - Run 01");

        // get the extra data report service
        DataReportService reportService = this.testRunServicesCache.getDataReportService(test, run);
        assertNotNull(reportService);

        // create a random value array
        for (int i = 0; i < 5; i++)
        {
            values[i] = UUID.randomUUID().toString();
        }

        // append some extra data and a description
        reportService.appendData(driver, test, run, sheet1, fieldNames, values);
        reportService.appendData(driver, test, run, sheet2, fieldNames, values);
        reportService.appendData(driver, test, run, sheet2, fieldNames, values);
        reportService.appendData(driver, test, run, sheet3, fieldNames, values);
        reportService.appendData(driver, test, run, sheet3, fieldNames, values);
        reportService.appendData(driver, test, run, sheet3, fieldNames, values);
        reportService.setDescription(driver, test, run, sheet3, fieldNames, descriptions);

        // create and download an XSLX
        ResultsRestAPI resultsAPI = api.getTestRunResultsAPI(test, run);
        StreamingOutput xlsxOutput = resultsAPI.getReportXLSX();
        ByteArrayOutputStream xlsxBos = new ByteArrayOutputStream();
        xlsxOutput.write(xlsxBos);
        xlsxBos.close();
        ByteArrayInputStream xlsxBis = new ByteArrayInputStream(xlsxBos.toByteArray());
        XSSFWorkbook xlsxWorkbook = new XSSFWorkbook(xlsxBis);
        xlsxBis.close();
        File xlsxFile = File.createTempFile(UUID.randomUUID().toString(), ".xlsx");
        FileOutputStream xlsxFos = new FileOutputStream(xlsxFile);
        xlsxWorkbook.write(xlsxFos);
        xlsxFos.close();

        // validate data in DB
        assertNull("Expected no description row for sheet 1, but found one?",
                reportService.getDescription(null, test, run, sheet1));
        assertNull("Expected no description row for sheet 2, but found one?",
                reportService.getDescription(null, test, run, sheet2));
        List<String> descList = reportService.getDescription(null, test, run, sheet3);
        assertNotNull("Expected a description row for sheet 3, but can't find any ...", descList);
        int count = 0;
        for (String desc : descList)
        {
            assertEquals(
                    "Description of sheet 3 mismatch. Expected '" + descriptions[count] + "' found '" + desc + "'.",
                    descriptions[count++], desc);
        }

        // validate number of sheets
        String[] sheetNames = reportService.getSheetNames(null, test, run);
        assertEquals("Expected 3 sheets of extra data, found: " + sheetNames.length, 3, sheetNames.length);

        // validate number of entries for each extra data
        int countTestEntries = 0;
        for (int j = 0; j < 3; j++)
        {
            DBCursor dbCursor = reportService.getData(null, test, run, sheetNames[j]);
            while (null != dbCursor && dbCursor.hasNext())
            {
                List<String> valuesFromMongo = reportService.getNextValueRow(dbCursor);
                countTestEntries++;
                assertEquals("Expected 5 columns in extra data, found: " + valuesFromMongo.size(), 5,
                        valuesFromMongo.size());
            }
        }
        assertEquals("Expected 6 lines of extra data ... found: " + countTestEntries, 6, countTestEntries);

        // delete test
        api.deleteTest(test, true);

        // validate
        assertNull("Sheet names expected to be empty.", reportService.getSheetNames(null, test, run));
        assertNull("Description of sheet 3 expected to be empty.",
                reportService.getDescription(null, test, run, sheet3));

        // clean-up: delete XLSX file
        xlsxWorkbook.close();
        xlsxFile.delete();
    }
}
