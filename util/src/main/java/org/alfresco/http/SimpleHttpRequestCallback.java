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
package org.alfresco.http;

import org.apache.http.HttpResponse;

/**
 * Response callback that returns the {@link HttpResponse} on success
 * and throws runtime exceptions on errors.
 * 
 * @author Derek Hulley
 * @since 1.2
 */
public class SimpleHttpRequestCallback extends AbstractHttpRequestCallback<HttpResponse>
{
    private static final HttpRequestCallback<HttpResponse> INSTANCE = new SimpleHttpRequestCallback();
    
    /**
     * Get a singleton instance of this class
     * 
     * @return          Returns the singleton instance that can be used for direct response handling
     */
    public static HttpRequestCallback<HttpResponse> getInstance()
    {
        return INSTANCE;
    }
    
    /**
     * Returns the response without any modifications
     */
    @Override
    public HttpResponse onCallSuccess(HttpResponse response)
    {
        return response;
    }
}
