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

import org.alfresco.bm.driver.event.CreateSessionsEventProcessor;
import org.alfresco.bm.driver.session.SessionService;

/**
 * Schedules random user sessions according to the session concurrency required.
 * 
 * <h1>Input</h1>
 * 
 * None.
 * 
 * <h1>Actions</h1>
 * 
 * Each new user 'session' is created by choosing a random user
 * 
 * <h1>Output</h1>
 * 
 * Events with data having the 'username' string as data.
 *
 * @author Derek Hulley
 * @since 2.0.10
 */
public class ConcurrentUserSessionsEventProcessor extends CreateSessionsEventProcessor
{
    private final UserDataService userDataService;
    
    /**
     * @param userDataService           provides access to random users
     * @param sessionService            required to ensure the correct number of concurrent sessions
     * @param outputEventName           the name of the event to emit
     * @param concurrentSessions        the number of concurrent sessions to maintain
     * @param totalSessions             the maxiumum number of sessions to create
     */
    public ConcurrentUserSessionsEventProcessor(
            UserDataService userDataService,
            SessionService sessionService,
            String outputEventName,
            int concurrentSessions,
            int totalSessions)
    {
        super(sessionService, outputEventName, concurrentSessions, totalSessions);
        this.userDataService = userDataService;
    }

    /**
     * Chooses a random user.
     */
    @Override
    protected Object getNextEventData()
    {
        UserData user = userDataService.getRandomUser();
        if (user == null)
        {
            throw new IllegalStateException("No users available to generate user sessions.");
        }
        String username = user.getUsername();
        // Done
        return username;
    }
}
