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
package org.alfresco.bm.session;

import java.util.UUID;

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
    protected abstract void saveSessionData(SessionData sessionData);
    
    /**
     * Find session data for the given ID
     * @return          all the session info or <tt>null</tt> if not found
     */
    protected abstract SessionData findSessionData(String sessionId);
    
    /**
     * Update session end time (including elapsed time).
     */
    protected abstract void updateSessionEndTime(String sessionId, long endTime, long elapsedTime);
    
    /**
     * Update session's client-provided data matching the ID.
     */
    protected abstract void updateSessionData(String sessionId, String data);
    
    @Override
    public String startSession(String data)
    {
        String sessionId = UUID.randomUUID().toString();
        SessionData sessionData = new SessionData(data);    // Start time will be set
        sessionData.setSessionId(sessionId);
        saveSessionData(sessionData);
        return sessionId;
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
        SessionData sessionData =  getSessionDataNotNull(sessionId);
        long now = System.currentTimeMillis();
        long elapsed = now - sessionData.getStartTime();
        updateSessionEndTime(sessionId, now, elapsed);
    }

    @Override
    public void setSessionData(String sessionId, String data)
    {
        getSessionDataNotNull(sessionId);           // Just a check
        updateSessionData(sessionId, data);
    }

    @Override
    public String getSessionData(String sessionId)
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