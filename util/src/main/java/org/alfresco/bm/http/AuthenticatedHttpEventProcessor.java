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
package org.alfresco.bm.http;

import java.io.IOException;

import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.EventProcessor;
import org.alfresco.http.AuthenticationDetailsProvider;
import org.alfresco.http.HttpClientProvider;
import org.alfresco.http.HttpRequestCallback;
import org.alfresco.json.JSONUtil;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * <p>
 * A base class used for {@link EventProcessor}s that makes use of HTTP-calls
 * that should be done when authenticated against Alfresco. Subclasses can just
 * do the HTTP-call(s) without having to worry about authentication. Use
 * {@link #executeHttpMethodAuthenticated(HttpRequestBase, String)} instead of
 * using {@link HttpClient} manually or
 * {@link #executeHttpMethodAsAdmin(HttpRequestBase)} to run as Alfresco
 * Administrator.
 * </p>
 * <p>
 * Supports both BASIC Authentication (default) and Ticket-based authentication.
 * In case ticket-based authentication is used, the ticket value is stored on
 * the user data-provider and a new ticket is fetched transparantly when the
 * ticket is expired.
 * </p>
 * 
 * @author Frederik Heremans
 * @author Derek Hulley
 */
public abstract class AuthenticatedHttpEventProcessor extends AbstractEventProcessor
{
    private static final String UTF_8_ENCODING = "UTF-8";

    /**
     * URL for obtaining an alfresco-ticket
     */
    private static final String LOGIN_URL = "/alfresco/service/api/login";
    
    private static final String JSON_LOGIN_USERNAME = "username";
    private static final String JSON_LOGIN_PASSWORD = "password";
    private static final String JSON_TICKET_KEY = "ticket";

    private static final String TICKET_CREDENTIAL_PLACEHOLDER = "ROLE_TICKET";

    private HttpClientProvider httpClientProvider;
    private AuthenticationDetailsProvider authDetailProvider;
    private boolean ticketBasedAuthentication;
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
        this.ticketBasedAuthentication = false;
        // Ensure path ends with forward slash
        if (baseUrl != null && !baseUrl.endsWith("/"))
        {
            baseUrl = baseUrl.concat("/");
        }
        this.baseUrl = baseUrl;
    }

    /**
     * Enable ticket-based authentication. If set to false, BASIC Authentication
     * will be used instead. Defaults to false.
     * 
     * @param ticketBasedAuthentication whether or not to use ticket for
     *            authentication
     */
    public void setTicketBasedAuthentication(boolean ticketBasedAuthentication)
    {
        this.ticketBasedAuthentication = ticketBasedAuthentication;
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
        if (ticketBasedAuthentication)
        {
            return executeWithTicketAuthentication(
                    request,
                    username,
                    authDetailProvider.getPasswordForUser(username),
                    callback);
        }
        else
        {
            return executeWithBasicAuthentication(
                    request,
                    username,
                    authDetailProvider.getPasswordForUser(username),
                    callback);
        }
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
        if (ticketBasedAuthentication)
        {
            return executeWithTicketAuthentication(
                    request,
                    authDetailProvider.getAdminUsername(),
                    authDetailProvider.getAdminPassword(),
                    callback);
        }
        else
        {
            return executeWithBasicAuthentication(
                    request,
                    authDetailProvider.getAdminUsername(),
                    authDetailProvider.getAdminPassword(),
                    callback);
        }
    }

    /**
     * Execute the given method, authenticated as the given user using Basic
     * Authentication.
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
     * Execute the given method, authenticated as the given user using
     * ticket-based authentication.
     * 
     * @param request       request to execute
     * @param username      name of user to authenticate
     * @return              whatever the callback dictates
     */
    private <T extends Object> T executeWithTicketAuthentication(
            HttpRequestBase request,
            String username, String password,
            HttpRequestCallback<T> callback)
    {
        if (callback == null)
        {
            throw new IllegalArgumentException("No callback provided.");
        }

        String ticket = authDetailProvider.getTicketForUser(username);
        
        HttpResponse response = null;
        try
        {
            if (ticket == null)
            {
                ticket = fetchLoginTicket(username, password);
                authDetailProvider.updateTicketForUser(username, ticket);
            }
            applyTicketToMethod(request, ticket);

            // Try executing the method
            HttpClient client = httpClientProvider.getHttpClient();
            response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            
            if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN)
            {
                request.reset();
                // Fetch new ticket, store and apply to HttpMethod
                ticket = fetchLoginTicket(username, username);
                authDetailProvider.updateTicketForUser(username, ticket);

                applyTicketToMethod(request, ticket);

                // Run method again with new ticket
                response = httpClientProvider.getHttpClient().execute(request);
                status = response.getStatusLine().getStatusCode();
                if (status != HttpStatus.SC_OK)
                {
                    throw new RuntimeException(
                            "Failed to execute method: \r\n" +
                            "   request:  " + request + "\r\n" +
                            "   response: " + response + "\r\n" +
                            "   user:     " + username);
                }
            }

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
     * Add the ticket to the method. In case of {@link EntityEnclosingMethod}s
     * (which don't support Query-parameters), the ticket is added as Username
     * in BASIC Authentication, this is a supported way of passing in ticket
     * into Alfresco.
     * 
     * @param method method to apply
     * @param ticket ticket to apply
     * @return a {@link HttpState} object to use. Null, if no specific state
     *         should be used.
     */
    private void applyTicketToMethod(HttpRequestBase request, String ticket)
    {
        // POST and PUT methods don't support Query-params, use Basic
        // Authentication to pass
        // in the ticket (ROLE_TICKET) for all methods.
        Credentials cred = new UsernamePasswordCredentials(TICKET_CREDENTIAL_PLACEHOLDER, ticket);
        request.addHeader(BasicScheme.authenticate(cred, UTF_8_ENCODING, false));

    }

    /**
     * Perform the login-call to obtain a ticket.
     * 
     * @param userName user to log in
     * @return ticket to use for authentication.
     * @throws RuntimeException when no ticket can be obtained for the user.
     */
    @SuppressWarnings("unchecked")
    private String fetchLoginTicket(String userName, String password)
    {
        String url = getFullUrlForPath(LOGIN_URL);
        HttpPost loginMethod = null;
        HttpEntity entity = null;
        HttpResponse response = null;
        try
        {
            loginMethod = new HttpPost(url);
            JSONUtil.setJSONExpected(loginMethod);

            // Populate resuest body
            JSONObject requestBody = new JSONObject();
            requestBody.put(JSON_LOGIN_USERNAME, userName);
            requestBody.put(JSON_LOGIN_PASSWORD, password);

            JSONUtil.populateRequestBody(loginMethod, requestBody);

            HttpClient client = httpClientProvider.getHttpClient();

            // Since no authentication info is available yet, no need to use a
            // custom HostConfiguration for the login-call
            response = client.execute(loginMethod);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
            {
                // Extract the ticket
                entity = response.getEntity();
                String rsp = EntityUtils.toString(entity, "UTF-8");
                JSONObject data = null;
                try
                {
                    JSONParser parser = new JSONParser();
                    JSONObject rspJSON = (JSONObject) parser.parse(rsp);
                    data = (JSONObject) rspJSON.get(JSONUtil.JSON_DATA);
                }
                catch (ParseException e)
                {
                    throw new RuntimeException("Authentication failed: Response was: \n" + rsp);
                }

                // Extract the actual ticket
                String ticket = JSONUtil.getString(data, JSON_TICKET_KEY, null);
                if (ticket == null)
                {
                    throw new RuntimeException(
                            "Failed to login to Alfresco with user " + userName +
                            " (No ticket found in JSON-response)");
                }
                return ticket;
            }
            else
            {
                // Unable to login
                throw new RuntimeException(
                        "Failed to login to Alfresco with user " + userName +
                        " (" + response.getStatusLine().getStatusCode() + response.getStatusLine().getReasonPhrase() + ")");
            }
        }
        catch (Throwable e)
        {
            loginMethod.abort();
            // Something went wrong when sending request
            throw new RuntimeException("Failed to login to Alfresco with user " + userName, e);
        }
        finally
        {
            releaseResources(loginMethod, response);
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
