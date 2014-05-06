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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.test.mongo.MongoTestDAO;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * <b>REST API V1</b><br/>
 * <p>
 * The url pattern:
 *     <ul>
 *         <li>&lt;API URL&gt;/v1/test-defs</pre></li>
 *     </ul>
 * </p>
 * Delegate the request to service layer and responds with json.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@Path("/v1/test-defs")
public class TestDefinitionRestAPI extends AbstractRestResource
{
    private final MongoTestDAO testDAO;
    
    /**
     * @param testDAO                   low-level data service for tests
     */
    public TestDefinitionRestAPI(MongoTestDAO testDAO)
    {
        this.testDAO = testDAO;
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTestDefs(
            @DefaultValue("true") @QueryParam("activeOnly") boolean activeOnly,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue("50") @QueryParam("count") int count
            )
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[active:" + activeOnly +
                    ",skip:" + skip +
                    ",count:" + count +
                    "]");
        }
        try
        {
            DBCursor cursor = testDAO.getTestDefs(activeOnly, skip, count);
            String json = "[]";
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
    }

    @GET
    @Path("/{release}/{schema}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTestDef(
            @PathParam("release") String release,
            @PathParam("schema") int schema
            )
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[release:" + release +
                    ",schema:" + schema +
                    "]");
        }
        try
        {
            DBObject dbObject = testDAO.getTestDef(release, schema);
            if (dbObject == null)
            {
                throwAndLogException(Status.NOT_FOUND, "Did not find a test definition for " + release + ":" + schema);
            }
            String json = JSON.serialize(dbObject);
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
    }
}
