package org.alfresco.bm.result;

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
        initializeCache();
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
     * Initializes the local cache
     */
    protected void initializeCache()
    {
        // TODO
    }
    
    /**
     * Initializes the database
     */
    protected abstract void initializeDB();
    
}
