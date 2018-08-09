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
package org.alfresco.bm.driver.session;

import com.mongodb.DBObject;

/**
 * Abstract implemnetation for methods that don't persist or retrieve values directly.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public abstract class AbstractSessionService implements SessionService
{
    /**
     * Persist session data.
     * <p/>
     * All session IDs must be unique.
     */
    protected abstract String newSession(SessionData sessionData);
    
    /**
     * Find session data for the given ID
     * @return          all the session info or <tt>null</tt> if not found
     */
    protected abstract SessionData findSessionData(String sessionId);
    
    /**
     * Update session end time.
     */
    protected abstract void updateSessionEndTime(String sessionId, long endTime);
    
    /**
     * Update session's client-provided data matching the ID.
     * @return              <tt>true</tt> if the session was updated
     */
    protected abstract boolean updateSessionData(String sessionId, DBObject data);
    
    @Override
    public String startSession(DBObject data)
    {
        SessionData sessionData = new SessionData(data);    // Start time will be set
        return newSession(sessionData);
    }
    
    /**
     * @return              the session data (never <tt>null</tt>)
     */
    private SessionData getSessionDataNotNull(String sessionId)
    {
        SessionData sessionData = findSessionData(sessionId);
        if (sessionData == null)
        {
            throw new RuntimeException("Session ID not recorded: " + sessionId);
        }
        return sessionData;
    }

    @Override
    public void endSession(String sessionId)
    {
        long now = System.currentTimeMillis();
        updateSessionEndTime(sessionId, now);
    }

    @Override
    public void setSessionData(String sessionId, DBObject data)
    {
        updateSessionData(sessionId, data);
    }

    @Override
    public DBObject getSessionData(String sessionId)
    {
        SessionData sessionData =  getSessionDataNotNull(sessionId);
        return sessionData.getData();
    }

    @Override
    public long getSessionStartTime(String sessionId)
    {
        SessionData sessionData =  getSessionDataNotNull(sessionId);
        return sessionData.getStartTime();
    }

    @Override
    public long getSessionEndTime(String sessionId)
    {
        SessionData sessionData =  getSessionDataNotNull(sessionId);
        return sessionData.getEndTime();
    }

    @Override
    public long getSessionElapsedTime(String sessionId)
    {
        SessionData sessionData =  getSessionDataNotNull(sessionId);
        return sessionData.getEndTime();
    }
}