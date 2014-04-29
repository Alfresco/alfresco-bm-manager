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

import java.net.UnknownHostException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * A Spring-friendly, <b>singleton factory</b> of {@link MongoClient} instances.
 * The client will be closed by the factory   Client code should retrieve the
 * DB instance and keep hold of it.  The DB instance will be closed by this factory when the Spring context
 * shuts down.
 * <p/>
 * It is really simple to create mongo instances and we need control over the location of usernames/password
 * combinations i.e. in most circumstances we don't want to put sensitive information in the URL.  The
 * ability to fetch a database by name is therefore not an option.
 * <p/>
 * The use of the Spring {@link FactoryBean} means that we can use an instance of this to inject DB instances
 * into the DAO layers directly.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class MongoClientFactory implements FactoryBean<MongoClient>, DisposableBean
{
    /** The Mongo client instance that will be created */
    private MongoClient mongoClient;
    
    /**
     * Create a string that masks the username password from the uri
     * 
     * @param mongoClientURI            a mongo client URI
     * @return                          a string that masks the username password
     */
    public static String toStringSafe(MongoClientURI mongoClientURI)
    {
        String currentURI = mongoClientURI.toString();
        // Always starts with "mongodb://"
        int idx = currentURI.indexOf('@');
        if (idx < 0)
        {
            // There was no username passowrd
            return currentURI;
        }
        String newURI = "mongodb://" + "***:***" + (currentURI.substring(idx));
        return newURI;
    }
    
    /**
     * Create an instance of the factory.  The URI given must not contain a database name or user/password details.
     * This forces the client URI to be an instance that can be shared between instances of this factory.
     * 
     * @param mongoClientURI            the client URI, which <b>must not</b> reference a database, username or password
     * 
     * @throws IllegalArgumentException if the arguments are null when not allowed or contain invalid information
     */
    public MongoClientFactory(MongoClientURI mongoClientURI) throws UnknownHostException
    {
        if (mongoClientURI == null)
        {
            throw new IllegalArgumentException("'mongoClientURI' argument may not be null.");
        }
        if (mongoClientURI.getDatabase() != null)
        {
            throw new IllegalArgumentException(
                    "The provided 'mongoClientURI' instance may not reference a specific database: " + MongoClientFactory.toStringSafe(mongoClientURI));
        }
        else if (mongoClientURI.getUsername() != null)
        {
            throw new IllegalArgumentException(
                    "The provided 'mongoClientURI' instance may not reference a specific username: " + MongoClientFactory.toStringSafe(mongoClientURI));
        }
        else if (mongoClientURI.getPassword() != null)
        {
            throw new IllegalArgumentException(
                    "The provided 'mongoClientURI' instance may not reference a specific password: " + MongoClientFactory.toStringSafe(mongoClientURI));
        }
        
        // Construct the client
        mongoClient = new MongoClient(mongoClientURI);
    }

    /**
     * Get the Mongo client that this instance holds
     * 
     * @return          the <i>same</i> MongoClient instance  
     */
    @Override
    public MongoClient getObject() throws Exception
    {
        return mongoClient;
    }

    @Override
    public Class<?> getObjectType()
    {
        return MongoClient.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
    
    @Override
    public void destroy() throws Exception
    {
        mongoClient.close();
    }
}
