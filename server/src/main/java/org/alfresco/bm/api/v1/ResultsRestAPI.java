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
package org.alfresco.bm.api.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.test.TestRunServicesCache;
import org.apache.commons.io.IOUtils;

/**
 * <b>REST API V1</b><br/>
 * <p>
 * The url pattern:
 *     <ul>
 *         <li>&lt;API URL&gt;/v1/tests/{test}/runs/{run}</pre></li>
 *     </ul>
 * </p>
 * This class presents APIs for retrieving test run results and related information.
 * <p/>
 * It is a meant to be a subresource, hence there is no defining path annotation.
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public class ResultsRestAPI extends AbstractRestResource
{
    private final TestRunServicesCache services;
    
    /**
     * @param services                  object providing access to necessary test run services
     */
    public ResultsRestAPI(TestRunServicesCache services)
    {
        this.services = services;
    }
    
    @Path("/csv")
    @GET
    @Produces("text/csv")
    public StreamingOutput getResultsSummaryCSV(
            @QueryParam("test") String test,
            @QueryParam("run") String run
            )
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[test:" + test +
                    ",run:" + run +
                    "]");
        }
        try
        {
//            ResultService resultService = services.getResultService(test, run);
            final String csv = "test," + test + "\n" + "run," + run;
            if (logger.isDebugEnabled())
            {
                logger.debug("Outbound: " + csv);
            }
            StreamingOutput so = new StreamingOutput()
            {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException
                {
                    InputStream is = new ByteArrayInputStream(csv.getBytes("UTF-8"));
                    try
                    {
                        IOUtils.copy(is, output);
                    }
                    finally
                    {
                        try { is.close(); } catch (IOException e) {}
                    }
                }
            };
            return so;
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
