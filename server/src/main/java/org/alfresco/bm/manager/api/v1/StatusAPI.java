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
package org.alfresco.bm.manager.api.v1;

import java.util.Date;

import org.alfresco.bm.common.spring.LifecycleController;
import org.alfresco.bm.common.util.log.LogService;
import org.alfresco.bm.common.util.log.LogService.LogLevel;
import org.alfresco.bm.manager.api.AbstractRestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.mongodb.DBCursor;
import com.mongodb.util.JSON;

/**
 * <b>REST API V1</b><br/>
 * <p>
 * The url pattern:
 * <ul>
 * <li>&lt;API URL&gt;/v1/status
 * </pre>
 * </li>
 * </ul>
 * </p>
 * Delegate the request to service layer and responds with json.
 * 
 * @author Derek Hulley
 * @since 2.0
 */

@RestController
@RequestMapping("api/v1/status")
public class StatusAPI extends AbstractRestResource
{
    @Autowired
    private final LifecycleController lifeCycleController;
    @Autowired
    private final LogService logService;

    /**
     * @param lifeCycleController
     *            used to report on startup issues
     * @param logService
     *            get log messages
     */
    public StatusAPI(LifecycleController lifeCycleController, LogService logService)
    {
        this.lifeCycleController = lifeCycleController;
        this.logService = logService;
    }

    @GetMapping(path = "/startup", produces = {"text/plain"})
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
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(path = "/logs", produces = { "application/json" })
    public String getLogs(@RequestParam(value = "driverId", required = false) String driverId, @RequestParam("test") String test, @RequestParam("run") String run,
            @RequestParam(value = "level", defaultValue = "INFO") String levelStr, @RequestParam(value = "from", defaultValue = "0") Long from,
            @RequestParam(value = "to", defaultValue = "" + Long.MAX_VALUE) Long to, @RequestParam(value = "skip", defaultValue = "0") int skip,
            @RequestParam(value = "count", defaultValue = "50") int count)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Inbound: " + "[driverId:" + driverId + ",test:" + test + ",run:" + run + ",level:" + levelStr + ",from:" + new Date(from)
                    + ",to:" + new Date(to) + ",skip:" + skip + ",count:" + count + "]");
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
        
        catch (HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {

            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        finally
        {
            if (cursor != null)
            {
                try
                {
                    cursor.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }
}
