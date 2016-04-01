package org.alfresco.bm.test;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.log.LogService;
import org.alfresco.bm.log.LogService.LogLevel;
import org.alfresco.bm.result.ResultDataService;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.util.ArgumentCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;

/**
 * Test run services for logging and result report
 * @author Frank Becker
 * @since 2.1.2
 */
public class TestRunServices
{
    /** logger */
    protected Log logger = LogFactory.getLog(TestRunServices.class);
    
    /** Stores the log service */
    private final LogService logService;
    
    /** Stores the result data service */
    private ResultDataService resultDataService;
    
    /** Stores the benchmark ID */
    private  String bmId;
    
    /** Stores the driver ID */
    private String driverId;
    
    /** Stores test name */
    private String testName;
    
    /** Stores the test run name */
    private String testRunName;

    /**
     * Constructor 
     * 
     * @param logService (LogService, mandatory) the global log service to use
     * @param resultDataService (ResultDataService, mandatory) the global result data service to use
     * @param driverId (String, mandatory) driver ID (${driverId}) 
     * @param testName (String, mandatory) test name (${test})
     * @param testRunName (String, mandatory) test run name (${testRun})
     * @param release (String, mandatory) release version (${app.release})
     * @param schema (Integer) schema version number (${app.schema})
     * 
     * @throws BenchmarkResultException
     */
    public TestRunServices(
            LogService logService, 
            ResultDataService resultDataService,
            String driverId,
            String testName,
            String testRunName,
            String release,
            Integer schema) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryObject(logService, "logService");
        ArgumentCheck.checkMandatoryObject(resultDataService, "resultDataService");
        ArgumentCheck.checkMandatoryString(driverId, "driverId");
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");
        ArgumentCheck.checkMandatoryString(release, "release");

        this.logService = logService;
        this.resultDataService = resultDataService;
        this.driverId = driverId;
        this.testName = testName;
        this.testRunName = testRunName;
        this.bmId = this.resultDataService.getBenchmarkId(null, null, testName, testRunName, release, schema);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("[" + release + "." + schema +  ".TestRunServices] initialized with driver ID: '" + driverId + "', test name: '" + testName + ", test run name: '" + testRunName + "'.");
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
    * @param durationMs
    *        (long, > 0) duration of operation in [ms]
    * @param bsonDescription
    *        (BSON Document, optional) description data field for result data
    */
   public void setResultData(
           ResultObjectType objectType,
           ResultOperation operation,
           int numberOfObjects,
           long durationMs,
           Document bsonDescription) throws BenchmarkResultException
   {
       this.resultDataService.notifyData(this.bmId, this.driverId, this.testName, this.testRunName, objectType, operation, numberOfObjects, durationMs, bsonDescription);
   }
    
    /**
     * Log a message specific to this instance
     * 
     * @param level
     *            the severity of the message
     * @param msg
     *            the log message
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
     *            (String) message to log.
     */
    public void logTrace(String msg)
    {
        log(LogLevel.TRACE, msg);
    }

    /**
     * Logs a DEBUG message.
     * 
     * @param msg
     *            (String) message to log.
     */
    public void logDebug(String msg)
    {
        log(LogLevel.DEBUG, msg);
    }

    /**
     * Logs an INFO message.
     * 
     * @param msg
     *            (String) message to log.
     */
    public void logInfo(String msg)
    {
        log(LogLevel.INFO, msg);
    }

    /**
     * Logs a WARN message.
     * 
     * @param msg
     *            (String) message to log.
     */
    public void logWarn(String msg)
    {
        log(LogLevel.WARN, msg);
    }

    /**
     * Logs an ERROR message.
     * 
     * @param msg
     *            (String) message to log.
     */
    public void logError(String msg)
    {
        log(LogLevel.ERROR, msg);
    }

    /**
     * Logs a FATAL message.
     * 
     * @param msg
     *            (String) message to log.
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
