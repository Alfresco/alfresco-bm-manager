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

import java.util.UUID;

import org.alfresco.bm.test.TestConstants;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

import de.flapdoodle.embedmongo.distribution.Version;
import de.flapdoodle.embedmongo.tests.MongodForTestsFactory;

/**
 * This adaptor for the {@link MongodForTestsFactory}
 * in order to make use of it via Spring contexts.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class MongoDBForTestsFactory implements FactoryBean<DB>, DisposableBean, TestConstants
{
    private final MongodForTestsFactory mongoFactory;
    private final Mongo mongo;
    private final DB db;

    public MongoDBForTestsFactory() throws Exception
    {
        mongoFactory = MongodForTestsFactory.with(Version.Main.V2_2);
        mongo = mongoFactory.newMongo();
        db = mongo.getDB(UUID.randomUUID().toString());
    }
    
    @Override
    public synchronized DB getObject() throws Exception
    {
        return db;
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
        db.cleanCursors(true);
        mongo.close();
        mongoFactory.shutdown();
    }
}
