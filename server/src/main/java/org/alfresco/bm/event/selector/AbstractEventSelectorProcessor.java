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
package org.alfresco.bm.event.selector;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.session.SessionService;

import com.google.gson.Gson;
import com.mongodb.DBObject;

/**
 * Event processor that uses an eventSelector to choose the next event.
 *  
 * @author Steve Glover
 * @since 1.3
 */
public abstract class AbstractEventSelectorProcessor extends AbstractEventProcessor implements EventDataCreator
{
    private SessionService sessionService;
    private EventSelector eventSelector;
    private boolean persistResponse = false;
    private long maxSessionTime = 5*60*1000; // default 5 mins

    private Gson gson = new Gson();

    public AbstractEventSelectorProcessor(EventSelector eventSelector, SessionService sessionService)
    {
        super();
        this.eventSelector = eventSelector;
        this.sessionService = sessionService;
    }

    public void setMaxSessionTime(long maxSessionTime)
    {
        this.maxSessionTime = maxSessionTime;
    }

    /**
     * @param persistResponse          <tt>true</tt> to persist the entire response including the data
     *                                 or <tt>false</tt> just to persist the response message
     */
    public void setPersistResponse(boolean persistResponse) 
    {
        this.persistResponse = persistResponse;
    }

    protected abstract EventProcessorResponse processEventImpl(Event event) throws Exception;

    protected String toJSON(Object obj)
    {
        String json = gson.toJson(obj);
        return json;
    }

    protected Object getPersistableResponse(EventProcessorResponse response)
    {
        Object ret = null;

        if (response != null && response.getResponseData() instanceof DBObject)
        {
            ret = response.getResponseData();
        }
        else if (response != null && response.isPersistAsString())
        {
            StringBuilder sb = new StringBuilder();
            String message = response.getMessage();
            if(message != null && !message.equals(""))
            {
                sb.append(response.getMessage());
                sb.append(" : ");
            }
            sb.append(toJSON(response.getResponseData()));
            ret = sb.toString();
        }
        else
        {
            ret = response;
        }
        
        return ret;
    }
    
    /**
     * @return                  the data associated with the session or <tt>null</tt> if there was none
     */
    protected DBObject getSessionData(String sessionId)
    {
        return sessionService.getSessionData(sessionId);
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        super.suspendTimer();
        
        Object input = event.getData();
        String sessionId = event.getSessionId();
        // Start a session, if necessary
        long sessionStartTime = -1L;
        long sessionEndTime = -1L;
        if (sessionId == null)
        {
            sessionId = sessionService.startSession(null);
            sessionStartTime = sessionService.getSessionStartTime(sessionId);
            sessionEndTime = -1L;
        }
        else
        {
            sessionStartTime = sessionService.getSessionStartTime(sessionId);
            sessionEndTime = sessionService.getSessionEndTime(sessionId);
        }
        
        List<Event> nextEvents = new ArrayList<Event>();

        super.resumeTimer();
        EventProcessorResponse response = processEventImpl(event);
        super.stopTimer();

        long now = System.currentTimeMillis();
        
        if (response == null)
        {
            logger.warn("Response is null for event " + event);
        }
        else if (sessionEndTime > 0L)
        {
            // The session has finished
        	if (logger.isDebugEnabled())
        	{
        		logger.debug(this.getClass().getName() + ": session finished.");
        	}
        }
        else if (now > (sessionStartTime + maxSessionTime))
        {
            // The session has expired
        	if (logger.isDebugEnabled())
        	{
        		logger.debug(this.getClass().getName() + ": session expired.");
        	}
        }
        else if (response.getResult() == EventProcessorResult.SUCCESS && eventSelector != null)
        {
        	if (logger.isDebugEnabled())
        	{
        		logger.debug(this.getClass().getName() + ": session OK, selecting next event.");
        	}

        	// if we haven't passed the expected session end time and the previous event was successful
            Object responseData = response.getResponseData();
            Event evt = eventSelector.nextEvent(input, responseData);
            if(evt != null)
            {
                nextEvents.add(evt);

                if (logger.isDebugEnabled())
            	{
            		logger.debug(this.getClass().getName() + ": next event: " + evt.getName());
            	}

            }
        }

        // No more events, end the session
        if (nextEvents.size() < 1)
        {
            sessionService.endSession(sessionId);
        	if (logger.isDebugEnabled())
        	{
        		logger.debug(this.getClass().getName() + ": no more events, ending session.");
        	}
        }
        
        Object data = null;
        if (response != null)
        {
            data = persistResponse ? getPersistableResponse(response) : response.getMessage();
        }

        EventProcessorResult eventProcessorResult = response.getResult();
        boolean success = eventProcessorResult != EventProcessorResult.FAIL; 
        EventResult result = new EventResult(
                data,
                nextEvents,
                success);
        return result;
    }
}
