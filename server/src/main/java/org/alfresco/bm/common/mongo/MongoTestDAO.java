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
package org.alfresco.bm.common.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoException;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.alfresco.bm.common.ImportResult;
import org.alfresco.bm.common.TestConstants;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.common.spring.LifecycleListener;
import org.alfresco.bm.common.util.ArgumentCheck;
import org.alfresco.bm.common.util.cipher.AESCipher;
import org.alfresco.bm.common.util.cipher.CipherException;
import org.alfresco.bm.common.util.cipher.CipherVersion;
import org.alfresco.bm.common.util.exception.ObjectNotFoundException;
import org.alfresco.bm.driver.test.prop.TestProperty;
import org.alfresco.bm.driver.test.prop.TestPropertyOrigin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.alfresco.bm.common.TestConstants.FIELD_CAPABILITIES;
import static org.alfresco.bm.common.TestConstants.FIELD_CIPHER;
import static org.alfresco.bm.common.TestConstants.FIELD_COMPLETED;
import static org.alfresco.bm.common.TestConstants.FIELD_CONTEXT_PATH;
import static org.alfresco.bm.common.TestConstants.FIELD_DEFAULT;
import static org.alfresco.bm.common.TestConstants.FIELD_DESCRIPTION;
import static org.alfresco.bm.common.TestConstants.FIELD_DRIVERS;
import static org.alfresco.bm.common.TestConstants.FIELD_DURATION;
import static org.alfresco.bm.common.TestConstants.FIELD_EXPIRES;
import static org.alfresco.bm.common.TestConstants.FIELD_HOSTNAME;
import static org.alfresco.bm.common.TestConstants.FIELD_ID;
import static org.alfresco.bm.common.TestConstants.FIELD_IP_ADDRESS;
import static org.alfresco.bm.common.TestConstants.FIELD_MASK;
import static org.alfresco.bm.common.TestConstants.FIELD_MESSAGE;
import static org.alfresco.bm.common.TestConstants.FIELD_NAME;
import static org.alfresco.bm.common.TestConstants.FIELD_ORIGIN;
import static org.alfresco.bm.common.TestConstants.FIELD_PING;
import static org.alfresco.bm.common.TestConstants.FIELD_PROGRESS;
import static org.alfresco.bm.common.TestConstants.FIELD_PROPERTIES;
import static org.alfresco.bm.common.TestConstants.FIELD_RELEASE;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULT;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_FAIL;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_SUCCESS;
import static org.alfresco.bm.common.TestConstants.FIELD_RESULTS_TOTAL;
import static org.alfresco.bm.common.TestConstants.FIELD_RUN;
import static org.alfresco.bm.common.TestConstants.FIELD_SCHEDULED;
import static org.alfresco.bm.common.TestConstants.FIELD_SCHEMA;
import static org.alfresco.bm.common.TestConstants.FIELD_STARTED;
import static org.alfresco.bm.common.TestConstants.FIELD_STATE;
import static org.alfresco.bm.common.TestConstants.FIELD_STOPPED;
import static org.alfresco.bm.common.TestConstants.FIELD_SUCCESS_RATE;
import static org.alfresco.bm.common.TestConstants.FIELD_SYSTEM;
import static org.alfresco.bm.common.TestConstants.FIELD_TEST;
import static org.alfresco.bm.common.TestConstants.FIELD_TIME;
import static org.alfresco.bm.common.TestConstants.FIELD_VALUE;
import static org.alfresco.bm.common.TestConstants.FIELD_VERSION;
import static org.alfresco.bm.common.TestConstants.PROP_NAME_REGEX;
import static org.alfresco.bm.common.TestConstants.RUN_NAME_REGEX;
import static org.alfresco.bm.common.TestConstants.TEST_NAME_REGEX;
import static org.alfresco.bm.common.TestConstants.VERSION_ZERO;
/**
 * MongoDB persistence of test metadata
 * 
 * @author Derek Hulley
 * @author Frank Becker
 * 
 * @since 2.0
 */
public class MongoTestDAO implements LifecycleListener
{
    public static final String COLLECTION_TEST_DEFS = "test.defs";
    public static final String COLLECTION_TEST_DRIVERS = "test.drivers";
    public static final String COLLECTION_TESTS = "tests";
    public static final String COLLECTION_TEST_PROPS = "test.props";
    public static final String COLLECTION_TEST_RUNS = "test.runs";

    private static Log logger = LogFactory.getLog(MongoTestDAO.class);

    private final Map<String, TestDefEntry> testDefCache;
    private final ReentrantReadWriteLock testDefCacheLock;

    private final DB db;
    private final DBCollection testDrivers;
    private final DBCollection testDefs;
    private final DBCollection tests;
    private final DBCollection testRuns;
    private final DBCollection testProps;

    /**
     * Construct the DAO using the Mongo DB directly
     */
    public MongoTestDAO(DB db)
    {
        ArgumentCheck.checkMandatoryObject(db, "db");

        this.testDefCache = new HashMap<String, TestDefEntry>(17);
        this.testDefCacheLock = new ReentrantReadWriteLock();

        this.db = db;
        this.testDrivers = db.getCollection(COLLECTION_TEST_DRIVERS);
        this.testDefs = db.getCollection(COLLECTION_TEST_DEFS);
        this.tests = db.getCollection(COLLECTION_TESTS);
        this.testRuns = db.getCollection(COLLECTION_TEST_RUNS);
        this.testProps = db.getCollection(COLLECTION_TEST_PROPS);
    }

    /**
     * @return the database being used
     */
    public DB getDb()
    {
        return db;
    }

    /**
     * Initialize indexes
     */
    @Override
    public void start()
    {
        // Clean up old data and indexes
        String[] indexesToDrop = new String[] {
                "TESTS_NAME_RELEASE",
                "TEST_DRIVERS_UNIQUE_RELEASE_SCHEMA_IPADDRESS",
                "TEST_RUNS_TEST",
                "TEST_RUNS_TEST_NAME_SCHEDULED",
                "TEST_PROPS_TEST_NAME",
                "TEST_DEFS_UNIQUE_RELEASE",
                "TEST_DEFS_RELEASE_SCHEMA",
                "TEST_DRIVERS_IDX_NAME_EXPIRES" };
        for (String indexToDrop : indexesToDrop)
        {
            try
            {
                testDrivers.dropIndex(indexToDrop);
            }
            catch (MongoException e)
            {
                // ignore
            }
            try
            {
                testDefs.dropIndex(indexToDrop);
            }
            catch (MongoException e)
            {
                // ignore
            }
            try
            {
                tests.dropIndex(indexToDrop);
            }
            catch (MongoException e)
            {
                // ignore
            }
            try
            {
                testRuns.dropIndex(indexToDrop);
            }
            catch (MongoException e)
            {
                // ignore
            }
            try
            {
                testProps.dropIndex(indexToDrop);
            }
            catch (MongoException e)
            {
                // ignore
            }
        }

        // @since 2.0
        DBObject idx_TEST_DRIVERS_RELEASE_SCHEMA_IPADDRESS = BasicDBObjectBuilder
                .start( FIELD_RELEASE, 1)
                .add(FIELD_SCHEMA, 1)
                .add(FIELD_IP_ADDRESS, 1)
                .get();
        DBObject opts_TEST_DRIVERS_RELEASE_SCHEMA_IPADDRESS = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_DRIVERS_RELEASE_SCHEMA_IPADDRESS")
                .add("unique", Boolean.FALSE)
                .get();
        testDrivers.createIndex(idx_TEST_DRIVERS_RELEASE_SCHEMA_IPADDRESS, opts_TEST_DRIVERS_RELEASE_SCHEMA_IPADDRESS);

        // @since 2.0
        DBObject idx_TEST_DRIVERS_IDX_NAME_EXPIRES_SCHEMA_RELEASE = BasicDBObjectBuilder
                .start()
                .add(FIELD_PING + "." + FIELD_EXPIRES, -1)
                .add(FIELD_SCHEMA, 1)
                .add(FIELD_RELEASE, 1)
                .get();
        DBObject opts_TEST_DRIVERS_IDX_NAME_EXPIRES_SCHEMA_RELEASE = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_DRIVERS_IDX_NAME_EXPIRES_SCHEMA_RELEASE")
                .add("unique", Boolean.FALSE)
                .get();
        testDrivers.createIndex(idx_TEST_DRIVERS_IDX_NAME_EXPIRES_SCHEMA_RELEASE,
                opts_TEST_DRIVERS_IDX_NAME_EXPIRES_SCHEMA_RELEASE);

        // @since 2.0
        DBObject idx_TEST_DEFS_UNIQUE_RELEASE_SCHEMA = BasicDBObjectBuilder
                .start(FIELD_RELEASE, 1)
                .add(FIELD_SCHEMA, 1)
                .get();
        DBObject opts_TEST_DEFS_UNIQUE_RELEASE_SCHEMA = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_DEFS_UNIQUE_RELEASE_SCHEMA")
                .add("unique", Boolean.TRUE)
                .get();
        testDefs.createIndex(idx_TEST_DEFS_UNIQUE_RELEASE_SCHEMA, opts_TEST_DEFS_UNIQUE_RELEASE_SCHEMA);

        // @since 2.0
        DBObject idx_TESTS_UNIQUE_NAME = BasicDBObjectBuilder
                .start(FIELD_NAME, 1)
                .get();
        DBObject opts_TESTS_UNIQUE_NAME = BasicDBObjectBuilder
                .start()
                .add("name", "TESTS_UNIQUE_NAME")
                .add("unique", Boolean.TRUE)
                .get();
        tests.createIndex(idx_TESTS_UNIQUE_NAME, opts_TESTS_UNIQUE_NAME);

        // @since 2.0
        DBObject idx_TESTS_NAME_RELEASE_SCHEMA = BasicDBObjectBuilder
                .start(FIELD_NAME, 1)
                .add(FIELD_RELEASE, 1)
                .add(FIELD_SCHEMA, 1)
                .get();
        DBObject opts_TESTS_NAME_RELEASE_SCHEMA = BasicDBObjectBuilder
                .start()
                .add("name", "TESTS_NAME_RELEASE_SCHEMA")
                .add("unique", Boolean.FALSE)
                .get();
        tests.createIndex(idx_TESTS_NAME_RELEASE_SCHEMA, opts_TESTS_NAME_RELEASE_SCHEMA);

        // @since 2.0
        DBObject idx_TEST_RUNS_NAME = BasicDBObjectBuilder
                .start(FIELD_NAME, 1)
                .get();
        DBObject opts_TEST_RUNS_NAME = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_RUNS_NAME")
                .add("unique", Boolean.FALSE)
                .get();
        testRuns.createIndex(idx_TEST_RUNS_NAME, opts_TEST_RUNS_NAME);

        // @since 2.0
        DBObject idx_TEST_RUNS_UNIQUE_TEST_NAME = BasicDBObjectBuilder
                .start(FIELD_TEST, 1)
                .add(FIELD_NAME, 1)
                .get();
        DBObject opts_TEST_RUNS_UNIQUE_TEST_NAME = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_RUNS_UNIQUE_TEST_NAME")
                .add("unique", Boolean.TRUE)
                .get();
        testRuns.createIndex(idx_TEST_RUNS_UNIQUE_TEST_NAME, opts_TEST_RUNS_UNIQUE_TEST_NAME);

        // @since 2.0
        DBObject idx_TEST_RUNS_TEST_STATE_SCHEDULED = BasicDBObjectBuilder
                .start(FIELD_TEST, 1)
                .add(FIELD_STATE, 1)
                .add(FIELD_SCHEDULED, 1)
                .get();
        DBObject opts_TEST_RUNS_TEST_STATE_SCHEDULED = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_RUNS_TEST_STATE_SCHEDULED")
                .add("unique", Boolean.FALSE)
                .get();
        testRuns.createIndex(idx_TEST_RUNS_TEST_STATE_SCHEDULED, opts_TEST_RUNS_TEST_STATE_SCHEDULED);

        // @since 2.0
        DBObject idx_TEST_PROPS_UNIQUE_TEST_NAME = BasicDBObjectBuilder
                .start(FIELD_TEST, 1)
                .add(FIELD_RUN, 1)
                .add(FIELD_NAME, 1)
                .get();
        DBObject opts_TEST_PROPS_UNIQUE_TEST_NAME = BasicDBObjectBuilder
                .start()
                .add("name", "TEST_PROPS_UNIQUE_TEST_NAME")
                .add("unique", Boolean.TRUE)
                .get();
        testProps.createIndex(idx_TEST_PROPS_UNIQUE_TEST_NAME, opts_TEST_PROPS_UNIQUE_TEST_NAME);
    }

