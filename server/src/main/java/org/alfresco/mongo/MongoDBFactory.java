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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * A Spring-friendly, <b>singleton factory</b> of {@link DB} instances.
 * Client code should retrieve the DB instance and keep hold of it.
 * The DB instance will be closed by this factory when the Spring context shuts down.
 * <p/>
 * The use of the Spring {@link FactoryBean} means that we can use an instance of this to inject DB instances
 * into the DAO layers directly.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class MongoDBFactory implements FactoryBean<DB>, DisposableBean
{
    /** the Mongo DB instance that will be created */
    private DB db;
    
    /**
     * Create an instance of the factory.  The URI given must not contain a database name or user/password details.
     * This forces the client URI to be an instance that can be shared between instances of this factory.
     * 
     * @param mongoClient               the mongo client
     * @param database                  the database to connect to (never <tt>null</tt>)
     * @param username                  the username to use when connecting (<tt>null</tt> allowed and empty string is ignored)
     * @param password                  the user password for the database (<tt>null</tt> allowed and empty string is ignored)
     * 
     * @throws IllegalArgumentException if the arguments are null when not allowed or contain invalid information
     */
    public MongoDBFactory(MongoClient mongoClient, String database, String username, String password)
    {
        if (mongoClient == null || database == null)
        {
            throw new IllegalArgumentException("'mongoClientURI' and 'database' arguments may not be null.");
        }
        
        // Handle empty strings
        if (username != null && username.isEmpty())
        {
            username = null;
        }
        if (password != null && password.isEmpty())
        {
            password = null;
        }
        
        // Get the database
        this.db = mongoClient.getDB(database);
        if (username != null)
        {
            boolean authenticated = db.authenticate(username, password.toCharArray());
            if (!authenticated)
            {
                throw new RuntimeException(
                        "MongoDB authentication failed when connecting to " + mongoClient.getAddress() + " " + database + " for user " + username);
            }
        }
    }
    
    @Override
    public void destroy() throws Exception
    {
        db.cleanCursors(true);
    }

    /**
     * Get the database that this instance is produces.  The calling code need not shut down the database
     * as it will be done by the factory.
     * 
     * @return          a <i>singleton</i> DB instance  
     */
    @Override
    public DB getObject() throws Exception
    {
        return db;
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
