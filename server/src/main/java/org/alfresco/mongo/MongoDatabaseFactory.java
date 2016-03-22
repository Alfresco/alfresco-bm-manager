package org.alfresco.mongo;

import org.alfresco.bm.util.ArgumentCheck;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * A Spring-friendly, <b>singleton factory</b> of {@link MongoDatabase}
 * instances. Client code should retrieve the MongoDatabase instance and keep
 * hold of it. The MongoDatabase instance will be closed by this factory when
 * the Spring context shuts down.
 * <p/>
 * The use of the Spring {@link FactoryBean} means that we can use an instance
 * of this to inject MongoDatabase instances into the DAO layers directly.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class MongoDatabaseFactory implements FactoryBean<MongoDatabase>, DisposableBean
{
    /** the MongoDB instance that will be created */
    private MongoDatabase db;

    /**
     * Constructor
     * 
     * @param mongoClient
     *        (MongoClient) the mongo client instance
     * @param databaseName
     *        (String) name of the database to create
     */
    public MongoDatabaseFactory(MongoClient mongoClient, String databaseName)
    {
        ArgumentCheck.checkMandatoryObject(mongoClient, "mongoClient");
        ArgumentCheck.checkMandatoryString(databaseName, "database");

        // Get the database
        this.db = mongoClient.getDatabase(databaseName);
    }

    @Override
    public void destroy() throws Exception
    {
    }

    @Override
    public MongoDatabase getObject() throws Exception
    {
        return this.db;
    }

    @Override
    public Class<?> getObjectType()
    {
        return DB.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
