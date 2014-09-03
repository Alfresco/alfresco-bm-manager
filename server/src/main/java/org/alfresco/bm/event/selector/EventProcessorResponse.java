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

import com.mongodb.DBObject;

/**
 * Represents a full response from an event processor:
 * a success/failure indication, a response message and the raw response data.
 *  
 * @author Steve Glover
 * @author Derek Hulley
 * @since 1.3
 */
public class EventProcessorResponse
{
    private final String message;
    private final EventProcessorResult result;
    private final Object responseData;
    private final Object input;
    private final boolean persistAsString;

    /**
     * @see EventProcessorResponse#EventProcessorResponse(String, EventProcessorResult, Object, Object, boolean)
     */
    public EventProcessorResponse(String message, boolean success, Object responseData)
    {
        this(message, success, null, responseData, false);
    }
    
    /**
     * @see EventProcessorResponse#EventProcessorResponse(String, EventProcessorResult, Object, Object, boolean)
     */
    public EventProcessorResponse(String message, EventProcessorResult result, Object responseData)
    {
        this(message, result, null, responseData, false);
    }

    /**
     * @see EventProcessorResponse#EventProcessorResponse(String, EventProcessorResult, Object, Object, boolean)
     */
    public EventProcessorResponse(String message, boolean success, Object responseData, boolean persistAsString)
    {
        this(message, success, null, responseData, persistAsString);
    }
    
    /**
     * @param success                   <tt>true</tt> if successful otherwise <tt>false</tt>
     * 
     * @see EventProcessorResponse#EventProcessorResponse(String, EventProcessorResult, Object, Object, boolean)
     */
    public EventProcessorResponse(String message, boolean success, Object input, Object responseData, boolean persistAsString)
    {
        this(message, (success ? EventProcessorResult.SUCCESS : EventProcessorResult.FAIL), input, responseData, persistAsString);
    }
    
    /**
     * An event response containing all the input and output details
     * 
     * @param message                   the response message
     * @param result                    the result (positive, negative or irrelevant)
     * @param input                     the original input data
     * @param responseData              the response data
     * @param persistAsString           <tt>true</tt> if the response data must be converted to a String (default is <tt>false</tt>).
     *                                  Use a {@link DBObject MongoDB DBObject} for full JSON-aware searchable data.
     */
    public EventProcessorResponse(String message, EventProcessorResult result, Object input, Object responseData, boolean persistAsString)
    {
        this.message = message;
        this.result = result;
        this.input = input;
        this.responseData = responseData;
        this.persistAsString = persistAsString;
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
