/**
 * 
 */
package org.alfresco.bm.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.CommandFailureException;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * extra data report service
 * 
 * Allows one or more extra data sheets for Excel results export.
 * 
 * @author Frank Becker
 * @since 2.0.10
 */
public class DataReportServiceImpl implements LifecycleListener, DataReportService
{
    /** Log4J */
    private static Log logger = LogFactory.getLog(DataReportServiceImpl.class);
    
    /** Stores the collection name */
    public static final String COLLECTION_EXTRA_DATA = "test.extraData";
    public static final String COLLECTION_EXTRA_DATA_DESCRIPTION = "test.extraDataDescription";

    /** static field names */
    public static final String FIELD_ID = "_id";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_DRIVER_ID = "d_id";
    public static final String FIELD_TEST = "t";
    public static final String FIELD_TEST_RUN = "tr";
    public static final String FIELD_SHEET_NAME = "s";

    /** stores the DB collections to read/write */
    private DBCollection collectionExtraData;
    private DBCollection collectionDescription;

    /**
     * Constructor
     * 
     * @param db
     *            (DB, required) the database to use
     */
    public DataReportServiceImpl(DB db)
    {
        if (null == db)
        {
            throw new IllegalArgumentException("'db' is mandatory!");
        }
        try
        {
            // create collection with no options
            this.collectionExtraData = db.createCollection(COLLECTION_EXTRA_DATA, null);
            this.collectionDescription = db.createCollection(COLLECTION_EXTRA_DATA_DESCRIPTION, null);
        }
        catch (CommandFailureException e)
        {
            // try to get collection anyway - if not there, re-throw
            if (!db.collectionExists(COLLECTION_EXTRA_DATA) || !db.collectionExists(COLLECTION_EXTRA_DATA_DESCRIPTION))
            {
                throw e;
            }

            this.collectionExtraData = db.getCollection(COLLECTION_EXTRA_DATA);
            this.collectionDescription = db.getCollection(COLLECTION_EXTRA_DATA_DESCRIPTION);
        }
    }

    /**
     * check mandatory String param
     * 
     * @param paramValue
     *            (String) string value to check not null or empty
     * @param paramName
     *            (String, required) name of value to check
     */
    private void checkMandatoryString(String paramValue, String paramName)
    {
        // self-check
        if (null == paramName || paramName.isEmpty())
        {
            throw new IllegalArgumentException("'paramName' is mandatory!");
        }

        // check value
        if (null == paramValue || paramValue.isEmpty())
        {
            throw new IllegalArgumentException("'" + paramName + "' is mandatory!");
        }
    }

    /**
     * Check mandatory object param
     * 
     * @param obj
     *            (Object) object to check not to be null
     * @param objectName
     *            (String, required) Name of param / object to check not to be null
     */
    private void checkMandatoryObject(Object obj, String objectName)
    {
        checkMandatoryString(objectName, "objectName");
        if (null == obj)
        {
            throw new IllegalArgumentException("'" + objectName + "' is mandatory!");
        }
    }

