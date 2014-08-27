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
package org.alfresco.bm.user;

import java.util.Collections;
import java.util.Properties;

import org.alfresco.bm.data.DataCreationState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import com.mongodb.DuplicateKeyException;

/**
 * @see UserDataService
 * 
 * @author Derek Hulley
 * @since 1.3
 */
@RunWith(JUnit4.class)
public class UserDataServiceTest
{
    private final static String COLLECTION_BM_USER_DATA_SERVICE = "BenchmarkUserDataServiceTest";
    
    private final static String[] USERS = new String[] {"fsmith", "jblogg", "bjones", "msimonds", "ddevries"};

    private static AbstractApplicationContext ctx;
    private static UserDataService userDataService;

    @Before
    public void setUp()
    {
        Properties props = new Properties();
        props.put("mongoCollection", COLLECTION_BM_USER_DATA_SERVICE);
        
        ctx = new ClassPathXmlApplicationContext(new String[] {"test-MongoUserDataTest-context.xml"}, false);
        ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("TestProps", props));
        ctx.refresh();
        ctx.start();
        userDataService = ctx.getBean(UserDataService.class);

        // Generate some random users
        for (int i = 0; i < USERS.length; i++)
        {
            String username = USERS[i];
            UserData user = createUserData(username);
            userDataService.createNewUser(user);
        }
    }
    
    @After
    public void tearDown()
    {
        ctx.close();
    }
    
    /**
     * Create user data object using the username provided
     */
    private static UserData createUserData(String username)
    {
        String first = "" + System.currentTimeMillis();
        String last = "" + System.currentTimeMillis();
        
        UserData user = new UserData();
        user.setUsername(username);
        user.setCreationState(DataCreationState.NotScheduled);
        user.setDomain("example.com");
        user.setEmail(username + "@example.com");
        user.setFirstName(first);
        user.setLastName(last);
        user.setPassword(username);
        
        return user;
    }
    
    @Test
    public void testDuplicateUsername()
    {
        UserData user = createUserData("testDuplicateUsername" + System.nanoTime());
        userDataService.createNewUser(user);
        UserData userDup = createUserData("testDuplicateUsername" + System.nanoTime());
        userDup.setUsername(user.getUsername());
        // This should fail
        try
        {
            userDataService.createNewUser(userDup);
            Assert.fail("Should fail due to duplicate username.");
        }
        catch (DuplicateKeyException e)
        {
            // Expected
        }
    }
    
    @Test
    public void testDuplicateEmail()
    {
        UserData user = createUserData("testDuplicateEmail" + System.nanoTime());
        userDataService.createNewUser(user);
        UserData userDup = createUserData("testDuplicateEmail" + System.nanoTime());
        userDup.setEmail(user.getEmail());
        // This should fail
        try
        {
            userDataService.createNewUser(userDup);
            Assert.fail("Should fail due to duplicate email.");
        }
        catch (DuplicateKeyException e)
        {
            // Expected
        }
    }
    
    @Test
    public void testDeletedByCreated()
    {
        // Start with 1 created user and 5 uncreated users
        UserData createdUser = createUserData("testDeletedByCreated.createdUser" + System.nanoTime());
        createdUser.setCreationState(DataCreationState.Created);
        userDataService.createNewUser(createdUser);
        
        Assert.assertEquals(6, userDataService.countUsers(null, null));
        Assert.assertEquals(1, userDataService.countUsers(null, DataCreationState.Created));
        Assert.assertEquals(5, userDataService.countUsers(null, DataCreationState.NotScheduled));

        long deleted = userDataService.deleteUsers(DataCreationState.NotScheduled);
        Assert.assertEquals(5L, deleted);
        Assert.assertEquals(1, userDataService.countUsers(null, null));
        Assert.assertEquals(1, userDataService.countUsers(null, DataCreationState.Created));
        Assert.assertEquals(0, userDataService.countUsers(null, DataCreationState.NotScheduled));
    }
    
    @Test
    public void testRandomUser()
    {
        UserData user = userDataService.getRandomUser();
        Assert.assertNull("Don't expect any created user.", user);
        // Mark user as created and find it
        for (int i = 0; i < USERS.length; i++)
        {
            user = userDataService.findUserByUsername(USERS[i]);
            Assert.assertNotNull("No user found for " + USERS[i], user);
            // Mark user created
            userDataService.setUserCreationState(USERS[i], DataCreationState.Created);
            // Repeat and repeat ...
            for (int j = 0; j < 1000; j++)
            {
                user = userDataService.getRandomUser();
                Assert.assertNotNull("Should have at least one created user to choose randomly.", user);
            }
        }
    }
    
    @Test
    public void testRandomUserFromDomain()
    {
        // Convert all the users to created users
        for (String username : USERS)
        {
            userDataService.setUserCreationState(username, DataCreationState.Created);
        }
        
        UserData userData = userDataService.getRandomUserFromDomain("example.net");
        Assert.assertNull("No domain 'example.net' so there should not be a user.", userData);
        
        userData = userDataService.getRandomUserFromDomain("example.com");
        Assert.assertNotNull(userData);
        
        userData = createUserData("fblogs");
        userData.setDomain("example.net");
        userDataService.createNewUser(userData);
        userData = userDataService.getRandomUserFromDomain("example.net");
        Assert.assertNull("User in example.net was not created so should not show up", userData);
        userDataService.setUserCreationState("fblogs", DataCreationState.Created);
        userData = userDataService.getRandomUserFromDomain("example.net");
        Assert.assertNotNull("User in example.net is created so should show up", userData);
        
        userData = userDataService.getRandomUserFromDomains(Collections.singletonList("example.net"));
        Assert.assertNotNull(userData);
        Assert.assertEquals("fblogs", userData.getUsername());
    }
    
    @Test
    public void testUsernameDoesNotExist()
    {
        try
        {
            userDataService.setUserCreationState("Bob", DataCreationState.Created);
            Assert.fail("Missing username not detected.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        try
        {
            userDataService.setUserPassword("Joe", "pwd");
            Assert.fail("Missing username not detected.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
    }
}
