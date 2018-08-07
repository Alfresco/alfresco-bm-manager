/*
 * #%L
 * Alfresco Benchmark Framework Manager
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
package org.alfresco.bm.http;

import java.io.IOException;

import org.alfresco.bm.driver.event.AbstractEventProcessor;
import org.alfresco.bm.driver.event.EventProcessor;
import org.alfresco.http.AuthenticationDetailsProvider;
import org.alfresco.http.HttpClientProvider;
import org.alfresco.http.HttpRequestCallback;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

/**
 * A base class used for {@link EventProcessor}s that makes use of HTTP-calls
 * that should be done when authenticated against Alfresco. Subclasses can just
 * do the HTTP-call(s) without having to worry about authentication. Use
 * {@link #executeHttpMethodAuthenticated(HttpRequestBase, String)} instead of
 * using {@link HttpClient} manually or
 * {@link #executeHttpMethodAsAdmin(HttpRequestBase)} to run as Alfresco
 * Administrator.
 * <p/>
 * Supports both BASIC Authentication only.
 * 
 * @author Frederik Heremans
 * @author Derek Hulley
 */
public abstract class AuthenticatedHttpEventProcessor extends AbstractEventProcessor
{
    private HttpClientProvider httpClientProvider;
    private AuthenticationDetailsProvider authDetailProvider;
    private final String baseUrl;

    /**
     * @param httpClientProvider    provider class for http-client
     * @param authDetailProvider    provider for authentication details
     * @param baseUrl               the URL to append to
     */
    public AuthenticatedHttpEventProcessor(
            HttpClientProvider httpClientProvider,
            AuthenticationDetailsProvider authDetailProvider,
            String baseUrl)
    {
        this.httpClientProvider = httpClientProvider;
        this.authDetailProvider = authDetailProvider;
        // Ensure path ends with forward slash
        if (baseUrl != null && !baseUrl.endsWith("/"))
        {
            baseUrl = baseUrl.concat("/");
        }
        this.baseUrl = baseUrl;
    }

    /**
     * @return the {@link HttpClientProvider} used by this class.
     */
    public HttpClientProvider getHttpProvider()
    {
        return this.httpClientProvider;
    }

    /**
     * @return the {@link AuthenticationDetailsProvider} used by this class.
     */
    public AuthenticationDetailsProvider getAuthDetailProvider()
    {
        return this.authDetailProvider;
    }
    
    /**
     * @param path relative path of the URL from alfresco host.
     * @return full URL including hostname and port for the given path.
     */
    public String getFullUrlForPath(String path)
    {
        if(path.startsWith("/"))
        {
            return baseUrl.concat(path.substring(1, path.length()));
        }
        return baseUrl.concat(path);
    }


    /**
     * Execute the given method, authenticated as the given user. Automatically
     * closes the response-stream to release the connection. If response should
     * be extracted, this should be done in the {@link HttpRequestCallback}.
     * 
     * @param request       request to execute
     * @param username      name of user to authenticate as
     * @param callback      called after http-call is executed. When callback
     *            returns, the response stream is closed, so all respose-related
     *            operations should be done in the callback. Can be null.
     * @return              whatever the callback dictates
     */
    protected <T extends Object> T executeHttpMethodAsUser(
            HttpRequestBase request,
            String username,
            HttpRequestCallback<T> callback)
    {
        return executeWithBasicAuthentication(
                request,
                username,
                authDetailProvider.getPasswordForUser(username),
                callback);
    }

    /**
     * Execute the given method, authenticated as the Alfresco Administrator.
     * 
     * @param request       request to execute
     * @param callback      called after http-call is executed. When callback
     *            returns, the response stream is closed, so all respose-related
     *            operations should be done in the callback. Can be null.
     * @return              whatever the callback dictates
     */
    protected <T extends Object> T executeHttpMethodAsAdmin(
            HttpRequestBase request,
            HttpRequestCallback<T> callback)
    {
        return executeWithBasicAuthentication(
                request,
                authDetailProvider.getAdminUsername(),
                authDetailProvider.getAdminPassword(),
                callback);
    }

    /**
     * Execute the given method, authenticated as the given user using Basic Authentication.
     * 
     * @param request       request to execute
     * @param username      name of user to authenticate
     * @param callback      called after http-call is executed. When callback
     *            returns, the response stream is closed, so all response-related
     *            operations should be done in the callback. Can be null.
     * @return              whatever the callback dictates
     */
    private <T extends Object> T executeWithBasicAuthentication(
            HttpRequestBase request,
            String username, String password,
            HttpRequestCallback<T> callback)
    {
        if (callback == null)
        {
            throw new IllegalArgumentException("No callback provided.");
        }
        HttpResponse response = null;
        try
        {
            HttpClient client = httpClientProvider.getHttpClient(username, password);

            response = client.execute(request);
            return callback.onCallSuccess(response);
        }
        catch (Throwable t)
        {
            request.abort();
            return callback.onCallException(response, t);
        }
        finally
        {
            releaseResources(request, response);
        }
    }

    /**
     * Release resources associated with an HTTP request.  This method handles exceptions
     * internall so no try-catch is required.
     * 
     * @param request           the HTTP request that might have open resources (<tt>null</tt> allowed)
     */
    protected void releaseResources(HttpRequestBase request, HttpResponse response)
    {
        if (request != null)
        {
            if (request instanceof HttpEntityEnclosingRequest)
            {
                HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
                // Consume entity to completion
                if (entityRequest.getEntity() != null)
                {
                    HttpEntity entity = entityRequest.getEntity();
                    try {EntityUtils.consume(entity); } catch (IOException e) {}
                }
            }
            // Release connection
            try {request.reset(); } catch (Throwable e) {}
        }
        if (response != null)
        {
            HttpEntity entity = response.getEntity();
            try {EntityUtils.consume(entity); } catch (IOException e) {}
        }
    }
}
