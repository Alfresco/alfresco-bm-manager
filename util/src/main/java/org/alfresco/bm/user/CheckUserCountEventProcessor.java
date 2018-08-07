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

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.driver.event.AbstractEventProcessor;
import org.alfresco.bm.driver.event.Event;
import org.alfresco.bm.common.EventResult;

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
 * <h1>Enhancements</h1>
 * 
 * If {@link setEventNameSelf} is set and there are still scheduled users 
 * for creation the event processor reschedules self as long as a progress
 * in user creation is detected. You may adjust the delay between 
 * rescheduled events with {@link setDelayRescheduleSelf}.
 * 
 *  Note: this may happen if Alfresco is rather low on user creation or if
 *  you use a long delay between user create events. 
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_USERS_READY}: Passes inbound data through<br/>
 * 
 * @author Derek Hulley
 * @author Frank Becker
 * @since 1.4
 */
public class CheckUserCountEventProcessor extends AbstractEventProcessor
{
    public static final String EVENT_NAME_USERS_READY = "users.ready";
    public static final String ERR_NOT_ENOUGH_USERS = "Needed %1d created users but only found %1d.";
    public static final String MSG_FOUND_USERS = "Found %1d created users.  Minimum was %1d.";
    
    private String eventNameUsersReady = EVENT_NAME_USERS_READY;
    private String eventNameSelf = null;
    private final UserDataService userDataService;
    private final long userCount;
    private long delayRescheduleSelf = 1000;
    private boolean rescheduleSelf = false;
    
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
    
    /**
     * Sets the self event name to reschedule 
     * 
     * @param eventName name of event or null to NOT reschedule self
     * @since 2.1.4
     */
    public void setEventNameSelf(String eventName)
    {
        this.eventNameSelf = eventName;
        if (null != eventName && !eventName.isEmpty())
        {
            setRescheduleSelf(true);
        }
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        // process event data
        Object eventData = event.getData();
        CheckUserCountEventData data = null;
        if (null != eventData && eventData instanceof CheckUserCountEventData)
        {
            data = (CheckUserCountEventData) eventData;
        }
        
        // check for failed users
        long failedUserCount = userDataService.countUsers(null, DataCreationState.Failed); 

        // Check the number of users
        long scheduledUserCount = userDataService.countUsers(null, DataCreationState.Scheduled);
        long actualUserCount = userDataService.countUsers(null, DataCreationState.Created);
        if (actualUserCount < userCount)
        {
            // not enough users ....
            
            // there must be no failed users to reschedule, if there are any failed just report
            if (0 == failedUserCount )
            {                
                // check if still scheduled users - and if we CAN reschedule self
                if (scheduledUserCount > 0 && null != this.eventNameSelf && this.rescheduleSelf)
                {
                    boolean hasError = false;
                    
                    if (null != data && (actualUserCount <= data.getUserCountCreated()  || data.getUserCountScheduled() <= scheduledUserCount))
                    {
                            // no progress ...
                            hasError = true;
                    }
                    
                    if (!hasError)
                    {
                        if (null == data)
                        {
                            data = new CheckUserCountEventData(eventData, actualUserCount, scheduledUserCount);
                        }
                        else
                        {
                            data.setUserCountCreated(actualUserCount);
                            data.setUserCountScheduled(scheduledUserCount);
                        }
                        
                        // re-schedule self 
                        Event nextEvent = new Event(this.eventNameSelf, System.currentTimeMillis() + this.delayRescheduleSelf, data);
                        String msg = "Rescheduled CheckUserCount; still " + scheduledUserCount + " scheduled users!";
                        return new EventResult(msg, nextEvent);
                    }
                }
            }
            
            // report error
            String msg = String.format(ERR_NOT_ENOUGH_USERS, userCount, actualUserCount);
            return new EventResult(msg, false);
        }
        
        // There are enough users - pass through the input event data
        Object nextEventData = (null == data) ? event.getData() : data.getEventData();
        Event nextEvent = new Event(
                eventNameUsersReady,
                System.currentTimeMillis(),
                nextEventData);
        return new EventResult(
                String.format(MSG_FOUND_USERS, actualUserCount, userCount),
                nextEvent);
    }

    /**
     * @param delayRescheduleSelfMs (long, > 0) number of milliseconds to reschedule self 
     * @since 2.1.4
     */
    public void setDelayRescheduleSelf(long delayRescheduleSelfMs)
    {
        if (delayRescheduleSelfMs < 0)
        {
            throw new IllegalArgumentException("'delayRescheduleSelfMs': a positive value is required!");
        }
        this.delayRescheduleSelf = delayRescheduleSelfMs;
    }

    /**
     * @param rescheduleSelf
     *        (boolean) reschedule self if scheduled users found (true) or fail
     *        (false)?
     *        Note: requires eventNameSelf not to be null or empty!
     * 
     * @since 2.1.4
     */
    public void setRescheduleSelf(boolean rescheduleSelf)
    {
        this.rescheduleSelf = rescheduleSelf;
        if (rescheduleSelf && (null == this.eventNameSelf || this.eventNameSelf.isEmpty()))
        {
            throw new IllegalArgumentException("'setRescheduleSelf' requires 'eventNameSelf' to be set!");
        }
    }
}