    @Override
    public void stop()
    {
    }

    /**
     * Register a test driver
     * 
     * @param release
     *        the software release version
     * @param schema
     *        the schema number
     * @param ipAddress
     *        the IP address of the machine the application is running on
     * @param contextPath
     *        the application context path (or similar) for information
     * @param capabilities
     *        the features supported by the driver
     * @return a unique registration key
     */
    public String registerDriver(
            String release,
            Integer schema,
            String ipAddress,
            String hostname,
            String contextPath,
            Set<String> capabilities)
    {
        DBObject insertObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_RELEASE, release)
                .add(FIELD_SCHEMA, schema)
                .add(FIELD_IP_ADDRESS, ipAddress)
                .add(FIELD_HOSTNAME, hostname)
                .add(FIELD_CONTEXT_PATH, contextPath)
                .add(FIELD_CAPABILITIES,
                        BasicDBObjectBuilder.start()
                                .add(FIELD_SYSTEM, capabilities)
                                .get())
                .add(FIELD_PING,
                        BasicDBObjectBuilder.start()
                                .add(FIELD_TIME, new Date())
                                .add(FIELD_EXPIRES, new Date(0L))
                                .get())
                .get();
        testDrivers.insert(insertObj);
        // Get the object ID
        ObjectId objId = (ObjectId) insertObj.get(FIELD_ID);
        String id = objId == null ? null : objId.toString();

