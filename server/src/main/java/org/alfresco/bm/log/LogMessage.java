/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.alfresco.bm.log;


/**
 * A cluster-wide log message
 * 
 * @author Derek Hulley
 * @since 1.4
 */
public class LogMessage
{
    public static final String FIELD_ID = "id";
    public static final String FIELD_CLUSTER_UUID = "clusterUUID";
    public static final String FIELD_SERVER_ID = "serverId";
    public static final String FIELD_TESTRUN_FQN = "testRunFQN";
    public static final String FIELD_SEVERITY = "severity";
    public static final String FIELD_TIME = "time";

    public static final String INDEX_SEVERITY = "idx_severity";
    public static final String INDEX_SERVER = "idx_server";
    public static final String INDEX_TIME = "idx_time";

    private String id;
    private String clusterUUID;
    private String serverId;
    private String testRunFQN;
    private int severity;
    private long time;
    private String message;

    /**
     * Default constructor for auto-construction by frameworks.
     */
    @SuppressWarnings("unused")
    private LogMessage()
    {
    }
    
    /**
     * @param clusterUUID       the unique identifier of the cluster
     * @param serverId          the server identifier
     * @param testRunFQN        the name of the test run
     * @param severity          the log severity (higher:more serious)
     * @param time              the log message time
     * @param message           the log message itself
     */
    public LogMessage(String clusterUUID, String serverId, String testRunFQN, int severity, long time, String message)
    {
        this.clusterUUID = clusterUUID;
        this.serverId = serverId;
        this.testRunFQN = testRunFQN;
        this.severity = severity;
        this.time = time;
        this.message = message;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(clusterUUID);
        builder.append(":");
        builder.append(serverId);
        builder.append(":");
        builder.append(testRunFQN);
        builder.append("] ");
        builder.append(message);
        return builder.toString();
    }

    public String getId()
    {
        return id;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setId(String id)
    {
        this.id = id;
    }

    public String getClusterUUID()
    {
        return clusterUUID;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setClusterUUID(String clusterUUID)
    {
        this.clusterUUID = clusterUUID;
    }

    public String getServerId()
    {
        return serverId;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setServerId(String serverId)
    {
        this.serverId = serverId;
    }

    public String getTestRunFQN()
    {
        return testRunFQN;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setTestRunFQN(String testRunFQN)
    {
        this.testRunFQN = testRunFQN;
    }

    public int getSeverity()
    {
        return severity;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setSeverity(int severity)
    {
        this.severity = severity;
    }

    public long getTime()
    {
        return time;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setTime(long time)
    {
        this.time = time;
    }

    public String getMessage()
    {
        return message;
    }
    /**
     * Used by persistence frameworks
     */
    @SuppressWarnings("unused")
    private void setMessage(String message)
    {
        this.message = message;
    }
}
