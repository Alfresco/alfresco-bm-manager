package org.alfresco.bm.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.log.LogService;
import org.alfresco.bm.log.LogService.LogLevel;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.alfresco.bm.util.ArgumentCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;

/**
 * Test run services for logging and result report
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class TestRunService
{
    /** logger */
    protected Log logger = LogFactory.getLog(TestRunService.class);

    /** Stores the log service */
    private final LogService logService;

    /** Stores the result data service */
    private MongoTestDAO testDAO;

    /** Stores the benchmark ID */
    private String bmId;

    /** Stores the driver ID */
    private String driverId;

    /** Stores test name */
    private String testName;

    /** Stores the test run name */
    private String testRunName;

    /** lock object to synchronize */
    private Object lock = new Object();

    /**
     * Helper class to store in-between results
     * 
     * @author Frank Becker
     * @since 2.1.2
     */
    private class TempResultDataStorage
    {
        /**
         * Constructor
         */
        public TempResultDataStorage()
        {
            this.tmStart = System.currentTimeMillis();
        }

        /** Stores the start time of an ResultObjectType */
        public final long tmStart;

        /**
         * Stores the stop time of an ResultObjectType for ResultOperation.None
         */
        public long tmStopOpNone = 0;

        /**
         * Stores the stop time of an ResultObjectType for
         * ResultOperation.Created
         */
        public long tmStopOpCreated = 0;

        /**
         * Stores the stop time of an ResultObjectType for
         * ResultOperation.Deleted
         */
        public long tmStopOpDeleted = 0;

        /**
         * Stores the stop time of an ResultObjectType for
         * ResultOperation.Updated
         */
        public long tmStopOpUpdated = 0;

        /**
         * Stores the stop time of an ResultObjectType for
         * ResultOperation.Unchanged
         */
        public long tmStopOpUnchanged = 0;

        /**
         * Stores the stop time of an ResultObjectType for
         * ResultOperation.Failed
         */
        public long tmStopOpFailed = 0;

        /**
         * Stores the number of objects of an ResultObjectType for
         * ResultOperation.None
         */
        public int numOpNone = 0;

        /**
         * Stores the number of objects of an ResultObjectType for
         * ResultOperation.Created
         */
        public int numOpCreated = 0;

        /**
         * Stores the number of objects of an ResultObjectType for
         * ResultOperation.Deleted
         */
        public int numOpDeleted = 0;

        /**
         * Stores the number of objects of an ResultObjectType for
         * ResultOperation.Updated
         */
        public int numOpUpdated = 0;

        /**
         * Stores the number of objects of an ResultObjectType for
         * ResultOperation.Unchanged
         */
        public int numOpUnchanged = 0;

        /**
         * Stores the number of objects of an ResultObjectType for
         * ResultOperation.Failed
         */
        public int numOpFailed = 0;
    }

    /**
     * collection of open threads not committed. Stores
     * "ThreadName-ResultOperation"
     */
    private List<String> openThreads = new ArrayList<String>();

    /**
     * collection of open object types handled by multiple threads, not
     * committed
     */
    private Map<ResultObjectType, TempResultDataStorage> openObjectTypes = new HashMap<ResultObjectType, TempResultDataStorage>();

    private int countObjRepository = 0;
    private int countObjUnspecifiedNode = 0;
    private int countObjUser = 0;
    private int countObjSite = 0;
    private int countObjFolder = 0;
    private int countObjDocument = 0;
    private int countObjAspect = 0;
    private int countObjProperty = 0;
    private int countObjRelationship = 0;

    /**
     * Constructor
     * 
     * @param logService
     *        (LogService, mandatory) the global log service to use
     * @param testDAO
     *        (MongoTestDAO, mandatory) the MongoTestDAO
     * @param driverId
     *        (String, mandatory) driver ID (${driverId})
     * @param testName
     *        (String, mandatory) test name (${test})
     * @param testRunName
     *        (String, mandatory) test run name (${testRun})
     * @param release
     *        (String, mandatory) release version (${app.release})
     * @param schema
     *        (Integer) schema version number (${app.schema})
     * 
     * @throws BenchmarkResultException
     */
    public TestRunService(
            LogService logService,
            MongoTestDAO testDAO,
            String driverId,
            String testName,
            String testRunName) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryObject(logService, "logService");
        ArgumentCheck.checkMandatoryObject(testDAO, "testDAO");
        ArgumentCheck.checkMandatoryString(driverId, "driverId");
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");

        this.logService = logService;
        this.testDAO = testDAO;
        this.driverId = driverId;
        this.testName = testName;
        this.testRunName = testRunName;
        this.bmId = this.testDAO.getBenchmarkID(testName);

        if (logger.isDebugEnabled())
        {
            logger.debug("[TestRunService] initialized with driver ID: '" + driverId + "', test name: '" + testName
                    + ", test run name: '" + testRunName + "'.");
        }
    }

    /**
     * Opens a transaction for a specific ResultObjectType
     * 
     * @param threadName
     *        (String) name of the Thread that opens the transaction
     * @param objectType
     *        (ResultObjectType)
     * 
     * @throws BenchmarkResultException
     */
    public void openResultData(
            String threadName,
            ResultObjectType objectType) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryString(threadName, "threadName");

        String id = threadName + "-" + objectType.toString();

        synchronized (lock)
        {
            // detect if not committed
            if (openThreads.contains(id))
            {
                throw new BenchmarkResultException(
                        "Uncommited changes detected. Thread name: '"
                                + threadName
                                + "', ResultObjectType: '"
                                + objectType.toString()
                                + "'.");
            }

            // create a sufficient TempResultDataStorage object, if not exists
            if (!this.openObjectTypes.containsKey(objectType))
            {
                TempResultDataStorage storage = new TempResultDataStorage();
                this.openObjectTypes.put(objectType, storage);
            }

            // mark thread as open
            openThreads.add(id);

            // count number of open object types
            switch (objectType)
            {
                case Repository:
                    this.countObjRepository++;
                    break;

                case UnspecifiedNode:
                    this.countObjUnspecifiedNode++;
                    break;

                case User:
                    this.countObjUser++;
                    break;

                case Site:
                    this.countObjSite++;
                    break;

                case Folder:
                    this.countObjFolder++;
                    break;

                case Document:
                    this.countObjDocument++;
                    break;

                case Aspect:
                    this.countObjAspect++;
                    break;

                case Property:
                    this.countObjProperty++;
                    break;

                case Relationship:
                    this.countObjRelationship++;
                    break;

                default:
                    throw new BenchmarkResultException("Unknown 'ResultObjectType' of '" + objectType + "'!");
            }
        }
    }

    /**
     * Notification of an operation that triggers the creation of benchmark
     * results.
     * 
     * @param objectType
     *        (ResultObjectType, mandatory) type of objects affected
     * @param operation
     *        (ResultOperation, mandatory) operation executed with the object
     *        type
     * @param numberOfObjects
     *        (int, > 0) number of objects affected
     */
    public void reportResultData(
            ResultObjectType objectType,
            ResultOperation operation,
            int numberOfObjects) throws BenchmarkResultException
    {
        if (numberOfObjects < 1)
        {
            throw new BenchmarkResultException("'numberOfObjects' must be positive!");
        }
        synchronized (lock)
        {
            if (this.openThreads.isEmpty() || !this.openObjectTypes.containsKey(objectType))
            {
                throw new BenchmarkResultException("Transaction error: please make sure to 'openResultData' first!");
            }

            TempResultDataStorage storage = this.openObjectTypes.get(objectType);
            long now = System.currentTimeMillis();
            switch (operation)
            {
                case None:
                    storage.tmStopOpNone = now;
                    storage.numOpNone += numberOfObjects;
                    break;

                case Created:
                    storage.tmStopOpCreated = now;
                    storage.numOpCreated += numberOfObjects;
                    break;

                case Deleted:
                    storage.tmStopOpDeleted = now;
                    storage.numOpDeleted += numberOfObjects;
                    break;

                case Updated:
                    storage.tmStopOpUpdated = now;
                    storage.numOpUpdated += numberOfObjects;
                    break;

                case Unchanged:
                    storage.tmStopOpUnchanged = now;
                    storage.numOpUnchanged += numberOfObjects;
                    break;

                case Failed:
                    storage.tmStopOpFailed = now;
                    storage.numOpFailed += numberOfObjects;
                    break;

                default:
                    throw new BenchmarkResultException(
                            "Unknown 'ResultOperation' type '" + operation.toString() + "'!");
            }
        }
    }

    /**
     * Commits a thread-based temporary result.
     * 
     * @param threadName
     *        (String, mandatory) name of the thread, that wants to commit the
     *        value
     * 
     * @param objectType
     * @param bsonDescription
     *        (BSON Document, optional) description data field for result data
     * 
     * @throws BenchmarkResultException
     */
    public void commitResultData(
            String threadName,
            ResultObjectType objectType,
            Document bsonDescription) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryString(threadName, "threadName");

        String id = threadName + "-" + objectType.toString();

        synchronized (lock)
        {
            if (!this.openThreads.contains(id))
            {
                throw new BenchmarkResultException("No stored data found for Thread '" + threadName + "'!");
            }

            // remove from thread collection
            this.openThreads.remove(id);

            TempResultDataStorage storage = this.openObjectTypes.get(objectType);
            boolean committed = false;
            boolean error = false;

            // count number of open object types and commit data 
            switch (objectType)
            {
                case Repository:
                    this.countObjRepository--;
                    if (this.countObjRepository < 0)
                    {
                        error = true;
                        this.countObjRepository = 0;
                    }
                    if (0 == this.countObjRepository)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case UnspecifiedNode:
                    this.countObjUnspecifiedNode--;
                    if (this.countObjUnspecifiedNode < 0)
                    {
                        error = true;
                        this.countObjUnspecifiedNode = 0;
                    }
                    if (0 == this.countObjUnspecifiedNode)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case User:
                    this.countObjUser--;
                    if (this.countObjUser < 0)
                    {
                        error = true;
                        this.countObjUser = 0;
                    }
                    if (0 == this.countObjUser)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case Site:
                    this.countObjSite--;
                    if (this.countObjSite < 0)
                    {
                        error = true;
                        this.countObjSite = 0;
                    }
                    if (0 == this.countObjSite)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case Folder:
                    this.countObjFolder--;
                    if (this.countObjFolder < 0)
                    {
                        error = true;
                        this.countObjFolder = 0;
                    }
                    if (0 == this.countObjFolder)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case Document:
                    this.countObjDocument--;
                    if (this.countObjDocument < 0)
                    {
                        error = true;
                        this.countObjDocument = 0;
                    }

                    if (0 == this.countObjDocument)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case Aspect:
                    this.countObjAspect--;
                    if (this.countObjAspect < 0)
                    {
                        error = true;
                        this.countObjAspect = 0;
                    }
                    if (0 == this.countObjAspect)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case Property:
                    this.countObjProperty--;
                    if (this.countObjProperty < 0)
                    {
                        error = true;
                        this.countObjProperty = 0;
                    }
                    if (0 == this.countObjProperty)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                case Relationship:
                    this.countObjRelationship--;
                    if (this.countObjRelationship < 0)
                    {
                        error = true;
                        this.countObjRelationship = 0;
                    }
                    if (0 == this.countObjRelationship)
                    {
                        committed |= commitResult(storage, objectType, bsonDescription);
                    }
                    break;

                default:
                    throw new BenchmarkResultException("Unknown 'ResultObjectType' of '" + objectType + "'!");
            }
            
            if (committed)
            {
                // if committed - remove
                this.openObjectTypes.remove(objectType);
            }
            
            if (error)
            {
                // report internal code error ....
                throw new BenchmarkResultException("Transaction error detected. Check your code!");
            }
        }
    }

    /**
     * Do a real commit of result object type for all possible operations.
     * 
     * @param storage
     *        (TempResultDataStorage) stored result to commit
     * @param objType
     *        (ResultObjectType) type to commit for
     * @param bsonDescription
     *        (Document) BSON document to commit
     * @throws BenchmarkResultException
     */
    private boolean commitResult(TempResultDataStorage storage, ResultObjectType objType, Document bsonDescription)
            throws BenchmarkResultException
    {
        boolean commited = false;

        // do for all operations ...

        // None
        if (storage.tmStopOpNone > 0)
        {
            this.testDAO.getResultDataService().notifyData(
                    this.bmId,
                    this.driverId,
                    this.testName,
                    this.testRunName,
                    objType,
                    ResultOperation.None,
                    storage.numOpNone,
                    storage.tmStopOpNone - storage.tmStart,
                    bsonDescription);
            commited = true;
        }

        // Created
        if (storage.tmStopOpCreated > 0)
        {
            this.testDAO.getResultDataService().notifyData(
                    this.bmId,
                    this.driverId,
                    this.testName,
                    this.testRunName,
                    objType,
                    ResultOperation.Created,
                    storage.numOpCreated,
                    storage.tmStopOpCreated - storage.tmStart,
                    bsonDescription);
            commited = true;
        }

        // Deleted
        if (storage.tmStopOpDeleted > 0)
        {
            this.testDAO.getResultDataService().notifyData(
                    this.bmId,
                    this.driverId,
                    this.testName,
                    this.testRunName,
                    objType,
                    ResultOperation.Deleted,
                    storage.numOpDeleted,
                    storage.tmStopOpDeleted - storage.tmStart,
                    bsonDescription);
            commited = true;
        }

        // Updated
        if (storage.tmStopOpUpdated > 0)
        {
            this.testDAO.getResultDataService().notifyData(
                    this.bmId,
                    this.driverId,
                    this.testName,
                    this.testRunName,
                    objType,
                    ResultOperation.Updated,
                    storage.numOpUpdated,
                    storage.tmStopOpUpdated - storage.tmStart,
                    bsonDescription);
            commited = true;
        }

        // Unchanged
        if (storage.tmStopOpUnchanged > 0)
        {
            this.testDAO.getResultDataService().notifyData(
                    this.bmId,
                    this.driverId,
                    this.testName,
                    this.testRunName,
                    objType,
                    ResultOperation.Unchanged,
                    storage.numOpUnchanged,
                    storage.tmStopOpUnchanged - storage.tmStart,
                    bsonDescription);
            commited = true;
        }

        // Failed
        if (storage.tmStopOpFailed > 0)
        {
            this.testDAO.getResultDataService().notifyData(
                    this.bmId,
                    this.driverId,
                    this.testName,
                    this.testRunName,
                    objType,
                    ResultOperation.Failed,
                    storage.numOpFailed,
                    storage.tmStopOpFailed - storage.tmStart,
                    bsonDescription);
            commited = true;
        }
        return commited;
    }

    /**
     * Log a message specific to this instance
     * 
     * @param level
     *        the severity of the message
     * @param msg
     *        the log message
     */
    public void log(LogLevel level, String msg)
    {
        if (null != msg && !msg.isEmpty())
        {
            logService.log(driverId, testName, testRunName, level, msg);
        }
    }

    /**
     * Logs a TRACE message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logTrace(String msg)
    {
        log(LogLevel.TRACE, msg);
    }

    /**
     * Logs a DEBUG message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logDebug(String msg)
    {
        log(LogLevel.DEBUG, msg);
    }

    /**
     * Logs an INFO message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logInfo(String msg)
    {
        log(LogLevel.INFO, msg);
    }

    /**
     * Logs a WARN message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logWarn(String msg)
    {
        log(LogLevel.WARN, msg);
    }

    /**
     * Logs an ERROR message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logError(String msg)
    {
        log(LogLevel.ERROR, msg);
    }

    /**
     * Logs a FATAL message.
     * 
     * @param msg
     *        (String) message to log.
     */
    public void logFatal(String msg)
    {
        log(LogLevel.FATAL, msg);
    }

    /**
     * @return (String) test name
     */
    public String getTestName()
    {
        return this.testName;
    }

    /**
     * @return (String) test run name
     */
    public String getTestRunName()
    {
        return this.testRunName;
    }

    /**
     * @return (String) driver ID
     */
    public String getDriverId()
    {
        return this.driverId;
    }

    /**
     * @return (String) Benchmark ID
     */
    public String getBenchmarkId()
    {
        return this.bmId;
    }
}
