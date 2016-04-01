package org.alfresco.bm.result;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.util.ArgumentCheck;
import org.bson.Document;

/**
 * Test run result data service to be injected into the event processors 
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class TestRunResultDataService
{
    private ResultDataService resultDataService;
    private  String bmId;
    private String driverId;
    private String testName;
    private String testRunName;
    
    /**
     * Constructor 
     * 
     * @param resultDataService (ResultDataService, mandatory)
     * @param driverId (String, mandatory) Driver ID (${driverId})
     * @param testName (String, mandatory) Test name (${test})
     * @param testRunName (String, mandatory) Test run name (${testRun})
     * @param release (String, mandatory) release version (${app.release})
     * @param schema (Integer) Schema version number (${app.schema})
     * 
     * @throws BenchmarkResultException
     */
    public TestRunResultDataService(
            ResultDataService resultDataService,
            String driverId,
            String testName,
            String testRunName,
            String release,
            Integer schema) throws BenchmarkResultException
    {
        ArgumentCheck.checkMandatoryObject(resultDataService, "resultDataService");
        ArgumentCheck.checkMandatoryString(driverId, "driverId");
        ArgumentCheck.checkMandatoryString(testName, "testName");
        ArgumentCheck.checkMandatoryString(testRunName, "testRunName");
        
        this.resultDataService = resultDataService;
        this.driverId = driverId;
        this.testName = testName;
        this.testRunName = testRunName;
        this.bmId = this.resultDataService.getBenchmarkId(null, null, testName, testRunName, release, schema);
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
    public void notifyData(
            ResultObjectType objectType,
            ResultOperation operation,
            int numberOfObjects,
            long durationMs,
            Document bsonDescription) throws BenchmarkResultException
    {
        this.resultDataService.notifyData(this.bmId, this.driverId, this.testName, this.testRunName, objectType, operation, numberOfObjects, durationMs, bsonDescription);
    }
}
