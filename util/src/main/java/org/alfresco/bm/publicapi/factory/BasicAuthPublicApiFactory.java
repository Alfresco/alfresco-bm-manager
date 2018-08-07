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
package org.alfresco.bm.publicapi.factory;

import org.alfresco.bm.user.UserData;
import org.alfresco.bm.user.UserDataService;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.CMISEndpoint;
import org.springframework.social.alfresco.api.impl.ConnectionDetails;
import org.springframework.social.alfresco.connect.BasicAuthAlfrescoConnectionFactory;
import org.springframework.social.connect.Connection;

/**
 * A public api factory that uses basic authentication to communicate with a repository.
 * 
 * @author steveglover
 *
 */
public class BasicAuthPublicApiFactory implements PublicApiFactory
{
    private String scheme;
    private String host;
    private int port;
    private int maxNumberOfConnections;
    private int connectionTimeoutMs;
    private int socketTimeoutMs;
    private int socketTtlMs;
    private String context;
    private boolean ignoreServletName;
    private String publicApiServletName;
    private String serviceServletName;
    private CMISEndpoint preferredCMISEndPoint;

    private UserDataService userDataService;
    
    public BasicAuthPublicApiFactory(String scheme, String host, int port, CMISEndpoint preferredCMISEndPoint,
            int maxNumberOfConnections, int connectionTimeoutMs, 
            int socketTimeoutMs, int socketTtlMs, UserDataService userDataService)
    {
        this(scheme, host, port, preferredCMISEndPoint, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs,
                socketTtlMs, userDataService, "alfresco", "api", "service");
        this.preferredCMISEndPoint = preferredCMISEndPoint;
    }

    public BasicAuthPublicApiFactory(String scheme, String host, int port, CMISEndpoint preferredCMISEndPoint,
            int maxNumberOfConnections, int connectionTimeoutMs,  int socketTimeoutMs, int socketTtlMs,
            UserDataService userDataService, String context, String publicApiServletName, String serviceServletName)
    {
        super();
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.preferredCMISEndPoint = preferredCMISEndPoint;
        this.maxNumberOfConnections= maxNumberOfConnections;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
        this.socketTtlMs= socketTtlMs; 
        this.userDataService = userDataService;
        this.context = context;
        this.publicApiServletName = publicApiServletName;
        this.serviceServletName = serviceServletName;
    }

    public String getContext()
    {
        return context;
    }

    public boolean isIgnoreServletName()
    {
        return ignoreServletName;
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }
    
    public CMISEndpoint getPreferredCMISEndPoint()
    {
        return preferredCMISEndPoint;
    }

    @Override
    public Alfresco getPublicApi(String username)
    {
        Alfresco alfresco = null;
        UserData user = userDataService.findUserByUsername(username);
        if(user != null)
        {
            ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, username, user.getPassword(), context,
                    publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
            BasicAuthAlfrescoConnectionFactory basicAuthConnectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
            Connection<Alfresco> connection = basicAuthConnectionFactory.createConnection();
            alfresco = connection.getApi();
        }
        else
        {
            throw new RuntimeException("Username not held in local data mirror: " + username);
        }
        
        if (alfresco == null)
        {
            throw new RuntimeException("Unable to retrieve API connection to Alfresco.");
        }

        return alfresco;
    }
    
    @Override
    public Alfresco getTenantAdminPublicApi(String domain)
    {
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, "admin@" + domain, "admin", context,
                publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory connectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = connectionFactory.createConnection();
        Alfresco alfresco = connection.getApi();
        return alfresco;
    }
    
    @Override
    public Alfresco getAdminPublicApi()
    {
        ConnectionDetails connectionDetails = new ConnectionDetails(scheme, host, port, "admin", "admin", context,
                publicApiServletName, serviceServletName, maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, socketTtlMs);
        BasicAuthAlfrescoConnectionFactory connectionFactory = new BasicAuthAlfrescoConnectionFactory(connectionDetails, null);
        Connection<Alfresco> connection = connectionFactory.createConnection();
        Alfresco alfresco = connection.getApi();
        return alfresco;
    }
}
