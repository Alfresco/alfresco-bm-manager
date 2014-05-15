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
import org.alfresco.bm.session.PersistedSessionData;
import org.alfresco.bm.session.SessionService;

import com.google.gson.Gson;

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

        if(response != null && response.isPersistAsString())
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
    
    protected PersistedSessionData createSessionData(String sessionId)
    {
        long startTime = sessionService.getSessionStartTime(sessionId);
        long maxEndTime = startTime + maxSessionTime;
        PersistedSessionData sessionData = new PersistedSessionData(maxEndTime);
        return sessionData;
    }

    /**
     * Get session data associated with the given sessionId, creating it with the expected minimum session end time if it doesn't exist.
     * Override this method to control what is persisted.
     * 
     * @param sessionId
     * @return
     */
    protected PersistedSessionData getSessionData(String sessionId)
    {
        PersistedSessionData sessionData = null;

        String sessionDataJSON = sessionService.getSessionData(sessionId);
        if (sessionDataJSON == null)
        {
            sessionData = createSessionData(sessionId);
            sessionDataJSON = sessionData.toJSON();
            sessionService.setSessionData(sessionId, sessionDataJSON);
        }
        else
        {
            sessionData = PersistedSessionData.fromJSON(sessionDataJSON);
        }

        return sessionData;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        Object input = event.getDataObject();
        List<Event> nextEvents = new ArrayList<Event>();
        String sessionId = event.getSessionId();

        EventProcessorResponse response = processEventImpl(event);

        // stop the bm timer for this task, the main event processing that we want to time is complete
        super.stopTimer();

        if (response == null)
        {
            logger.warn("Response is null for event " + event);
        }
        else
        {
            PersistedSessionData sessionData = getSessionData(sessionId);
            long maxEndTime = sessionData.getMaxEndTime();
            long current = System.currentTimeMillis();
            // if we haven't passed the expected session end time and the previous event was successful
            if (current < maxEndTime && response.getResult() == EventProcessorResult.SUCCESS && eventSelector != null)
            {
                Object responseData = response.getResponseData();
                Event evt = eventSelector.nextEvent(input, responseData);
                if(evt != null)
                {
                    nextEvents.add(evt);
                }
            }
        }

        if (nextEvents.size() < 1)
        {
            // belt and braces: no more events, close the session if there is one>>>>>>> .r54943
            if(sessionId != null)
            {
                sessionService.endSession(sessionId);
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
