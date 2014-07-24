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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.report.SummaryReporter;
import org.alfresco.bm.test.TestRunServicesCache;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.mongodb.util.Hash;

/**
 * <b>REST API V1</b><br/>
 * <p>
 * The url pattern:
 *     <ul>
 *         <li>&lt;API URL&gt;/v1/tests/{test}/runs/{run}/results</pre></li>
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
    private final String test;
    private final String run;
    
    /**
     * @param services                  object providing access to necessary test run services
     * @param test                      the name of the test
     * @param run                       the name of the run
     */
    public ResultsRestAPI(TestRunServicesCache services, String test, String run)
    {
        this.services = services;
        this.test = test;
        this.run = run;
    }
    
    /**
     * @return              the {@link ResultService} for the test run
     * @throws WebApplicationException if the service could not be found
     */
    private ResultService getResultService()
    {
        ResultService resultService = services.getResultService(test, run);
        if (resultService == null)
        {
            throwAndLogException(
                    Status.NOT_FOUND,
                    "Unable to find results for test run " + test + "." + run + ".  Check that the run was configured properly and started.");
        }
        return resultService;
    }
    
    @Path("/csv")
    @GET
    @Produces("text/csv")
    public StreamingOutput getResultsSummaryCSV()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[test:" + test +
                    ",run:" + run +
                    "]");
        }
        final ResultService resultService = getResultService();
        
        try
        {
            // Construct the utility that aggregates the results
            StreamingOutput so = new StreamingOutput()
            {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException
                {
                    Writer writer = new OutputStreamWriter(output);

                    SummaryReporter summaryReporter = new SummaryReporter(resultService);
                    summaryReporter.export(writer, "TODO: Allow editing of notes");
                    writer.flush();
                    writer.close();
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
    
    /**
     * Retrieve results for each requested time unit.  If {@link TimeUnit#MINUTES} are requested
     * and the report period is one, for example, there will be one result per event per minute.
     * If results are infrequent, the window size factor can be increased to ensure that aggregation
     * of results is done across a longer time frame.
     * 
     * @param timeUnitStr       a {@link TimeUnit} value
     * @param reportPeriod      the time between reports, aggregating data across the report
     *                          period multipled by the window size factor.
     * @param windowSizeFactor  the number of most reports to include in aggregation calculations.
     *                          If this is 1, then each report will contain aggregation data from the
     *                          current report period.  The larger this is, the more older results
     *                          affect the current report, acting as a smoothing factor for line charts.
     * @param fromTime          the time of the first result (inclusive)
     * @param chartOnly         <tt>true</tt> to only return results that should be charted
     * @return                  the JSON results
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getResults(
            @DefaultValue("SECONDS") @QueryParam("timeUnit") String timeUnitStr,
            @DefaultValue("1") @QueryParam("reportPeriod") int reportPeriod,
            @DefaultValue("1") @QueryParam("windowSizeFactor") int windowSizeFactor,
            @DefaultValue("0") @QueryParam("fromTime") long fromTime,
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue("1000") @QueryParam("limit") int limit,
            @DefaultValue("true") @QueryParam("chartOnly") boolean chartOnly)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[test:" + test +
                    ",timeUnit:" + timeUnitStr +
                    ",reportPeriod:" + reportPeriod +
                    ",windowSizeFactor:" + windowSizeFactor +
                    ",fromTime:" + fromTime +
                    ",skip:" + skip +
                    ",limit:" + limit +
                    ",chartOnly:" + chartOnly +
                    "]");
        }
        final ResultService resultService = getResultService();
        
        // A running total of the statistics for the windows
        final Map<String, DescriptiveStatistics> results = new HashMap<String, DescriptiveStatistics>(17);
        
        try
        {
            // Convert TimeUnit
            TimeUnit timeUnit = TimeUnit.valueOf(timeUnitStr);
            
            // Construct the utility that aggregates the results
            StreamingOutput so = new StreamingOutput()
            {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException
                {
                    Writer writer = new OutputStreamWriter(output);

                    SummaryReporter summaryReporter = new SummaryReporter(resultService);
                    summaryReporter.export(writer, "TODO: Allow editing of notes");
                    writer.flush();
                    writer.close();
                }
            };
//            return resultService.get;
            return null;
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