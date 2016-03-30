package org.alfresco.bm.result.data;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.util.ArgumentCheck;

/**
 * Helper class to store ResultData in a cache
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class ResultDataCache
{
    private ObjectsPerSecondResultData objectsPerSecondResultData;
    private ObjectsResultData objectsResultData;
    private RuntimeResultData runtimeResultData;

    /**
     * Constructor
     * 
     * @param opsResDat
     *        (ObjectsPerSecondResultData, mandatory)
     * @param objResData
     *        (ObjectsResultData , mandatory)
     * @param rtResData
     *        (RuntimeResultData , mandatory)
     */
    public ResultDataCache(ObjectsPerSecondResultData opsResDat, ObjectsResultData objResData,
            RuntimeResultData rtResData)
    {
        this.setObjectsPerSecondResultData(opsResDat);
        this.setObjectsResultData(objResData);
        this.setRuntimeResultData(rtResData);
    }

    /**
     * @return (ObjectsPerSecondResultData)
     */
    public ObjectsPerSecondResultData getObjectsPerSecondResultData()
    {
        return objectsPerSecondResultData;
    }

    /**
     * Sets ObjectsPerSecondResultData
     * 
     * @param objectsPerSecondResultData
     *        (ObjectsPerSecondResultData, mandatory)
     */
    public void setObjectsPerSecondResultData(ObjectsPerSecondResultData objectsPerSecondResultData)
    {
        ArgumentCheck.checkMandatoryObject(objectsPerSecondResultData, "objectsPerSecondResultData");
        this.objectsPerSecondResultData = objectsPerSecondResultData;
    }

    /**
     * @return (ObjectsResultData)
     */
    public ObjectsResultData getObjectsResultData()
    {
        return objectsResultData;
    }

    /**
     * Sets ObjectsResultData
     * 
     * @param objectsResultData
     *        (ObjectsResultData, mandatory)
     */
    public void setObjectsResultData(ObjectsResultData objectsResultData)
    {
        ArgumentCheck.checkMandatoryObject(objectsResultData, "objectsResultData");
        this.objectsResultData = objectsResultData;
    }

    /**
     * @return (RuntimeResultData)
     */
    public RuntimeResultData getRuntimeResultData()
    {
        return runtimeResultData;
    }

    /**
     * Sets RuntimeResultData
     * 
     * @param runtimeResultData
     *        (RuntimeResultData, mandatory)
     */
    public void setRuntimeResultData(RuntimeResultData runtimeResultData)
    {
        ArgumentCheck.checkMandatoryObject(runtimeResultData, "runtimeResultData");
        this.runtimeResultData = runtimeResultData;
    }

    /**
     * Adds the value from the cache object to this
     * 
     * @param cacheObj
     *        (ResultDataCache, optional)
     * 
     * @throws BenchmarkResultException
     */
    public void combine(ResultDataCache cacheObj) throws BenchmarkResultException
    {
        if (null == cacheObj)
        {
            // nothing to do
            return;
        }

        setObjectsPerSecondResultData(ObjectsPerSecondResultData.combine(this.objectsPerSecondResultData,
                cacheObj.getObjectsPerSecondResultData()));
        setObjectsResultData(ObjectsResultData.combine(this.objectsResultData, cacheObj.getObjectsResultData()));
        setRuntimeResultData(RuntimeResultData.combine(this.runtimeResultData, cacheObj.getRuntimeResultData()));
    }
}
