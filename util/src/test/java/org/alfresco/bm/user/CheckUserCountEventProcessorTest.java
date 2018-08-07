package org.alfresco.bm.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.common.EventResult;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Tests CheckUserCountEventProcessor
 * 
 * @author Frank Becker
 * @since 2.1.4
 */
@RunWith(JUnit4.class)
public class CheckUserCountEventProcessorTest
{
    private final static String COLLECTION_BM_USER_DATA_SERVICE = "BenchmarkCheckUserCountTest";

    private static AbstractApplicationContext ctx;
    private static UserDataService userDataService;

    @Before
    public void setUp()
    {
        Properties props = new Properties();
        props.put("mongoCollection", COLLECTION_BM_USER_DATA_SERVICE);

        ctx = new ClassPathXmlApplicationContext(new String[] { "test-MongoUserDataTest-context.xml" }, false);
        ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("TestProps", props));
        ctx.refresh();
        ctx.start();
        userDataService = ctx.getBean(UserDataService.class);

        // Generate some random users
        for (int i = 0; i < UserDataServiceTest.USERS.length; i++)
        {
            String username = UserDataServiceTest.USERS[i];
            UserData user = UserDataServiceTest.createUserData(username);
            userDataService.createNewUser(user);
            userDataService.setUserCreationState(username, DataCreationState.Created);
        }
    }

    @After
    public void tearDown()
    {
        ctx.close();
    }

    /**
     * Users are created on {@link setUp} - check all users to be created.
     * 
     * @throws Exception
     */
    @Test
    public void testUsersCreated() throws Exception
    {
        // test created users
        String eventName = "checkUserCount";
        CheckUserCountEventProcessor processor = new CheckUserCountEventProcessor(userDataService, UserDataServiceTest.USERS.length);
        Event event = new Event(eventName, null);
        EventResult result = processor.processEvent(event, new StopWatch());
        assertEquals("Users should be created.", true, result.isSuccess());
        assertEquals("Should have scheduled one success event.", 1, result.getNextEvents().size());
        Event nextEvent = result.getNextEvents().get(0);
        assertEquals("Should have scheduled success.", CheckUserCountEventProcessor.EVENT_NAME_USERS_READY ,nextEvent.getName());

        // set one user to fail and retry 
        userDataService.setUserCreationState(UserDataServiceTest.USERS[0], DataCreationState.Failed);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have failed.", false, result.isSuccess());

        // set one user to Not Scheduled and retry 
        userDataService.setUserCreationState(UserDataServiceTest.USERS[0], DataCreationState.NotScheduled);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have failed.", false, result.isSuccess());

        // set one user to scheduled and retry (should fail because self event is not set) 
        userDataService.setUserCreationState(UserDataServiceTest.USERS[0], DataCreationState.Scheduled);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have failed.", false, result.isSuccess());
        
        // now set self event name and retry
        processor.setEventNameSelf("Doesntmatter");
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have rescheduled self.", true, result.isSuccess());
        
        // switch off reschedule and retry
        processor.setRescheduleSelf(false);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have failed.", false, result.isSuccess());
        
        // change user to fail and retry with reschedule ... should also fail
        processor.setRescheduleSelf(true);
        userDataService.setUserCreationState(UserDataServiceTest.USERS[0], DataCreationState.Failed);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have failed.", false, result.isSuccess());
}

    /**
     * Add some users with only DataCreationState.Scheduled and check if
     * rescheduled self
     * 
     * @throws Exception
     */
    @Test
    public void testUsersScheduled() throws Exception
    {
        // add two users
        long count = UserDataServiceTest.USERS.length + 2;
        String user1 = "mogli";
        String user2 = "balu";
        String eventName = "checkUserCountScheduled";
        UserData user = UserDataServiceTest.createUserData(user1);
        userDataService.createNewUser(user);
        user = UserDataServiceTest.createUserData(user2);
        userDataService.createNewUser(user);

        // set the new users to Scheduled
        userDataService.setUserCreationState(user1, DataCreationState.Scheduled);
        userDataService.setUserCreationState(user2, DataCreationState.Scheduled);

        // create event processor
        CheckUserCountEventProcessor processor = new CheckUserCountEventProcessor(userDataService, count);
        processor.setEventNameSelf(eventName);
        processor.setDelayRescheduleSelf(500);
        Event event = new Event(eventName, null);
        EventResult result = processor.processEvent(event, new StopWatch());
        
        // should have rescheduled self
        assertEquals("Should have rescheduled self only.", 1, result.getNextEvents().size());
        Event nextEvent = result.getNextEvents().get(0);
        assertEquals("Should have rescheduled self.", eventName ,nextEvent.getName());
        Object eventData = nextEvent.getData();
        assertNotNull("Should have event data.", eventData);
        CheckUserCountEventData data = (CheckUserCountEventData)eventData;
        assertEquals("Should have two pending users", 2, data.getUserCountScheduled());
        
        // set first user to created and retry
        userDataService.setUserCreationState(user1, DataCreationState.Created);
        event = new Event(eventName, data);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have rescheduled self only.", 1, result.getNextEvents().size());
        nextEvent = result.getNextEvents().get(0);
        assertEquals("Should have rescheduled self.", eventName ,nextEvent.getName());
        eventData = nextEvent.getData();
        assertNotNull("Should have event data.", eventData);
        data = (CheckUserCountEventData)eventData;
        assertEquals("Should have one pending user", 1, data.getUserCountScheduled());
        
        // set second user to fail and retry
        userDataService.setUserCreationState(user2, DataCreationState.Failed);
        event = new Event(eventName, data);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should have failed.", false, result.isSuccess());
        
        // set second user to created and retry
        userDataService.setUserCreationState(user2, DataCreationState.Created);
        event = new Event(eventName, null);
        result = processor.processEvent(event, new StopWatch());
        assertEquals("Should not fail.", true, result.isSuccess());
        assertEquals("Should have scheduled one success event.", 1, result.getNextEvents().size());
        nextEvent = result.getNextEvents().get(0);
        assertEquals("Should have scheduled success.", CheckUserCountEventProcessor.EVENT_NAME_USERS_READY ,nextEvent.getName());
        eventData = nextEvent.getData();
        assertEquals("Should deliver original event data", data.getEventData(), nextEvent.getData());
    }
    
    @Test(expected=Exception.class)
    public void checkBounds() throws Exception
    {
        CheckUserCountEventProcessor processor = new CheckUserCountEventProcessor(userDataService, 10);
        processor.setDelayRescheduleSelf(-100);
    }
}
