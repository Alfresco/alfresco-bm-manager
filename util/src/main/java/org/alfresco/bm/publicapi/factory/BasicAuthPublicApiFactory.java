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
