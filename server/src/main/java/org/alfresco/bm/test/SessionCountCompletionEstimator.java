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
package org.alfresco.bm.test;

import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.session.SessionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Measure the completion ratio based on the number of completed sessions.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class SessionCountCompletionEstimator extends AbstractCompletionEstimator
{
    private static Log logger = LogFactory.getLog(EventCountCompletionEstimator.class);
    
    private final SessionService sessionService;
    private final long sessionCount;
    
    /**
     * Constructor with required dependencies
     * 
     * @param sessionService                used to do final checks of event counts
     * @param sessionCount                  the total number of sessions expected
     */
    public SessionCountCompletionEstimator(
            EventService eventService,
            ResultService resultService,
            SessionService sessionService, long sessionCount)
    {
        super(eventService, resultService);
        this.sessionService = sessionService;
        this.sessionCount = sessionCount;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Gives a ratio of the number of completed sessions relative to the total expected
     */
    @Override
    protected double getCompletionImpl()
    {
        if (sessionCount <= 0L)
        {
            // Bypass the calculation
            return 1.0;
        }
        long completedSessions = sessionService.getCompletedSessionsCount();
        // Check if the event count values were chosen correctly.
        if (completedSessions > sessionCount)
        {
            logger.warn("The number of sessions exceeds the target: " + completedSessions + " exceeds " + sessionCount);
            completedSessions = sessionCount;       // Make sure it's 1.0 again
        }
        // Return the ratio
        double completion = (double) completedSessions/sessionCount;
        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Completion by sessions: %0.2d", completion));
        }
        return completion;
    }
}