        // Done
        if (logger.isDebugEnabled())
        {
            // Retrieve the object for debug
            logger.debug(
                    "Registered test driver: \n" +
                            "   ID:  " + id + "\n" +
                            "   New: " + insertObj);
        }
        return id;
    }

    /**
     * Refresh the expiry time of a driver
     * 
     * @param id
     *        the driver id
     * @param expiryTime
     *        the new expiry time
     */
    public void refreshDriver(String id, long expiryTime)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(new ObjectId(id))
                .get();
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .push("$set")
                .add(FIELD_PING + "." + FIELD_EXPIRES, new Date(expiryTime))
                .pop()
                .get();
        testDrivers.findAndModify(queryObj, null, null, false, updateObj, false, false);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Updated test driver expiry: \n" +
                            "   ID:  " + id + "\n" +
                            "   New: " + expiryTime);
        }
    }

    /**
     * Unregister a test driver
     * 
     * @param id
     *        the ID of the registration
     */
    public void unregisterDriver(String id)
    {
        // Find the driver by ID
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_ID, new ObjectId(id))
                .get();
        testDrivers.remove(queryObj);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Unregistered test driver: " + id);
        }
    }

    /**
     * Get registered test drivers
     * 
     * @param release
     *        the release name of the test or <tt>null</tt> for all releases
     * @param schema
     *        the schema number of the driver or <tt>null</tt> for all schemas
     */
    public DBCursor getDrivers(String release, Integer schema, boolean active)
    {
        QueryBuilder queryBuilder = QueryBuilder.start();
        if (release != null)
        {
            queryBuilder.and(FIELD_RELEASE).is(release);
        }
        if (schema != null)
        {
            queryBuilder.and(FIELD_SCHEMA).is(schema);
        }
        if (active)
        {
            queryBuilder.and(FIELD_PING + "." + FIELD_EXPIRES).greaterThan(new Date());
        }
        DBObject queryObj = queryBuilder.get();
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_RELEASE, 1)
                .add(FIELD_SCHEMA, 1)
                .get();

        DBCursor cursor = testDrivers.find(queryObj).sort(sortObj);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Retrieved test driver: \n" +
                            "   Release: " + release + "\n" +
                            "   Schema:  " + schema + "\n" +
                            "   active:  " + active + "\n" +
                            "   Results: " + cursor.count());
        }
        return cursor;
    }

    /**
     * Count registered drivers
     * 
     * @param release
     *        the release name of the test or <tt>null</tt> for all releases
     * @param schema
     *        the schema number of the driver or <tt>null</tt> for all schemas
     * @param liveOnly
     *        <tt>true</tt> to retrieve only live instances
     * @return a count of the number of drivers matching the criteria
     */
    public long countDrivers(String release, Integer schema, boolean active)
    {
        QueryBuilder queryBuilder = QueryBuilder.start();
        if (release != null)
        {
            queryBuilder.and(FIELD_RELEASE).is(release);
        }
        if (schema != null)
        {
            queryBuilder.and(FIELD_SCHEMA).is(schema);
        }
        if (active)
        {
            queryBuilder.and(FIELD_PING + "." + FIELD_EXPIRES).greaterThan(new Date());
        }
        DBObject queryObj = queryBuilder.get();

        long count = testDrivers.count(queryObj);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Retrieved test driver: \n" +
                            "   Release: " + release + "\n" +
                            "   Schema:  " + schema + "\n" +
                            "   active:  " + active + "\n" +
                            "   Results: " + count);
        }
        return count;
    }

    /**
     * Write all the test application's property definitions against the release
     * and schema number
     * 
     * @param release
     *        the test release name
     * @param schema
     *        the property schema
     * @param description
     *        a description of the test definition
     * @param testProperties
     *        the property definitions
     * @return <tt>true</tt> if the properties were written or <tt>false</tt>
     *         if they already existed
     */
    public boolean writeTestDef(String release, Integer schema, String description, List<TestProperty> testProperties)
    {
        // Check the schema number for any existing instance
        DBObject queryObjExistingSchema = QueryBuilder
                .start()
                .and(FIELD_RELEASE).is(release)
                .and(FIELD_SCHEMA).greaterThanEquals(schema)
                .get();
        DBObject fieldsObjExistingSchema = BasicDBObjectBuilder
                .start()
                .add(FIELD_SCHEMA, true)
                .get();
        DBObject resultsObjExistingSchema = testDefs.findOne(queryObjExistingSchema, fieldsObjExistingSchema);
        Integer existingSchema = (resultsObjExistingSchema == null) ? null
                : (Integer) resultsObjExistingSchema.get(FIELD_SCHEMA);

        if (existingSchema == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No test definition exists for " + release + ":" + schema);
            }
            // Fall through to write the test definition
        }
        else
            if (existingSchema.equals(schema))
            {
                // We have an exact match. Don't do anything.
                if (logger.isDebugEnabled())
                {
                    logger.debug("Test definition exists for " + release + ":" + schema);
                }
                return false;
            }
            else
            {
                // The query found an instance with a larger schema number. We
                // don't run downgrades on the same release.
                throw new RuntimeException(
                        "The current test is out of date and needs to be upgraded to a later version or schema "
                                + release + ":" + schema);
            }

        // Pattern for valid property names
        Pattern pattern = Pattern.compile(PROP_NAME_REGEX);

        // Build a DB-safe map for direct persistence
        Collection<Properties> testPropertiesForDb = new ArrayList<Properties>(testProperties.size());
        for (TestProperty testProperty : testProperties)
        {
            // Do not write properties with invalid names
            Matcher matcher = pattern.matcher(testProperty.getName());
            if (!matcher.matches())
            {
                logger.warn("Property will be ignored.  The name is non-standard: " + matcher);
                continue;
            }

            // Convert the Java object to Java Properties
            Properties propValues = testProperty.toProperties();

            // That's it. Add it the properties.
            testPropertiesForDb.add(propValues);
        }

        // Attempt an insert
        DBObject newObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_RELEASE, release)
                .add(FIELD_SCHEMA, schema)
                .add(FIELD_DESCRIPTION, description)
                .add(FIELD_PROPERTIES, testPropertiesForDb)
                .get();

        try
        {
            WriteResult result = testDefs.insert(newObj);
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Created test definition: " + result + "\n" +
                                "   Release: " + release + "\n" +
                                "   Schema:  " + schema + "\n" +
                                "   New:     " + newObj);
            }
            return true;
        }
        catch (DuplicateKeyException e)
        {
            // Already present
            if (logger.isDebugEnabled())
            {
                logger.debug("Test definition exists for " + release + ":" + schema);
            }
            return false;
        }
    }

    /**
     * @param count
     *        the number of results to retrieve (must be greater than zero)
     * @return a list of tests, active or all
     */
    public DBCursor getTestDefs(boolean active, int skip, int count)
    {
        if (count < 1)
        {
            throw new IllegalArgumentException("'count' must be larger than zero.");
        }

        DBObject fieldsObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_RELEASE, true)
                .add(FIELD_SCHEMA, true)
                .get();
        DBObject sortObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_RELEASE, 1)
                .add(FIELD_SCHEMA, 1)
                .get();

        DBCursor cursor;
        if (active)
        {
            DBObject queryObj = QueryBuilder
                    .start()
                    .put(FIELD_PING + "." + FIELD_EXPIRES).greaterThanEquals(new Date())
                    .get();
            cursor = testDrivers.find(queryObj, fieldsObj).sort(sortObj).skip(skip).limit(count);
        }
        else
        {
            cursor = testDefs.find(null, fieldsObj).sort(sortObj).skip(skip).limit(count);
        }

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Fetching test definitions: \n" +
                            "   active:  " + active + "\n" +
                            "   skip:    " + skip + "\n" +
                            "   count:   " + count + "\n" +
                            "   Results: " + cursor.count());
        }
        return cursor;
    }

    /**
     * Get the test definition for internal use
     * 
     * @return the test definition (untouched) or <tt>null</tt>
     */
    private DBObject getTestDefRaw(String release, Integer schema)
    {
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_RELEASE, release)
                .add(FIELD_SCHEMA, schema)
                .get();

        DBObject testDefObj = testDefs.findOne(queryObj);
        // Done
        return testDefObj;
    }

    /**
     * A cacheable object for holding the test definition
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    private static class TestDefEntry
    {
        public final DBObject testDefObj;
        public final Map<String, DBObject> testDefPropsMap;

        public TestDefEntry(DBObject testDefObj)
        {
            this.testDefObj = testDefObj;
            // Extract the properties
            Map<String, DBObject> testDefPropsMap = new HashMap<String, DBObject>(29);
            BasicDBList dbList = (BasicDBList) testDefObj.get(FIELD_PROPERTIES);
            for (Object dbListObj : dbList)
            {
                DBObject propObj = (DBObject) dbListObj;
                propObj.put(FIELD_VERSION, Integer.valueOf(0));
                propObj.put(FIELD_ORIGIN, TestPropertyOrigin.DEFAULTS.name());
                String propName = (String) propObj.get(FIELD_NAME);
                testDefPropsMap.put(propName, propObj);
            }
            this.testDefPropsMap = Collections.unmodifiableMap(testDefPropsMap);
        }
    }

    /**
     * Retrieve a cached version of the test definition
     * 
     * @return a cached test definition or <tt>null</tt> if not found
     */
    private TestDefEntry getTestDefCached(String release, Integer schema)
    {
        String schemaKey = release + "-" + schema;

        // The common case: read
        testDefCacheLock.readLock().lock();
        try
        {
            TestDefEntry entry = testDefCache.get(schemaKey);
            if (entry != null)
            {
                return entry;
            }
        }
        finally
        {
            testDefCacheLock.readLock().unlock();
        }

        // Uncommon case: write
        testDefCacheLock.writeLock().lock();
        try
        {
            // No need for a double-check, we don't care
            DBObject testDefObj = getTestDefRaw(release, schema);
            if (testDefObj == null)
            {
                // We won't cache nulls
                return null;
            }
            TestDefEntry entry = new TestDefEntry(testDefObj);
            testDefCache.put(schemaKey, entry);
            return entry;
        }
        finally
        {
            testDefCacheLock.writeLock().unlock();
        }
    }

    /**
     * Retrieve a specific (and full) test definition
     * 
     * @param release
     *        the test definition software release
     * @param schema
     *        the schema number
     * @return the test definition or <tt>null</tt> if not found
     */
    public DBObject getTestDef(String release, Integer schema)
    {
        TestDefEntry testDefEntry = getTestDefCached(release, schema);
        DBObject testDefObj = testDefEntry == null ? null : testDefEntry.testDefObj;

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Fetched test definition: \n" +
                            "   Release: " + release + "\n" +
                            "   Schema:  " + schema + "\n" +
                            "   Results: " + testDefObj);
        }
        return testDefObj;
    }

    /**
     * Get a list of all defined tests
     * 
     * @param release
     *        the test definition software release or <tt>null</tt> for all test
     *        releases
     * @param schema
     *        the schema number or <tt>null</tt> for all schemas
     * @return all the currently-defined tests
     */
    public DBCursor getTests(String release, Integer schema, int skip, int count)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder
                .start();
        if (release != null && release.length() > 0)
        {
            queryObjBuilder.add(FIELD_RELEASE, release);
        }
        if (schema != null)
        {
            queryObjBuilder.add(FIELD_SCHEMA, schema);
        }
        DBObject queryObj = queryObjBuilder.get();

        // We don't want everything just now
        DBObject fieldsObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_NAME, true)
                .add(FIELD_VERSION, true)
                .add(FIELD_DESCRIPTION, true)
                .add(FIELD_RELEASE, true)
                .add(FIELD_SCHEMA, true)
                .get();

        DBCursor dbCursor = tests.find(queryObj, fieldsObj).skip(skip).limit(count);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Fetched tests: \n" +
                            "   Release: " + release + "\n" +
                            "   Schema:  " + schema + "\n" +
                            "   Results: " + dbCursor.count());
        }
        return dbCursor;
    }

    /**
     * Fetch the low-level ID for a test run
     * 
     * @return the test ID or <tt>null</tt> if not found
     */
    private ObjectId getTestId(String test)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_NAME).is(test)
                .get();
        DBObject fieldsObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_ID, true)
                .get();
        DBObject testObj = tests.findOne(queryObj, fieldsObj);
        ObjectId testObjId = null;
        if (testObj != null)
        {
            testObjId = (ObjectId) testObj.get(FIELD_ID);
        }
        // Done
        if (logger.isTraceEnabled())
        {
            logger.trace(
                    "Fetched test ID: \n" +
                            "   Test:    " + test + "\n" +
                            "   Result:  " + testObjId);
        }
        return testObjId;
    }

    /**
     * Retrieve the data for given test
     * 
     * @param testObjId
     *        the ID of the test
     * @param includeProperties
     *        <tt>true</tt> to flesh out the properties
     * @return the test object or <tt>null</tt> if not found
     */
    public DBObject getTest(ObjectId testObjId, boolean includeProperties)
    {
        DBObject queryObj = QueryBuilder
                .start(FIELD_ID).is(testObjId)
                .get();

        BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                .start(FIELD_NAME, 1)
                .add(FIELD_VERSION, true)
                .add(FIELD_DESCRIPTION, true)
                .add(FIELD_RELEASE, true)
                .add(FIELD_SCHEMA, true);
        DBObject fieldsObj = fieldsObjBuilder.get();

        DBObject testObj = tests.findOne(queryObj, fieldsObj);
        if (testObj == null)
        {
            // The test run no longer exists
            logger.warn("Test not found.  Returning null test: " + testObjId);
            return null;
        }

        BasicDBList propsList = new BasicDBList();
        if (includeProperties)
        {
            // Get the associated test definition
            String test = (String) testObj.get(FIELD_NAME);
            String release = (String) testObj.get(FIELD_RELEASE);
            Integer schema = (Integer) testObj.get(FIELD_SCHEMA);
            TestDefEntry testDefEntry = getTestDefCached(release, schema);
            if (testDefEntry == null)
            {
                // Again, we don't bother trying to resolve this
                logger.warn("Test definition not found for test: " + testObj);
                logger.warn("Deleting test without a test definition: " + testObj);
                this.deleteTest(test);
                return null;
            }
            else
            {
                // Start with the properties from the test definition
                Map<String, DBObject> propsMap = new HashMap<String, DBObject>(testDefEntry.testDefPropsMap);

                // Fetch the properties for the test
                DBCursor testPropsCursor = getTestPropertiesRaw(testObjId, null);
                // Combine
                MongoTestDAO.mergeProperties(propsMap, testPropsCursor);

                // Turn into a map and add back into the object
                propsList = MongoTestDAO.getPropertyList(propsMap);
                testObj.put(FIELD_PROPERTIES, propsList);
            }
        }

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Found test: " + testObj);
        }
        return testObj;
    }

    /**
     * Retrieve the data for given test
     * 
     * @param test
     *        the test name
     * @param includeProperties
     *        <tt>true</tt> to flesh out the properties
     * @return the test object or <tt>null</tt> if not found
     */
    public DBObject getTest(String test, boolean includeProperties)
    {
        ObjectId testObjId = getTestId(test);
        if (testObjId == null)
        {
            // The test run no longer exists
            logger.warn("Test not found.  Returning null test: " + test);
            return null;
        }
        return getTest(testObjId, includeProperties);
    }

    /**
     * Create a new test by copying an existing test.
     * <p/>
     * All property overrides will be copied, which is where the value really
     * lies.
     * Test runs are not copied.
     * 
     * @param name
     *        a globally-unique name using
     *        {@link ConfigConstants#TEST_NAME_REGEX}
     * @param release
     *        the test definition software release or <tt>null</tt> to use the
     *        same release as the source test
     * @param schema
     *        the schema number or <tt>null</tt> to use the same schema as the
     *        source test
     * @param copyOfTest
     *        the test name to copy
     * @param copyOfVersion
     *        the version of the test to copy
     * @return <tt>true</tt> if the test was copied or <tt>false</tt> if not
     */
    public boolean copyTest(String test, String release, Integer schema, String copyOfTest, int copyOfVersion)
    {
        // Get the test
        DBObject copyOfTestObj = getTest(copyOfTest, true);
        if (copyOfTestObj == null || !Integer.valueOf(copyOfVersion).equals(copyOfTestObj.get(FIELD_VERSION)))
        {
            logger.warn("Did not find test to copy: " + test + " (V" + copyOfVersion + ")");
            return false;
        }
        if (release == null)
        {
            // Use the source test release
            release = (String) copyOfTestObj.get(FIELD_RELEASE);
        }
        if (schema == null)
        {
            schema = (Integer) copyOfTestObj.get(FIELD_SCHEMA);
        }
        String description = (String) copyOfTestObj.get(FIELD_DESCRIPTION);

        // Copy the test
        if (!createTest(test, description, release, schema))
        {
            logger.warn("Failed to create a test via copy: " + test);
            return false;
        }

        // Get the properties to copy
        BasicDBList copyOfPropObjs = (BasicDBList) copyOfTestObj.get(FIELD_PROPERTIES);
        if (copyOfPropObjs == null)
        {
            copyOfPropObjs = new BasicDBList();
        }
        for (Object obj : copyOfPropObjs)
        {
            DBObject copyPropObj = (DBObject) obj;
            Integer copyPropVer = (Integer) copyPropObj.get(FIELD_VERSION);
            if (copyPropVer == null || copyPropVer.intValue() <= 0)
            {
                // There has been no override
                continue;
            }
            String propName = (String) copyPropObj.get(FIELD_NAME);
            // Is this property present in the new test (might be copying
            // between releases)
            if (getProperty(test, null, propName) == null)
            {
                // The new test does not have the property so do not import the
                // overridden value
                continue;
            }

            String propValue = (String) copyPropObj.get(FIELD_VALUE);
            Integer versionZero = Integer.valueOf(0);
            this.setPropertyOverride(test, null, propName, versionZero, propValue);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Copied test: \n" +
                            "   From Test: " + copyOfTest + "\n" +
                            "   New Test:  " + test);
        }
        return true;
    }

    /**
     * Create a new test for the precise release and schema
     * 
     * @param test
     *        a globally-unique name using
     *        {@link ConfigConstants#TEST_NAME_REGEX}
     * @param description
     *        any description
     * @param release
     *        the test definition software release
     * @param schema
     *        the schema number
     * @return <tt>true</tt> if the test was written other <tt>false</tt> if not
     */
    public boolean createTest(String test, String description, String release, Integer schema)
    {
        if (test == null || test.length() == 0)
        {
            throw new IllegalArgumentException("Name length must be non-zero");
        }
        else
            if (release == null || schema == null)
            {
                throw new IllegalArgumentException("A release and schema number must be supplied for a test.");
            }
        Pattern pattern = Pattern.compile(TEST_NAME_REGEX);
        Matcher matcher = pattern.matcher(test);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException(
                    "The test name '" + test + "' is invalid.  " +
                            "Test names must start with a character and contain only characters, numbers or underscore.");
        }

        // There are no properties to start with
        DBObject writeObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_NAME, test)
                .add(FIELD_VERSION, Integer.valueOf(0))
                .add(FIELD_DESCRIPTION, description)
                .add(FIELD_RELEASE, release)
                .add(FIELD_SCHEMA, schema)
                .get();

        try
        {
            WriteResult result = tests.insert(writeObj);
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Created test: " + result + "\n" +
                                "   Name:    " + test + "\n" +
                                "   Descr:   " + description + "\n" +
                                "   Release: " + release + "\n" +
                                "   Schema:  " + schema);
            }
            return true;
        }
        catch (DuplicateKeyException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Test exists: " + test + ".");
            }
            return false;
        }
    }

    /**
     * Update an existing test to use new test details
     * 
     * @param name
     *        the name of the test (must exist)
     * @param version
     *        the version of the test for concurrency checking
     * @param newName
     *        the new test name
     * @param newDescription
     *        the new description or <tt>null</tt> ot leave it
     * @param newRelease
     *        the new software release or <tt>null</tt> to leave it
     * @param newSchema
     *        the new schema number or <tt>null</tt> to leave it
     * @return <tt>true</tt> if the test run was modified or <tt>false</tt> if
     *         not
     */
    public boolean updateTest(
            String name, int version,
            String newName, String newDescription, String newRelease, Integer newSchema)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Updated requires a name and version.");
        }

        // Find the test by name and version
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_NAME).is(name)
                .and(FIELD_VERSION).is(version)
                .get();

        // Handle version wrap-around
        Integer newVersion = version >= Short.MAX_VALUE ? 1 : version + 1;
        // Gather all the setters required
        BasicDBObjectBuilder setObjBuilder = BasicDBObjectBuilder
                .start()
                .add(FIELD_VERSION, newVersion);
        if (newName != null)
        {
            Pattern pattern = Pattern.compile(TEST_NAME_REGEX);
            Matcher matcher = pattern.matcher(newName);
            if (!matcher.matches())
            {
                throw new IllegalArgumentException(
                        "The test name '" + newName + "' is invalid.  " +
                                "Test names must start with a character and contain only characters, numbers or underscore.");
            }

            setObjBuilder.add(FIELD_NAME, newName);
        }
        if (newDescription != null)
        {
            setObjBuilder.add(FIELD_DESCRIPTION, newDescription);
        }
        if (newRelease != null)
        {
            setObjBuilder.add(FIELD_RELEASE, newRelease);
        }
        if (newSchema != null)
        {
            setObjBuilder.add(FIELD_SCHEMA, newSchema);
        }
        DBObject setObj = setObjBuilder.get();

        // Now push the values to set into the update
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .add("$set", setObj)
                .get();

        WriteResult result = tests.update(queryObj, updateObj);
        boolean written = (result.getN() > 0);

        // Done
        if (logger.isDebugEnabled())
        {
            if (written)
            {
                logger.debug(
                        "Updated test: \n" +
                                "   Test:      " + name + "\n" +
                                "   Update:    " + updateObj);
            }
            else
            {
                logger.debug("Did not update test: " + name);
            }
        }
        return written;
    }

    /**
     * Delete an existing test
     * 
     * @param test
     *        the name of the test (must exist)
     * @return <tt>true</tt> if the test was deleted or <tt>false</tt> if not
     */
    public boolean deleteTest(String test)
    {
        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            // The test no longer exists, so the run effectively doesn't either
            logger.warn("Test not found: " + test);
            return false;
        }
        ObjectId testObjId = (ObjectId) testObj.get(FIELD_ID);

        // Find the test by name and version
        DBObject testDelObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(testObjId)
                .get();

        WriteResult result = tests.remove(testDelObj);
        boolean written = (result.getN() > 0);

        // Clean up test-related runs
        DBObject runDelObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_TEST, testObjId)
                .get();
        testRuns.remove(runDelObj);

        // Clean up properties
        DBObject propDelObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_TEST, testObjId)
                .get();
        testProps.remove(propDelObj);

        // Done
        if (logger.isDebugEnabled())
        {
            if (written)
            {
                logger.debug("Deleted test: " + test);
            }
            else
            {
                logger.debug("Did not delete test: " + test);
            }
        }
        return written;
    }

    /**
     * Get the test run names associated with a given test
     * 
     * @param test
     *        the name of the test
     * @return the names of all test runs associated with the given test
     */
    public List<String> getTestRunNames(String test)
    {
        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            // The test no longer exists, so the run effectively doesn't either
            logger.warn("Test not found: " + test);
            return Collections.emptyList();
        }
        ObjectId testObjId = (ObjectId) testObj.get(FIELD_ID);

        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_TEST).is(testObjId)
                .get();
        DBObject fieldsObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_ID, true)
                .add(FIELD_NAME, true)
                .get();
        DBCursor cursor = testRuns.find(queryObj, fieldsObj);
        List<String> testRunNames = new ArrayList<String>(cursor.count());
        try
        {
            while (cursor.hasNext())
            {
                DBObject testRunObj = cursor.next();
                String testRunName = (String) testRunObj.get(FIELD_NAME);
                testRunNames.add(testRunName);
            }
        }
        finally
        {
            cursor.close();
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Found and returned " + testRunNames.size() + " test run names for test '" + test + "'");
        }
        return testRunNames;
    }

    /**
     * @param test
     *        only fetch runs for this test or <tt>null</tt> to get all test
     *        runs
     * @param testRunStates
     *        optional states that the test runs must be in or empty for all
     * @return a cursor onto the test runs for the given test
     */
    public DBCursor getTestRuns(String test, int skip, int count, TestRunState... testRunStates)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder
                .start();
        if (test != null)
        {
            ObjectId testObjId = getTestId(test);
            if (testObjId == null)
            {
                // The test no longer exists, so the run effectively doesn't
                // either
                logger.warn("Test not found: " + test);
                // Use a ficticious ID that will never match
                testObjId = new ObjectId();
            }
            queryObjBuilder.add(FIELD_TEST, testObjId);
        }

        // Build query for the test run states
        if (testRunStates.length > 0)
        {
            List<String> stateStrs = new ArrayList<String>(testRunStates.length);
            for (int i = 0; i < testRunStates.length; i++)
            {
                stateStrs.add(testRunStates[i].toString());
            }
            queryObjBuilder.push(FIELD_STATE);
            queryObjBuilder.add("$in", stateStrs);
        }

        DBObject queryObj = queryObjBuilder.get();

        DBObject fieldsObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_NAME, true)
                .add(FIELD_TEST, true)
                .add(FIELD_VERSION, true)
                .add(FIELD_DESCRIPTION, true)
                .add(FIELD_STATE, true)
                .add(FIELD_SCHEDULED, true)
                .add(FIELD_STARTED, true)
                .add(FIELD_STOPPED, true)
                .add(FIELD_COMPLETED, true)
                .add(FIELD_DURATION, true)
                .add(FIELD_PROGRESS, true)
                .add(FIELD_RESULTS_SUCCESS, true)
                .add(FIELD_RESULTS_FAIL, true)
                .add(FIELD_RESULTS_TOTAL, true)
                .add(FIELD_SUCCESS_RATE, true)
                .get();

        DBCursor dbCursor = testRuns.find(queryObj, fieldsObj).skip(skip).limit(count);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Fetched test runs: \n" +
                            "   Test:    " + test + "\n" +
                            "   Results: " + dbCursor.count());
        }
        return dbCursor;
    }

    /**
     * Fetch the low-level ID for a test run
     */
    private ObjectId getTestRunId(ObjectId testObjId, String run)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_TEST).is(testObjId)
                .and(FIELD_NAME).is(run)
                .get();
        DBObject fieldsObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_ID, true)
                .get();
        DBObject runObj = testRuns.findOne(queryObj, fieldsObj);
        ObjectId runObjId = null;
        if (runObj != null)
        {
            runObjId = (ObjectId) runObj.get(FIELD_ID);
        }
        // Done
        if (logger.isTraceEnabled())
        {
            logger.trace(
                    "Fetched test run ID: \n" +
                            "   Test ID: " + testObjId + "\n" +
                            "   Run:     " + run + "\n" +
                            "   Result:  " + runObjId);
        }
        return runObjId;
    }

    /**
     * Retrieve the data for given test run
     * 
     * @param runObjId
     *        (ObjectId, mandatory) the ID of the test run
     * 
     * @param includeProperties
     *        <tt>true</tt> to flesh out all the properties
     * 
     * @return the test object
     */
    public DBObject getTestRun(ObjectId runObjId, boolean includeProperties) throws ObjectNotFoundException
    {
        ArgumentCheck.checkMandatoryObject(runObjId, "runObjId");

        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(runObjId)
                .get();

        BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                .start()
                .add(FIELD_NAME, true)
                .add(FIELD_TEST, true)
                .add(FIELD_VERSION, true)
                .add(FIELD_DESCRIPTION, true)
                .add(FIELD_STATE, true)
                .add(FIELD_SCHEDULED, true)
                .add(FIELD_STARTED, true)
                .add(FIELD_STOPPED, true)
                .add(FIELD_COMPLETED, true)
                .add(FIELD_DURATION, true)
                .add(FIELD_PROGRESS, true)
                .add(FIELD_RESULTS_SUCCESS, true)
                .add(FIELD_RESULTS_FAIL, true)
                .add(FIELD_RESULTS_TOTAL, true)
                .add(FIELD_SUCCESS_RATE, true)
                .add(FIELD_DRIVERS, true);
        DBObject fieldsObj = fieldsObjBuilder.get();

        DBObject runObj = testRuns.findOne(queryObj, fieldsObj);
        if (runObj == null)
        {
            // The test run no longer exists
            throw new ObjectNotFoundException("Test run");
        }

        if (includeProperties)
        {
            ObjectId testObjId = (ObjectId) runObj.get(FIELD_TEST);
            String testName = runObj.get(FIELD_TEST).toString();
            String runName = runObj.get(FIELD_NAME).toString();

            BasicDBList propsList = getTestRunProperties(testObjId, runObjId, testName, runName);
            runObj.put(FIELD_PROPERTIES, propsList);
        }

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Found test run " + runObjId + ": " + runObj);
        }
        return runObj;
    }

    /**
     * Checks whether test and test run still exists and cleans up the database
     * if not
     * 
     * @param testName
     *        (String, mandatory) test name
     * @param testRunName
     *        (String, optional) test run name
     * 
     * @since 2.1.2
     */
    private boolean cleanup(String testName, String testRunName)
    {
        ArgumentCheck.checkMandatoryString(testName, "testName");
        boolean written = false;

        // first check for the test itself ...
        ObjectId testObjId = getTestId(testName);
        if (testObjId == null)
        {
            String msg = "Test run no longer has a matching test definition: \n" +
                    "   Test: " + testName;
            if (null != testRunName && !testRunName.isEmpty())
            {
                msg += "\n" + "   Run:  " + testRunName;
            }
            logger.warn(msg);

            written = this.deleteTest(testName);
        }

        return written;
    }

    /**
     * Returns the "final" test run properties of the test run.
     * 
     * @param testObjId
     *        (ObjectId, optional)
     * @param runObjId
     *        (ObjectId, optional)
     * @param testName
     *        (String, mandatory)
     * @param testRunName
     *        (String, mandatory)
     * 
     * @return final property collection with overrides of the test/run or
     *         exception
     * 
     * @throws ObjectNotFoundException
     */
    private BasicDBList getTestRunProperties(
            ObjectId testObjId,
            ObjectId runObjId,
            String testName,
            String testRunName) throws ObjectNotFoundException
    {
        // get map of properties
        Map<String, DBObject> propsMap = getTestRunPropertiesMap(testObjId, runObjId, testName, testRunName);

        // Turn into a list and return
        return MongoTestDAO.getPropertyList(propsMap);
    }

    /**
     * Returns the map with "final" test run properties of the test run.
     * 
     * @param testObjId
     *        (ObjectId, optional)
     * @param runObjId
     *        (ObjectId, optional)
     * @param testName
     *        (String, mandatory)
     * @param testRunName
     *        (String, mandatory)
     * 
     * @return (Map<String, DBObject>) or exception
     * 
     * @throws ObjectNotFoundException
     */
    public Map<String, DBObject> getTestRunPropertiesMap(
            ObjectId testObjId,
            ObjectId runObjId,
            String testName,
            String testRunName) throws ObjectNotFoundException
    {
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");

        // check optional arguments
        if (null == testObjId)
        {
            testObjId = getTestId(testName);
            if (null == testObjId)
            {
                cleanup(testName, testRunName);
                throw new ObjectNotFoundException(testName);
            }
        }
        if (null == runObjId)
        {
            runObjId = getTestRunId(testObjId, testRunName);
            if (null == runObjId)
            {
                throw new ObjectNotFoundException(testName + "." + testRunName);
            }
        }

        // Retrieve the test
        DBObject testObj = getTest(testObjId, false);
        if (testObj == null)
        {
            cleanup(testName, testRunName);
            throw new ObjectNotFoundException(testName + "." + testRunName);
        }

        // Get the associated test definition
        String release = (String) testObj.get(FIELD_RELEASE);
        Integer schema = (Integer) testObj.get(FIELD_SCHEMA);
        TestDefEntry testDefEntry = getTestDefCached(release, schema);
        if (testDefEntry == null)
        {
            cleanup(testName, testRunName);
            throw new ObjectNotFoundException(testName + "." + testRunName);
        }

        // now get the properties
        // Start with the properties from the test definition
        Map<String, DBObject> propsMap = new HashMap<String, DBObject>(testDefEntry.testDefPropsMap);

        // Fetch the properties for the test
        DBCursor testPropsCursor = getTestPropertiesRaw(testObjId, null);
        // Combine
        MongoTestDAO.mergeProperties(propsMap, testPropsCursor);
        // Fetch the properties for the test run
        DBCursor runPropsCursor = getTestPropertiesRaw(testObjId, runObjId);
        // Combine
        MongoTestDAO.mergeProperties(propsMap, runPropsCursor);

        return propsMap;
    }

    /**
     * Returns the map with "final" test run properties of the test run.
     * 
     * @param testObjId
     *        (ObjectId, optional)
     * @param testName
     *        (String, mandatory)
     * 
     * @return (Map<String, DBObject>) or exception
     * 
     * @throws ObjectNotFoundException
     */
    public Map<String, DBObject> getTestPropertiesMap(
            ObjectId testObjId,
            String testName) throws ObjectNotFoundException
    {
        ArgumentCheck.checkMandatoryString(testName, "testName");

        // check optional arguments
        if (null == testObjId)
        {
            testObjId = getTestId(testName);
            if (null == testObjId)
            {
                cleanup(testName, null);
                throw new ObjectNotFoundException(testName);
            }
        }

        // Retrieve the test
        DBObject testObj = getTest(testObjId, false);
        if (testObj == null)
        {
            cleanup(testName, null);
            throw new ObjectNotFoundException(testName);
        }

        // Get the associated test definition
        String release = (String) testObj.get(FIELD_RELEASE);
        Integer schema = (Integer) testObj.get(FIELD_SCHEMA);
        TestDefEntry testDefEntry = getTestDefCached(release, schema);
        if (testDefEntry == null)
        {
            cleanup(testName, null);
            throw new ObjectNotFoundException(testName);
        }

        // now get the properties
        // Start with the properties from the test definition
        Map<String, DBObject> propsMap = new HashMap<String, DBObject>(testDefEntry.testDefPropsMap);

        // Fetch the properties for the test
        DBCursor testPropsCursor = getTestPropertiesRaw(testObjId, null);
        // Combine
        MongoTestDAO.mergeProperties(propsMap, testPropsCursor);

        return propsMap;
    }
    /**
     * Retrieve the data for given test run
     * 
     * @param test
     *        (String, mandatory) the name of the test
     * @param run
     *        (String, mandatory) the test run name
     * 
     * @param includeProperties
     *        <tt>true</tt> to flesh out all the properties
     * 
     * @return (DBObject) the test object
     * 
     * @throws ObjectNotFoundException
     */
    public DBObject getTestRun(String test, String run, boolean includeProperties) throws ObjectNotFoundException
    {
        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            // The test no longer exists, so the run effectively doesn't either
            throw new ObjectNotFoundException(test);
        }

        ObjectId testObjId = (ObjectId) testObj.get(FIELD_ID);

        // Get the ID of the test run
        ObjectId runObjId = getTestRunId(testObjId, run);
        if (runObjId == null)
        {
            // The test run no longer exists
            throw new ObjectNotFoundException(test + "." + run);
        }

        return getTestRun(runObjId, includeProperties);
    }

    /**
     * Create a new test run by copying an existing test run.
     * <p/>
     * All property overrides will be copied, which is where the value really
     * lies.
     * 
     * @param test
     *        the name of the test to which the run belongs
     * @param run
     *        a test-unique name using {@link ConfigConstants#RUN_NAME_REGEX}
     * @param copyOfRun
     *        the test run name to copy
     * @param copyOfVersion
     *        the version of the test run to copy
     * @return <tt>true</tt> if the test run was copied or <tt>false</tt> if not
     */
    public boolean copyTestRun(String test, String run, String copyOfRun, int copyOfVersion)
    {
        // Get the test
        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            // The test no longer exists, so the run effectively doesn't either
            logger.warn("Test not found: " + test);
            return false;
        }
        // Get the test run
        DBObject copyOfTestRunObj;
        try
        {
            copyOfTestRunObj = getTestRun(test, copyOfRun, true);
        }
        catch (ObjectNotFoundException e)
        {
            copyOfTestRunObj = null;
        }

        if (copyOfTestRunObj == null || !Integer.valueOf(copyOfVersion).equals(copyOfTestRunObj.get(FIELD_VERSION)))
        {
            logger.warn("Did not find test run to copy: " + test + "." + copyOfRun + " (V" + copyOfVersion + ")");
            return false;
        }
        String description = (String) copyOfTestRunObj.get(FIELD_DESCRIPTION);
        // Copy the test run
        if (!createTestRun(test, run, description))
        {
            logger.warn("Failed to create a test run via copy: " + test + "." + run);
            return false;
        }

        // Get the properties to copy
        BasicDBList copyOfPropObjs = (BasicDBList) copyOfTestRunObj.get(FIELD_PROPERTIES);
        if (copyOfPropObjs == null)
        {
            copyOfPropObjs = new BasicDBList();
        }
        for (Object obj : copyOfPropObjs)
        {
            DBObject copyPropObj = (DBObject) obj;
            Integer copyPropVer = (Integer) copyPropObj.get(FIELD_VERSION);
            if (copyPropVer == null || copyPropVer.intValue() <= 0)
            {
                // There has been no override
                continue;
            }
            String copyPropOrigin = (String) copyPropObj.get(FIELD_ORIGIN);
            if (!TestPropertyOrigin.RUN.name().equals(copyPropOrigin))
            {
                // We also don't copy values that did not originate in the test
                // run
                continue;
            }
            String propName = (String) copyPropObj.get(FIELD_NAME);
            String propValue = (String) copyPropObj.get(FIELD_VALUE);
            Integer versionZero = Integer.valueOf(0);
            this.setPropertyOverride(test, run, propName, versionZero, propValue);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Copied test run: \n" +
                            "   Test:     " + test + "\n" +
                            "   From Run: " + copyOfRun + "\n" +
                            "   New Run:  " + run);
        }
        return true;
    }

    /**
     * Create a new test run
     * 
     * @param test
     *        the name of the test to which the run belongs
     * @param run
     *        a test-unique name using {@link ConfigConstants#RUN_NAME_REGEX}
     * @param description
     *        any description
     * @return <tt>true</tt> if the test run was written other <tt>false</tt> if
     *         not
     */
    public boolean createTestRun(String test, String run, String description)
    {
        if (run == null || run.length() == 0)
        {
            throw new IllegalArgumentException("Name length must be non-zero");
        }
        Pattern pattern = Pattern.compile(RUN_NAME_REGEX);
        Matcher matcher = pattern.matcher(run);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException(
                    "The test run name '" + run + "' is invalid.  " +
                            "Test run names may contain only characters, numbers or underscore.");
        }

        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            // The test no longer exists, so the run effectively doesn't either
            logger.warn("Test not found: " + test);
            return false;
        }
        ObjectId testObjId = (ObjectId) testObj.get(FIELD_ID);

        // There are no properties to start with
        DBObject writeObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_TEST, testObjId)
                .add(FIELD_NAME, run)
                .add(FIELD_VERSION, Integer.valueOf(0))
                .add(FIELD_DESCRIPTION, description)
                .add(FIELD_STATE, TestRunState.NOT_SCHEDULED.toString())
                .add(FIELD_SCHEDULED, Long.valueOf(-1L))
                .add(FIELD_STARTED, Long.valueOf(-1L))
                .add(FIELD_STOPPED, Long.valueOf(-1L))
                .add(FIELD_COMPLETED, Long.valueOf(-1L))
                .add(FIELD_DURATION, Long.valueOf(0L))
                .add(FIELD_PROGRESS, Double.valueOf(0.0D))
                .add(FIELD_RESULTS_SUCCESS, Long.valueOf(0L))
                .add(FIELD_RESULTS_FAIL, Long.valueOf(0L))
                .add(FIELD_RESULTS_TOTAL, Long.valueOf(0L))
                .add(FIELD_SUCCESS_RATE, Double.valueOf(1.0))
                .add(FIELD_DRIVERS, new BasicDBList())              // Ensure we
                                                                    // have an
                                                                    // empty
                                                                    // list to
                                                                    // start
                .get();

        try
        {
            WriteResult result = testRuns.insert(writeObj);
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Created test run: " + result + "\n" +
                                "   Test:    " + test + "\n" +
                                "   Name:    " + run + "\n" +
                                "   Descr:   " + description);
            }
            return true;
        }
        catch (DuplicateKeyException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Test run exists: " + test + ". " + run);
            }
            return false;
        }
    }

    /**
     * Update an existing test run with new details
     *
     * @param test
     *        the name of the test
     * @param run
     *        the name of the test run (must exist)
     * @param version
     *        the version of the test for concurrency checking
     * @param newName
     *        the new name of the test run
     * @param newDescription
     *        the new description or <tt>null</tt> ot leave it
     * @return <tt>true</tt> if the test run was modified or <tt>false</tt> if
     *         not
     */
    public boolean updateTestRun(
            String test, String run, int version,
            String newName, String newDescription)
    {
        if (test == null || run == null)
        {
            throw new IllegalArgumentException("Updated requires a name and version.");
        }

        // Get the test
        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Unable to update test run; test not found: " + test);
            }
            return false;
        }

        // Find the test run by name and version
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_TEST).is(testObj.get(FIELD_ID))
                .and(FIELD_NAME).is(run)
                .and(FIELD_VERSION).is(version)
                .get();

        // Handle version wrap-around
        Integer newVersion = version >= Short.MAX_VALUE ? 1 : version + 1;
        // Gather all the setters required
        BasicDBObjectBuilder setObjBuilder = BasicDBObjectBuilder
                .start()
                .add(FIELD_VERSION, newVersion);
        if (newName != null)
        {
            Pattern pattern = Pattern.compile(RUN_NAME_REGEX);
            Matcher matcher = pattern.matcher(newName);
            if (!matcher.matches())
            {
                throw new IllegalArgumentException(
                        "The test run name '" + newName + "' is invalid.  " +
                                "Test run names may only contain characters, numbers or underscore.");
            }
            setObjBuilder.add(FIELD_NAME, newName);
        }
        if (newDescription != null)
        {
            setObjBuilder.add(FIELD_DESCRIPTION, newDescription);
        }
        DBObject setObj = setObjBuilder.get();

        // Now push the values to set into the update
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .add("$set", setObj)
                .get();

        WriteResult result = testRuns.update(queryObj, updateObj);
        boolean written = (result.getN() > 0);

        // Done
        if (logger.isDebugEnabled())
        {
            if (written)
            {
                logger.debug(
                        "Updated test run: \n" +
                                "   Test:      " + test + "\n" +
                                "   Run:       " + run + "\n" +
                                "   Update:    " + updateObj);
            }
            else
            {
                logger.debug("Did not update test run: " + test + "." + run);
            }
        }
        return written;
    }

    /**
     * Update the run state of a test run.
     * <p/>
     * The test run {@link TestConstants#FIELD_STATE state} will be set based on
     * the values.
     * <p/>
     * Note that a test run can either be stopped or completed but not both. In
     * both cases,
     * though, the test run must have been scheduled and then started.
     *
     * @param test
     *        the name of the test
     * @param run
     *        the name of the test run (must exist)
     * @param version
     *        the version of the test for concurrency checking
     * @param testRunState
     *        the test run state to set (<null> to ignore)
     * @param scheduled
     *        the time when the test run is scheduled to start (<null> to
     *        ignore)
     * @param started
     *        the time when the test run started (<null> to ignore)
     * @param stopped
     *        the time when the test run was stopped (<null> to ignore)
     * @param completed
     *        the time when the test run was completed (<null> to ignore)
     * @param duration
     *        the time the test has been running for in ms (<null> to ignore)
     * @param progress
     *        the new progress for the test run (<null> to ignore)
     * @param resultsSuccess
     *        the number of successful results (<null> to ignore)
     * @param resultsFailure
     *        the number of failed results (<null> to ignore)
     * @return <tt>true</tt> if the test run was modified or <tt>false</tt> if
     *         not
     */
    public boolean updateTestRunState(
            ObjectId runId, int version,
            TestRunState testRunState,
            Long scheduled,
            Long started, Long stopped, Long completed,
            Long duration,
            Double progress,
            Long resultsSuccess, Long resultsFail)
    {
        // Find the test run by name and version
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(runId)
                .and(FIELD_VERSION).is(version)
                .get();

        // Gather all the setters required
        BasicDBObjectBuilder setObjBuilder = BasicDBObjectBuilder.start();
        if (testRunState != null)
        {
            setObjBuilder.add(FIELD_STATE, testRunState.toString());
        }
        if (scheduled != null)
        {
            setObjBuilder.add(FIELD_SCHEDULED, scheduled);
        }
        if (started != null)
        {
            setObjBuilder.add(FIELD_STARTED, started);
        }
        if (stopped != null)
        {
            setObjBuilder.add(FIELD_STOPPED, stopped);
        }
        if (completed != null)
        {
            setObjBuilder.add(FIELD_COMPLETED, completed);
        }
        if (duration != null)
        {
            setObjBuilder.add(FIELD_DURATION, duration);
        }
        if (progress != null)
        {
            // Adjust accuracy of the progress
            long progressLong = Math.round(progress * 10000.0);
            if (progressLong < 0L || progressLong > 10000L)
            {
                throw new IllegalArgumentException("Progress must be expressed as a double in range [0.0, 1.0].");
            }
            progress = progressLong / 10000.0;      // Accuracy

            setObjBuilder.add(FIELD_PROGRESS, progress);
        }
        if (resultsSuccess != null || resultsFail != null)
        {
            if (resultsSuccess == null || resultsFail == null)
            {
                throw new IllegalArgumentException("resultsSuccess and resultsFail must be updated together.");
            }
            long resultsTotal = Long.valueOf(resultsSuccess.longValue() + resultsFail.longValue());
            double successRate = (resultsTotal == 0) ? 1.0 : (resultsSuccess / (double) resultsTotal);
            setObjBuilder.add(FIELD_RESULTS_SUCCESS, resultsSuccess);
            setObjBuilder.add(FIELD_RESULTS_FAIL, resultsFail);
            setObjBuilder.add(FIELD_RESULTS_TOTAL, resultsTotal);
            setObjBuilder.add(FIELD_SUCCESS_RATE, successRate);
        }
        if (resultsFail != null)
        {
            setObjBuilder.add(FIELD_RESULTS_FAIL, resultsFail);
        }
        // Check that we are actually going to do something
        if (setObjBuilder.get().keySet().size() == 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No updates provided for test run: " + runId);
            }
            return false;
        }

        // Handle version wrap-around
        Integer newVersion = version >= Short.MAX_VALUE ? 1 : version + 1;
        setObjBuilder.add(FIELD_VERSION, newVersion);
        // Get the object containing the set values
        DBObject setObj = setObjBuilder.get();

        // Now push the values to set into the update
        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .add("$set", setObj)
                .get();

        WriteResult result = testRuns.update(queryObj, updateObj);
        boolean written = (result.getN() > 0);

        // Done
        if (logger.isDebugEnabled())
        {
            if (written)
            {
                logger.debug(
                        "Updated test run state: \n" +
                                "   Run ID:    " + runId + "\n" +
                                "   Update:    " + updateObj);
            }
            else
            {
                logger.debug("Did not update test run state: " + runId);
            }
        }
        return written;
    }

    /**
     * Register a driver with a test run
     * 
     * @param runObjId
     *        the ID of the test run
     * @param driverId
     *        the ID of the driver to include
     */
    public void addTestRunDriver(ObjectId runObjId, String driverId)
    {
        // Find the test run
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(runObjId)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$addToSet")
                .add(FIELD_DRIVERS, driverId)
                .pop()
                .get();
        DBObject runObj = testRuns.findAndModify(queryObj, null, null, false, updateObj, true, false);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Added driver ID to run drivers: \n" +
                            "   Run ID:     " + runObjId + "\n" +
                            "   Driver:     " + driverId + "\n" +
                            "   Drivers:    " + runObj.get(FIELD_DRIVERS));
        }
    }

    /**
     * Derigister a driver from a test run
     * 
     * @param runObjId
     *        the ID of the test run
     * @param driverId
     *        the ID of the driver to remove
     */
    public void removeTestRunDriver(ObjectId runObjId, String driverId)
    {
        // Find the test run
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(runObjId)
                .get();
        DBObject updateObj = BasicDBObjectBuilder.start()
                .push("$pull")
                .add(FIELD_DRIVERS, driverId)
                .pop()
                .get();
        DBObject runObj = testRuns.findAndModify(queryObj, null, null, false, updateObj, true, false);

        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Removed driver ID from run drivers: \n" +
                            "   Run ID:     " + runObjId + "\n" +
                            "   Driver:     " + driverId + "\n" +
                            "   Drivers:    " + runObj.get(FIELD_DRIVERS));
        }
    }

    /**
     * Delete an existing test run
     * 
     * @param runObjId
     *        the ID of the test run
     * @return <tt>true</tt> if the test run was deleted or <tt>false</tt> if
     *         not
     */
    public boolean deleteTestRun(ObjectId runObjId)
    {
        // Get the test run
        DBObject runObj;
        try
        {
            runObj = getTestRun(runObjId, false);
        }
        catch (ObjectNotFoundException e)
        {
            logger.warn("Unable to delete test run as it does not exist: " + runObjId, e);
            return false;
        }
        ObjectId testObjId = (ObjectId) runObj.get(FIELD_TEST);

        // Find the test run
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_ID).is(runObjId)
                .get();

        WriteResult result = testRuns.remove(queryObj);
        boolean written = (result.getN() > 0);

        // Clean up properties
        DBObject propDelObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_TEST, testObjId)
                .add(FIELD_RUN, runObjId)
                .get();
        testProps.remove(propDelObj);

        // Done
        if (logger.isDebugEnabled())
        {
            if (written)
            {
                logger.debug("Deleted test run: " + queryObj);
            }
            else
            {
                logger.debug("Did not delete test run: " + runObjId);
            }
        }
        return written;
    }

    /**
     * Delete an existing test run
     * 
     * @param test
     *        the name of the test
     * @param run
     *        the run name (must exist)
     * @return <tt>true</tt> if the test run was deleted or <tt>false</tt> if
     *         not
     */
    public boolean deleteTestRun(String test, String run)
    {
        // Get the test ID
        ObjectId testObjId = getTestId(test);
        if (testObjId == null)
        {
            logger.warn("Unable to delete test run; test not found: " + test);
            return false;
        }

        // Get the run ID
        ObjectId runObjId = getTestRunId(testObjId, run);
        if (runObjId == null)
        {
            logger.warn("Unable to delete test run; run not found: " + test + "." + run);
            return false;
        }

        // Delete by ID
        boolean deleted = deleteTestRun(runObjId);

        // Done
        if (logger.isDebugEnabled())
        {
            if (deleted)
            {
                logger.debug("Deleted test run: " + test + "." + run);
            }
            else
            {
                logger.debug("Did not delete test run: " + test + "." + run);
            }
        }
        return deleted;
    }

    /**
     * Utility method to copy a DBObject
     */
    private static DBObject copyDBObject(DBObject input)
    {
        // Copy the property to a new instance
        BasicDBObjectBuilder newPropObjBuilder = BasicDBObjectBuilder.start();
        for (String fieldName : input.keySet())
        {
            Object fieldValue = input.get(fieldName);
            newPropObjBuilder.add(fieldName, fieldValue);
        }
        return newPropObjBuilder.get();
    }

    /**
     * Merges in overriding values from a collection of properties into a map
     * 
     * @param propsMap
     *        the properties to update; the map is updated in place
     * @param overridingPropObjs
     *        the properties that taken precedence
     */
    private static void mergeProperties(Map<String, DBObject> propsMap, DBCursor overridingPropObjs)
    {
        // Keep track of properties that were not overridden
        Set<String> unmerged = new HashSet<String>(propsMap.keySet());

        while (overridingPropObjs != null && overridingPropObjs.hasNext())
        {
            DBObject overridePropObj = overridingPropObjs.next();
            String key = (String) overridePropObj.get(FIELD_NAME);
            unmerged.remove(key);

            MongoTestDAO.mergeProperty(propsMap, overridePropObj);
        }

        // All the untouched properties need to have 'value' moved to 'default'
        for (String key : unmerged)
        {
            DBObject sourceObj = propsMap.get(key);
            String sourceObjValue = (String) sourceObj.get(FIELD_VALUE);
            if (sourceObjValue == null)
            {
                // There is no value. So the default remains the same.
                continue;
            }
            // Copy the object
            DBObject targetObj = copyDBObject(sourceObj);
            // Set the new default
            targetObj.put(FIELD_DEFAULT, sourceObjValue);
            // Remove the value
            targetObj.removeField(FIELD_VALUE);
            // Reset the version number
            targetObj.put(FIELD_VERSION, VERSION_ZERO);
            // Replace the object
            propsMap.put(key, targetObj);
        }
        // Done
    }

    /**
     * Merges in overriding value from an object into a map
     * 
     * @param propsMap
     *        the properties to update; the map is updated in place
     * @param overridePropObj
     *        the property that taken precedence
     */
    private static void mergeProperty(Map<String, DBObject> propsMap, DBObject overridePropObj)
    {
        String key = (String) overridePropObj.get(FIELD_NAME);
        Integer overrideVersion = (Integer) overridePropObj.get(FIELD_VERSION);
        String overrideValue = (String) overridePropObj.get(FIELD_VALUE);
        String overrideOrigin = (String) overridePropObj.get(FIELD_ORIGIN);
        DBObject propObj = propsMap.get(key);
        if (propObj == null)
        {
            // The property is not (or is no longer) relevant to the test
            return;
        }
        // Copy the property to a new instance
        DBObject newPropObj = copyDBObject(propObj);
        // If the new property already has a value, then that becomes the
        // default
        String newPropValue = (String) newPropObj.get(FIELD_VALUE);
        if (newPropValue != null)
        {
            newPropObj.put(FIELD_DEFAULT, newPropValue);
            newPropObj.removeField(FIELD_VALUE);
        }
        // Now overwrite with the overriding values
        newPropObj.put(FIELD_VERSION, overrideVersion);
        newPropObj.put(FIELD_ORIGIN, overrideOrigin);
        newPropObj.put(FIELD_VALUE, overrideValue);
        // Put that into the map
        propsMap.put(key, newPropObj);
        // Done
    }

    /**
     * Convert the map entries into a collection of DBObjects
     */
    private static BasicDBList getPropertyList(Map<String, DBObject> propsMap)
    {
        BasicDBList list = new BasicDBList();
        for (DBObject propObj : propsMap.values())
        {
            list.add(propObj);
        }
        return list;
    }

    /**
     * Get all test-specific overrides for properties
     * 
     * @param testObjId
     *        the ID of the test
     * @param runObjId
     *        the ID of the test run or <tt>null</tt> to find generic test
     *        properties
     * @return all properties for the test or test run
     */
    private DBCursor getTestPropertiesRaw(ObjectId testObjId, ObjectId runObjId)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_TEST).is(testObjId)
                .and(FIELD_RUN).is(runObjId)
                .get();
        return testProps.find(queryObj);
    }

    /**
     * Get all test-specific overrides for properties. Note that this does not
     * include any
     * inherited fields from the property definitions.
     * 
     * @param testObjId
     *        the ID of the test
     * @param runObjId
     *        the ID of the test run or <tt>null</tt> to find generic test
     *        properties
     * @param propertyName
     *        the name of the property (never <tt>null</tt>)
     * @return the property for the test run as a cursor
     */
    private DBCursor getTestPropertyRaw(ObjectId testObjId, ObjectId runObjId, String propertyName)
    {
        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_TEST).is(testObjId)
                .and(FIELD_RUN).is(runObjId)
                .and(FIELD_NAME).is(propertyName)
                .get();
        return testProps.find(queryObj);
    }

    /**
     * Retrieve the effective property for a given test, test run and property
     * name.
     * <p/>
     * This combines all values in the property inheritance hierarchy to get to
     * the value
     * applicable to the test or test run.
     * 
     * @param test
     *        the name of the test
     * @param run
     *        the name of the run or <tt>null</tt> to find the value for the
     *        test only
     * @param propertyName
     *        the name of the property (never <tt>null</tt>)
     * @return the property for the test or <tt>null</tt> if it does not exist
     */
    public DBObject getProperty(String test, String run, String propertyName)
    {
        if (propertyName == null)
        {
            throw new IllegalArgumentException("'propertyName' may not be null.");
        }

        // First see if there is a test as requested
        DBObject testObj = getTest(test, false);
        if (testObj == null)
        {
            // The test does not exist
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Could not get property for test that does not exist: \n" +
                                "   Test:      " + test + "\n" +
                                "   Run:       " + run + "\n" +
                                "   Property:  " + propertyName);
            }
            return null;
        }
        ObjectId testObjId = (ObjectId) testObj.get(FIELD_ID);

        // Get the property definition
        String release = (String) testObj.get(FIELD_RELEASE);
        Integer schema = (Integer) testObj.get(FIELD_SCHEMA);

        TestDefEntry testDefEntry = getTestDefCached(release, schema);
        if (testDefEntry == null)
        {
            logger.warn("Test definition not found: " + testObj);
            return null;
        }
        Map<String, DBObject> propsMap = new HashMap<String, DBObject>(testDefEntry.testDefPropsMap);

        // Get the overriding value from the test, if present
        DBCursor testPropsCursor = getTestPropertyRaw(testObjId, null, propertyName);
        // Combine
        MongoTestDAO.mergeProperties(propsMap, testPropsCursor);

        // Check if we want the next level of overrides
        if (run != null)
        {
            ObjectId runObjId = getTestRunId(testObjId, run);
            if (runObjId == null)
            {
                // The test no longer exists, so the run effectively doesn't
                // either
                logger.warn("Test run not found: " + test + "." + run);
                return null;
            }

            // Get the overriding value from the test, if present
            DBCursor runPropsCursor = getTestPropertyRaw(testObjId, runObjId, propertyName);
            // Combine
            MongoTestDAO.mergeProperties(propsMap, runPropsCursor);
        }

        // Pull out the property we want
        DBObject propObj = propsMap.get(propertyName);

        // Done
        if (logger.isDebugEnabled())
        {
            String msg = (propObj == null) ? "Property not found: \n" : "Found property: \n";
            logger.debug(
                    msg +
                            "   Test:      " + test + "\n" +
                            "   Run:       " + run + "\n" +
                            "   Property:  " + propObj);
        }
        return propObj;
    }

    /**
     * Override a specific test property value.
     * <p/>
     * A version number of zero indicates that there is no existing override
     * defined.<br/>
     * A value of <tt>null</tt> indicates that the existing override should be
     * removed.
     * 
     * @param test
     *        the name of the test
     * @param run
     *        the name of the test run (<tt>null</tt> to reference the test
     *        alone)
     * @param propertyName
     *        the name of the property
     * @param version
     *        the current version of the property
     * @param value
     *        the new value to set or <tt>null</tt> to remove any override
     * @throws IllegalStateException
     *         if the test has started
     */
    public boolean setPropertyOverride(String test, String run, String propertyName, int version, String value)
    {
        // Handle version wrap-around
        int newVersion = (version >= Short.MAX_VALUE) ? 1 : version + 1;

        // We need to keep the IDs
        ObjectId runObjId = null;
        ObjectId testObjId = null;
        String origin = null;

        if (run == null)
        {
            origin = TestPropertyOrigin.TEST.name();
            // Get the test
            DBObject testObj = getTest(test, false);
            if (testObj == null)
            {
                logger.warn("Unable to set property override for test as it was not found: " + test);
                return false;
            }
            // Get the ID
            testObjId = (ObjectId) testObj.get(FIELD_ID);
        }
        else
        {
            origin = TestPropertyOrigin.RUN.name();
            // Get the test run
            DBObject runObj;
            try
            {
                runObj = getTestRun(test, run, false);
            }
            catch (ObjectNotFoundException e1)
            {
                logger.warn("Test run not found: " + test + "." + run, e1);
                return false;
            }
            // Check the state of the run
            try
            {
                TestRunState runState = TestRunState.valueOf((String) runObj.get(FIELD_STATE));
                if (runState != TestRunState.NOT_SCHEDULED && runState != TestRunState.SCHEDULED)
                {
                    throw new IllegalStateException(
                            "Property overrides can only be set for test runs that have not started: \n" +
                                    "   Run:      " + runObj + "\n" +
                                    "   Property: " + propertyName);
                }
            }
            catch (IllegalArgumentException e)
            {
                logger.error("Test run state is unknown: " + runObj);
                this.deleteTestRun(runObjId);
                return false;
            }
            // Get the ID
            runObjId = (ObjectId) runObj.get(FIELD_ID);
            testObjId = (ObjectId) runObj.get(FIELD_TEST);
        }

        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_TEST).is(testObjId)
                .and(FIELD_RUN).is(runObjId)
                .and(FIELD_NAME).is(propertyName)
                .and(FIELD_VERSION).is(Integer.valueOf(version))
                .get();

        DBObject updateObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_TEST, testObjId)
                .add(FIELD_RUN, runObjId)
                .add(FIELD_NAME, propertyName)
                .add(FIELD_VERSION, Integer.valueOf(newVersion))
                .add(FIELD_VALUE, value)
                .add(FIELD_ORIGIN, origin)
                .get();

        WriteResult result = null;
        boolean written = false;
        try
        {
            if (value == null)
            {
                // remove property
                result = testProps.remove(queryObj);
                written = (result.getN() > 0);
            }
            else
            {
                // A value was provided, so either INSERT or UPDATE
                if (version == 0)
                {
                    // This indicates that no override should exist, yet
                    result = testProps.insert(updateObj);
                    written = true;
                }
                else
                {
                    // There must an update
                    result = testProps.update(queryObj, updateObj);
                    written = result.getN() > 0;
                }
            }
        }
        catch (DuplicateKeyException e)
        {
            written = false;
        }

        // Done
        if (logger.isDebugEnabled())
        {
            if (written)
            {
                logger.debug(
                        "Wrote property override: \n" +
                                "   Test:      " + test + "\n" +
                                "   Run:       " + run + "\n" +
                                "   Property:  " + propertyName + "\n" +
                                "   Version:   " + version);
            }
            else
            {
                logger.debug(
                        "Did not update property override: \n" +
                                "   Test:      " + test + "\n" +
                                "   Run:       " + run + "\n" +
                                "   Property:  " + propertyName + "\n" +
                                "   Version:   " + version);
            }
        }
        return written;
    }

    /**
     * Record a final set of locked properties for a test run. The properties
     * written will not be updatable.
     * 
     * @param testObjId
     *        the ID of the test
     * @param runObjId
     *        the ID of the test run
     * 
     * @throws ObjectNotFoundException
     */
    public void lockProperties(ObjectId testObjId, ObjectId runObjId) throws ObjectNotFoundException
    {
        // Get all the test run overrides
        DBObject runObj = getTestRun(runObjId, true);
        BasicDBList propObjs = (BasicDBList) runObj.get(FIELD_PROPERTIES);

        // First remove all current property overrides for the run
        DBObject propDelObj = BasicDBObjectBuilder
                .start()
                .add(FIELD_TEST, testObjId)
                .add(FIELD_RUN, runObjId)
                .get();
        testProps.remove(propDelObj);

        // Now add them all back
        int version = Short.MAX_VALUE + 1;              // This puts it out of
                                                        // the normal range of
                                                        // editing
        List<DBObject> insertObjList = new ArrayList<DBObject>(propObjs.size());
        for (Object obj : propObjs)
        {
            DBObject propObj = (DBObject) obj;
            String propName = (String) propObj.get(FIELD_NAME);
            String propOrigin = (String) propObj.get(FIELD_ORIGIN);
            Object defValue = propObj.get(FIELD_DEFAULT);
            Object value = propObj.get(FIELD_VALUE);
            if (value == null)
            {
                // We override ALL values
                value = defValue;
            }

            DBObject insertObj = BasicDBObjectBuilder
                    .start()
                    .add(FIELD_TEST, testObjId)
                    .add(FIELD_RUN, runObjId)
                    .add(FIELD_NAME, propName)
                    .add(FIELD_VERSION, Integer.valueOf(version))
                    .add(FIELD_ORIGIN, propOrigin)
                    .add(FIELD_VALUE, value)
                    .get();
            insertObjList.add(insertObj);
        }

        // Do the bulk insert
        try
        {
            testProps.insert(insertObjList, WriteConcern.ACKNOWLEDGED);
        }
        catch (MongoException e)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "Lost test run property overrides: \n" +
                            "   Error: " + e.getMessage() + "\n" +
                            "   Props:");
            for (Object propObj : propObjs)
            {
                sb.append("\n").append("      ").append(propObj);
            }
            String msg = sb.toString();
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Successfully fixed property overrides for run: " + runObjId);
        }
    }

    /**
     * Fetch masked property names (passwords) by release and version name.
     * 
     * @param release
     *        (String, mandatory) test release name
     * @param version
     *        (Integer) test version number
     * 
     * @return (Set<String>) or exception
     * 
     * @throws ObjectNotFoundException
     * @since 2.1.2
     */
    public Set<String> getMaskedProperyNames(String release, Integer schema) throws ObjectNotFoundException
    {
        ArgumentCheck.checkMandatoryString(release, "release");

        Set<String> result = new HashSet<String>();
        TestDefEntry testDefEntry = getTestDefCached(release, schema);
        ObjectNotFoundException.checkObject(testDefEntry, release + "-schema:" + schema);

        // Start with the properties from the test definition
        Map<String, DBObject> propsMap = new HashMap<String, DBObject>(testDefEntry.testDefPropsMap);
        for (final DBObject dbObjProp : propsMap.values())
        {
            if (isMaskedProperty(dbObjProp))
            {
                result.add((String) dbObjProp.get(FIELD_NAME));
            }
        }
        return result;
    }
    
    /**
     * Checks if the DBObject contains a field FIELD_MASK and returns true if
     * set
     * 
     * @param property
     *        (DBObject) property
     * 
     * @return true if masked, false if not or field not contained.
     */
    public boolean isMaskedProperty(DBObject property)
    {
        ArgumentCheck.checkMandatoryObject(property, "property");

        boolean result = false;
        if (property.containsField(FIELD_MASK))
        {
            Object maskObj = property.get(FIELD_MASK);
            if (null != maskObj)
            {
                if (maskObj instanceof String)
                {
                    result = maskObj.equals("true");
                }
                else
                    if (maskObj instanceof Boolean)
                    {
                        result = (Boolean) maskObj;
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unknown type of field '" + FIELD_MASK + "'");
                    }
            }
        }
        return result;
    }

    /**
     * Fetch masked property names (passwords) by test name.
     * 
     * @param testName
     *        (String, mandatory) test name
     * 
     * @return (Set<String>) or exception
     * 
     * @throws ObjectNotFoundException
     * @since 2.1.2
     */
    public Set<String> getMaskedProperyNames(String testName) throws ObjectNotFoundException
    {
        ArgumentCheck.checkMandatoryString(testName, "testName");

        DBObject queryObj = QueryBuilder
                .start()
                .and(FIELD_NAME).is(testName)
                .get();

        BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                .start(FIELD_RELEASE, true)
                .add(FIELD_SCHEMA, true);

        DBObject testObj = tests.findOne(queryObj, fieldsObjBuilder.get());
        ObjectNotFoundException.checkObject(testObj, testName);

        return getMaskedProperyNames((String) testObj.get(FIELD_RELEASE), (Integer) testObj.get(FIELD_SCHEMA));
    }

    /**
     * Imports the properties of a test from JSON.
     * 
     * @param testName (String, mandatory) test name to import properties to
     * @param importObj (DBObject, mandatory) properties to import
     * @return (DBObject) Result and message
     */
    public DBObject importTest(String testName, DBObject importObj)
    {
        // create return object
        DBObject resultObj = new BasicDBObject();
        String message = "Import succeeded.";
        ImportResult result = ImportResult.OK;

        try
        {
            ArgumentCheck.checkMandatoryString(testName, "testName");
            ArgumentCheck.checkMandatoryObject(importObj, "importObj");

            // get object IDs
            ObjectId testObjId = getTestId(testName);
            if (null == testObjId)
            {
                throw new ObjectNotFoundException(testName);
            }
            
            // get test definition
            DBObject queryObj = QueryBuilder
                    .start(FIELD_ID).is(testObjId)
                    .get();
            BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                    .start(FIELD_NAME, 1)
                    .add(FIELD_RELEASE, true)
                    .add(FIELD_SCHEMA, true);
            DBObject fieldsObj = fieldsObjBuilder.get();

            DBObject testObj = tests.findOne(queryObj, fieldsObj);
            if (testObj == null)
            {
                throw new ObjectNotFoundException(testName);
            }

            // get values from test
            String release = (String) testObj.get(FIELD_RELEASE);
            Object tmp = testObj.get(FIELD_SCHEMA);
            Integer schema = null == tmp ? 0 : Integer.valueOf(tmp.toString());

            // get properties
            Map<String, DBObject> mapProps = getTestPropertiesMap(testObjId, testName);

            // get values from the import object
            Object relObj = importObj.get(FIELD_RELEASE);
            Object schemaObj = importObj.get(FIELD_SCHEMA);
            if (null != relObj && !relObj.toString().equals(release))
            {
                result = ImportResult.WARN;
                message += "\r\nWARN: Release '"
                        + release
                        + "' from test to import doesn't match import release '"
                        + relObj.toString()
                        + "'!";
            }
            if (null != schemaObj && !schemaObj.toString().equals(schema.toString()))
            {
                result = ImportResult.WARN;
                message += "\r\nWARN: Schema '"
                        + schema
                        + "' from test to import doesn't match import schema '"
                        + schemaObj.toString()
                        + "'!";
            }
            
            // decrypt all values in the properties 
            // separate from set value - might throw exception and nothing should be changed if
            BasicDBList propsListEnc = (BasicDBList) importObj.get(FIELD_PROPERTIES);
            BasicDBList propsListDec = new BasicDBList();
            for (final Object obj : propsListEnc)
            {
                final DBObject dbObj = (DBObject) obj;
                String propName = (String) dbObj.get(FIELD_NAME);
                
                // decrypt
                DBObject prop = decryptPropertyValue(dbObj, propName);
                propsListDec.add(prop);
            }
            
            // again a loop and update the values 
            for (final Object objProp : propsListDec)
            {
                // get property
                final DBObject dbObj = (DBObject) objProp;
                String propName = (String) dbObj.get(FIELD_NAME);

                // get oldProperty
                final DBObject oldProp = mapProps.get(propName);
                if (null == oldProp)
                {
                    result = ImportResult.WARN;
                    message += "\r\nWARN: Ignored property '"
                            + propName
                            + "' not found";
                }
                else
                {
                    // see if the value differs
                    String oldValue = getPropValueAsString(oldProp);
                    String newValue = getPropValueAsString(dbObj);
                    if (!oldValue.equals(newValue))
                    {
                        // update property
                        updateProperty(testName, null, propName, newValue, oldProp);
                    }
                }
            }
        }
        catch (ObjectNotFoundException onfe)
        {
            message = "Test not found: '" + testName + "'!";
            result = ImportResult.ERROR;
            logger.error(message, onfe);

            message += "\r\n\r\n" + onfe.toString();
        }
        catch (CipherException ce)
        {
            message = "Error during decryption while import properties of test: '" + testName + "'! No value imported";
            result = ImportResult.ERROR;
            logger.error(message, ce);

            message += "\r\n\r\n" + ce.toString();
        }

        // put return values
        resultObj.put(FIELD_RESULT, result.toString());
        resultObj.put(FIELD_MESSAGE, message);

        return resultObj;
    }
    
    public DBObject importTestRun(String testName, String runName, DBObject importObj)
    {
        // create return object
        DBObject resultObj = new BasicDBObject();
        String message = "Import succeeded.";
        ImportResult result = ImportResult.OK;

        try
        {
            ArgumentCheck.checkMandatoryString(testName, "testName");
            ArgumentCheck.checkMandatoryString(runName, "runName");
            ArgumentCheck.checkMandatoryObject(importObj, "importObj");

            // get object IDs
            ObjectId testObjId = getTestId(testName);
            if (null == testObjId)
            {
                throw new ObjectNotFoundException(testName + "." + runName);
            }
            ObjectId runObjId = getTestRunId(testObjId, runName);
            if (null == runObjId)
            {
                throw new ObjectNotFoundException(testName + "." + runName);
            }
            
            // get test definition
            DBObject queryObj = QueryBuilder
                    .start(FIELD_ID).is(testObjId)
                    .get();
            BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                    .start(FIELD_NAME, 1)
                    .add(FIELD_RELEASE, true)
                    .add(FIELD_SCHEMA, true);
            DBObject fieldsObj = fieldsObjBuilder.get();

            DBObject testObj = tests.findOne(queryObj, fieldsObj);
            if (testObj == null)
            {
                throw new ObjectNotFoundException(testName + "." + runName);
            }

            // get values from test
            String release = (String) testObj.get(FIELD_RELEASE);
            Object tmp = testObj.get(FIELD_SCHEMA);
            Integer schema = null == tmp ? 0 : Integer.valueOf(tmp.toString());

            // get properties
            Map<String, DBObject> mapProps = getTestRunPropertiesMap(testObjId, runObjId, testName, runName);

            // get values from the import object
            Object relObj = importObj.get(FIELD_RELEASE);
            Object schemaObj = importObj.get(FIELD_SCHEMA);
            if (null != relObj && !relObj.toString().equals(release))
            {
                result = ImportResult.WARN;
                message += "\r\nWARN: Release '"
                        + release
                        + "' from test to import doesn't match import release '"
                        + relObj.toString()
                        + "'!";
            }
            if (null != schemaObj && !schemaObj.toString().equals(schema.toString()))
            {
                result = ImportResult.WARN;
                message += "\r\nWARN: Schema '"
                        + schema
                        + "' from test to import doesn't match import schema '"
                        + schemaObj.toString()
                        + "'!";
            }
            
            // decrypt all values in the properties 
            // separate from set value - might throw exception and nothing should be changed if
            BasicDBList propsListEnc = (BasicDBList) importObj.get(FIELD_PROPERTIES);
            BasicDBList propsListDec = new BasicDBList();
            for (final Object obj : propsListEnc)
            {
                final DBObject dbObj = (DBObject) obj;
                String propName = (String) dbObj.get(FIELD_NAME);
                
                // decrypt
                DBObject prop = decryptPropertyValue(dbObj, propName);
                propsListDec.add(prop);
            }
            
            // again a loop and update the values 
            for (final Object objProp : propsListDec)
            {
                // get property
                final DBObject dbObj = (DBObject) objProp;
                String propName = (String) dbObj.get(FIELD_NAME);

                // get oldProperty
                final DBObject oldProp = mapProps.get(propName);
                if (null == oldProp)
                {
                    result = ImportResult.WARN;
                    message += "\r\nWARN: Ignored property '"
                            + propName
                            + "' not found";
                }
                else
                {
                    // see if the value differs
                    String oldValue = getPropValueAsString(oldProp);
                    String newValue = getPropValueAsString(dbObj);
                    if (!oldValue.equals(newValue))
                    {
                        // update property
                        updateProperty(testName, runName, propName, newValue, oldProp);
                    }
                }
            }
        }
        catch (ObjectNotFoundException onfe)
        {
            message = "Test or test run not found: '" + testName + "." + runName + "'!";
            result = ImportResult.ERROR;
            logger.error(message, onfe);

            message += "\r\n\r\n" + onfe.toString();
        }
        catch (CipherException ce)
        {
            message = "Error during decryption while import properties of test run: '" + testName + "." + runName + "'! No value imported";
            result = ImportResult.ERROR;
            logger.error(message, ce);

            message += "\r\n\r\n" + ce.toString();
        }

        // put return values
        resultObj.put(FIELD_RESULT, result.toString());
        resultObj.put(FIELD_MESSAGE, message);

        return resultObj;
    }

    /**
     * Updates a property
     * 
     * @param testName
     *        (String) test name
     * @param runName
     *        (String) run name
     * @param propName
     *        (String) property name to update
     * @param newValue
     *        (String) value to set
     * @param oldProp
     *        (DBObject) current property to replace value
     */
    private void updateProperty(String testName, String runName, String propName, String newValue, DBObject oldProp)
    {
        // get the default value from the old property first        
        String oldDefault = (String)oldProp.get(FIELD_DEFAULT);
        
        // get the version from the old property
        Object objVersion = oldProp.get(FIELD_VERSION);
        String oldVersionStr = "0";
        if (null != objVersion)
        {
            oldVersionStr = objVersion.toString();
        }
        int version = Integer.valueOf(oldVersionStr);
                
        // if new value matches default -> delete else update
        if (oldDefault.equals(newValue))
        {
            newValue = null;
        }
        setPropertyOverride(testName, runName, propName, version, newValue);
    }

    /**
     * Returns the value or default of the property as string
     * 
     * @param dbPropertyObj (DBObject) property to read 
     * 
     * @return Either value (if set) or default (if present) or an empty string
     */
    public String getPropValueAsString(DBObject dbPropertyObj)
    {
        ArgumentCheck.checkMandatoryObject(dbPropertyObj, "dbPropertyObj");
        
        String result = "";
        
        Object obj = dbPropertyObj.get(FIELD_VALUE);
        if (null == obj)
        {
            obj = dbPropertyObj.get(FIELD_DEFAULT);
        }
        if (null != obj)
        {
            result = obj.toString();
        }
        
        return result;
    }

    /**
     * Exports the test with properties as JSON
     * 
     * @param testName (string, mandatory) name of the test to export
     * @return DBObject to serialize as JSON
     * 
     * @throws ObjectNotFoundException
     * @throws CipherException
     */
    public DBObject exportTest(String testName) throws ObjectNotFoundException, CipherException
    {
        ArgumentCheck.checkMandatoryString(testName, "testName");

        // get object IDs
        ObjectId testObjId = getTestId(testName);
        if (null == testObjId)
        {
            throw new ObjectNotFoundException(testName);
        }

        // get test definition
        DBObject queryObj = QueryBuilder
                .start(FIELD_ID).is(testObjId)
                .get();
        BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                .start(FIELD_NAME, 1)
                .add(FIELD_VERSION, true)
                .add(FIELD_RELEASE, true)
                .add(FIELD_SCHEMA, true);
        DBObject fieldsObj = fieldsObjBuilder.get();

        DBObject testObj = tests.findOne(queryObj, fieldsObj);
        if (testObj == null)
        {
            throw new ObjectNotFoundException(testName);
        }

        // get values from test
        String release = (String) testObj.get(FIELD_RELEASE);
        Object tmp = testObj.get(FIELD_SCHEMA);
        Integer schema = null == tmp ? 0 : Integer.valueOf(tmp.toString());
        tmp = testObj.get(FIELD_VERSION);
        Integer version = null == tmp ? 0 : Integer.valueOf(tmp.toString());

        // get properties
        Set<String> maskedProps = getMaskedProperyNames(testName);
        Map<String, DBObject> mapProps = getTestPropertiesMap(testObjId, testName);

        // encrypt passwords
        for (final String propName : maskedProps)
        {
            DBObject propDbObj = mapProps.get(propName);
            if (null != propDbObj)
            {
                // encrypt
                propDbObj = encryptPropertyValue(propDbObj, propName);
                mapProps.put(propName, propDbObj);
            }
        }

        // prepare return object
        DBObject exportObj = new BasicDBObject();
        exportObj.put(FIELD_TEST, testName);
        exportObj.put(FIELD_RELEASE, release);
        exportObj.put(FIELD_SCHEMA, schema);
        exportObj.put(FIELD_VERSION, version);

        // Turn into a map and add
        BasicDBList propsList = MongoTestDAO.getPropertyList(mapProps);
        exportObj.put(FIELD_PROPERTIES, propsList);

        return exportObj;
    }
    
    /**
     * Exports a test run with encrypted password properties by test and run
     * name.
     * 
     * @param testName
     *        (String, mandatory) test name
     * @param runName
     *        (String, mandatory) run name
     * 
     * @return (DBObject) or exception
     * 
     * @throws ObjectNotFoundException
     * @throws CipherException
     * 
     * @since 2.1.2
     */
    public DBObject exportTestRun(String testName, String runName) throws ObjectNotFoundException, CipherException
    {
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(runName, "runName");

        // get object IDs
        ObjectId testObjId = getTestId(testName);
        ObjectId runObjId = getTestRunId(testObjId, runName);
        if (null == testObjId)
        {
            throw new ObjectNotFoundException(testName + "." + runName);
        }

        // get test definition
        DBObject queryObj = QueryBuilder
                .start(FIELD_ID).is(testObjId)
                .get();
        BasicDBObjectBuilder fieldsObjBuilder = BasicDBObjectBuilder
                .start(FIELD_NAME, 1)
                .add(FIELD_VERSION, true)
                .add(FIELD_RELEASE, true)
                .add(FIELD_SCHEMA, true);
        DBObject fieldsObj = fieldsObjBuilder.get();

        DBObject testObj = tests.findOne(queryObj, fieldsObj);
        if (testObj == null)
        {
            throw new ObjectNotFoundException(testName + "." + runName);
        }

        // get values from test
        String release = (String) testObj.get(FIELD_RELEASE);
        Object tmp = testObj.get(FIELD_SCHEMA);
        Integer schema = null == tmp ? 0 : Integer.valueOf(tmp.toString());
        tmp = testObj.get(FIELD_VERSION);
        Integer version = null == tmp ? 0 : Integer.valueOf(tmp.toString());

        // get properties
        Set<String> maskedProps = getMaskedProperyNames(testName);
        Map<String, DBObject> mapProps = getTestRunPropertiesMap(testObjId, runObjId, testName, runName);

        // encrypt passwords
        for (final String propName : maskedProps)
        {
            DBObject propDbObj = mapProps.get(propName);
            if (null != propDbObj)
            {
                // encrypt
                propDbObj = encryptPropertyValue(propDbObj, propName);
                mapProps.put(propName, propDbObj);
            }
        }

        // prepare return object
        DBObject exportObj = new BasicDBObject();
        exportObj.put(FIELD_TEST, testName);
        exportObj.put(FIELD_RUN, runName);
        exportObj.put(FIELD_RELEASE, release);
        exportObj.put(FIELD_SCHEMA, schema);
        exportObj.put(FIELD_VERSION, version);

        // Turn into a map and add
        BasicDBList propsList = MongoTestDAO.getPropertyList(mapProps);
        exportObj.put(FIELD_PROPERTIES, propsList);

        return exportObj;
    }

    /**
     * Encrypts property DB object
     * 
     * @param dbObject
     *        (DBObject, mandatory)
     * @param propName
     *        (String, mandatory) name of the property
     * 
     * @return (DBObject) where fields 'FIELD_DEFAULT' and 'FIELD_VALUE' are
     *         encrypted.
     * 
     * @throws CipherException
     */
    public DBObject encryptPropertyValue(DBObject dbObject, String propName) throws CipherException
    {
        ArgumentCheck.checkMandatoryObject(dbObject, "dbObject");
        ArgumentCheck.checkMandatoryString(propName, "propName");
        
        // create a copy first
        DBObject newObj = copyDBObject(dbObject);

        // get potential value fields
        Object defObj = dbObject.get(FIELD_DEFAULT);
        Object valObj = dbObject.get(FIELD_VALUE);

        if (null != defObj)
        {
            String defValue = AESCipher.encode(propName, defObj.toString());
            newObj.put(FIELD_DEFAULT, defValue);
        }

        if (null != valObj)
        {
            String value = AESCipher.encode(propName, valObj.toString());
            newObj.put(FIELD_VALUE, value);
        }

        // store cipher version
        newObj.put(FIELD_CIPHER, CipherVersion.V1.toString());
        return newObj;
    }

    /**
     * Decrypt property if encrypted
     * 
     * @param dbObject
     *        (DBObject, mandatory) Property, that might be encrypted
     * @param propName
     *        (String, mandatory) Name of the property
     * 
     * @return (DBObject) where fields 'FIELD_DEFAULT' and 'FIELD_VALUE' are no
     *         longer encrypted.
     * 
     * @throws CipherException
     */
    public DBObject decryptPropertyValue(DBObject dbObject, String propName) throws CipherException
    {
        ArgumentCheck.checkMandatoryObject(dbObject, "dbObject");
        ArgumentCheck.checkMandatoryString(propName, "propName");

        String cipher = (String) dbObject.get(FIELD_CIPHER);
        if (null == cipher || cipher.isEmpty())
        {
            // not encrypted
            return dbObject;
        }

        // create a copy first
        DBObject newObj = copyDBObject(dbObject);

        // get potential value fields
        Object defObj = dbObject.get(FIELD_DEFAULT);
        Object valObj = dbObject.get(FIELD_VALUE);

        CipherVersion version = CipherVersion.valueOf(cipher);
        switch (version)
        {
            case NONE:
                // nothing to do
                return dbObject;

            case V1:
                if (null != defObj)
                {
                    String defValue = AESCipher.decode(propName, defObj.toString());
                    newObj.put(FIELD_DEFAULT, defValue);
                }

                if (null != valObj)
                {
                    String value = AESCipher.decode(propName, valObj.toString());
                    newObj.put(FIELD_VALUE, value);
                }
                break;

            default:
                throw new CipherException("Unknown ciper version: '" + cipher + "'");
        }

        return newObj;
    }
}
