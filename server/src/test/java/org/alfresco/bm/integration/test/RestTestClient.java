package org.alfresco.bm.integration.test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.alfresco.bm.common.TestDetails;
import org.alfresco.bm.common.TestRunDetails;
import org.alfresco.bm.common.TestRunSchedule;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.integration.test.model.*;

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
