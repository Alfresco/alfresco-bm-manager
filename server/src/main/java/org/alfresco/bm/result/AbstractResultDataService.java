package org.alfresco.bm.result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.data.ObjectsPerSecondResultData;
import org.alfresco.bm.result.data.ObjectsResultData;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.data.RuntimeResultData;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.test.LifecycleListener;
import org.alfresco.bm.util.ArgumentCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;

/**
 * Abstract basic implementation of result data service with no DB.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public abstract class AbstractResultDataService implements ResultDataService, LifecycleListener
{
    /** logger */
    protected Log logger = LogFactory.getLog(this.getClass());

    /** cache: ResultData */
    private Map<String, ObjectsPerSecondResultData> cacheObjPerSec = new HashMap<String, ObjectsPerSecondResultData>();
    private Map<String, ObjectsResultData> cacheObjRes = new HashMap<String, ObjectsResultData>();
    private Map<String, RuntimeResultData> cacheRt = new HashMap<String, RuntimeResultData>();

    @Override
    public void start() throws Exception
    {
        initializeDB();
    }

    @Override
    public void stop() throws Exception
    {

    }

    @Override
    public synchronized void notifyData(
            String bmId,
            String driverId,
            String testName,
            String testRunName,
            ResultObjectType objectType,
            ResultOperation operation,
            int numberOfObjects,
            long durationMs,
            Document bsonDesc) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryString(driverId, "driverId");
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");
        if (numberOfObjects < 1 || durationMs < 1)
        {
            throw new IllegalArgumentException("'numberOfObjects' and 'durationMs' must be positive!");
        }

        String keyRT = bmId + "-"
                + driverId + "-"
                + testRunName + "-"
                + operation.toString();
        String keyORS = keyRT
                + "-" + objectType.toString();
        if (null != bsonDesc)
        {
            keyRT += "-" + bsonDesc.toJson();
            keyORS += "-" + bsonDesc.toJson();
        }

        double objPerSecond = (double) numberOfObjects / ((double) durationMs / Double.valueOf(1000.0));

        // create initial ResultData objects
        ObjectsPerSecondResultData ops = new ObjectsPerSecondResultData(objPerSecond, objectType, bsonDesc, operation);
        ObjectsResultData obj = new ObjectsResultData(numberOfObjects, objectType, bsonDesc, operation);
        RuntimeResultData rt = new RuntimeResultData(durationMs, bsonDesc, operation);
        // create a RTAll object if operation != None
        RuntimeResultData rtAll = null;
        if (operation != ResultOperation.None)
        {
            rtAll = new RuntimeResultData(durationMs, bsonDesc, ResultOperation.None);
        }
        // check if object exists in cache
        if (this.cacheObjPerSec.containsKey(keyORS))
        {
            ObjectsPerSecondResultData cacheOps = this.cacheObjPerSec.get(keyORS);
            ops = ObjectsPerSecondResultData.combine(cacheOps, ops);
            this.cacheObjPerSec.replace(keyORS, ops);
        }
        else
        {
            // query persisted data and store to cache
            ResultData res = readData(bmId, driverId, testName, testRunName, objectType, operation, bsonDesc, ObjectsPerSecondResultData.DATA_TYPE);
            if (null != res)
            {
                ops = ObjectsPerSecondResultData.combine((ObjectsPerSecondResultData)res, ops);
            }
            this.cacheObjPerSec.put(keyORS, ops);
        }
        if (this.cacheObjRes.containsKey(keyORS))
        {
            ObjectsResultData cacheObj = this.cacheObjRes.get(keyORS);
            obj = ObjectsResultData.combine(obj, cacheObj);
            this.cacheObjRes.replace(keyORS, obj);
        }
        else
        {
            // query persisted data and store to cache
            ResultData res = readData(bmId, driverId, testName, testRunName, objectType, operation, bsonDesc, ObjectsResultData.DATA_TYPE);
            if (null != res)
            {
                obj = ObjectsResultData.combine((ObjectsResultData)res, obj);
            }
            this.cacheObjRes.put(keyORS, obj);
        }
        if (this.cacheRt.containsKey(keyRT))
        {
            RuntimeResultData cacheRt = this.cacheRt.get(keyRT);
            rt = RuntimeResultData.combine(cacheRt, rt);
            this.cacheRt.replace(keyRT, rt);
        }
        else
        {
            // query persisted data and store to cache
            ResultData res = readData(bmId, driverId, testName, testRunName, objectType, operation, bsonDesc, RuntimeResultData.DATA_TYPE);
            if (null != res)
            {
                rt = RuntimeResultData.combine((RuntimeResultData)res, rt);
            }
            this.cacheRt.put(keyRT, rt);
        }
        if (null != rtAll)
        {
            String keyRtAll = bmId + "-"
                    + driverId + "-"
                    + testRunName + "-"
                    + ResultOperation.None.toString();
            if (null != bsonDesc)
            {
                keyRtAll += "-" + bsonDesc.toJson();
            }
            if (this.cacheRt.containsKey(keyRtAll))
            {
                RuntimeResultData cacheRt = this.cacheRt.get(keyRtAll);
                rtAll = RuntimeResultData.combine(cacheRt, rtAll);
                this.cacheRt.replace(keyRtAll, rtAll);
            }
            else
            {
                // query persisted data and store to cache
                ResultData res = readData(bmId, driverId, testName, testRunName, objectType, ResultOperation.None, bsonDesc, RuntimeResultData.DATA_TYPE);
                if (null != res)
                {
                    rtAll = RuntimeResultData.combine((RuntimeResultData)res, rtAll);
                }
                this.cacheRt.put(keyRtAll, rtAll);
            }
        }

        // persist data
        writeData(ops, bmId, driverId, testName, testRunName);
        writeData(obj, bmId, driverId, testName, testRunName);
        writeData(rt, bmId, driverId, testName, testRunName);
        if (null != rtAll)
        {
            writeData(rtAll, bmId, driverId, testName, testRunName);
        }
    }

    @Override
    public List<ResultData> queryData(Document queryDoc, boolean compress) throws BenchmarkResultException
    {
        return readData(queryDoc, compress);
    }

    /**
     * Persists the data
     * 
     * @param data
     *        (ResultData, mandatory)
     * @param bmId
     *        (String, mandatory) benchmark ID
     * @param driverId
     *        (String, mandatory) driver ID
     * @param testName
     *        (String, mandatory) test name
     * @param runName
     *        (String, mandatory) test run name
     */
    protected abstract void writeData(ResultData data, String bmId, String driverId, String testName, String runName) throws BenchmarkResultException;

    /**
     * reads persisted ResultData
     * 
     * @param queryDoc
     *        (BSON Document, mandatory)
     * 
     * @param compress
     *        (boolean) if true the results from different drivers will be
     *        compressed to one "final" ResulData object
     * 
     * @return List<ResultData>
     * 
     * @throws BenchmarkResultException
     */
    protected abstract List<ResultData> readData(Document queryDoc, boolean compression)
            throws BenchmarkResultException;

    /**
     * Reads persisted data
     * 
     * @param bmId
     *        (String, mandatory) benchmark ID
     * @param driverId
     *        (String, mandatory) driver ID
     * @param testName
     *        (String, mandatory) test name
     * @param testRunName
     *        (String, mandatory) test run name
     * @param objectType
     *        (ResultObjectType) object type
     * @param operation
     *        (ResultOperation) result operation
     * @param bsonDesc
     *        (BSON Document, optional)
     * @param dataType
     *        (String, mandatory) type of ResultData to return
     *        
     * @return (ResultData) or null if no entry
     * 
     * @throws BenchmarkResultException
     *         if entry not unique
     */
    protected abstract ResultData readData(String bmId,
            String driverId,
            String testName,
            String testRunName,
            ResultObjectType objectType,
            ResultOperation operation,
            Document bsonDesc,
            String dataType) throws BenchmarkResultException;

    /**
     * Initializes the database
     */
    protected abstract void initializeDB();

    @Override
    public void flushCache()
    {
        this.cacheObjPerSec = new HashMap<String, ObjectsPerSecondResultData>();
        this.cacheObjRes = new HashMap<String, ObjectsResultData>();
        this.cacheRt = new HashMap<String, RuntimeResultData>();
    }
}
