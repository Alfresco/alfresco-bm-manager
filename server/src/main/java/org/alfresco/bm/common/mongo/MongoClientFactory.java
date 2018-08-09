/*
 * #%L
 * Alfresco Benchmark Manager
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.bm.common.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.net.URI;
import java.net.URISyntaxException;

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
    private static Log logger = LogFactory.getLog(MongoClientFactory.class);
    
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
     * @param username                  the username to use when connecting (<tt>null</tt> allowed and empty string is ignored)
     * @param password                  the user password for the database (<tt>null</tt> allowed and empty string is ignored)
     * 
     * @throws IllegalArgumentException if the arguments are null when not allowed or contain invalid information
     */
    public MongoClientFactory(MongoClientURI mongoClientURI, String username, String password)
    {
        validateMongoClientURI(mongoClientURI);

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
        
        // Reformat the URI if credentials were supplied
        if (username != null && username.length() > 0)
        {
            String userPwdCombo = username;
            if (password != null && password.length() > 0)
            {
                userPwdCombo = username + ":" + password;
            }
            String mongoClientURIstr = mongoClientURI.getURI().replace("mongodb://", "mongodb://" + userPwdCombo + "@");
            mongoClientURI = new MongoClientURI(mongoClientURIstr);
        }
        
        // Construct the client
        mongoClient = new MongoClient(mongoClientURI);
        
        // Done
        if (logger.isInfoEnabled())
        {
            logger.info("New MongoDB client created using URL: " + MongoClientFactory.toStringSafe(mongoClientURI));
        }
    }

    /**
     * Get the Mongo client that this instance holds
     * 
     * @return          the <i>same</i> MongoClient instance  
     */
    @Override
    public MongoClient getObject()
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
    public void destroy()
    {
        mongoClient.close();
    }
    
    
    /**
     * Validates MongoClientURI 
     * @param mongoClientURI {MongoClientURI} must not be null and contain valid host and (optional) port. 
     */
    private void validateMongoClientURI(MongoClientURI mongoClientURI)
    {
        // 1. Argument not optional
        if (null== mongoClientURI)
        {
            throw new IllegalArgumentException("'mongoClientURI' argument may not be null.");
        }
        
        // 2.  Validate host
        for(String host : mongoClientURI.getHosts())
        {
            // ensure not null or empty or just whitespace chars
            if ( null != host && host.trim().length() > 0)
            {
                try
                {
                    // create a URI from the host name - may throw URISyntaxException
                    URI uri = new URI("my://" + host); 
                    
                    // get host without port from URI
                    host = uri.getHost();

                    if (null == host || host.trim().length() == 0 )
                    {
                        throw new IllegalArgumentException("'mongoClientURI' argument must contain a valid host: " + mongoClientURI);
                    }
                }
                catch (URISyntaxException ex)
                {
                    // validation failed due to malformed host
                    throw new IllegalArgumentException("'mongoClientURI' argument must contain a valid host: " + mongoClientURI);
                }
            }
            else
            {
                throw new IllegalArgumentException("'mongoClientURI' argument: host is mandatory: " + mongoClientURI);
            }
        }
    }
}
