package org.alfresco.bm.result;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.alfresco.bm.event.mongo.Mongo3Helper;
import org.alfresco.bm.event.mongo.MongoResultDataService;
import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.mongo.MongoDatabaseForTestsFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
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
    
    private final static String COL_NAME_DEFS = "bm.v21.defs";
    private final static String COL_NAME_RESULTS = "bm21-results";
    private final static String FIELD_VALUE_TEST_NAME = "test";
    private final static String FIELD_VALUE_RELEASE = "V1.0";
    private final static Integer FIELD_VALUE_SCHEMA = 1;

    MongoDatabase db;
    MongoDatabaseForTestsFactory factory;
    MongoResultDataService service;

    /**
     * Creates test mongo factory and starts data result services
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.factory = new MongoDatabaseForTestsFactory();
        this.db = factory.getObject();
        this.service = new MongoResultDataService(this.db, COL_NAME_DEFS, COL_NAME_RESULTS);
        this.service.start();
    }

    /**
     * stops the data result service and shut-down mongo
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.service.stop();
        this.factory.destroy();
    }

    /**
     * Makes sure a unique index is set on test name/version/schema
     */
    @Test(expected = MongoException.class)
    public void ensureIndexesDefs()
    {
        // make sure collection exists
        assertTrue(Mongo3Helper.collectionExists(this.db, COL_NAME_DEFS));

        // get collection and create test document
        MongoCollection<Document> colDefs = db.getCollection(COL_NAME_DEFS);
        Document doc = createTestDefsDocument();

        // insert first
        colDefs.insertOne(doc);
        // should throw MongoException
        colDefs.insertOne(doc);
        
        assertTrue("Expected MongoDB exception for violating unique index.", false);
    }

    /**
     * Makes sure a BM_ID is generated and can be retrieved
     */
    @Test
    public void testBmIdDefs() throws BenchmarkResultException
    {
        // get BM_ID
        String bmId = this.service.getBenchmarkIdV2(FIELD_VALUE_TEST_NAME, FIELD_VALUE_RELEASE, FIELD_VALUE_SCHEMA);
        assertNotNull(bmId);
        assertFalse(bmId.isEmpty());
        
        logger.debug("BM_ID for '" + FIELD_VALUE_TEST_NAME + "/" + FIELD_VALUE_RELEASE + "/" + FIELD_VALUE_SCHEMA + "' is '" + bmId + "'");
    }

    /**
     * Creates a simple test document for the COL_NAME_DEFS collection
     * 
     * @return (Document)
     */
    private Document createTestDefsDocument()
    {
        Document doc = new Document(MongoResultDataService.FIELD_V2_TEST_NAME, FIELD_VALUE_TEST_NAME)
                .append(MongoResultDataService.FIELD_V2_RELEASE, FIELD_VALUE_RELEASE)
                .append(MongoResultDataService.FIELD_V2_SCHEMA, FIELD_VALUE_SCHEMA);
        return doc;
    }
}
