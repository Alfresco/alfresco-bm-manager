/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.driver.event.EventProcessor;
import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.common.util.junit.tools.MongoDBForTestsFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.mongodb.DB;

/**
 * @see UserDataService
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class GenerateUserSessionsEventProcessorTest
{
    private final static String COLLECTION_BM_USER_DATA_SERVICE = "BenchmarkUserDataServiceTest";
    
    private final static String[] USERS = new String[] {"fsmith", "jblogg", "bjones", "msimonds", "ddevries"};

    private static UserDataServiceImpl userDataService;
    private static MongoDBForTestsFactory mongoDBFactory;

    @BeforeClass
    public static void setUp() throws Exception
    {
        mongoDBFactory = new MongoDBForTestsFactory();
        DB db = mongoDBFactory.getObject();
        userDataService = new UserDataServiceImpl(db, COLLECTION_BM_USER_DATA_SERVICE);
        userDataService.afterPropertiesSet();
        
        // Generate some random users
        for (int i = 0; i < USERS.length; i++)
        {
            String username = USERS[i];
            UserData user = createUserData(username);
            userDataService.createNewUser(user);
        }
    }
    
    @AfterClass
    public static void tearDown() throws Exception
    {
        mongoDBFactory.destroy();
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
        user.setCreationState(DataCreationState.Created);
        user.setDomain("example.com");
        user.setEmail(username + "@example.com");
        user.setFirstName(first);
        user.setLastName(last);
        user.setPassword(username);
        
        return user;
    }
    
    @Test
    public void testProcess() throws Exception
    {
        EventProcessor processor = new GenerateUserSessionsEventProcessor(userDataService, "login", 100L, 10);
        Event event = new Event("createUserSessions", null);
        
        EventResult result = processor.processEvent(event, new StopWatch());
        
        assertEquals(10, result.getNextEvents().size());        // No rescheduling
        for (Event nextEvent : result.getNextEvents())
        {
            String username = (String) nextEvent.getData();
            assertNotNull("User not found: " + username, userDataService.findUserByUsername(username));
        }
    }
}