    /**
     * Ensures arrays are not null and of equal size
     * 
     * @param array1
     *            (String[]) array 1 to check
     * @param array1Name
     *            (String, required) name of array 1
     * @param array2
     *            (String[]) array 2 to check
     * @param array2Name
     *            (String, required) name of array 2
     */
    private void checkBounds(String[] array1, String array1Name, String[] array2, String array2Name)
    {
        checkMandatoryObject(array1, "array1");
        checkMandatoryObject(array2, "array2");
        checkMandatoryString(array1Name, "array1Name");
        checkMandatoryString(array2Name, "array2Name");

        if (array1.length != array2.length)
        {
            throw new IllegalArgumentException("String arrays '" + array1Name + "' and '" + array2Name
                    + "' must have the same dimension!");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.bm.report.DataReportService#appendData(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public void appendData(String driverId, String test, String run, String sheetName, String[] fieldNames,
            String[] values)
    {
        // check params
        checkMandatoryString(driverId, "driverId");
        checkMandatoryString(test, "test");
        checkMandatoryString(run, "run");
        checkMandatoryString(sheetName, "sheetName");
        checkBounds(fieldNames, "fieldNames", values, "values");

        // create builder with static field values
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder.start().add(FIELD_TIME, new Date())
                .add(FIELD_DRIVER_ID, driverId).add(FIELD_TEST, test).add(FIELD_TEST_RUN, run)
                .add(FIELD_SHEET_NAME, sheetName);

        // create values
        for (int i = 0; i < fieldNames.length; i++)
        {
            String fieldName = fieldNames[i];
            String value = values[i];

            checkMandatoryString(fieldName, "fieldNames[" + i + "]");
            insertObjBuilder.add(fieldName, value);
        }

        // insert
        DBObject insertObj = insertObjBuilder.get();
        collectionExtraData.insert(insertObj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.bm.report.DataReportService#setDescription(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public void setDescription(String driverId, String test, String run, String sheetName, String[] fieldNames,
            String[] description)
    {
        // check params
        checkMandatoryString(driverId, "driverId");
        checkMandatoryString(test, "test");
        checkMandatoryString(run, "run");
        checkMandatoryString(sheetName, "sheetName");
        checkBounds(fieldNames, "fieldNames", description, "description");

        // create builder with static field values
        BasicDBObjectBuilder insertObjBuilder = BasicDBObjectBuilder.start().add(FIELD_TIME, new Date())
                .add(FIELD_DRIVER_ID, driverId).add(FIELD_TEST, test).add(FIELD_TEST_RUN, run)
                .add(FIELD_SHEET_NAME, sheetName);

        // create values
        for (int i = 0; i < fieldNames.length; i++)
        {
            String fieldName = fieldNames[i];
            String desc = description[i];

            checkMandatoryString(fieldName, "fieldNames[" + i + "]");
            insertObjBuilder.add(fieldName, desc);
        }

        // insert
        DBObject insertObj = insertObjBuilder.get();
        this.collectionDescription.insert(insertObj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.bm.report.DataReportService#getDescription(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public List<String> getDescription(String driverId, String test, String testRun, String sheetName)
    {
        // check params
        checkMandatoryString(test, "test");
        checkMandatoryString(testRun, "testRun");
        checkMandatoryString(sheetName, "sheetName");

        // create query
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (null != driverId && !driverId.isEmpty())
        {
            queryObjBuilder.add(FIELD_DRIVER_ID, driverId);
        }
        queryObjBuilder.add(FIELD_TEST, test).add(FIELD_TEST_RUN, testRun).add(FIELD_SHEET_NAME, sheetName);

        DBObject queryObj = queryObjBuilder.get();

        // find first row with description
        DBCursor cursor = this.collectionDescription.find(queryObj);
        if (null != cursor && cursor.hasNext())
        {
            DBObject row = cursor.next();
            List<String> retList = new ArrayList<String>();
            for (String key : row.keySet())
            {
                if (isValueField(key))
                {
                    retList.add((String) row.get(key));
                }
            }
            return retList;
        }

        // no description available
        return null;
    }

    /**
     * checks if the field name is a user defined value field or a value field
     * 
     * @param fieldName
     *            (String, required) field name to validate
     * 
     * @return true if not one of the predefined fixed field names, false else
     */
    public boolean isValueField(String fieldName)
    {
        checkMandatoryString(fieldName, "fieldName");

        if (fieldName.equals(FIELD_DRIVER_ID))
            return false;
        if (fieldName.equals(FIELD_ID))
            return false;
        if (fieldName.equals(FIELD_SHEET_NAME))
            return false;
        if (fieldName.equals(FIELD_TEST))
            return false;
        if (fieldName.equals(FIELD_TEST_RUN))
            return false;
        if (fieldName.equals(FIELD_TIME))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.bm.report.DataReportService#getSheetNames(java.lang.String, java.lang.String)
     */
    @Override
    public String[] getSheetNames(String driverId, String test, String testRun)
    {
        checkMandatoryString(test, "test");
        checkMandatoryString(testRun, "testRun");

        // create query
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (null != driverId && !driverId.isEmpty())
        {
            queryObjBuilder.add(FIELD_DRIVER_ID, driverId);
        }
        queryObjBuilder.add(FIELD_TEST, test);
        queryObjBuilder.add(FIELD_TEST_RUN, testRun);
        DBObject queryObj = queryObjBuilder.get();
        DBObject fieldsObj = BasicDBObjectBuilder.start().add(FIELD_ID, false).add(FIELD_TIME, true)
                .add(FIELD_DRIVER_ID, true).add(FIELD_TEST, true).add(FIELD_TEST_RUN, true).add(FIELD_SHEET_NAME, true)
                .get();

        @SuppressWarnings("rawtypes")
        List valuesList = this.collectionExtraData.find(queryObj, fieldsObj).getCollection().distinct(FIELD_SHEET_NAME);
        if (null != valuesList)
        {
            String[] sheetNames = new String[valuesList.size()];
            for (int i = 0; i < valuesList.size(); i++)
            {
                Object valueObject = valuesList.get(i);
                if (null != valueObject)
                {
                    sheetNames[i] = valueObject.toString();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Found XLSX sheet name '" + sheetNames[i]+ "'." );
                    }
                }
            }
            return sheetNames;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.bm.report.DataReportService#getData(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public DBCursor getData(String driverId, String test, String testRun, String sheetName)
    {
        // check params
        checkMandatoryString(test, "test");
        checkMandatoryString(testRun, "testRun");
        checkMandatoryString(sheetName, "sheetName");

        // create query
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (null != driverId && !driverId.isEmpty())
        {
            queryObjBuilder.add(FIELD_DRIVER_ID, driverId);
        }
        queryObjBuilder.add(FIELD_TEST, test).add(FIELD_TEST_RUN, testRun).add(FIELD_SHEET_NAME, sheetName);
        DBObject queryObj = queryObjBuilder.get();

        // return cursor
        return this.collectionExtraData.find(queryObj);
    }

    @Override
    public void start() throws Exception
    {
        checkIndexes();
    }

    @Override
    public void stop() throws Exception
    {
    }

    /**
     * Ensure that the MongoDB collection has the required indexes
     */
    private void checkIndexes()
    {
        // COLLECTION_EXTRA_DATA

        // Ensure ordering
        DBObject idxTime = BasicDBObjectBuilder.start().add(FIELD_TIME, -1).get();
        DBObject optTime = BasicDBObjectBuilder.start().add("unique", Boolean.FALSE).get();
        this.collectionExtraData.createIndex(idxTime, optTime);

        // Select by driver, order by time
        DBObject idxDriverTime = BasicDBObjectBuilder.start().add(FIELD_DRIVER_ID, 1).add(FIELD_TIME, -1).get();
        DBObject optDriverTime = BasicDBObjectBuilder.start().add("unique", Boolean.FALSE).get();
        this.collectionExtraData.createIndex(idxDriverTime, optDriverTime);

        // Select by test, order by time
        DBObject idxTestTime = BasicDBObjectBuilder.start().add(FIELD_TEST, 1).add(FIELD_TIME, -1).get();
        DBObject optTestTime = BasicDBObjectBuilder.start().add("unique", Boolean.FALSE).get();
        this.collectionExtraData.createIndex(idxTestTime, optTestTime);

        // Select by test run, order by time
        DBObject idxTestRunTime = BasicDBObjectBuilder.start().add(FIELD_TEST_RUN, 1).add(FIELD_TIME, -1).get();
        DBObject optTestRunTime = BasicDBObjectBuilder.start().add("unique", Boolean.FALSE).get();
        this.collectionExtraData.createIndex(idxTestRunTime, optTestRunTime);

        // COLLECTION_EXTRA_DATA_DESCRIPTION

        // Select by driver, test and run
        DBObject order = BasicDBObjectBuilder.start().add(FIELD_DRIVER_ID, 1).add(FIELD_TEST, 1).add(FIELD_TEST_RUN, 1)
                .get();
        DBObject opt = BasicDBObjectBuilder.start().add("unique", Boolean.TRUE).get();
        this.collectionDescription.createIndex(order, opt);
    }

    @Override
    public List<String> getNextValueRow(DBCursor cursor)
    {
        if (null != cursor && cursor.hasNext())
        {
            DBObject row = cursor.next();
            List<String> retList = new ArrayList<String>();
            for (String key : row.keySet())
            {
                if (isValueField(key))
                {
                    retList.add((String) row.get(key));
                }
            }
            return retList;
        }
        return null;
    }
}
