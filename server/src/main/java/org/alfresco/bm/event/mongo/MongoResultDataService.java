package org.alfresco.bm.event.mongo;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.AbstractResultDataService;
import org.alfresco.bm.result.data.ResultData;
import org.alfresco.bm.result.defs.BenchmarkV2DataFields;
import org.alfresco.bm.result.defs.ResultDBDataFields;
import org.alfresco.bm.util.ArgumentCheck;

import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;

/**
 * MongoDB specific implementation of the result data service.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class MongoResultDataService extends AbstractResultDataService
        implements BenchmarkV2DataFields, ResultDBDataFields
{
    /** name of the collection that stores the benchmark ID */
    private final String bmDefsCollectionName;
    /** name of the collection that stores the benchmark results */
    private final String bmResultsCollectionName;
    /** DB that stores the collections */
    private final MongoDatabase db;

    private MongoCollection<Document> colDefs;
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
    public MongoResultDataService(MongoDatabase db, String bmDefsCollectionName, String bmResultsCollectionName)
    {
        ArgumentCheck.checkMandatoryObject(db, "db");
        ArgumentCheck.checkMandatoryString(bmDefsCollectionName, "bmDefsCollectionName");
        ArgumentCheck.checkMandatoryString(bmResultsCollectionName, "bmResultsCollectionName");

        this.db = db;
        this.bmDefsCollectionName = bmDefsCollectionName;
        this.bmResultsCollectionName = bmResultsCollectionName;
    }

    @Override
    public String getBenchmarkIdV2(
            String testName,
            String release,
            Integer schema) throws BenchmarkResultException
    {
        String bmId = null;

        Document query = new Document(FIELD_V2_TEST_NAME, testName)
                .append(FIELD_V2_RELEASE, release)
                .append(FIELD_V2_SCHEMA, schema);
        FindIterable<Document> results = this.colDefs.find(query);
        Document result = results.first();
        if (null == result)
        {
            // doesn't exist, create
            try
            {
                this.colDefs.insertOne(query);
            }
            catch (MongoException ex)
            {
                // another driver may have registered the test ...
                FindIterable<Document> resultsEx = this.colDefs.find(query);
                Document resultEx = resultsEx.first();
                if (null != resultEx)
                {
                    bmId = Mongo3Helper.objectIdToString(resultEx.getObjectId(FIELD_V2_BM_ID));
                }
            }
            results = this.colDefs.find(query);
            result = results.first();
        }

        // get value from result
        if (null != result)
        {
            bmId = Mongo3Helper.objectIdToString(result.getObjectId(FIELD_V2_BM_ID));
        }

        if (null == bmId)
        {
            throw new BenchmarkResultException("Unable to find a BM_ID for test '" + testName + "', version '" + release
                    + "' and schema '" + schema + "'");
        }
        return bmId;
    }

    @Override
    protected void initializeDB()
    {
        initDefs();
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
            this.colDefs.createIndex(new Document(FIELD_BM_ID, 1), optionNotUnique);
            this.colDefs.createIndex(new Document(FIELD_TEST_NAME, 1), optionNotUnique);
            this.colDefs.createIndex(new Document(FIELD_TEST_RUN_NAME, 1), optionNotUnique);
            this.colDefs.createIndex(new Document(FIELD_DRIVER_ID, 1), optionNotUnique);
        }
        else
        {
            colResults = db.getCollection(bmResultsCollectionName);
        }
    }

    /**
     * Initializes the V2 definition collection
     */
    private void initDefs()
    {
        if (!Mongo3Helper.collectionExists(db, bmDefsCollectionName))
        {
            // create and get collection
            db.createCollection(bmDefsCollectionName);
            this.colDefs = db.getCollection(bmDefsCollectionName);

            // indexing _id and compound test name/release/schema
            IndexOptions options = new IndexOptions();
            options.unique(true);
            this.colDefs.createIndex(new Document(FIELD_V2_BM_ID, 1), options);
            this.colDefs.createIndex(
                    new Document(FIELD_V2_TEST_NAME, 1).append(FIELD_V2_RELEASE, 1).append(FIELD_V2_SCHEMA, 1),
                    options);
        }
        else
        {
            colDefs = db.getCollection(bmDefsCollectionName);
        }
    }

    @Override
    protected void writeData(ResultData data, String bmId, String driverId, String runName)
    {
        Document writeDoc = data.toDocumentBSON()
                .append(FIELD_BM_ID, bmId)
                .append(FIELD_DRIVER_ID, driverId)
                .append(FIELD_TEST_RUN_NAME, runName)
                .append(FIELD_TIMESTAMP, System.currentTimeMillis());

        Document queryDoc = new Document(FIELD_BM_ID, bmId)
                .append(FIELD_DRIVER_ID, driverId)
                .append(FIELD_TEST_RUN_NAME, runName);
        // makes query unique
        data.appendQuery(queryDoc);
        
        // insert of not exists
        UpdateOptions updateOption = new UpdateOptions();
        updateOption.upsert(true);

        // do update (insert) 
        this.colResults.updateOne(queryDoc, writeDoc, updateOption);
    }

    @Override
    protected List<ResultData> readData(Document queryDoc) throws BenchmarkResultException
    {        
        List<ResultData>list = new ArrayList<ResultData>();
        
        for (final Document doc : this.colResults.find(queryDoc))
        {
            ResultData data = ResultData.create(doc);
            list.add(data);
        }
        
        return list;
    }
}
