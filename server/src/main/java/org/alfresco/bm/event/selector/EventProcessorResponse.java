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

/**
 * Represents a full response from an event processor:
 * a success/failure indication, a response message and the raw response data.
 *  
 * @author Steve Glover
 * @since 1.3
 */
public class EventProcessorResponse
{
    private String message;
    private EventProcessorResult result;
    private Object responseData;
    private Object input;
    private boolean persistAsString = false;

    /**
     * Constructor for an event response
     * 
     * @param message              any message associated with the response
     * @param success              whether the event processing was successful or not
     * @param responseData         any other data associated with the response
     */
    public EventProcessorResponse(String message, boolean success, Object responseData)
    {
        this.message = message;
        this.result = success ? EventProcessorResult.SUCCESS : EventProcessorResult.FAIL;
        this.responseData = responseData;
    }
    
    public EventProcessorResponse(String message, EventProcessorResult result, Object responseData)
    {
        this.message = message;
        this.result = result;
        this.responseData = responseData;
    }

    /**
     * Constructor for an event response
     * 
     * @param message              any message associated with the response
     * @param success              whether the event processing was successful or not
     * @param responseData         any other data associated with the response
     * @param forceStringResponse  call 
     */
    public EventProcessorResponse(String message, boolean success, Object responseData, boolean persistAsString)
    {
        this(message, success, responseData);
        this.persistAsString = persistAsString;
    }
    
    public EventProcessorResponse(String message, boolean success, Object input, Object responseData, boolean persistAsString)
    {
        this(message, success, responseData);
        this.persistAsString = persistAsString;
        this.input = input;
    }
    
    public EventProcessorResponse(String message, EventProcessorResult result, Object input, Object responseData, boolean persistAsString)
    {
        this(message, result, responseData);
        this.persistAsString = persistAsString;
        this.input = input;
    }

    @Override
    public String toString()
    {
        return "EventProcessorResponse [message=" + message + ", result="
                + result + ", responseData=" + responseData + ", input="
                + input + ", persistAsString=" + persistAsString + "]";
    }

    public Object getInput()
    {
        return input;
    }

    public String getMessage()
    {
        return message;
    }
    
    public EventProcessorResult getResult()
    {
        return result;
    }
    
    public Object getResponseData()
    {
        return responseData;
    }

    public boolean isPersistAsString()
    {
        return persistAsString;
    }
}
