/*
 * #%L
 * Alfresco Benchmark Manager
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
package org.alfresco.bm.manager.api.v1;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.alfresco.bm.common.mongo.MongoTestDAO;
import org.alfresco.bm.manager.api.AbstractRestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;



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

@RestController
@RequestMapping(path="api/v1/test-defs")
public class TestDefinitionRestAPI extends AbstractRestResource
{
    @Autowired
    private final MongoTestDAO testDAO;
    
    /**
     * @param testDAO                   low-level data service for tests
     */
    public TestDefinitionRestAPI(MongoTestDAO testDAO)
    {
        this.testDAO = testDAO;
    }
    
    @GetMapping(produces = {"application/json"})
    public String getTestDefs(
            @RequestParam(value="activeOnly", defaultValue="true") boolean activeOnly,
            @RequestParam(value="skip", defaultValue="0") int skip,
            @RequestParam(value="count", defaultValue="50") int count)
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
        catch(HttpClientErrorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    @GetMapping(path="/{release}/{schema}", produces = {"application/json"})
    public String getTestDef(
            @PathVariable("release") String release,
            @PathVariable("schema") int schema
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
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Did not find a test definition for " + release + ":" + schema);
            }
            String json = JSON.serialize(dbObject);
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + json);
            }
            return json;
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
}
