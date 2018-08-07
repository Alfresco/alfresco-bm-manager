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
package org.alfresco.bm.driver.test.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.alfresco.bm.common.ImportResult;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.driver.test.prop.TestProperty;
import org.alfresco.bm.driver.test.prop.TestPropertyFactory;
import org.alfresco.bm.driver.test.prop.TestPropertyOrigin;
import org.alfresco.bm.common.util.junit.tools.MongoDBForTestsFactory;
import org.alfresco.bm.common.util.cipher.CipherException;
import org.alfresco.bm.common.util.cipher.CipherVersion;
import org.alfresco.bm.common.util.exception.ObjectNotFoundException;
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.alfresco.bm.common.TestConstants.CAPABILITY_JAVA7;
import static org.alfresco.bm.common.TestConstants.FIELD_CIPHER;
import static org.alfresco.bm.common.TestConstants.FIELD_COMPLETED;
import static org.alfresco.bm.common.TestConstants.FIELD_DEFAULT;
import static org.alfresco.bm.common.TestConstants.FIELD_DESCRIPTION;
import static org.alfresco.bm.common.TestConstants.FIELD_DRIVERS;
import static org.alfresco.bm.common.TestConstants.FIELD_DURATION;
import static org.alfresco.bm.common.TestConstants.FIELD_ID;
import static org.alfresco.bm.common.TestConstants.FIELD_NAME;
import static org.alfresco.bm.common.TestConstants.FIELD_ORIGIN;
import static org.alfresco.bm.common.TestConstants.FIELD_PROGRESS;
import static org.alfresco.bm.common.TestConstants.FIELD_PROPERTIES;
import static org.alfresco.bm.common.TestConstants.FIELD_RELEASE;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULT;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_FAIL;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_SUCCESS;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_TOTAL;
import static org.alfresco.bm.common.TestConstants.FIELD_SCHEDULED;
import static org.alfresco.bm.common.TestConstants.FIELD_SCHEMA;
import static org.alfresco.bm.common.TestConstants.FIELD_STARTED;
import static org.alfresco.bm.common.TestConstants.FIELD_STATE;
import static org.alfresco.bm.common.TestConstants.FIELD_STOPPED;
import static org.alfresco.bm.common.TestConstants.FIELD_SUCCESS_RATE;
import static org.alfresco.bm.common.TestConstants.FIELD_TEST;
import static org.alfresco.bm.common.TestConstants.FIELD_VALUE;
import static org.alfresco.bm.common.TestConstants.FIELD_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @see MongoTestDAO
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class MongoTestDAOTest
{
    private static final Set<String> capabilities = Collections.singleton(CAPABILITY_JAVA7);
    private static final String INHERITANCE = "sample,common,crud";

    private static Properties properties;
    private MongoDBForTestsFactory mongoFactory;
    private MongoTestDAO dao;
    private DB db;
    
    @BeforeClass
    public static void setUpProperties() throws Exception
    {
        InputStream propStream = MongoTestDAOTest.class.getClassLoader().getResourceAsStream("prop/samples-1.properties");
        try
        {
            assertNotNull("Properties not found: prop/samples-1.properties", propStream);
            properties = new Properties();
            properties.load(propStream);
        }
        finally
        {
            try { propStream.close(); } catch (Exception e) {}
        }
    }
    
    @Before
    public void setUp() throws Exception
    {
        this.mongoFactory = new MongoDBForTestsFactory();
        this.db = mongoFactory.getObject();
        this.dao = new MongoTestDAO(db);
        this.dao.start();
    }
    
    @After
    public void tearDown() throws Exception
    {
        this.dao.stop();
        this.mongoFactory.destroy();
        
        this.mongoFactory = null;
        this.db = null;
        this.dao = null;
    }
    
    @Test
    public void basic()
    {
        assertNotNull(properties);
        assertNotNull(db);
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("test.drivers");
        collectionNames.add("test.defs");
        collectionNames.add("tests");
        collectionNames.add("test.runs");
        collectionNames.add("test.props");
        assertEquals(collectionNames, removeSystemValues( db.getCollectionNames()));
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
    public void testDrivers()
    {
        String releaseA = "A." + UUID.randomUUID().toString();
        String releaseB = "B." + UUID.randomUUID().toString();
        Integer schema1 = 1;
        Integer schema2 = 2;
        long expires1 = System.currentTimeMillis() - 10000L;         // 10s ago
        long expires2 = System.currentTimeMillis() + 10000L;         // 10s yet

        // Note the insertion order
        String id3 = dao.registerDriver(releaseB, schema1, "192.168.0.1", "localhost", "/app", capabilities);
        String id1 = dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/app", capabilities);
        String id2 = dao.registerDriver(releaseA, schema2, "192.168.0.1", "localhost", "/app", capabilities);

        DBCursor cursor = null;
        
        cursor = dao.getDrivers(null, null, true);          // Get only active.  They should all be inactive
        assertEquals(0, cursor.size());
        
        // Update their registrations (active)
        dao.refreshDriver(id1, expires2);
        dao.refreshDriver(id2, expires2);
        dao.refreshDriver(id3, expires2);

        cursor = dao.getDrivers(null, null, false);
        assertEquals(3, cursor.size());
        assertEquals("Order is wrong. ", releaseA, cursor.next().get(FIELD_RELEASE));
        assertEquals("Order is wrong. ", schema1, cursor.curr().get(FIELD_SCHEMA));
        assertEquals("Order is wrong. ", releaseA, cursor.next().get(FIELD_RELEASE));
        assertEquals("Order is wrong. ", schema2, cursor.curr().get(FIELD_SCHEMA));
        assertEquals("Order is wrong. ", releaseB, cursor.next().get(FIELD_RELEASE));
        assertEquals("Order is wrong. ", schema1, cursor.curr().get(FIELD_SCHEMA));

        cursor = dao.getDrivers(null, null, true);
        assertEquals(3, cursor.size());

        cursor = dao.getDrivers(releaseA, null, false);
        assertEquals(2, cursor.size());

        cursor = dao.getDrivers(releaseA, schema1, false);
        assertEquals(1, cursor.size());

        cursor = dao.getDrivers(null, schema1, false);
        assertEquals(2, cursor.size());
        
        // Update their registrations (expired)
        dao.refreshDriver(id1, expires1);
        dao.refreshDriver(id2, expires1);
        dao.refreshDriver(id3, expires1);

        cursor = dao.getDrivers(null, null, false);
        assertEquals(3, cursor.size());

        cursor = dao.getDrivers(null, null, true);
        assertEquals(0, cursor.size());

        cursor = dao.getDrivers(releaseA, null, false);
        assertEquals(2, cursor.size());

        cursor = dao.getDrivers(releaseA, schema1, false);
        assertEquals(1, cursor.size());

        cursor = dao.getDrivers(null, schema1, false);
        assertEquals(2, cursor.size());
        
        // Unregister a valid ID
        dao.unregisterDriver(id1);
        cursor = dao.getDrivers(null, null, false);
        assertEquals(2, cursor.size());

        // Ensure that we can register as many as we want
        dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/app", capabilities);
        dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/app", capabilities);
        dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/app", capabilities);
        dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/app", capabilities);
        dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/app", capabilities);
    }
    
    @Test
    public void testWriteTestDef()
    {
        String releaseA = UUID.randomUUID().toString();
        String releaseB = UUID.randomUUID().toString();
        Integer schema1 = 1;
        String description = "For Definitions Testing";
        
        List<TestProperty> testProperties = TestPropertyFactory.getTestProperties(INHERITANCE, properties);
        boolean written = dao.writeTestDef(releaseA, schema1, description, testProperties);
        assertTrue(written);
        boolean writtenAgain = dao.writeTestDef(releaseA, schema1, "Another", testProperties);
        assertFalse(writtenAgain);
        written = dao.writeTestDef(releaseB, schema1, description, testProperties);
        assertTrue(written);
        
        written = dao.writeTestDef(releaseB, schema1, description, testProperties);
        assertFalse("Should not write for the same release:schema", written);
        
        try
        {
            // Now write a test definition that downgrades the schema number
            dao.writeTestDef(releaseB, 0, description, testProperties);
            fail("Downgrading of schema should not be considered.");
        }
        catch (RuntimeException  e)
        {
            // Expected
        }
    }
    
    @Test
    public void testDefs()
    {
        String releaseA = "A." + UUID.randomUUID().toString();
        String releaseB = "B." + UUID.randomUUID().toString();
        Integer schema1 = 1;
        String description = "For Definitions Testing";
        
        List<TestProperty> testProperties = TestPropertyFactory.getTestProperties(INHERITANCE, properties);
        // Write B before A
        boolean written = dao.writeTestDef(releaseB, schema1, description, testProperties);
        assertTrue(written);
        written = dao.writeTestDef(releaseA, schema1, description, testProperties);
        assertTrue(written);
        boolean writtenAgain = dao.writeTestDef(releaseA, schema1, "Another", testProperties);
        assertFalse(writtenAgain);
        
        // Get all test defs
        DBCursor cursor = dao.getTestDefs(false, 0, 5);
        assertEquals("Incorrect number of test definitions", 2, cursor.size());
        cursor = dao.getTestDefs(false, 0, 1);
        assertEquals("Test definition paging broken", 1, cursor.size());
        assertEquals("Order is wrong. ", releaseA, cursor.next().get(FIELD_RELEASE));
        cursor = dao.getTestDefs(false, 1, 5);
        assertEquals("Test definition paging broken", 1, cursor.size());
        assertEquals("Order is wrong. ", releaseB, cursor.next().get(FIELD_RELEASE));
        
        // Now get active test defs
        cursor = dao.getTestDefs(true, 0, 5);
        assertEquals("Incorrect number of active test definitions", 0, cursor.size());
        // Write an expired driver entry
        String releaseAID = dao.registerDriver(releaseA, schema1, "192.168.0.1", "localhost", "/something", Collections.emptySet());
        // Still nothing active
        cursor = dao.getTestDefs(true, 0, 5);
        assertEquals("Incorrect number of active test definitions", 0, cursor.size());
        // Make it active
        dao.refreshDriver(releaseAID, System.currentTimeMillis() + 20);
        // One active
        cursor = dao.getTestDefs(true, 0, 5);
        assertEquals("Incorrect number of active test definitions", 1, cursor.size());
    }
    
    @Test
    public void testDef()
    {
        String releaseA = UUID.randomUUID().toString();
        String releaseB = UUID.randomUUID().toString();
        Integer schema1 = 1;
        String description = "For Definitions Testing";
        
        List<TestProperty> testProperties = TestPropertyFactory.getTestProperties(INHERITANCE, properties);
        boolean written = dao.writeTestDef(releaseA, schema1, description, testProperties);
        assertTrue(written);
        boolean writtenAgain = dao.writeTestDef(releaseA, schema1, "Another", testProperties);
        assertFalse(writtenAgain);
        written = dao.writeTestDef(releaseB, schema1, description, testProperties);
        assertTrue(written);
        
        assertNull(dao.getTestDef("GONE", schema1));
        assertNull(dao.getTestDef(releaseA, 0));
        
        DBObject testDefObj = dao.getTestDef(releaseA, schema1);
        assertNotNull(testDefObj);
        testDefObj = dao.getTestDef(releaseB, schema1);
        assertNotNull(testDefObj);
        
        // Check the structure of the test defintion
        assertEquals(description, testDefObj.get(FIELD_DESCRIPTION));
        Object propsObj = testDefObj.get(FIELD_PROPERTIES);
        assertNotNull(propsObj);
        assertTrue(propsObj instanceof Collection);
        String propsStr = propsObj.toString();
        assertTrue(propsStr.contains("\"name\" : \"two.str\" , \"min\" : \"0\""));
    }
    
    @Test
    public void testMaskPropertyNames() throws ObjectNotFoundException
    {
        // Create test definition
        String release = UUID.randomUUID().toString();
        Integer schema = 1;
        String description = "Mask Testing";
        List<TestProperty> testProperties = TestPropertyFactory.getTestProperties(INHERITANCE, properties);
        
        // check with non-existing test 
        try
        {
            dao.getMaskedProperyNames(release, schema);
            fail("Expected 'ObjectNotFoundException'!");
        }
        catch(ObjectNotFoundException ex)
        {
            // expected
        }
        
        // write test definition 
        boolean written = dao.writeTestDef(release, schema, description, testProperties);
        assertTrue("Unable to write test definition '" + release + "-schema:" + schema + "'", written);
        
        // check to get by release and schema
        Set<String>namesToMask1 = dao.getMaskedProperyNames(release, schema);
        assertTrue(namesToMask1 instanceof Set);
        
        // Create test
        String testName = "Test_" + release.replace("-", "_");
        boolean created = dao.createTest(testName, description, release, schema);
        assertTrue("Unable to create test '" + testName + "'!", created);
        
        // get by test name
        Set<String>namesToMask2 = dao.getMaskedProperyNames(testName);
        assertTrue(namesToMask2 instanceof Set);
        
        // check basics (one masked property)
        assertEquals(namesToMask1.size(), namesToMask2.size());
        assertEquals(1, namesToMask1.size());
        
        // make sure "one.str" is contained
        assertTrue("Missing masked 'one.str'!", namesToMask1.contains("one.str"));
        
        // cleanup
        assertTrue("Unable to delete test '" + testName + "'", dao.deleteTest(testName));
        
        // check again
        try
        {
            dao.getMaskedProperyNames(testName);
            fail("Expected 'ObjectNotFoundException'!");
        }
        catch(ObjectNotFoundException ex)
        {
            // expected
        }
    }

    @Test
    public void testCreateTestWithInvalidNames()
    {
        String[] invalid = new String[] {"1TEST", "TEST 1", "TEST-1"};
        for (String name : invalid)
        {
            try
            {
                dao.createTest(name, null, null, null);
                fail("Test name should not have been allowed: " + name);
            } catch (IllegalArgumentException e) {}
        }
    }
    
    /**
     * Helper method to create a common test
     */
    private String createTest(String description)
    {
        String testA = "T" + UUID.randomUUID().toString().replace("-", "_");
        String releaseA = "T" + UUID.randomUUID().toString().replace("-", "_");
        Integer schema1 = 1;
        
        List<TestProperty> testProperties = TestPropertyFactory.getTestProperties(INHERITANCE, properties);
        dao.writeTestDef(releaseA, schema1, description, testProperties);
        
        boolean created = dao.createTest(testA, description, releaseA, schema1);
        assertTrue(created);
        
        return testA;
    }
    
    @Test
    public void testCreateTest()
    {
        String description = "For Test Testing";
        String testA = createTest(description);
        
        DBObject testObj = dao.getTest(testA, true);
        assertNotNull(testObj);
        assertEquals(7, testObj.toMap().size());
        assertEquals(testA, testObj.get(FIELD_NAME));
        assertEquals(Integer.valueOf(0), testObj.get(FIELD_VERSION));
        assertEquals(description, testObj.get(FIELD_DESCRIPTION));
        Object propsObj = testObj.get(FIELD_PROPERTIES);
        assertNotNull(propsObj);
        assertTrue(propsObj instanceof Collection);
        String propsStr = propsObj.toString();
        assertTrue(propsStr.contains("\"name\" : \"one.dec\" , \"min\" : \"-5.4\""));
    }
    
    @Test
    public void testCreateTestDuplicate()
    {
        String release = "R";
        Integer schema = Integer.valueOf(10);
        
        assertTrue(dao.createTest("T", null, release, schema));
        assertFalse(dao.createTest("T", null, release, schema));
    }
    
    @Test
    public void testDeleteTest()
    {
        String description = "For Test Testing";
        String testA = createTest(description);

        DBObject testObj = dao.getTest(testA, false);
        assertNotNull(testObj);
        
        boolean deleted = dao.deleteTest("missing");
        assertFalse(deleted);
        deleted = dao.deleteTest(testA);
        assertTrue(deleted);
    }
    
    @Test
    public void testGetTests()
    {
        dao.createTest("A", null, "R1", 0);
        dao.createTest("B", null, "R2", 1);
        dao.createTest("C", "D", "R3", 2);
        
        assertEquals(3, dao.getTests(null, null, 0, 5).size());
        assertEquals(3, dao.getTests(null, null, 0, 5).size());
        assertEquals(3, dao.getTests(null, null, 0, 5).size());
        assertEquals(1, dao.getTests(null, null, 0, 1).size());
        assertEquals(1, dao.getTests(null, null, 1, 1).size());
        assertEquals(1, dao.getTests(null, null, 2, 1).size());
        assertEquals(0, dao.getTests(null, null, 3, 1).size());
        dao.deleteTest("a");
        assertEquals(3, dao.getTests(null, null, 0, 5).size());
        dao.deleteTest("A");
        assertEquals(2, dao.getTests(null, null, 0, 5).size());
        
        assertEquals(0, dao.getTests("R3", 0, 0, 5).size());
        assertEquals(0, dao.getTests("X", 2, 0, 5).size());
        
        DBCursor cursor = dao.getTests("R3", 2, 0, 5);
        assertEquals(1, cursor.size());
        DBObject testObj = cursor.next();
        assertEquals(6, testObj.toMap().size());
        assertEquals("C", testObj.get(FIELD_NAME));
        assertEquals(Integer.valueOf(0), testObj.get(FIELD_VERSION));
        assertEquals("D", testObj.get(FIELD_DESCRIPTION));
        assertEquals("R3", testObj.get(FIELD_RELEASE));
        assertEquals(Integer.valueOf(2), testObj.get(FIELD_SCHEMA));
    }
    
    @Test
    public void testGetTest()
    {
        String description = "For Test test property management";
        String name = createTest(description);
        
        DBObject testObj = dao.getTest(name, true);
        assertNotNull(testObj);
        assertNotNull(testObj.get(FIELD_PROPERTIES));
        // Now repeat without retrieving properties
        testObj = dao.getTest(name, false);
        assertNotNull(testObj);
        assertNull(testObj.get(FIELD_PROPERTIES));
        
        // Find the same test by ID
        ObjectId testObjId = (ObjectId) testObj.get(FIELD_ID);
        testObj = dao.getTest(testObjId, true);
        assertNotNull(testObj);
        assertNotNull(testObj.get(FIELD_PROPERTIES));
    }
        
    @Test
    public void testUpdateTest()
    {
        String testA = "T" + UUID.randomUUID().toString().replace("-", "_");
        String testB = "T" + UUID.randomUUID().toString().replace("-", "_");
        String releaseA = UUID.randomUUID().toString();
        String releaseB = UUID.randomUUID().toString();
        Integer schema1 = 1;
        Integer schema2 = 2;
        String description = "For Test Testing";
        
        List<TestProperty> testProperties = TestPropertyFactory.getTestProperties(INHERITANCE, properties);
        dao.writeTestDef(releaseA, schema1, description, testProperties);
        
        try
        {
            dao.createTest(testA, null, releaseA, null);
            fail("Null schema not checked.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        
        try
        {
            dao.createTest(testA, null, null, schema1);
            fail("Null release not checked.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        
        // Create a test
        boolean created = dao.createTest(testA, null, releaseA, schema1);
        assertTrue("Valid test not created", created);
        
        DBObject testObj = dao.getTest(testA, true);
        assertNotNull(testObj);
        assertEquals(Integer.valueOf(0), testObj.get(FIELD_VERSION));
        assertEquals(null, testObj.get(FIELD_DESCRIPTION));
        assertEquals(releaseA, testObj.get(FIELD_RELEASE));
        assertEquals(schema1, testObj.get(FIELD_SCHEMA));
        BasicDBList propsObj = (BasicDBList) testObj.get(FIELD_PROPERTIES);
        assertNotNull(propsObj);
        assertTrue(propsObj instanceof Collection);
        assertEquals("Properties not stored with test", 9, propsObj.size());
        
        try
        {
            dao.updateTest(testA, 0, "Wibble#", null, releaseA, schema1);
            fail("Invalid test name not detected.");
        } catch (IllegalArgumentException e) {}
        
        boolean written = dao.updateTest("No Test", 0, testB, description, releaseB, schema2);
        assertFalse("Should not return true when updating missing tests", written);
        
        written = dao.updateTest(testA, 5, testB, description, releaseA, schema1);
        assertFalse("Version number was not checked", written);
        
        written = dao.updateTest(testA, 0, testB, description, releaseA, schema1);
        assertTrue(written);
        
        // Get and check the test
        testObj = dao.getTest(testB, true);
        
        assertNotNull(testObj);
        
        assertEquals(Integer.valueOf(1), testObj.get(FIELD_VERSION));
        assertEquals(description, testObj.get(FIELD_DESCRIPTION));
        assertEquals(releaseA, testObj.get(FIELD_RELEASE));
        assertEquals(schema1, testObj.get(FIELD_SCHEMA));
        propsObj = (BasicDBList) testObj.get(FIELD_PROPERTIES);
        String propsStr = propsObj.toString();
        assertTrue(propsStr.contains("\"name\" : \"one.dec\" , \"min\" : \"-5.4\""));
    }
    
    @Test
    public void testManageTestProperties()
    {
        String description = "For test property management";
        String testA = createTest(description);
        
        // Get a property that has not been touched
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", null, 0, TestPropertyOrigin.DEFAULTS);
        
        boolean written = dao.setPropertyOverride(testA, null, "one.str", 0, "ONE_STR");
        assertTrue(written); 
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "ONE_STR", 1, TestPropertyOrigin.TEST);
        
        written = dao.setPropertyOverride(testA, null, "one.str", 100, "ONE_BIG_STR");
        assertFalse("Optimistic write check failed", written);
        written = dao.setPropertyOverride(testA, null, "one.str", 1, "ONE_BIG_STR");
        assertTrue("Optimistic write check failed", written);
        written = dao.setPropertyOverride(testA, null, "one.str", 2, "ONE_BIG_STR_AGAIN");
        assertTrue("Optimistic write check failed", written);
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "ONE_BIG_STR_AGAIN", 3, TestPropertyOrigin.TEST);
        
        // Now unset the property
        written = dao.setPropertyOverride(testA, null, "one.str", 2, null);
        assertFalse("Should not have removed a property because the version was incorrect", written);
        written = dao.setPropertyOverride(testA, null, "one.str", 3, null);
        assertTrue("Optimistic check on property removal failed", written);
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", null, 0, TestPropertyOrigin.DEFAULTS);
        
        dao.setPropertyOverride(testA, null, "one.int", 0, "456");
        checkPropertyValue(testA, null, "one.int", "123", "456", 1, TestPropertyOrigin.TEST);

        dao.setPropertyOverride(testA, null, "one.int", 1, null);
        checkPropertyValue(testA, null, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
    }

    @Test
    public void testCreateTestRunNames()
    {
        String[] invalid = new String[] {"#1TEST", "TEST 1", "TEST-1"};
        for (String name : invalid)
        {
            try
            {
                dao.createTestRun("TEST", name, null);
                fail("Test run name should not have been allowed: " + name);
            } catch (IllegalArgumentException e) {}
        }
        String[] valid = new String[] {"a1_01", "01", "TEST_01"};
        for (String name : valid)
        {
            dao.createTestRun("TEST", name, null);
        }
    }
    
    /**
     * Helper method to create a test run
     */
    private String createTestRun(String test, String description)
    {
        String run = "T" + UUID.randomUUID().toString().replace("-", "_");
        
        boolean created = dao.createTestRun(test, run, description);
        assertTrue(created);
        
        return run;
    }
    
    @Test
    public void testCreateTestRunDuplicate()
    {
        String test = createTest(null);
        
        assertTrue(dao.createTestRun(test, "R", null));
        assertFalse(dao.createTestRun(test, "R", null));
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testCreateAndListTestRuns()
    {
        String testA = createTest(null);
        String testB = createTest(null);
        
        DBObject testAObj = dao.getTest(testA, false);
        ObjectId testAID = (ObjectId) testAObj.get(FIELD_ID);
        DBObject testBObj = dao.getTest(testB, false);
        ObjectId testBID = (ObjectId) testBObj.get(FIELD_ID);
        
        DBCursor cursor = dao.getTestRuns(null, 0, 10);
        assertEquals(0, cursor.size());
        cursor.close();
        
        // Create some runs
        String runA1 = createTestRun(testA, "A's description");
        String runA2 = createTestRun(testA, "A's description");
        String runA3 = createTestRun(testA, "A's description");
        String runA4 = createTestRun(testA, "A's description");
        String runA5 = createTestRun(testA, "A's description");
        String runB1 = createTestRun(testB, "B's description");
        String runB2 = createTestRun(testB, "B's description");
        String runB3 = createTestRun(testB, "B's description");
        String runB4 = createTestRun(testB, "B's description");
        String runB5 = createTestRun(testB, "B's description");

        cursor = dao.getTestRuns("C", 0, 10);
        assertEquals(0, cursor.size());
        cursor.close();
        
        cursor = dao.getTestRuns(null, 0, 10);
        assertEquals(10, cursor.size());
        DBObject runObj = cursor.next();
        assertEquals(testAID, runObj.get(FIELD_TEST));
        assertNotNull(runObj.get(FIELD_NAME));
        assertEquals("A's description", runObj.get(FIELD_DESCRIPTION));
        assertEquals(Integer.valueOf(0), runObj.get(FIELD_VERSION));
        assertEquals(TestRunState.NOT_SCHEDULED.toString(), runObj.get(FIELD_STATE));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_SCHEDULED));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_STARTED));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_STOPPED));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(0L), runObj.get(FIELD_DURATION));
        assertEquals(Double.valueOf(0.0D), runObj.get(FIELD_PROGRESS));
        cursor.close();

        cursor = dao.getTestRuns(testB, 0, 5);
        assertEquals(5, cursor.size());
        runObj = cursor.next();
        assertEquals(testBID, runObj.get(FIELD_TEST));
        assertNotNull(runObj.get(FIELD_NAME));
        assertEquals("B's description", runObj.get(FIELD_DESCRIPTION));
        assertEquals(Integer.valueOf(0), runObj.get(FIELD_VERSION));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_SCHEDULED));
        assertEquals(TestRunState.NOT_SCHEDULED.toString(), runObj.get(FIELD_STATE));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_STARTED));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_STOPPED));
        assertEquals(Long.valueOf(-1L), runObj.get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(0L), runObj.get(FIELD_DURATION));
        assertEquals(Double.valueOf(0.0D), runObj.get(FIELD_PROGRESS));
        cursor.close();
        
        // Page through
        for (int i = 0; i < 6; i++)
        {
            cursor = dao.getTestRuns(null, i*2, 2);
            if (i < 5)
            {
                assertEquals("Paging ended early", 2, cursor.size());
            }
            else
            {
                assertEquals("Paging did not end", 0, cursor.size());
            }
            cursor.close();
        }
        
        // Check individual
        DBObject runA1Obj;
        try
        {
            runA1Obj = dao.getTestRun(testA, runA1, true);
        }
        catch (ObjectNotFoundException e)
        {
            fail("Test '" + testA + "' not found!");
        }
        
        // Check filtering by state
        cursor = dao.getTestRuns(testB, 0, 5);
        assertEquals(5, cursor.size());
        cursor.close();
        cursor = dao.getTestRuns(testB, 0, 5, TestRunState.NOT_SCHEDULED);
        assertEquals(5, cursor.size());
        cursor.close();
        cursor = dao.getTestRuns(testB, 0, 5, TestRunState.NOT_SCHEDULED, TestRunState.COMPLETED);
        assertEquals(5, cursor.size());
        cursor.close();
        cursor = dao.getTestRuns(testB, 0, 5, TestRunState.COMPLETED);
        assertEquals(0, cursor.size());
        cursor.close();
        
        // Get all the names
        List<String> testRunNames = dao.getTestRunNames(testB);
        assertEquals(5, testRunNames.size());
        assertTrue(testRunNames.contains(runB1));
        assertTrue(testRunNames.contains(runB2));
        assertTrue(testRunNames.contains(runB3));
        assertTrue(testRunNames.contains(runB4));
        assertTrue(testRunNames.contains(runB5));
    }
    
    
    @SuppressWarnings("unused")
    @Test
    public void testTestRunCRUD()
    {
        String testA = createTest(null);
        
        DBObject testAObj = dao.getTest(testA, false);
        ObjectId testAID = (ObjectId) testAObj.get(FIELD_ID);
        
        DBCursor cursor = dao.getTestRuns(null, 0, 10);
        assertEquals(0, cursor.size());
        cursor.close();
        
        // create a test run
        String runA1 = createTestRun(testA, "A's description");
        try
        {
            assertNull(dao.getTestRun("NO", runA1, false));
            fail("Should not find 'NO'!");
        }
        catch (ObjectNotFoundException e1)
        {
            // expected!
        }
        try
        {
            assertNull(dao.getTestRun(testA, "missing", false));
            fail("Should not find 'missing'test run!");
        }
        catch (ObjectNotFoundException e1)
        {
            // expected
        }
        DBObject runA1Obj = null;
        try
        {
            runA1Obj = dao.getTestRun(testA, runA1, false);
        }
        catch (ObjectNotFoundException e1)
        {
            fail("Should have found test '" + testA + "', run '" + runA1 + "'.");
        }
        
        try
        {
            ObjectId runA1ObjId = (ObjectId) runA1Obj.get(FIELD_ID);
            runA1Obj = dao.getTestRun(runA1ObjId, true);
            assertNotNull("Test run not returned with properties", runA1Obj.get(FIELD_PROPERTIES));
        }
        catch (ObjectNotFoundException e1)
        {
            fail("Test run retrieved by ObjectId was null :" + runA1Obj);
        }
        
        // Rename the test and ensure that the update appears in the test run,
        // which should be ID based for lookups
        String renamedTestName = "RenamedTestA";
        assertTrue("Failed to update test", dao.updateTest(testA, 0, renamedTestName, null, null, null));
        DBObject runA1ObjCheck = null;
        try
        {
            runA1ObjCheck = dao.getTestRun(renamedTestName, runA1, false);
            assertEquals("Expected exactly the same ID for the test run", runA1Obj.get(FIELD_ID), runA1ObjCheck.get(FIELD_ID));
        }
        catch (ObjectNotFoundException e1)
        {
            fail("Should have found renamed test to " + renamedTestName);
        }        
        
        // Attempt an illegal rename of the test run
        try
        {
            dao.updateTestRun(renamedTestName, runA1, 0, "CHANGED NAME", null);
            fail("Illegal test run name not detected.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        
        // Update the description of the test run
        try
        {
            assertFalse("Expected nothing to be updated.", dao.updateTestRun("RenamedA", runA1, 0, null, null));
            assertTrue(dao.updateTestRun(renamedTestName, runA1, 0, "CHANGED_NAME", null));
            runA1ObjCheck = dao.getTestRun(renamedTestName, "CHANGED_NAME", false);
            assertEquals("CHANGED_NAME", runA1ObjCheck.get(FIELD_NAME));
            assertTrue(dao.updateTestRun(renamedTestName, "CHANGED_NAME", 1, null, "CHANGED DESCRIPTION"));
            runA1ObjCheck = dao.getTestRun(renamedTestName, "CHANGED_NAME", false);
            assertEquals("CHANGED DESCRIPTION", runA1ObjCheck.get(FIELD_DESCRIPTION));
        }
        catch (ObjectNotFoundException e)
        {
            fail("Test not found: " + renamedTestName);
        }
        
        assertFalse(dao.deleteTestRun(testA, runA1));
        assertFalse(dao.deleteTestRun(testA, "CHANGED_NAME"));
        assertFalse(dao.deleteTestRun(renamedTestName, runA1));
        assertTrue(dao.deleteTestRun(renamedTestName, "CHANGED_NAME"));
        try
        {
            runA1ObjCheck = dao.getTestRun(renamedTestName, "CHANGED_NAME", false);
            fail("Shouldn't find deleted test " + renamedTestName);
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }
        
        
        // create a test run
        String runA2 = createTestRun(renamedTestName, "A2's description");
        try
        {
            dao.getTestRun("NO", runA2, false);
            dao.getTestRun(renamedTestName, "missing2", false);
            fail("Shouldn't find non-existing tests!");
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }
        
        ObjectId runA2ObjId = null;
        try
        {
            DBObject runA2Obj = dao.getTestRun(renamedTestName, runA2, false);
            runA2ObjId = (ObjectId) runA2Obj.get(FIELD_ID);
            assertNotNull(runA2ObjId);
        }
        catch (ObjectNotFoundException e1)
        {
            fail("Should have found test " + renamedTestName);
        }
            
        try
        {
            // Delete test run by ID
            assertTrue(dao.deleteTestRun(runA2ObjId));
            dao.getTestRun(renamedTestName, runA2, false);
            fail("Shouldn't find deleted test " + renamedTestName);
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }
    }
    
    /**
     * Ensures that a property value is correct.
     * <p/>
     * This checks the property value directly as well as retrieving the full test or test run
     * and checking the property there.
     * 
     * @param test                  the name of the test
     * @param run                   the name of the test run (<null> allowed)
     * @param property              the name of the property
     * @param defaultValue          the default value expected
     * @param value                 the overridden value expected
     * @param version               the current version of the property
     * @param origin                the origin of the property
     */
    private void checkPropertyValue(String test, String run, String property, String defaultValue, String value, int version, TestPropertyOrigin origin)
    {
        // First do the direct check
        DBObject propObj = dao.getProperty(test, run, property);
        assertNotNull("Property not found via direct lookup.", propObj);
        assertEquals("Default value not correct: ", defaultValue, propObj.get(FIELD_DEFAULT));
        assertEquals("Overridden value not correct: ", value, propObj.get(FIELD_VALUE));
        assertEquals("Version not correct: ", Integer.valueOf(version), propObj.get(FIELD_VERSION));
        assertEquals("Origin not correct: ", origin, TestPropertyOrigin.valueOf((String) propObj.get(FIELD_ORIGIN)));
        
        // Get the test or test run
        BasicDBList propObjs = null;
        if (run == null)
        {
            DBObject testObj = dao.getTest(test, true);
            propObjs = (BasicDBList) testObj.get(FIELD_PROPERTIES);
        }
        else
        {
            DBObject testRunObj;
            try
            {
                testRunObj = dao.getTestRun(test, run, true);
                propObjs = (BasicDBList) testRunObj.get(FIELD_PROPERTIES);
            }
            catch (ObjectNotFoundException e)
            {
                fail("Should find test '" + test + "' run '" + run + "'.");
            }
        }
        assertNotNull(propObjs);
        
        // Iterate and find
        for (Object obj : propObjs)
        {
            propObj = (DBObject) obj;
            String name = (String) propObj.get(FIELD_NAME);
            if (!property.equals(name))
            {
                continue;
            }
            // Check
            assertEquals("Default value not correct: ", defaultValue, propObj.get(FIELD_DEFAULT));
            assertEquals("Overridden value not correct: ", value, propObj.get(FIELD_VALUE));
            assertEquals("Version not correct: ", Integer.valueOf(version), propObj.get(FIELD_VERSION));
            // Success
            return;
        }
        fail("Did not find property " + property + " in " + test + "." + run);
    }
    
    @Test
    public void testManageTestRunProperties()
    {
        String testA = createTest(null);
        String runA1 = createTestRun(testA, null);
        
        // Check initial values
        checkPropertyValue(testA, null, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
        checkPropertyValue(testA, runA1, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
        
        // Set the value at the test level
        dao.setPropertyOverride(testA, null, "one.int", 0, "456");
        checkPropertyValue(testA, null, "one.int", "123", "456", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.int", "456", null, 0, TestPropertyOrigin.TEST);
        
        // Reset at the test level
        dao.setPropertyOverride(testA, null, "one.int", 1, null);
        checkPropertyValue(testA, null, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
        checkPropertyValue(testA, runA1, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
        
        // Set the value at the run level
        dao.setPropertyOverride(testA, runA1, "one.int", 0, "456");
        checkPropertyValue(testA, null, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
        checkPropertyValue(testA, runA1, "one.int", "123", "456", 1, TestPropertyOrigin.RUN);
        
        // Set the value at the test level
        dao.setPropertyOverride(testA, null, "one.int", 0, "789");
        checkPropertyValue(testA, null, "one.int", "123", "789", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.int", "789", "456", 1, TestPropertyOrigin.RUN);
        
        // Set the value at the run level
        dao.setPropertyOverride(testA, runA1, "one.int", 1, "0000");
        checkPropertyValue(testA, null, "one.int", "123", "789", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.int", "789", "0000", 2, TestPropertyOrigin.RUN);
        
        // Set the value at the test level
        dao.setPropertyOverride(testA, null, "one.int", 1, "1111");
        checkPropertyValue(testA, null, "one.int", "123", "1111", 2, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.int", "1111", "0000", 2, TestPropertyOrigin.RUN);
        
        // Ensure versioning is working
        assertFalse(dao.setPropertyOverride(testA, null, "one.int", 0, "0000"));
        assertFalse(dao.setPropertyOverride(testA, null, "one.int", 1, "0000"));
        assertFalse(dao.setPropertyOverride(testA, null, "one.int", 3, "0000"));
        assertFalse(dao.setPropertyOverride(testA, runA1, "one.int", 0, "0000"));
        assertFalse(dao.setPropertyOverride(testA, runA1, "one.int", 1, "0000"));
        assertFalse(dao.setPropertyOverride(testA, runA1, "one.int", 3, "0000"));
        
        // Reset at the run level
        dao.setPropertyOverride(testA, runA1, "one.int", 2, null);
        checkPropertyValue(testA, null, "one.int", "123", "1111", 2, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.int", "1111", null, 0, TestPropertyOrigin.TEST);
        
        // Reset at the test level
        dao.setPropertyOverride(testA, null, "one.int", 2, null);
        checkPropertyValue(testA, null, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
        checkPropertyValue(testA, runA1, "one.int", "123", null, 0, TestPropertyOrigin.DEFAULTS);
    }
    
    @Test
    public void testUpdateTestRunState() throws ObjectNotFoundException
    {
        String test = createTest(null);
        String run = createTestRun(test, null);
        DBObject runObj = dao.getTestRun(test, run, false);
        ObjectId runId = (ObjectId) runObj.get(FIELD_ID);
        
        long now = System.currentTimeMillis();

        // We are not scheduled so we can set things to be exactly as they were
        assertFalse(dao.updateTestRunState(runId, 0, null, null, null, null, null, null, null, null, null));
        assertEquals(TestRunState.NOT_SCHEDULED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));

        // Schedule
        assertTrue(dao.updateTestRunState(runId, 0, TestRunState.SCHEDULED, Long.valueOf(now-10L), null, null, null, null, null, null, null));
        assertEquals(TestRunState.SCHEDULED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(now-10L), dao.getTestRun(test, run, false).get(FIELD_SCHEDULED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_STARTED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_STOPPED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(0L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        
        // Re-schedule
        assertTrue(dao.updateTestRunState(runId, 1, TestRunState.SCHEDULED, Long.valueOf(now), null, null, null, null, null, null, null));
        assertEquals(TestRunState.SCHEDULED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(now), dao.getTestRun(test, run, false).get(FIELD_SCHEDULED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_STARTED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_STOPPED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(0L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        
        // Start
        assertTrue(dao.updateTestRunState(runId, 2, TestRunState.STARTED, null, Long.valueOf(now+10L), null, null, null, null, null, null));
        assertEquals(TestRunState.STARTED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(now+10L), dao.getTestRun(test, run, false).get(FIELD_STARTED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_STOPPED));
        assertEquals(Long.valueOf(-1L), dao.getTestRun(test, run, false).get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(0L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        
        // Progress
        assertTrue(dao.updateTestRunState(runId, 3, null, null, null, null, null, Long.valueOf(10L), Double.valueOf(0.5D), null, null));
        assertEquals(TestRunState.STARTED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(10L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        assertEquals(Double.valueOf(0.5D), dao.getTestRun(test, run, false).get(FIELD_PROGRESS));
        
        // Progress
        assertTrue(dao.updateTestRunState(runId, 4, null, null, null, null, null, Long.valueOf(20L), Double.valueOf(1.0D), null, null));
        assertEquals(TestRunState.STARTED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(20L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        assertEquals(Double.valueOf(1.0D), dao.getTestRun(test, run, false).get(FIELD_PROGRESS));
        
        // Complete
        assertTrue(dao.updateTestRunState(runId, 5, TestRunState.COMPLETED, null, null, null, Long.valueOf(now+1000L), Long.valueOf(50L), null, null, null));
        assertEquals(TestRunState.COMPLETED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(now+1000L), dao.getTestRun(test, run, false).get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(50L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        assertEquals(Double.valueOf(1.0D), dao.getTestRun(test, run, false).get(FIELD_PROGRESS));
        
        // Stop
        // This is actually not valid business logic but it is not forbidden by the DAO
        assertTrue(dao.updateTestRunState(runId, 6, TestRunState.STOPPED, null, null, Long.valueOf(now+1000L), null, Long.valueOf(75L), null, null, null));
        assertEquals(TestRunState.STOPPED.toString(), dao.getTestRun(test, run, false).get(FIELD_STATE));
        assertEquals(Long.valueOf(now), dao.getTestRun(test, run, false).get(FIELD_SCHEDULED));
        assertEquals(Long.valueOf(now+10L), dao.getTestRun(test, run, false).get(FIELD_STARTED));
        assertEquals(Long.valueOf(now+1000L), dao.getTestRun(test, run, false).get(FIELD_STOPPED));
        assertEquals(Long.valueOf(now+1000L), dao.getTestRun(test, run, false).get(FIELD_COMPLETED));
        assertEquals(Long.valueOf(75L), dao.getTestRun(test, run, false).get(FIELD_DURATION));
        assertEquals(Long.valueOf(0L), dao.getTestRun(test, run, false).get(FIELD_RESULTS_SUCCESS));
        assertEquals(Long.valueOf(0L), dao.getTestRun(test, run, false).get(FIELD_RESULTS_FAIL));
        
        // Results counts
        assertTrue(dao.updateTestRunState(runId, 7, null, null, null, null, null, null, null, 123L, 77L));
        assertEquals(Long.valueOf(123L), dao.getTestRun(test, run, false).get(FIELD_RESULTS_SUCCESS));
        assertEquals(Long.valueOf(77L), dao.getTestRun(test, run, false).get(FIELD_RESULTS_FAIL));
        
        // Must update result counts together
        try
        {
            dao.updateTestRunState(runId, 7, null, null, null, null, null, null, null, 123L, null);
            Assert.fail("Must update result counts together.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        
        // Make sure that the test runs cursor retrieve the correct data as well
        DBCursor cursor = dao.getTestRuns(test, 0, 1, TestRunState.STOPPED);
        Assert.assertEquals("Expected exactly 1 result.",  1, cursor.count());
        runObj = cursor.next();
        cursor.close();
        assertEquals("Incorrect number of fields", 16, runObj.keySet().size());
        assertNotNull(runObj.get(FIELD_STATE));
        assertNotNull(runObj.get(FIELD_SCHEDULED));
        assertNotNull(runObj.get(FIELD_STARTED));
        assertNotNull(runObj.get(FIELD_STOPPED));
        assertNotNull(runObj.get(FIELD_COMPLETED));
        assertNotNull(runObj.get(FIELD_DURATION));
        assertNotNull(runObj.get(FIELD_PROGRESS));
        assertNotNull(runObj.get(FIELD_RESULTS_SUCCESS));
        assertNotNull(runObj.get(FIELD_RESULTS_FAIL));
        assertNotNull(runObj.get(FIELD_RESULTS_TOTAL));
        assertNotNull(runObj.get(FIELD_SUCCESS_RATE));
    }
    
    @Test
    public void testAddAndRemoveTestRunDrivers() throws ObjectNotFoundException
    {
        String test = createTest(null);
        String run = createTestRun(test, null);
        DBObject runObj = dao.getTestRun(test, run, false);
        ObjectId runId = (ObjectId) runObj.get(FIELD_ID);
        
        assertNotNull("Drivers array not present in run object.", runObj.get(FIELD_DRIVERS));
        BasicDBList drivers = (BasicDBList) runObj.get(FIELD_DRIVERS);
        assertEquals("Expected driver entries incorrect", 0, drivers.size());
        
        dao.addTestRunDriver(runId, "driver0");
        dao.addTestRunDriver(runId, "driver1");
        dao.removeTestRunDriver(runId, "driver1");
        dao.removeTestRunDriver(runId, "NOT_THERE");

        // Check
        runObj = dao.getTestRun(test, run, false);
        assertNotNull("Drivers array not present in run object.", runObj.get(FIELD_DRIVERS));
        drivers = (BasicDBList) runObj.get(FIELD_DRIVERS);
        assertEquals("Expected driver entries incorrect", 1, drivers.size());
        String driver0 = (String) drivers.get(0);
        assertEquals("driver0", driver0);
    }
    
    @Test
    public void testCopyTest()
    {
        String testA = createTest(null);
        @SuppressWarnings("unused")
        String runA1 = createTestRun(testA, null);
        
        // Set the value at the test level
        dao.setPropertyOverride(testA, null, "one.str", 0, "SET_AT_TEST");

        // Check properties
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "SET_AT_TEST", 1, TestPropertyOrigin.TEST);
        
        // Get the runs
        DBCursor cursor = dao.getTestRuns(testA, 0, 5);
        assertEquals(1, cursor.size());
        cursor.close();
        
        // Copy the test
        assertFalse("Copied test version should not have matched", dao.copyTest("testA_CP1", null, null, testA, 1));
        try
        {
            dao.copyTest("testA(CP1)", null, null, testA, 0);
            fail("Copy test name not validated");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        assertTrue(dao.copyTest("testA_CP1", null, null, testA, 0));

        // Should be copied without runs
        cursor = dao.getTestRuns("testA_CP1", 0, 5);
        assertEquals(0, cursor.size());
        cursor.close();
        
        // Check properties
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "SET_AT_TEST", 1, TestPropertyOrigin.TEST);
        checkPropertyValue("testA_CP1", null, "one.str", "ONE DEFAULT", "SET_AT_TEST", 1, TestPropertyOrigin.TEST);
        
        // Create a new test and copy-create from one to the other
        String testB = createTest(null);
        DBObject testBObj = dao.getTest(testB, false);
        String testBRelease = (String) testBObj.get(FIELD_RELEASE);
        Integer testBSchema = (Integer) testBObj.get(FIELD_SCHEMA);
        assertTrue(dao.copyTest("testB_CP1", testBRelease, testBSchema, testA, 0));
        assertEquals(testBRelease, dao.getTest("testB_CP1", false).get(FIELD_RELEASE));
        assertEquals(testBSchema, dao.getTest("testB_CP1", false).get(FIELD_SCHEMA));
        
        // Check properties
        checkPropertyValue("testB_CP1", null, "one.str", "ONE DEFAULT", "SET_AT_TEST", 1, TestPropertyOrigin.TEST);
        
    }
    
    @Test
    public void testCopyTestRun()
    {
        String testA = createTest(null);
        String runA1 = createTestRun(testA, null);
        
        // Set the value at the test level
        assertTrue(dao.setPropertyOverride(testA, null, "one.str", 0, "SET_AT_TEST"));
        // Set the value at the run level
        assertTrue(dao.setPropertyOverride(testA, runA1, "one.str", 0, "SET_AT_RUN"));

        // Check properties
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "SET_AT_TEST", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.str", "SET_AT_TEST", "SET_AT_RUN", 1, TestPropertyOrigin.RUN);
        
        // Copy the test run
        assertFalse("Copied run version should not have matched", dao.copyTestRun(testA, "runA1_CP1", runA1, 1));
        try
        {
            dao.copyTestRun(testA, "runA1(CP1)", runA1, 0);
            fail("Copy run name not validated");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        assertTrue(dao.copyTestRun(testA, "runA1_CP1", runA1, 0));

        // Check properties
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "SET_AT_TEST", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.str", "SET_AT_TEST", "SET_AT_RUN", 1, TestPropertyOrigin.RUN);
        checkPropertyValue(testA, "runA1_CP1", "one.str", "SET_AT_TEST", "SET_AT_RUN", 1, TestPropertyOrigin.RUN);

        // Set the value at the test level again
        assertTrue(dao.setPropertyOverride(testA, null, "one.str", 1, "SET_AT_TEST_2"));

        // Check properties
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "SET_AT_TEST_2", 2, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.str", "SET_AT_TEST_2", "SET_AT_RUN", 1, TestPropertyOrigin.RUN);
        checkPropertyValue(testA, "runA1_CP1", "one.str", "SET_AT_TEST_2", "SET_AT_RUN", 1, TestPropertyOrigin.RUN);
        
        // Now fix the properties
        try
        {
            ObjectId testAObjId = (ObjectId) dao.getTest(testA, false).get(FIELD_ID);
            ObjectId runA1ObjId = (ObjectId) dao.getTestRun(testA, runA1, false).get(FIELD_ID);
            dao.lockProperties(testAObjId, runA1ObjId);
        }
        catch (ObjectNotFoundException e)
        {
            fail("Test/run not found!");
        }
        assertFalse(dao.setPropertyOverride(testA, runA1, "one.str", 2, "CAN'T MODIFY FIXED PROPERTIES"));
        // and copy again
        assertTrue(dao.copyTestRun(testA, "runA1_CP2", runA1, 0));

        // Set the value at the test level again
        assertTrue(dao.setPropertyOverride(testA, null, "one.str", 2, "SET_AT_TEST_3"));
        assertTrue(dao.setPropertyOverride(testA, null, "two.str", 0, "SET_AT_TEST_3"));

        // Check properties
        // The 'fix' process must not confuse the copy, which should only copy values that originated in the RUN
        checkPropertyValue(testA, null, "one.str", "ONE DEFAULT", "SET_AT_TEST_3", 3, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, "runA1_CP2", "one.str", "SET_AT_TEST_3", "SET_AT_RUN", 1, TestPropertyOrigin.RUN);
        checkPropertyValue(testA, null, "two.str", "${one.str}", "SET_AT_TEST_3", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, "runA1_CP2", "two.str", "SET_AT_TEST_3", null, 0, TestPropertyOrigin.TEST);
    }
    
    @Test
    public void testDeleteCleanup()
    {
        String testA = createTest(null);
        String runA1 = createTestRun(testA, null);
        String runA2 = createTestRun(testA, null);
        
        // Set the value at the test level
        dao.setPropertyOverride(testA, null, "one.int", 0, "456");
        checkPropertyValue(testA, null, "one.int", "123", "456", 1, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA1, "one.int", "456", null, 0, TestPropertyOrigin.TEST);
        checkPropertyValue(testA, runA2, "one.int", "456", null, 0, TestPropertyOrigin.TEST);
        
        // Set the value at the run level
        dao.setPropertyOverride(testA, runA1, "one.int", 0, "111");
        checkPropertyValue(testA, runA1, "one.int", "456", "111", 1, TestPropertyOrigin.RUN);
        dao.setPropertyOverride(testA, runA2, "one.int", 0, "222");
        checkPropertyValue(testA, runA2, "one.int", "456", "222", 1, TestPropertyOrigin.RUN);
        
        // Delete a test run
        assertTrue(dao.deleteTestRun(testA, runA2));
        // Delete the test
        assertTrue(dao.deleteTest(testA));
        // All tests, test runs and properties should be deleted
        DBCursor cursor = dao.getTestRuns(null, 0, 5);
        assertEquals("Expected all test runs to have been cleaned up. ", 0, cursor.size());
        cursor.close();
        cursor = dao.getTests(null, null, 0, 5);
        assertEquals("Expected all tests to have been cleaned up. ", 0, cursor.size());
        cursor.close();
        
        // Get at the properties
        assertEquals("Properties were not cleaned up.", 0, db.getCollection(MongoTestDAO.COLLECTION_TEST_PROPS).count());
    }
    
    /**
     * Scenario:
     * 
     * Create test 
     * Get masked property names 
     * Must contain ONE value "proc.pwd"
     * Delete test
     */
    @Test
    public void checkMask()
    {
        // TODO
    }
    
    
    /**
     * Scenario: 
     * 
     * Create test 
     * Create run 
     * Export properties to DBObject 
     * Check if all passwords are encrypted as expected 
     * Replace a value in the exported DBObject 
     * Create a new test run 
     * Re-import the DBObject 
     * Export the properties from the first test
     * Export the properties from the second test 
     * Compare - only the replaced value must differ 
     * Update password with an non-encrypted value 
     * Re-import DBObject - expected FAIL
     * Update CIPHER field to ensure NONE
     * Re-import DBObject 
     * check if password was updated
     * Delete first test run 
     * Try to re-import in the first run - object not found: expected ERROR
     * Delete test 
     * Try to re-import in the second test run - object not found: expected ERROR  
     */
    @Test
    public void testExportImport() throws ObjectNotFoundException, CipherException
    {
        final String newUserName = "NewThreeValue";
        final String newPasswordValue = "StrongSecretPassword";
        final String propToUpdateName = "three";
        final String propPasswordName = "one.str";
        
        // Create test and run
        String testName = createTest("Export-Import-Test");
        String runNameExport = createTestRun(testName, "Export-Run");
        
        // export 
        DBObject exportObj = this.dao.exportTestRun(testName, runNameExport);
        
        // check all PWD to be encrypted
        int count = 0;
        Set<String>maskedProps = this.dao.getMaskedProperyNames(testName);
        for (final Object obj : (BasicDBList)exportObj.get(FIELD_PROPERTIES))
        {
            final DBObject dbProp = (DBObject)obj;
            if (this.dao.isMaskedProperty(dbProp))
            {
                //must contain FIELD_CIPHER!
                Object cipher = dbProp.get(FIELD_CIPHER);
                assertNotNull("Missing field " + FIELD_CIPHER, cipher);
                assertEquals("Cipher version one expected!", CipherVersion.V1.toString(), cipher.toString());
                count++;
            }
        }
        assertEquals(count, maskedProps.size());
        
        // replace a value 
        for (Object objIt : (BasicDBList)exportObj.get(FIELD_PROPERTIES))
        {
            DBObject dbProp = (DBObject)objIt;
            String propName = (String)dbProp.get(FIELD_NAME);
            if (propName.equals(propToUpdateName))
            {
                dbProp.put(FIELD_VALUE, newUserName);
                break;
            }
        }
        
        // create new run 
        String runNameImport = createTestRun(testName, "Import-Run");
        
        // import 
        DBObject importResultDBObject = this.dao.importTestRun(testName, runNameImport, exportObj);
        assertEquals(ImportResult.OK, getImportResult( importResultDBObject));
        
        // get props exported and imported 
        Map<String, DBObject>mapRunExport = this.dao.getTestRunPropertiesMap(null, null, testName, runNameExport);
        Map<String, DBObject>mapRunImport = this.dao.getTestRunPropertiesMap(null, null, testName, runNameImport);
        assertEquals(mapRunExport.size(), mapRunImport.size());
        
        // the only difference allowed is the updated value!
        for (final String propName : mapRunExport.keySet())
        {
            DBObject propExport = mapRunExport.get(propName);
            DBObject propImport = mapRunImport.get(propName);
            if (propName.equals(propToUpdateName))
            {
                assertFalse(this.dao.getPropValueAsString(propExport).equals(this.dao.getPropValueAsString(propImport)));
            }
            else
            {
                assertEquals(this.dao.getPropValueAsString(propExport), this.dao.getPropValueAsString(propImport));
            }
        }
        
        // update password 
        DBObject propPwd = null;
        for(final Object propItObj : (BasicDBList)exportObj.get(FIELD_PROPERTIES))
        {
            DBObject prop = (DBObject)propItObj;
            String propName = (String)prop.get(FIELD_NAME);
            if  (propPasswordName.equals(propName))
            {
                propPwd = prop;
                break;
            }
        }
        assertNotNull(propPwd);
        propPwd.put(FIELD_VALUE, newPasswordValue);
        importResultDBObject = this.dao.importTestRun(testName, runNameImport, exportObj);
        assertEquals(ImportResult.ERROR, getImportResult( importResultDBObject));
        
        // update field that PWD is NOT encrypted and re-import
        propPwd.put(FIELD_CIPHER, CipherVersion.NONE.toString());
        importResultDBObject = this.dao.importTestRun(testName, runNameImport, exportObj);
        assertEquals(ImportResult.OK, getImportResult( importResultDBObject));
        
        // check if password was updated
        mapRunImport = this.dao.getTestRunPropertiesMap(null, null, testName, runNameImport);
        propPwd = mapRunImport.get(propPasswordName);
        String pwdValueNow = this.dao.getPropValueAsString(propPwd);
        assertEquals("Property '" + propPasswordName + "' expected new value!", newPasswordValue, pwdValueNow);
        
        // Delete first test run 
        this.dao.deleteTestRun(testName, runNameExport);
        importResultDBObject = this.dao.importTestRun(testName, runNameExport, exportObj);
        assertEquals(ImportResult.ERROR, getImportResult( importResultDBObject));
        
        // Delete test itself
        this.dao.deleteTest(testName);
        importResultDBObject = this.dao.importTestRun(testName, runNameImport, exportObj);
        assertEquals(ImportResult.ERROR, getImportResult( importResultDBObject));
    }
    
    /**
     * Extract result from DBObject
     *  
     * @param resultObj (DBObject)
     * 
     * @return ImportResult
     */
    private ImportResult getImportResult(DBObject resultObj)
    {
        String result = (String)resultObj.get(FIELD_RESULT);
        return ImportResult.valueOf(result);
    }
}
