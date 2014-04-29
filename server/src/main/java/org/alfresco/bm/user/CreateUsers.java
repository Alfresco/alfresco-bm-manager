/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;

/**
 * <h1>Input</h1>
 * 
 * None
 * 
 * <h1>Data</h1>
 * 
 * No data requirements.
 * 
 * <h1>Actions</h1>
 * 
 * A number of users is scheduled to be created in batches. This event will reschedule
 * itself repeatedly until enough users have been created.
 * 
 * <h1>Output</h1>
 * {@link #EVENT_NAME_CREATE_USER}: User to create<br/>
 * {@link #EVENT_NAME_CREATE_USERS}: Returned when more users should be scheduled
 *
 * @author Frederik Heremans
 */
public class CreateUsers extends AbstractEventProcessor
{
    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final long DEFAULT_EXPECTED_CREATE_TIME = 20;
    
    public static final String EVENT_NAME_CREATE_USER = "createUser";
    public static final String EVENT_NAME_CREATE_USERS = "createUsers";
    public static final String EVENT_NAME_USERS_CREATED = "usersCreated";
    
    private UserDataService userDataService;
    private long numberOfUsers;
    private int batchSize;
    private String eventNameCreateUsers;
    private String eventNameUsersCreated;
    
    /**
     * Creates a new instance.
     * 
     * @param numberOfUsers         number of users to create in total
     * @param userDataService       service for {@link UserData} operations
     */
    public CreateUsers(UserDataService userDataService, long numberOfUsers)
    {
        this.userDataService = userDataService;
        this.numberOfUsers = numberOfUsers;
        
        batchSize = DEFAULT_BATCH_SIZE;
        eventNameCreateUsers = EVENT_NAME_CREATE_USERS;
        eventNameUsersCreated = EVENT_NAME_USERS_CREATED;
    }

    /**
     * Override the {@link #DEFAULT_BATCH_SIZE default} batch size
     */
    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }
    
    /**
     * Override the {@link #EVENT_NAME_CREATE_USERS default} event name for more
     * user creation scheduling
     */
    public void setEventNameCreateUsers(String eventNameCreateUsers)
    {
        this.eventNameCreateUsers = eventNameCreateUsers;
    }

    /**
     * Override the {@link #EVENT_NAME_USERS_CREATED default} event name
     * indicating that enough users have been created
     */
    public void setEventNameUsersCreated(String eventNameUsersCreated)
    {
        this.eventNameUsersCreated = eventNameUsersCreated;
    }

    public EventResult processEvent(Event event) throws Exception
    {
        List<Event> nextEvents = new ArrayList<Event>();
        
        // Schedule events for each user to be created
        long createdUsers = userDataService.countUsers(true);
        long toCreate = numberOfUsers - createdUsers;
        int index = 0;
        List<UserData> pendingUsers = userDataService.getUsersPendingCreation(index, batchSize);
        
        // Terminate the user creation process if there is no more do to or nothing to do
        if (toCreate <= 0)
        {
            // There is nothing more to do
            Event doneEvent = new Event(eventNameUsersCreated, null);
            nextEvents.add(doneEvent);
            return new EventResult("" + createdUsers + " users have been created.", nextEvents);
        }
        else if (pendingUsers.size() == 0)
        {
            // There are no more pending users but we wanted to create more
            Event doneEvent = new Event(eventNameUsersCreated, null);
            nextEvents.add(doneEvent);
            return new EventResult("No more pending users but still need " + toCreate + " users.", nextEvents, false);
        }
        
        long lastEventTime = System.currentTimeMillis();
        for (UserData user : pendingUsers)
        {
            lastEventTime = System.currentTimeMillis();
            // Do we need to schedule it?
            String username = user.getUsername();
            Event nextEvent = new Event(EVENT_NAME_CREATE_USER, username);
            nextEvents.add(nextEvent);
            toCreate--;
            if (toCreate <= 0)
            {
                break;
            }
        }
        int scheduled = nextEvents.size();
        // Reschedule for the next batch (might zero next time)
        long scheduledTime = lastEventTime + DEFAULT_EXPECTED_CREATE_TIME;
        Event self = new Event(eventNameCreateUsers, scheduledTime, null);
        nextEvents.add(self);
        
        // Return messages + next events
        return new EventResult("Scheduled " + scheduled + " user(s) for creation", nextEvents);
    }
}
