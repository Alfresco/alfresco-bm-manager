/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.mongo;

import java.io.File;
import java.net.InetAddress;
import java.util.UUID;

import org.alfresco.bm.test.TestConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.processlistener.IMongoProcessListener;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version.Main;

/**
 * A factory for {@link DB MongoDB instances} in order to make use of it via Spring contexts or in tests.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class MongoDBForTestsFactory implements FactoryBean<DB>, DisposableBean, TestConstants
{
    /** Stores the Mongo version to test against.*/
    private final Main version = Main.V3_2;

    private Log logger = LogFactory.getLog(MongoDBForTestsFactory.class);
    private final MongodExecutable mongodExecutable;
    private final MongodProcess mongodProcess;
    private final DB db;
    private final MongoClient mongo;

    /**
     * Constructor
     * 
     * @throws Exception
     */
    public MongoDBForTestsFactory() throws Exception
    {
        MongodStarter starter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(version)
                .processListener(new MongoDBProcessListener())
                .build();
        
        mongodExecutable = starter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();

        // We use a randomly-assigned port, so get it
        InetAddress address = mongodProcess.getConfig().net().getServerAddress();
        int port = mongodProcess.getConfig().net().getPort();
        
        mongo = new MongoClient(new ServerAddress(address, port));
        db = mongo.getDB(UUID.randomUUID().toString());
    }
    
    @Override
    public synchronized DB getObject() throws Exception
    {
        return db;
    }
    
    /**
     * returns the Mongo version to test against
     * 
     * @since 2.1.2
     */
    public Main getMongoTestFeatureVersion()
    {
        return version;
    }
    
    /**
     * Utility method to build a MongoDB URI that references the {@link #getObject() DB} provided by this factory
     * 
     * @return                          a MongoDB URI that can be used to connect to the DB instance
     */
    public String getMongoURI()
    {
        String dbName = db.getName();
        ServerAddress mongoAddress = db.getMongo().getAddress();
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
        ServerAddress mongoAddress = db.getMongo().getAddress();
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
        return db.getMongo().getAddress();
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

    @Override
    public void destroy() throws Exception
    {
        mongo.close();
        mongodProcess.stop();
        mongodExecutable.stop();
    }
    
    /**
     * Log processes
     * 
     * @author Derek Hulley
     * @since 2.0
     */
    private class MongoDBProcessListener implements IMongoProcessListener
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
}
