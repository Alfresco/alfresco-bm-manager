package org.alfresco.mongo;

import java.io.File;
import java.net.InetAddress;
import java.util.UUID;

import org.alfresco.bm.test.TestConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.processlistener.IMongoProcessListener;
import de.flapdoodle.embed.mongo.distribution.Version.Main;

/**
 * A factory for {@link MongoDatabase instances} in order to make use of it via
 * Spring contexts or in tests.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class MongoDatabaseForTestsFactory implements
        FactoryBean<MongoDatabase>, DisposableBean, TestConstants
{
    private Log logger = LogFactory.getLog(MongoDBForTestsFactory.class);

    private final MongodExecutable mongodExecutable;
    private final MongodProcess mongodProcess;
    private final MongoDatabase database;
    private final MongoClient mongoClient;

    public MongoDatabaseForTestsFactory() throws Exception
    {
        MongodStarter starter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Main.V3_2)
                .processListener(new MongoDatabaseProcessListener())
                .build();

        this.mongodExecutable = starter.prepare(mongodConfig);
        this.mongodProcess = mongodExecutable.start();

        // We use a randomly-assigned port, so get it
        InetAddress address = mongodProcess.getConfig().net()
                .getServerAddress();
        int port = mongodProcess.getConfig().net().getPort();

        this.mongoClient = new MongoClient(new ServerAddress(address, port));
        this.database = this.mongoClient.getDatabase(UUID.randomUUID()
                .toString());
    }

    @Override
    public void destroy() throws Exception
    {
        this.mongoClient.close();
        this.mongodProcess.stop();
        this.mongodExecutable.stop();
    }

    @Override
    public MongoDatabase getObject() throws Exception
    {
        return this.database;
    }

    @Override
    public Class<?> getObjectType()
    {
        return this.database.getClass();
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    /**
     * MongoDB log listener
     * 
     * @author Frank Becker
     * @since 2.1.2
     */
    private class MongoDatabaseProcessListener implements IMongoProcessListener
    {
        @Override
        public void onBeforeProcessStart(File dbDir, boolean dbDirIsTemp)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Starting mock MongoDB processing on " + dbDir);
            }
        }

        @Override
        public void onAfterProcessStop(File dbDir, boolean dbDirIsTemp)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Stopped mock MongoDB processing on " + dbDir);
            }
        }
    }
    
    /**
     * Utility method to build a MongoDB URI that references the {@link #getObject() DB} provided by this factory
     * 
     * @return                          a MongoDB URI that can be used to connect to the DB instance
     */
    public String getMongoURI()
    {
        String dbName = this.database.getName();
        ServerAddress mongoAddress = this.mongoClient.getAddress();
        String mongoUri = MONGO_PREFIX + mongoAddress.getHost() + ":" + mongoAddress.getPort() + "/" + dbName;
        return mongoUri;
    }
    
    /**
     * Utility method to build a MongoDB URI that can be used to construct a {@link MongoClient mongo client}.
     * 
     * @return                          a MongoDB URI that can be used to connect to mongo
     */
    public String getMongoURIWithoutDB()
    {
        ServerAddress mongoAddress = this.mongoClient.getAddress();
        String mongoUri = MONGO_PREFIX + mongoAddress.getHost() + ":" + mongoAddress.getPort();
        return mongoUri;
    }
    
    /**
     * Get a Mongo host string e.g. <b>127.0.0.1:51932</b>
     */
    public String getMongoHost()
    {
        MongoClientURI mongoClientURI = new MongoClientURI(getMongoURIWithoutDB());
        return mongoClientURI.getHosts().get(0);
    }
    
    /**
     * Utility method to build a MongoDB host string (IP:PORT)
     * 
     * @return                          the MongoDB server address
     */
    public ServerAddress getServerAddress()
    {
        return this.mongoClient.getAddress();
    }
}
