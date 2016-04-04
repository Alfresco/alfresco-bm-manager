package org.alfresco.bm.result;

import java.util.List;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.bson.Document;

/**
 * Benchmark result data service.
 * 
 * The BM result data service collects results as "x number of documents created
 * in y ms"
 * and writes those data to a MongoDB collection that still exists even when the
 * benchmark
 * test runs are deleted to allow a compare benchmarks results with others on
 * the same
 * platform.
 * 
 * In V2.1.2 no platform definition exists and no authentication. But the
 * benchmark ID
 * will be created on the drivers test name, version and schema number. In a
 * future
 * version we may add platform and authentication to connect to the benchmark
 * ID.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public interface ResultDataService
{
    /**
     * Notification of an operation that triggers the creation of benchmark
     * results.
     * Results should be cached and updated or created and will be made
     * persistent.
     * 
     * @param bmId
     *        (String, mandatory) Benchmark ID executed to create the result
     *        data
     * @param driverId
     *        (String, mandatory) Driver ID that created the result data
     * @param testName
     *        (String, mandatory) test name
     * @param testRunName
     *        (String, mandatory) Test run name that created the result data
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
    void notifyData(
            String bmId,
            String driverId,
            String testName,
            String testRunName,
            ResultObjectType objectType,
            ResultOperation operation,
            int numberOfObjects,
            long durationMs,
            Document bsonDescription) throws BenchmarkResultException;

    /**
     * Query persisted results
     * 
     * @param queryDoc
     *        (BSON Document, mandatory) the query key/value pairs
     * @param compress
     *        (boolean) if true the results from different drivers will be
     *        compressed to one "final" ResulData object
     * 
     * @return (List<ResultData>) Collection of result data
     */
    List<ResultData> queryData(Document queryDoc, boolean compress) throws BenchmarkResultException;

    /**
     * Returns the Benchmark ID
     * 
     * @param platformId
     *        (String) if both platformId and userId is null, V2 BM ID is
     *        returned!
     * @param userId
     *        (String) if both platformId and userId is null, V2 BM ID is
     *        returned!
     * @param testName
     *        (String, mandatory) name of the test
     * @param testRunName
     *        (String, mandatory) name of the test run
     * @param release
     *        (String, mandatory) test release name
     * @param schema
     *        (Integer) schema version number
     * 
     * @return Benchmark ID for V2 or V3 of the framework
     * 
     * @throws BenchmarkResultException
     */
 /*   String getBenchmarkId(String platformId, String userId, String testName, String testRunName, String release,
            Integer schema) throws BenchmarkResultException; */

    /**
     * Clears the cached values from memory.
     */
    void flushCache();
}
