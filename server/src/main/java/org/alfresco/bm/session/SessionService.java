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

import com.mongodb.DBObject;

/**
 * Service providing access and management of {@link SessionData}.
 *
 * @author Derek Hulley
 * @since 1.4
 */
public interface SessionService
{
    /**
     * Starts a new session, persisting the given data and giving back a unique session ID.
     * The session start time is marked with the current time.
     * 
     * @param data              a map of session data to be stored (<tt>null</tt> allowed)
     * @return                  a new, unique session ID
     */
    String startSession(DBObject data);
    
    /**
     * Mark a session as complete.
     * 
     * @param sessionId         the unique session ID
     * 
     * @throws IllegalStateException if the session has already been ended
     * @throws RuntimeException if the session ID is invalid
     */
    void endSession(String sessionId);
    
    /**
     * Update or set the persistable data associated with a session
     * 
     * @param sessionId         the unique session ID
     * @param data              any persistable session data (<tt>null</tt> allowed)
     * 
     * @throws RuntimeException if the session ID is invalid
     */
    void setSessionData(String sessionId, DBObject data);
    /**
     * @param sessionId         the unique session ID
     * @return                  any data associated with the session or <tt>null</tt> if there was none
     * 
     * @throws RuntimeException if the session ID is invalid
     */
    DBObject getSessionData(String sessionId);
    
    /**
     * Get the session start time
     * 
     * @param sessionId         the unique session ID
     * @return                  the session start time or <tt>-1</tt> if not started
     * 
     * @throws RuntimeException if the session ID is invalid
     */
    long getSessionStartTime(String sessionId);
    
    /**
     * Get the session end time
     * 
     * @param sessionId         the unique session ID
     * @return                  the session start time or <tt>-1</tt> if not ended
     * 
     * @throws RuntimeException if the session ID is invalid
     */
    long getSessionEndTime(String sessionId);
    
    /**
     * Get the session elapsed time
     * 
     * @param sessionId         the unique session ID
     * @return                  the session elapsed time or <tt>-1</tt> if not ended
     * 
     * @throws RuntimeException if the session ID is invalid
     */
    long getSessionElapsedTime(String sessionId);
    
    /**
     * Returns a number of active sessions, i.e. where endTime = -1
     */
    long getActiveSessionsCount();
    
    /**
     * Returns a number of completed sessions, i.e. where endTime > 0
     */
    long getCompletedSessionsCount();
    
    /**
     * Returns a number of sessions, active or otherwise
     */
    long getAllSessionsCount();
}
