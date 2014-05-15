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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * A class responsible for managing shared {@link HttpClient HTTP} connections. This uses a
 * thread-safe connection-manager instead of creating a new istance on every call.
 * 
 * This is done for the folowing reasons:
 * <br> 
 * <ul>
 *   <li>Creating new HTTPClient instances for each request has memory-overhead and most 
 *   important, opens a new connection for each request. Even though the connection is released
 *   they are kept in 'CLOSE_WAIT' state by the OS. This can, on high usage, empty out available outgoing TCP-ports and
 *   influence the testing on the client, having impact on the results.</li>
 *   <li>Using a single HTTPClient allows creating a pool of connections that can be kept alive to
 *   a certain route (route = combination of host and port) for a max number of time. This eliminates connection 
 *   setup and additional server round-trips, raising the overall throughput of http-calls (Browsers use 
 *   this mechanism all the time to lower loading-times).</li>
 * </ul>
 * 
 * @author Frederik Heremans
 * @author Derek Hulley
 * @author Michael Suzuki
 */
public final class SharedHttpClientProvider implements HttpClientProvider
{
    /** Allow 10s for a socket to open */
    public static final int DEFAULT_CONNECTION_TIMEOUT_MILLISEC = 10000;
    /** Allow 30s for some sort of response before an error is thrown */
    public static final int DEFAULT_SOCKET_TIMEOUT_MILLISEC = 30000;
    /** Expire connections from the pool after 10 minutes by default */
    public static final int DEFAULT_SOCKET_TTL_MILLISEC = 600000;

    private final PoolingClientConnectionManager httpClientCM;
    private final HttpParams httpParams;
    
    /**
     * Constructs with {@link #DEFAULT_CONNECTION_TIMEOUT_MILLISEC} and
     *                 {@link #DEFAULT_SOCKET_TIMEOUT_MILLISEC}.
     * @see SharedHttpClientProvider#SharedHttpClientProvider(String, int, int)
     */
    public SharedHttpClientProvider(int maxNumberOfConnections)
    {
        this(maxNumberOfConnections, DEFAULT_CONNECTION_TIMEOUT_MILLISEC, DEFAULT_SOCKET_TIMEOUT_MILLISEC);
    }
    
    public SharedHttpClientProvider(int maxNumberOfConnections, int connectionTimeoutMs, int socketTimeoutMs)
    {
        this(maxNumberOfConnections, connectionTimeoutMs, socketTimeoutMs, DEFAULT_SOCKET_TTL_MILLISEC);
    }

    /**
     * See <a href="http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html">Connection management</a>.
     * 
     * @param maxNumberOfConnections        the maximum number of Http connections in the pool
     * @param connectionTimeoutMs           the time to wait for a connection from the pool before failure
     * @param socketTimeoutMs               the time to wait for data activity on a connection before failure
     * @param socketTtlMs                   the time for a socket to remain alive before being forcibly closed (0 for infinite)
     */
    public SharedHttpClientProvider(int maxNumberOfConnections, int connectionTimeoutMs, int socketTimeoutMs, int socketTtlMs)
    {
        SSLSocketFactory sslSf = null;
        try
        {
            TrustStrategy sslTs = new TrustAnyTrustStrategy();
            sslSf = new SSLSocketFactory(sslTs, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        }
        catch (Throwable e)
        {
            throw new RuntimeException("Unable to construct HttpClientProvider.", e);
        }

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
                 new Scheme("http", 8080, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(
                new Scheme("https", 443, sslSf));
        schemeRegistry.register(
                new Scheme("https", 80, sslSf));

        httpClientCM = new PoolingClientConnectionManager(schemeRegistry, (long) socketTtlMs, TimeUnit.MILLISECONDS);
        // Increase max total connections
        httpClientCM.setMaxTotal(maxNumberOfConnections);
        // Ensure that we don't throttle on a per-scheme basis (BENCH-45)
        httpClientCM.setDefaultMaxPerRoute(maxNumberOfConnections);

        httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMs);
        HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMs);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setStaleCheckingEnabled(httpParams, true);
        HttpConnectionParams.setSoKeepalive(httpParams, true);

    }
    
    @Override
    public HttpClient getHttpClient()
    {
        return new DefaultHttpClient(httpClientCM, httpParams);
    }

    @Override
    public HttpClient getHttpClient(String username, String password)
    {
        DefaultHttpClient client = (DefaultHttpClient) getHttpClient();
        
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials(username, password));
        client.setCredentialsProvider(credentialsProvider);
        
        return client;
    }

    /**
     * A {@link TrustManager} that trusts any certificate.
     * 
     * @author Derek Hulley
     * @since 1.0
     */
    @SuppressWarnings("unused")
    private class AllowAnyX509TrustManager implements X509TrustManager
    {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            int i = 0;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            int i = 0;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[0];
        }
    }
    
    /**
     * A {@link TrustStrategy} that trusts any certificate.
     * 
     * @author Derek Hulley
     * @since 1.0
     */
    private class TrustAnyTrustStrategy implements TrustStrategy
    {
        /**
         * @return          Returns <tt>true</tt> always
         */
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            return true;
        }
    }
}
