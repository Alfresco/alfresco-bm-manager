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
package org.alfresco.bm.mongo;

import org.alfresco.mongo.MongoClientFactory;
import org.alfresco.mongo.MongoDBFactory;
import org.alfresco.mongo.MongoDBForTestsFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoSocketException;

/**
 * @see MongoDBFactory
 * @see MongoClientFactory
 * @see MongoDBForTestsFactory
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class MongoFactoriesTest
{
    /**
     * Check safe {@link MongoClientURI#toString()}
     */
    @Test
    public void testSafeToStringForMongoClientURI() throws Exception
    {
        MongoClientURI uriOne = new MongoClientURI("mongodb://168.10.0.4:27017/fred");
        Assert.assertEquals(uriOne.toString(), MongoClientFactory.toStringSafe(uriOne));

        MongoClientURI uriTwo = new MongoClientURI("mongodb://admin:admin@168.10.0.4:27017/fred");
        Assert.assertEquals("mongodb://***:***@168.10.0.4:27017/fred", MongoClientFactory.toStringSafe(uriTwo));
    }
    
    @Test
    public void testMockDBLifecycle() throws Exception
    {
        MongoDBForTestsFactory factory = new MongoDBForTestsFactory();
        DB db = factory.getObject();
        Assert.assertTrue("Expect shared singleton", db == factory.getObject());
        // Make sure we can use it
        db.getCollectionNames();
        // Make sure that the URI generated is valid as far as MongoDB is concerned
        new MongoClientURI(factory.getMongoURI());
        factory.destroy();
        // Make sure that the DB has been shut down
        try
        {
            db.getCollectionNames();
            Assert.fail("DB must be closed.");
        }
        catch (MongoSocketException e)
        {
            // Expected
        }
    }
    
    @Test
    public void testMongoClientFactory() throws Exception
    {
        // Check that we kick out URIs with credentials
        try
        {
            new MongoClientFactory(new MongoClientURI("mongodb://jack:daw@168.10.0.4:27017"), null, null);
        }
        catch (IllegalArgumentException e)
        {
            // Expected
            Assert.assertTrue(e.getMessage().contains("***:***@"));
            Assert.assertTrue(e.getMessage().contains("username"));
        }
        // Check that we kick out URIs with DB name
        try
        {
            new MongoClientFactory(new MongoClientURI("mongodb://168.10.0.4:27017/data"), null, null);
        }
        catch (IllegalArgumentException e)
        {
            // Expected
            Assert.assertTrue(e.getMessage().contains("/data"));
            Assert.assertTrue(e.getMessage().contains("specific database"));
        }
        // But we must not get confused by the optional options
        new MongoClientFactory(new MongoClientURI("mongodb://168.10.0.4:27017/?safe=true"), null, null).destroy();
        
        // Get a DB running
        MongoDBForTestsFactory mockDBFactory = new MongoDBForTestsFactory();
        try
        {
            String uriWithDB = mockDBFactory.getMongoURI();
            int idx = uriWithDB.lastIndexOf("/");
            String uriWithoutDB = uriWithDB.substring(0, idx);
            MongoClientURI mongoClientURI = new MongoClientURI(uriWithoutDB);
            Assert.assertEquals(uriWithoutDB, mockDBFactory.getMongoURIWithoutDB());

            // Now connect to the DB
            MongoClientFactory clientFactoryOne = new MongoClientFactory(mongoClientURI, null, null);
            MongoClientFactory clientFactoryTwo = new MongoClientFactory(mongoClientURI, null, null);
            
            // Each of the factories must produce distinct instances
            MongoClient clientOne = clientFactoryOne.getObject();
            MongoClient clientTwo = clientFactoryTwo.getObject();
            Assert.assertTrue(clientOne != clientTwo);
            Assert.assertTrue(clientOne == clientFactoryOne.getObject());
            Assert.assertTrue(clientTwo == clientFactoryTwo.getObject());
            
            // Use them
            DB dbOneA = clientOne.getDB("oneA");
            DB dbOneB = clientOne.getDB("oneB");
            DB dbTwoA = clientTwo.getDB("twoA");
            DB dbTwoB = clientTwo.getDB("twoB");
            
            dbOneA.getStats();
            dbOneB.getStats();
            dbTwoA.getStats();
            dbTwoB.getStats();
            
            // Shutdown the client factory
            clientFactoryTwo.destroy();
            dbOneA.getStats();
            dbOneB.getStats();
            try
            {
                dbTwoA.getStats();
                Assert.fail("Second set of DBs did not close with second client factory.");
            }
            catch (IllegalStateException e)
            {
                // Expected
            }
            try
            {
                dbTwoB.getStats();
                Assert.fail("Second set of DBs did not close with second client factory.");
            }
            catch (IllegalStateException e)
            {
                // Expected
            }
            // Cleanup the last factory
            clientFactoryOne.destroy();
        }
        finally
        {
            mockDBFactory.destroy();
        }
    }
    
    @Test
    public void testMongoDBFactory() throws Exception
    {
        // Get a DB running
        MongoDBForTestsFactory mockDBFactory = new MongoDBForTestsFactory();
        try
        {
            String uriWithDB = mockDBFactory.getMongoURI();
            int idx = uriWithDB.lastIndexOf("/");
            String uriWithoutDB = uriWithDB.substring(0, idx);
            MongoClientURI mongoClientURI = new MongoClientURI(uriWithoutDB);

            // Connect without username:password
            MongoClientFactory clientFactory = new MongoClientFactory(mongoClientURI, null, null);
            try
            {
                MongoClient client = clientFactory.getObject();
                String credentials = client.getCredentialsList().toString();
                Assert.assertEquals("[]", credentials);
                
                MongoDBFactory dbFactory = new MongoDBFactory(client, "fred");
                try
                {
                    DB db = dbFactory.getObject();
                    Assert.assertTrue(db == dbFactory.getObject());
                    Assert.assertEquals("fred", db.getName());
                    db.getCollectionNames();
                }
                finally
                {
                    dbFactory.destroy();
                }
            }
            finally
            {
                clientFactory.destroy();
            }

            // Connect with username:password
            // This is merely checking that the URL generated has the credentials in it
            clientFactory = new MongoClientFactory(mongoClientURI, "ifa", "tiger");
            try
            {
                MongoClient client = clientFactory.getObject();
                String credentials = client.getCredentialsList().toString();
                // MongoDB 3.0 changed auth ...
                //Assert.assertEquals("[MongoCredential{mechanism='MONGODB-CR', userName='ifa', source='admin', password=<hidden>, mechanismProperties={}}]", credentials);
                Assert.assertTrue(credentials.endsWith("userName='ifa', source='admin', password=<hidden>, mechanismProperties={}}]"));
            }
            finally
            {
                clientFactory.destroy();
            }
        }
        finally
        {
            mockDBFactory.destroy();
        }
    }
    
    @Test
    public void testSpringEnabled() throws Exception
    {
        // Get a DB running
        MongoDBForTestsFactory mockDBFactory = new MongoDBForTestsFactory();
        try
        {
            String uriWithDB = mockDBFactory.getMongoURI();
            int idx = uriWithDB.lastIndexOf("/");
            String uriWithoutDB = uriWithDB.substring(0, idx);
            System.setProperty("mongo.uri.test", uriWithoutDB);
            
            // Count threads
            int threadCount = Thread.activeCount();
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("test-MongoFactoriesTest-context.xml");
            Assert.assertTrue(Thread.activeCount() > threadCount);
            
            // Get the DB
            
            ctx.close();
            
            // avoid failing test because Threads are not aligned/synchronized
            int count = 0;
            while (threadCount != Thread.activeCount())
            {
                Thread.sleep(1000);
                count ++;
                if (count > 10 )
                {
                    // wait a max. of 10 seconds ... than fail test
                    break; 
                }
            }
            Assert.assertEquals("Not all threads killed", threadCount, Thread.activeCount());
        }
        finally
        {
            mockDBFactory.destroy();
        }
    }
}
