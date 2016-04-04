package org.alfresco.mongo;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.event.mongo.Mongo3Helper;
import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.AbstractResultDataService;
import org.alfresco.bm.result.data.ObjectsPerSecondResultData;
import org.alfresco.bm.result.data.ObjectsResultData;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.data.RuntimeResultData;
import org.alfresco.bm.result.defs.BenchmarkV2DataFields;
import org.alfresco.bm.result.defs.ResultDBDataFields;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.alfresco.bm.util.ArgumentCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

/**
 * MongoDB specific implementation of the result data service.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class MongoResultDataService extends AbstractResultDataService
        implements BenchmarkV2DataFields, ResultDBDataFields
{
    /** logger */
    protected Log logger = LogFactory.getLog(MongoResultDataService.class);
    
    /** name of the collection that stores the benchmark results */
    private final String bmResultsCollectionName;
    /** DB that stores the collections */
    private final MongoDatabase db;

    private MongoCollection<Document> colResults;

    /**
     * Constructor
     * 
     * @param db
     *        (MongoDatabase, mandatory) DB that stores the collections
     * @param bmDefsCollectionName
     *        (String, mandatory) name of the collection that stores the
     *        benchmark ID
     * @param bmResultsCollectionName
     *        (String, mandatory) name of the collection that stores the
     *        benchmark results
     */
    public MongoResultDataService(MongoDatabase db, String bmResultsCollectionName)
    {
        ArgumentCheck.checkMandatoryObject(db, "db");
        ArgumentCheck.checkMandatoryString(bmResultsCollectionName, "bmResultsCollectionName");

        this.db = db;
        this.bmResultsCollectionName = bmResultsCollectionName;
    }

    @Override
    protected void initializeDB()
    {
        initResults();
    }

    /**
     * Initializes the results collection
     */
    private void initResults()
    {
        if (!Mongo3Helper.collectionExists(db, bmResultsCollectionName))
        {
            // create and get collection
            db.createCollection(this.bmResultsCollectionName);
            this.colResults = db.getCollection(this.bmResultsCollectionName);

            // create indexes
            IndexOptions optionNotUnique = new IndexOptions();
            optionNotUnique.unique(false);
            this.colResults.createIndex(new Document(FIELD_BM_ID, 1), optionNotUnique);
            this.colResults.createIndex(new Document(FIELD_TEST_NAME, 1), optionNotUnique);
            this.colResults.createIndex(new Document(FIELD_TEST_RUN_NAME, 1), optionNotUnique);
            this.colResults.createIndex(new Document(FIELD_DRIVER_ID, 1), optionNotUnique);
        }
        else
        {
            colResults = db.getCollection(bmResultsCollectionName);
        }
    }

    @Override
    protected void writeData(ResultData data, String bmId, String driverId, String testName, String runName)
            throws BenchmarkResultException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "writeData(\r\n" 
                    + "data=" + data.toDocumentBSON().toJson() + "\r\n" 
                    + "bmId=" + bmId + "\r\n"
                    + "driverId=" + driverId + "\r\n"
                    + "testName=" + testName + "\r\n"
                    + "runName=" + runName + ")");
        }
        
        Document writeDoc = data.toDocumentBSON()
                .append(FIELD_BM_ID, bmId)
                .append(FIELD_DRIVER_ID, driverId)
                .append(FIELD_TEST_NAME, testName)
                .append(FIELD_TEST_RUN_NAME, runName)
                .append(FIELD_TIMESTAMP, System.currentTimeMillis());

        Document queryDoc = new Document(FIELD_BM_ID, bmId)
                .append(FIELD_DRIVER_ID, driverId)
                .append(FIELD_TEST_NAME, testName)
                .append(FIELD_TEST_RUN_NAME, runName);
        // makes query unique
        data.appendQuery(queryDoc);

        // persist data to mongo
        long num = this.colResults.count(queryDoc);
        if (num > 1)
        {
            throw new BenchmarkResultException(
                    "Query is not unique, returned " + num + " records. Query: '" + queryDoc.toJson() + "'.");
        }
        else
            if (num == 1)
            {
                this.colResults.deleteOne(queryDoc);
            }

        this.colResults.insertOne(writeDoc);
    }

    @Override
    protected List<ResultData> readData(Document queryDoc, boolean compress) throws BenchmarkResultException
    {
        if (compress)
        {
            throw new BenchmarkResultException("Compression currently not implemented!");
        }

        List<ResultData> list = new ArrayList<ResultData>();

        for (final Document doc : this.colResults.find(queryDoc))
        {
            ResultData data = ResultData.create(doc);
            list.add(data);
        }

        return list;
    }

    @Override
    protected ResultData readData(String bmId, String driverId, String testName, String testRunName,
            ResultObjectType objectType, ResultOperation operation, Document bsonDesc, String dataType)
            throws BenchmarkResultException
    {
        // create query document
        Document queryDoc = new Document(FIELD_BM_ID, bmId)
                .append(FIELD_DRIVER_ID, driverId)
                .append(FIELD_TEST_NAME, testName)
                .append(FIELD_TEST_RUN_NAME, testRunName)
                .append(ResultData.FIELD_DATA_TYPE, dataType)
                .append(ResultData.FIELD_DESCRIPTION, bsonDesc)
                .append(ResultData.FIELD_RESULT_OP, operation.toString());
        switch (dataType)
        {
            case ObjectsPerSecondResultData.DATA_TYPE:
            case ObjectsResultData.DATA_TYPE:
                queryDoc.append(ObjectsPerSecondResultData.FIELD_OBJECT_TYPE, objectType.toString());
                break;

            case RuntimeResultData.DATA_TYPE:
                // nothing to do
                break;

            default:
                throw new BenchmarkResultException("Unknown ResultData type '" + dataType + "'!");
        }

        // execute query
        List<ResultData> list = readData(queryDoc, false);

        // if unique return result
        if (list.size() == 1)
        {
            return list.get(0);
        }

        // if not unique throw exception
        if (list.size() > 1)
        {
            throw new BenchmarkResultException(
                    "Query returned more than one result ... Query document '" + queryDoc.toJson() + "'.");
        }

        // no result - return null
        return null;
    }

    @Override
    public List<Document> queryDocuments(Document queryDoc) throws BenchmarkResultException
    {
        List<Document> documents = new ArrayList<Document>();
        for (final Document doc : this.colResults.find(queryDoc))
        {
            documents.add(doc);
        }
        return documents;
    }
}
