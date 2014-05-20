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

import java.util.Properties;

import junit.framework.Assert;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * @see PrepareUsers
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class UserEventHandlingTest
{
    private final static String COLLECTION_BM_USER_DATA_SERVICE = "UserEventHandlingTest";
    
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
    }
    
    @After
    public void tearDown()
    {
        ctx.close();
    }
    
    @Test
    public void prepareUsers() throws Exception
    {
        PrepareUsers prep = new PrepareUsers(userDataService, 200);
        prep.setUsersPerDomain(20);
        prep.setDomainPattern("[emailDomain]");
        
        Event event = new Event("X", null);
        EventResult result = prep.processEvent(event);
        Assert.assertTrue(result.getData().toString().contains("200"));
        
        // Check
        Assert.assertEquals(200,  userDataService.countUsers());
        UserData user = userDataService.getRandomUser();
        Assert.assertNull("No users have been created, yet.", user);
        user = userDataService.findUserByUsername("0000000.Test@00000.example.com");
        Assert.assertNotNull("Expect to find first user out of 200.", user);
        user = userDataService.findUserByUsername("0000199.Test@00009.example.com");
        Assert.assertNotNull("Expected to find last user out of 200.", user);
        Assert.assertEquals(user.getLastName(), PrepareUsers.DEFAULT_LAST_NAME_PATTERN);
        
        // Create a user
        userDataService.setUserCreated("0000199.Test@00009.example.com", true);
        Assert.assertEquals("Incorrect number of users in domain. ", 20, userDataService.getUsersInDomain("00009.example.com", 0, 200).size());
        Assert.assertEquals("Expected to find the created user only.", 1, userDataService.getUsersInDomain("00009.example.com", 0, 200, true).size());
        Assert.assertEquals("Expected to find the pending users only.", 19, userDataService.getUsersInDomain("00009.example.com", 0, 200, false).size());
        
        // Check that the cleanup is active: create the 201st user
        user.setUsername("fred");
        user.setEmail("fred@example.com");
        userDataService.createNewUser(user);
        Assert.assertEquals(201,  userDataService.countUsers());
        // If we run it again, it should wipe and repeat, except that it'll leave the created user
        prep.setEmailDomainPattern("%05d.second.com");
        prep.processEvent(event);
        
        // Since the email pattern changed, it will not find and count the created user
        Assert.assertEquals("Target number of users not restored", 201,  userDataService.countUsers());
        user = userDataService.findUserByUsername("0000000.Test@00000.example.com");
        Assert.assertNull("Email pattern has changed so user should not have been found.", user);
        user = userDataService.findUserByUsername("0000199.Test@00009.second.com");
        Assert.assertNotNull("User with changed email pattern not found.", user);
        // Check that original, created user still exists
        Assert.assertEquals("Expected to find the created user only.", 1, userDataService.getUsersInDomain("00009.example.com", 0, 200, true).size());
        Assert.assertEquals("All uncreated users should have been cleaned out.", 0, userDataService.getUsersInDomain("00009.example.com", 0, 200, false).size());
    }
}
