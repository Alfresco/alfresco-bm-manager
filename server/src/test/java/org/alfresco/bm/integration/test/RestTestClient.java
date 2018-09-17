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
package org.alfresco.bm.integration.test;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.bm.common.TestDetails;
import org.alfresco.bm.common.TestRunDetails;
import org.alfresco.bm.common.TestRunSchedule;
import org.alfresco.bm.integration.test.model.TestDefinition;
import org.alfresco.bm.integration.test.model.TestRunSummary;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Rest client that is used to make Rest-Api calls on Alfresco BM.
 * It is used on {@link IntegrationTests} to test the docker images for Alfresco BM and related drivers.
 * 
 * @since 3.0
 */
public class RestTestClient
{
    public RestTestClient()
    {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 9080;
    }

    public List<TestDefinition> getDriverDefinitions()
    {
        List<TestDefinition> testDefitions = new ArrayList<TestDefinition>();

        testDefitions = RestAssured
                       .when().
                               get("/alfresco-bm-manager/api/v1/test-defs")
                       .then().
                               statusCode(HttpStatus.SC_OK)
                       .and().
                               extract().
                               body().
                               jsonPath().
                               getList(".", TestDefinition.class);
        return testDefitions;
    }

    public void createTest(TestDetails testDetails)
    {
        JSONObject body = new JSONObject();
        try
        {
            body.put("description", testDetails.getDescription());
            body.put("name", testDetails.getName());
            body.put("release", testDetails.getRelease());
            body.put("schema", testDetails.getSchema());
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        RestAssured.given().
                           contentType(ContentType.JSON).
                           body(body.toString())
                   .when().
                           post("/alfresco-bm-manager/api/v1/tests")
                   .then().
                           assertThat().statusCode(HttpStatus.SC_OK);
    }

    public void createTestRun(TestDetails testDetails, TestRunDetails testRunDetails)
    {
        JSONObject body = new JSONObject();
        try
        {
            body.put("description", testRunDetails.getDescription());
            body.put("name", testRunDetails.getName());
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        RestAssured.given().
                           pathParam("test", testDetails.getName()).
                           contentType(ContentType.JSON).
                           body(body.toString())
                   .when().
                           post("/alfresco-bm-manager/api/v1/tests/{test}/runs")
                   .then().
                           assertThat().statusCode(HttpStatus.SC_OK);
    }

    public void scheduleTest(TestDetails testDetails, TestRunDetails testRunDetails, TestRunSchedule testRunSchedule)
    {
        JSONObject body = new JSONObject();
        try
        {
            body.put("scheduled", testRunSchedule.getScheduled());
            body.put("version", testRunSchedule.getVersion());
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        RestAssured.given().
                           pathParams("test", testDetails.getName(), "testrun", testRunDetails.getName()).
                           contentType(ContentType.JSON).
                           body(body.toString())
                   .when().
                           post("alfresco-bm-manager/api/v1/tests/{test}/runs/{testrun}/schedule")
                   .then().
                           assertThat().statusCode(HttpStatus.SC_OK);
    }

    public String getTestRunState(TestDetails testDetails, TestRunDetails testRunDetails)
    {
        String testRunState = 
                RestAssured.given().
                                   pathParams("test", testDetails.getName(), "testrun", testRunDetails.getName())
                           .when()
                                   .get("/alfresco-bm-manager/api/v1/tests/{test}/runs/{testrun}/state")
                           .then().statusCode(HttpStatus.SC_OK)
                           .and().
                                  extract().
                                  body().asString();

        return testRunState;
    }

    public TestRunSummary getTestRunSummary(TestDetails testDetails, TestRunDetails testRunDetails)
    {
        TestRunSummary testRunSummary = 
                RestAssured.given().
                                   pathParams("test", testDetails.getName(), "testrun", testRunDetails.getName())
                           .when()
                                   .get("/alfresco-bm-manager/api/v1/tests/{test}/runs/{testrun}/summary")
                           .then().statusCode(HttpStatus.SC_OK)
                           .and().
                                  extract().
                                  body().
                                  jsonPath().getObject(".", TestRunSummary.class);

        return testRunSummary;
    }

    public byte[] getTestRunResults(TestDetails testDetails, TestRunDetails testRunDetails, String reportType)
    {
        byte[] result = 
                RestAssured.given().
                                   pathParams("test", testDetails.getName(), "testrun", testRunDetails.getName(), "type", reportType)
                           .when()
                                   .get("/alfresco-bm-manager/api/v1/tests/{test}/runs/{testrun}/results/{type}")
                           .then().statusCode(HttpStatus.SC_OK)
                           .and().
                                  extract().
                                  body().asByteArray();

        return result;
    }

    public TestDefinition getDriverDefinition(String release, String schema)
    {
        TestDefinition testDef = 
                RestAssured.given().pathParams("release", release,  "schema", schema).get("/alfresco-bm-manager/api/v1/test-defs").then().statusCode(HttpStatus.SC_OK)
                           .and().
                           extract().
                           body().
                           jsonPath().getObject(".", TestDefinition.class);
        
        return testDef;
    }

    public void updateTestProperty(TestDetails testDetails, String property, String value)
    {
        JSONObject body = new JSONObject();
        try
        {
            body.put("value", value);
            body.put("version", 0);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        RestAssured.given().
                           pathParams("test", testDetails.getName(), "property", property).
                           contentType(ContentType.JSON).
                           body(body.toString())
                   .when().
                           put("alfresco-bm-manager/api/v1/tests/{test}/props/{property}")
                   .then().
                           assertThat().statusCode(HttpStatus.SC_OK);
    }
}
