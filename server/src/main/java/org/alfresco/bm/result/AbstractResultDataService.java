package org.alfresco.bm.result;

import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.test.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    // TODO add cache!

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
    public synchronized void notifyData(String bmId, String driverId,
            String testRunName,
            ResultObjectType objectType, ResultOperation operation,
            int numberOfObjects, long durationMs, String JSONDescription)
    {
        // TODO
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
    protected abstract void writeData(ResultData data, String bmId, String driverId, String testName, String runName);


    /**
     * Initializes the database
     */
    protected abstract void initializeDB();

}
