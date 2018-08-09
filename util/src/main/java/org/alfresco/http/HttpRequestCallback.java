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
package org.alfresco.http;

import org.apache.http.HttpResponse;

/**
 * Callback used when executing HTTP-request. After this has been called,
 * the response-stream is closed automatically.
 * 
 * @author Frederik Heremans
 * @author Michael Suzuki
 */
public interface HttpRequestCallback<T extends Object>
{
    /**
     * Called when call was successful.
     * 
     * @param method    the method executed which can be used to extract response from.
     * @return          any result extracted from the response body.
     */
    T onCallSuccess(HttpResponse response);

    /**
     * Called when an error occurs when sending the request.  Implementations can choose
     * to propogate the error, generate a new one or process the and return a result.
     * 
     * @param method    the method executed
     * @param t         optional exception that caused the error
     * @return          any result extracted from the response or error
     */
    T onCallException(HttpResponse response, Throwable t);
}
