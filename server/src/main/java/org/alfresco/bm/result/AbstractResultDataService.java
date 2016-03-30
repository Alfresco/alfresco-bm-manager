package org.alfresco.bm.result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.data.ObjectsPerSecondResultData;
import org.alfresco.bm.result.data.ObjectsResultData;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.data.ResultDataCache;
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
    private Map<String, ResultDataCache> cacheResultData = new HashMap<String, ResultDataCache>();

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
            String testRunName,
            ResultObjectType objectType,
            ResultOperation operation,
            int numberOfObjects,
            long durationMs,
            Document bsonDescription) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryString(bmId, "bmId");
        ArgumentCheck.checkMandatoryString(driverId, "driverId");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");

        String key = bmId + "-"
                + driverId + "-"
                + testRunName + "-"
                + objectType.toString() + "-"
                + operation.toString();
        if (null != bsonDescription)
        {
            key += "-" + bsonDescription.toJson();
        }

        double objectsPerSecond = (double) numberOfObjects / ((double) durationMs / Double.valueOf(1000.0));

        // create initial ResultDataCache object
        ObjectsPerSecondResultData ops = new ObjectsPerSecondResultData(objectsPerSecond, objectType, bsonDescription,
                operation);
        ObjectsResultData obj = new ObjectsResultData(numberOfObjects, objectType, bsonDescription, operation);
        RuntimeResultData rt = new RuntimeResultData(durationMs, bsonDescription, operation);
        ResultDataCache cacheObj = new ResultDataCache(ops, obj, rt);

        // check if object exists in cache
        if (this.cacheResultData.containsKey(key))
        {
            ResultDataCache cachedObj = this.cacheResultData.get(key);
            cachedObj.combine(cacheObj);
            cacheObj = cachedObj;
        }
        else
        {
            ResultDataCache cachedObj = queryResultDataCache(bmId, driverId, testRunName, objectType, operation,
                    bsonDescription);
            cacheObj.combine(cachedObj);
            this.cacheResultData.put(key, cacheObj);
        }

        // persist data
        writeData(cacheObj.getObjectsPerSecondResultData(), bmId, driverId, testRunName);
        writeData(cacheObj.getObjectsResultData(), bmId, driverId, testRunName);
        writeData(cacheObj.getRuntimeResultData(), bmId, driverId, testRunName);
    }

    /**
     * Try to get object to cache from persisted data
     * 
     * @param bmId
     *        (String, mandatory) Benchmark ID executed to create the result
     *        data
     * @param driverId
     *        (String, mandatory) Driver ID that created the result data
     * @param testRunName
     *        (String, mandatory) Test run name that created the result data
     * @param objectType
     *        (ResultObjectType, mandatory) type of objects affected
     * @param operation
     *        (ResultOperation, mandatory) operation executed with the object
     *        type
     * @param bsonDescription
     *        (BSON Document, optional) description data field for result data
     * 
     * @return (ResultDataCache or null)
     * 
     * @throws BenchmarkResultException
     */
    private ResultDataCache queryResultDataCache(
            String bmId,
            String driverId,
            String testRunName,
            ResultObjectType objectType,
            ResultOperation operation,
            Document bsonDescription) throws BenchmarkResultException
    {
        Document queryDoc = new Document();
        List<ResultData> list = readData(queryDoc);
        if (!list.isEmpty())
        {
            if (list.size() == 3)
            {
                ObjectsPerSecondResultData ops = null;
                ObjectsResultData obj = null;
                RuntimeResultData rt = null;
                for (final ResultData data : list)
                {
                    switch (data.getDataType())
                    {
                        case ObjectsPerSecondResultData.DATA_TYPE:
                            ops = (ObjectsPerSecondResultData)data;
                            break;
                            
                        case ObjectsResultData.DATA_TYPE:
                            obj = (ObjectsResultData)data;
                            break;

                        case RuntimeResultData.DATA_TYPE:
                            rt = (RuntimeResultData)data;
                            break;
                        default:
                            throw new BenchmarkResultException("Unknown data type '" + data.getDataType() + "'.");
                    }
                }
                
                return new ResultDataCache(ops, obj, rt);
            }
            else
            {
                throw new BenchmarkResultException("Query '"
                        + queryDoc.toJson()
                        + "' returned "
                        + list.size()
                        + " ResultData objects - expected 3 results!");
            }
        }
        return null;
    }

    @Override
    public List<ResultData> queryData(Document queryDoc, boolean compress) throws BenchmarkResultException
    {
        // TODO compression
        if (compress)
        {
            throw new BenchmarkResultException("Compression currently not implemented!");
        }
        return readData(queryDoc);
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
     * @param runName
     *        (String, mandatory) test run name
     */
    protected abstract void writeData(ResultData data, String bmId, String driverId, String runName);

    /**
     * reads persisted ResultData
     * 
     * @param queryDoc
     *        (BSON Document, mandatory)
     * 
     * @return List<ResultData>
     * 
     * @throws BenchmarkResultException
     */
    protected abstract List<ResultData> readData(Document queryDoc) throws BenchmarkResultException;

    /**
     * Initializes the database
     */
    protected abstract void initializeDB();

    @Override
    public void flushCache()
    {
        cacheResultData = new HashMap<String, ResultDataCache>();
    }
}
