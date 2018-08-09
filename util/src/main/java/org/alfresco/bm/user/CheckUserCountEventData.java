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
package org.alfresco.bm.user;

/**
 * Event data if to reschedule CheckUserCountEventProcessor
 * 
 * @author Frank Becker
 * @since 2.1.4
 */
public class CheckUserCountEventData
{
    private Object eventData;
    private long userCountCreated;
    private long userCountScheduled;

    /**
     * Constructor
     * 
     * @param originalEventData
     *        (Object, may be null) original event data as passed to event
     *        processor
     * @param usersCreated
     *        (long) number of users created
     * @param usersScheduled
     *        (long) number of users Scheduled
     */
    public CheckUserCountEventData(Object originalEventData, long usersCreated, long usersScheduled)
    {
        this.setEventData(originalEventData);
        this.setUserCountCreated(usersCreated);
        this.setUserCountScheduled(usersScheduled);
    }

    /**
     * @return the eventData
     */
    public Object getEventData()
    {
        return eventData;
    }

    /**
     * @param eventData (Object, may be null) the eventData to set
     */
    public void setEventData(Object eventData)
    {
        this.eventData = eventData;
    }

    /**
     * @return the number of users created
     */
    public long getUserCountCreated()
    {
        return userCountCreated;
    }

    /**
     * @param userCountCreated (long) the number of users created to set
     */
    public void setUserCountCreated(long userCountCreated)
    {
        this.userCountCreated = userCountCreated;
    }

    /**
     * @return the number of users scheduled for creation
     */
    public long getUserCountScheduled()
    {
        return userCountScheduled;
    }

    /**
     * @param userCountScheduled (long) number of users scheduled for creation to set
     */
    public void setUserCountScheduled(long userCountScheduled)
    {
        this.userCountScheduled = userCountScheduled;
    }
}
