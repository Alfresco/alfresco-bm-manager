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
package org.alfresco.bm.api.v1;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.log.LogWatcher;
import org.alfresco.bm.test.LifecycleController;

/**
 * <b>REST API V1</b><br/>
 * <p>
 * The url pattern:
 *     <ul>
 *         <li>&lt;API URL&gt;/v1/status</pre></li>
 *     </ul>
 * </p>
 * Delegate the request to service layer and responds with json.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@Path("/v1/status")
public class StatusAPI extends AbstractRestResource
{
    private final LifecycleController lifeCycleController;
    private final LogWatcher logWatcher;
    
    /**
     * @param lifeCycleController       used to report on startup issues
     * @param logWatcher                used to access log files
     */
    public StatusAPI(LifecycleController lifeCycleController, LogWatcher logWatcher)
    {
        this.lifeCycleController = lifeCycleController;
        this.logWatcher = logWatcher;
    }
    
    @GET
    @Path("/startup")
    @Produces(MediaType.TEXT_PLAIN)
    public String getStartupStatus()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: <none>");
        }
        try
        {
            String log = lifeCycleController.getLog();
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + log);
            }
            return log;
        }
        catch (Exception e)
        {
            logger.error(e);
            return e.getStackTrace().toString();
        }
    }
    
    @GET
    @Path("/logs")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLogFilenames()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: <none>");
        }
        try
        {
            List<String> logFiles = logWatcher.getLogFilenames();
            String json = gson.toJson(logFiles);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch (Exception e)
        {
            logger.error(e);
            return e.getStackTrace().toString();
        }
    }
}
