package org.alfresco.bm.result;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.data.ObjectsPerSecondResultData;
import org.alfresco.bm.result.data.ObjectsResultData;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.data.RuntimeResultData;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.mongo.MongoDatabaseForTestsFactory;
import org.alfresco.mongo.MongoResultDataService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.client.MongoDatabase;

/**
 * JUnit testing of result data service
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
@RunWith(JUnit4.class)
public class MongoResultDataServiceTest
{
    /** logger */
    private final Log logger = LogFactory.getLog(this.getClass());
    
    public final static String COL_NAME_RESULTS = "bm21.results";

    MongoDatabase mongoDatabase;
    MongoDatabaseForTestsFactory mongoDatabaseForTestsFactory;
    MongoResultDataService mongoResultDataService;

    /**
     * Creates test mongo factory and starts data result services
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.mongoDatabaseForTestsFactory = new MongoDatabaseForTestsFactory();
        this.mongoDatabase = mongoDatabaseForTestsFactory.getObject();
        this.mongoResultDataService = new MongoResultDataService(this.mongoDatabase, COL_NAME_RESULTS);
        this.mongoResultDataService.start();
    }

    /**
     * stops the data result service and shut-down mongo
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.mongoResultDataService.stop();
        this.mongoDatabaseForTestsFactory.destroy();
    
        this.mongoDatabase = null;
        this.mongoDatabaseForTestsFactory = null;
        this.mongoResultDataService = null;
    }
    
    @Test
    public void testStoreData() throws BenchmarkResultException
    {
        String bmId = "test-bm-id";
        String driverId = "test-driver-id";
        String testName = "test-test-name";
        String testRunName = "test-test-run-name";
        ResultObjectType objectType = ResultObjectType.Document;
        ResultOperation operation = ResultOperation.Created;
        int numberOfObjects = 1;
        long durationMs = 10;
        Document bsonDesc = null;
        
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        this.mongoResultDataService.notifyData(bmId, driverId, testName, testRunName, objectType, operation, numberOfObjects, durationMs, bsonDesc);
        
        // should now contain 10 objects / documents created 
        // should have 100 documents/second 
        // should have 100ms runtime in all and 100ms runtime with created objects
        
        // query document ...
        Document queryDoc = new Document(MongoResultDataService.FIELD_BM_ID, bmId);
        
        List<ResultData>results = this.mongoResultDataService.queryData(queryDoc, false);
        assertEquals(4, results.size());
        
        for(final ResultData data : results)
        {
            switch (data.getDataType())
            {
                case ObjectsPerSecondResultData.DATA_TYPE:
                    ObjectsPerSecondResultData ops = (ObjectsPerSecondResultData)data;
                    assertEquals(100.0, ops.getObjectsPerSecond(),0.0);
                    assertEquals(ResultObjectType.Document, ops.getObjectType());
                    assertEquals(ResultOperation.Created, ops.getResultOperation());
                    assertEquals(null, ops.getDescription());
                    break;

                case ObjectsResultData.DATA_TYPE:
                    ObjectsResultData obj = (ObjectsResultData)data;
                    assertEquals(10, obj.getNumberOfObjects());
                    assertEquals(ResultObjectType.Document, obj.getObjectType());
                    assertEquals(ResultOperation.Created, obj.getResultOperation());
                    assertEquals(null, obj.getDescription());
                    break;
                    
                case RuntimeResultData.DATA_TYPE:
                    RuntimeResultData rt = (RuntimeResultData)data;
                    assertEquals(100, rt.getRuntimeTicks());
                    assertEquals(null, rt.getDescription());
                    if (rt.getResultOperation() != ResultOperation.None)
                    {
                        // must be created than
                        assertEquals(ResultOperation.Created, rt.getResultOperation());
                    }
                    break;
                default:
                    logger.error("Data type extended? Check your code ...");
                    throw new BenchmarkResultException("Unknown data type '" + data.getDataType() + "'!");
            }
        }
    }
}
