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

import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.user.UserData.UserCreationState;
import org.alfresco.bm.user.UserDataService;

/**
 * An event processor that ensures the presence of a minimum number of users.
 * 
 * <h1>Input</h1>
 * 
 * No input requirements
 * 
 * <h1>Actions</h1>
 * 
 * The {@link UserDataService} is examined to check that the prescribed
 * number of users are present.
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_USERS_READY}: Passes inbound data through<br/>
 * 
 * @author Derek Hulley
 * @since 1.4
 */
public class CheckUserCountEventProcessor extends AbstractEventProcessor
{
    public static final String EVENT_NAME_USERS_READY = "users.ready";
    public static final String ERR_NOT_ENOUGH_USERS = "Needed %1d created users but only found %1d.";
    public static final String MSG_FOUND_USERS = "Found %1d created users.  Minimum was %1d.";
    
    private String eventNameUsersReady = EVENT_NAME_USERS_READY;
    private final UserDataService userDataService;
    private final long userCount;
    
    /**
     * @param userDataService           the service that provides a view onto the users
     * @param userCount                 the minimum number of users to have
     */
    public CheckUserCountEventProcessor(UserDataService userDataService, long userCount)
    {
        this.userDataService = userDataService;
        this.userCount = userCount;
    }

    /**
     * Override the {@link #EVENT_NAME_USERS_READY default} event name when users are ready
     */
    public void setEventNameUsersReady(String eventNameUsersReady)
    {
        this.eventNameUsersReady = eventNameUsersReady;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        // Check the number of users
        long actualUserCount = userDataService.countUsers(null, UserCreationState.Created);
        if (actualUserCount < userCount)
        {
            // Not enough
            String msg = String.format(ERR_NOT_ENOUGH_USERS, userCount, actualUserCount);
            return new EventResult(msg, false);
        }
        // There are enough users
        Event nextEvent = new Event(
                eventNameUsersReady,
                System.currentTimeMillis(),
                event.getDataObject());
        // Just pass the inbound data through
        return new EventResult(
                String.format(MSG_FOUND_USERS, actualUserCount, userCount),
                nextEvent);
    }
}
