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

import java.util.Date;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.log.LogService;
import org.alfresco.bm.log.LogService.LogLevel;
import org.alfresco.bm.test.LifecycleController;

import com.mongodb.DBCursor;
import com.mongodb.util.JSON;

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
    private final LogService logService;
    
    /**
     * @param lifeCycleController       used to report on startup issues
     * @param logService                get log messages
     */
    public StatusAPI(LifecycleController lifeCycleController, LogService logService)
    {
        this.lifeCycleController = lifeCycleController;
        this.logService = logService;
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
            throwAndLogException(Status.INTERNAL_SERVER_ERROR, e);
            return null;
        }
    }
    
    @GET
    @Path("/logs")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLogs(
            @QueryParam("driverId") String driverId,
            @QueryParam("test") String test,
            @QueryParam("run") String run,
            @DefaultValue("INFO") @QueryParam("level") String levelStr,
            @DefaultValue("0") @QueryParam("from") Long from,
            @DefaultValue("" + Long.MAX_VALUE) @QueryParam("to") Long to,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue("50") @QueryParam("count") int count
            )
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[driverId:" + driverId +
                    ",test:" + test +
                    ",run:" + run +
                    ",level:" + levelStr +
                    ",from:" + new Date(from) +
                    ",to:" + new Date(to) +
                    ",skip:" + skip +
                    ",count:" + count +
                    "]");
        }
        LogLevel level = LogLevel.INFO;
        try
        {
            level = LogLevel.valueOf(levelStr);
        }
        catch (Exception e)
        {
            // Just allow this
        }
        
        DBCursor cursor = null;
        try
        {
            String json = "[]";
            cursor = logService.getLogs(driverId, test, run, level, from, to, skip, count);
            if (cursor.count() > 0)
            {
                json = JSON.serialize(cursor);
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
        }
        catch (WebApplicationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throwAndLogException(Status.INTERNAL_SERVER_ERROR, e);
            return null;
        }
        finally
        {
            if (cursor != null)
            {
                try { cursor.close(); } catch (Exception e) {}
            }
        }
    }
}
