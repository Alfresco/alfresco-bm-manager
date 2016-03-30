package org.alfresco.bm.result;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;

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
     * @param JSONDescription
     *        (String, optional) JSON description data field for result data
     */
    void notifyData(
            String bmId,
            String driverId,
            String testRunName,
            ResultObjectType objectType,
            ResultOperation operation,
            int numberOfObjects,
            long durationMs,
            String JSONDescription);

    /**
     * Gets a benchmark ID for V2 of the benchmarks.
     * 
     * Note: the method will be called when a driver registers itself and needs
     * to
     * be synchronized with all drivers!
     * 
     * @param testName
     *        (String, mandatory) name of the test that is registered by a
     *        driver
     * @param release
     *        (String, mandatory) version of the test registered by a driver
     * @param schema
     *        (Integer, mandatory) schema number of the test registered by a
     *        driver
     * 
     * @return (String) a unique identifier for the test - the same value for
     *         all drivers that register the same test name, version and schema
     *         number
     * 
     * @throws BenchmarkResultException
     */
    String getBenchmarkIdV2(String testName, String release, Integer schema) throws BenchmarkResultException;
}
