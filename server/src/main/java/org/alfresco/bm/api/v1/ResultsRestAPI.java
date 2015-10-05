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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.alfresco.bm.api.AbstractRestResource;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.event.ResultService.ResultHandler;
import org.alfresco.bm.report.CSVReporter;
import org.alfresco.bm.report.XLSXReporter;
import org.alfresco.bm.test.TestRunServicesCache;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

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
    /** states not to filter by event names when query event results */
    public static final String ALL_EVENT_NAMES = "(All Events)"; 
    
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
    
    @GET
    @Path("/csv")
    @Produces("text/csv")
    public StreamingOutput getReportCSV()
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
            // First confirm that the test exists
            services.getTestService().getTestRunState(test, run);
            
            // Construct the utility that aggregates the results
            StreamingOutput so = new StreamingOutput()
            {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException
                {
                    CSVReporter csvReporter = new CSVReporter(services, test, run);
                    csvReporter.export(output);
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
    
    
    @GET
    @Path("/xlsx")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public StreamingOutput getReportXLSX()
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
            // First confirm that the test exists
            services.getTestService().getTestRunState(test, run);
            
            // Construct the utility that aggregates the results
            StreamingOutput so = new StreamingOutput()
            {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException
                {
                    Writer writer = new OutputStreamWriter(output);

                    XLSXReporter xlsxReporter = new XLSXReporter(services, test, run);
                    xlsxReporter.export(output);
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
    
    @GET
    @Path("/eventNames")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventResultEventNames()
    {
        final BasicDBList events = new BasicDBList();
        
        // always add the "all events" name in the first position
        events.add(ALL_EVENT_NAMES);

        // distinct get all recorded event names from Mongo
        List<String> eventNames = getResultService().getEventNames();
        for (String eventName : eventNames)
        {
            events.add(eventName);
        }

        return JSON.serialize(events);
    }
    
    @GET
    @Path("/allEventsFilterName")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllEventsFilterName()
    {
        final BasicDBList events = new BasicDBList();
        events.add(ALL_EVENT_NAMES);
        return JSON.serialize(events);
    }
    
    /**
     * Retrieve an approximate number of results, allowing for a smoothing factor
     * (<a href=http://en.wikipedia.org/wiki/Moving_average#Simple_moving_average>Simple Moving Average</a>) -
     * the number of data results to including in the moving average.
     * 
     * @param fromTime              the approximate time to start from
     * @param timeUnit              the units of the 'reportPeriod' (default SECONDS).  See {@link TimeUnit}.
     * @param reportPeriod          how often a result should be output.  This is expressed as a multiple of the 'timeUnit'.
     * @param smoothing             the number of results to include in the Simple Moving Average calculations
     * @param chartOnly             <tt>true</tt> to filter out results that are not of interest in performance charts
     * 
     * @return                      JSON representing the event start time (x-axis) and the smoothed average execution time
     *                              along with data such as the events per second, failures per second, etc.
     */
    @GET
    @Path("/ts")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTimeSeriesResults(
            @DefaultValue("0") @QueryParam("fromTime") long fromTime,
            @DefaultValue("SECONDS") @QueryParam("timeUnit") String timeUnit,
            @DefaultValue("1") @QueryParam("reportPeriod") long reportPeriod,
            @DefaultValue("1") @QueryParam("smoothing") int smoothing,
            @DefaultValue("true") @QueryParam("chartOnly") boolean chartOnly)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Inbound: " +
                    "[test:" + test +
                    ",fromTime:" + fromTime +
                    ",timeUnit:" + timeUnit +
                    ",reportPeriod:" + reportPeriod +
                    ",smoothing:" + smoothing +
                    ",chartOnly:" + chartOnly +
                    "]");
        }
        if (reportPeriod < 1)
        {
            throwAndLogException(Status.BAD_REQUEST, "'reportPeriod' must be 1 or more.");
        }
        if (smoothing < 1)
        {
            throwAndLogException(Status.BAD_REQUEST, "'smoothing' must be 1 or more.");
        }
        TimeUnit timeUnitEnum = null;
        try
        {
            timeUnitEnum = TimeUnit.valueOf(timeUnit.toUpperCase());
        }
        catch (Exception e)
        {
            // Invalid time unit
            throwAndLogException(Status.BAD_REQUEST, e);
        }
        
        final ResultService resultService = getResultService();

        // Calculate the window size
        long reportPeriodMs = timeUnitEnum.toMillis(reportPeriod);
        long windowSize = reportPeriodMs * smoothing;
        
        // This is just too convenient an API
        final BasicDBList events = new BasicDBList();
        ResultHandler handler = new ResultHandler()
        {
            @Override
            public boolean processResult(
                    long fromTime, long toTime,
                    Map<String, DescriptiveStatistics> statsByEventName,
                    Map<String, Integer> failuresByEventName) throws Throwable
            {
                for (Map.Entry<String, DescriptiveStatistics> entry : statsByEventName.entrySet())
                {
                    String eventName = entry.getKey();
                    DescriptiveStatistics stats = entry.getValue();
                    Integer failures = failuresByEventName.get(eventName);
                    if (failures == null)
                    {
                        logger.error("Found null failure count: " + entry);
                        // Do nothing with it and stop
                        return false;
                    }
                    // Per second
                    double numPerSec = (double) stats.getN() / ( (double) (toTime-fromTime) / 1000.0);
                    double failuresPerSec = (double) failures / ( (double) (toTime-fromTime) / 1000.0);
                    // Push into an object
                    DBObject eventObj = BasicDBObjectBuilder
                            .start()
                            .add("time", toTime)
                            .add("name", eventName)
                            .add("mean", stats.getMean())
                            .add("min", stats.getMin())
                            .add("max", stats.getMax())
                            .add("stdDev", stats.getStandardDeviation())
                            .add("num", stats.getN())
                            .add("numPerSec", numPerSec)
                            .add("fail", failures)
                            .add("failPerSec", failuresPerSec)
                            .get();
                    // Add the object to the list of events
                    events.add(eventObj);
                }
                // Go for the next result
                return true;
            }
        };
        try
        {
            // Get all the results
            resultService.getResults(handler, fromTime, windowSize, reportPeriodMs, chartOnly);
            // Muster into JSON
            String json = events.toString();
            
            // Done
            if (logger.isDebugEnabled())
            {
                int jsonLen = json.length();
                if (jsonLen < 500)
                {
                    logger.debug("Outbound: " + json);
                }
                else
                {
                    logger.debug("Outbound: " + json.substring(0, 250) + " ... " + json.substring(jsonLen - 250, jsonLen));
                }
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
    @Path("/eventResults")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventResults(
            @DefaultValue(ALL_EVENT_NAMES) @QueryParam("filterEventName") String filterEventName,
            @DefaultValue("All") @QueryParam("filterSuccess") String filterSuccess,
            @DefaultValue("0") @QueryParam("skipResults")int skipResults,
            @DefaultValue("10") @QueryParam("numberOfResults") int numberOfResults)
    {
        
        EventResultFilter filter = getFilter(filterSuccess);
        final ResultService resultService = getResultService();
        String nameFilterString = filterEventName.equals(ALL_EVENT_NAMES) ? "" : filterEventName;
        
        // get event details
        List<EventDetails> details = resultService.getEventDetails(filter, nameFilterString, skipResults, numberOfResults);
        
        // serialize back ....
        BasicDBList retList = new BasicDBList();
        for(EventDetails detail : details)
        {
            retList.add(detail.toDBObject());
        }
        return JSON.serialize( retList );
    } 
    
    /**
     * Returns the enum for a given string or the default value.
     * 
     * @param filterEvents (String) one of the string values of EventResultFilter
     * 
     * @return (EventResultFilter) - "all" as default, or success / fail 
     */
    private EventResultFilter getFilter(String filterEvents)
    {
        try
        {
            return EventResultFilter.valueOf( filterEvents);
        }
        catch(Exception e)
        {
            logger.error("Error converting " + filterEvents + " to EventResultFilter.", e);
        }
        
        return EventResultFilter.All;
    }
    
}