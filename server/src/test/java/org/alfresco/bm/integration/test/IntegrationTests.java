package org.alfresco.bm.integration.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.alfresco.bm.common.TestDetails;
import org.alfresco.bm.common.TestRunDetails;
import org.alfresco.bm.common.TestRunSchedule;
import org.alfresco.bm.common.TestRunState;
import org.alfresco.bm.integration.test.model.TestDefinition;
import org.alfresco.bm.integration.test.model.TestRunSummary;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(locations="classpath:prop/integration-tests.properties")
public class IntegrationTests
{
    @Value("${alfresco-int.server}")
    private String alfrescoServer;

    @Value("${mongo-int.host}")
    private String mongoHost;

    @Value("${mongo-int.port}")
    private String mongoPort;

    private RestTestClient client;

    @Before
    public void setUp()
    {
        client = new RestTestClient();
    }

    @Test
    public void testPopulateWithUsers()
    {
        // Get load user driver definition
        List<TestDefinition> testDefitions = client.getDriverDefinitions();
        TestDefinition loadUserDef = testDefitions.stream().filter(def -> def.getRelease().contains("user")).findAny().get();

        // Create test
        TestDetails test = new TestDetails();
        test.setName("UserTest" + RandomStringUtils.randomAlphanumeric(5));
        test.setDescription("Test user load driver.");
        test.setRelease(loadUserDef.getRelease());
        test.setSchema(Integer.valueOf(loadUserDef.getSchema()));

        client.createTest(test);
        client.updateTestProperty(test, "mongo.test.host", mongoHost + ":" + mongoPort);
        client.updateTestProperty(test, "alfresco.server", alfrescoServer);
        client.updateTestProperty(test, "user.numberOfUsers", "" + 100);

        // Create test run
        TestRunDetails testRun = new TestRunDetails();
        testRun.setName("UserTestRun" + RandomStringUtils.randomAlphanumeric(5));
        testRun.setDescription("Test run for user load driver.");

        client.createTestRun(test, testRun);

        // Schedule the test run
        TestRunSchedule testRunSchedule = new TestRunSchedule();
        testRunSchedule.setScheduled(System.currentTimeMillis());
        testRunSchedule.setVersion(0);

        client.scheduleTest(test, testRun, testRunSchedule);

        // Wait to complete test
        long starttime = System.currentTimeMillis();
        boolean testStatus = waitTestCompleted(starttime, 90000L, test, testRun);

        // Get test results
        TestRunSummary results = client.getTestRunSummary(test, testRun);

        // Check test results
        if(testStatus)
        {
            // Successful events should be more than the events scheduled for creation
            assertTrue("There are less than expected successful events", results.getResultsSuccess() > 5);
            assertTrue("The success rate is lower than expected", results.getSuccessRate() > 0.8);
        }
        else
        {
            fail("Testrun did not complete. The test result was: " +  results.getState());
        }
    }

    @Test
    public void testPopulateWithData()
    {
        // Get load data driver definition
        List<TestDefinition> testDefitions = client.getDriverDefinitions();
        TestDefinition loadUserDef = testDefitions.stream().filter(def -> def.getRelease().contains("load-data")).findAny().get();

        // Create test
        TestDetails test = new TestDetails();
        test.setName("LoadDataTest" + RandomStringUtils.randomAlphanumeric(5));
        test.setDescription("Test data load driver.");
        test.setRelease(loadUserDef.getRelease());
        test.setSchema(Integer.valueOf(loadUserDef.getSchema()));

        client.createTest(test);
        client.updateTestProperty(test, "mongo.test.host", mongoHost + ":" + mongoPort);
        client.updateTestProperty(test, "alfresco.server", alfrescoServer);

        // Create test run
        TestRunDetails testRun = new TestRunDetails();
        testRun.setName("DataLoadTestRun" + RandomStringUtils.randomAlphanumeric(5));
        testRun.setDescription("Test run for data load driver.");

        client.createTestRun(test, testRun);

        // Schedule the test run
        TestRunSchedule testRunSchedule = new TestRunSchedule();
        testRunSchedule.setScheduled(System.currentTimeMillis());
        testRunSchedule.setVersion(0);

        client.scheduleTest(test, testRun, testRunSchedule);

        // Wait to complete test
        long starttime = System.currentTimeMillis();
        boolean testStatus = waitTestCompleted(starttime, 90000L, test, testRun);

        // Get test results
        TestRunSummary results = client.getTestRunSummary(test, testRun);

        // Check test results
        if(testStatus)
        {
            // Successful events should be more than the events scheduled for creation
            assertTrue("There are less than expected successful events", results.getResultsSuccess() > 5);
            assertTrue("The success rate is lower than expected", results.getSuccessRate() > 0.8);
        }
        else
        {
            fail("Testrun did not complete. The test result was: " +  results.getState());
        }
    }

    @Test
    public void testRestApiScenario()
    {
        // Get rest-api driver definition
        List<TestDefinition> testDefitions = client.getDriverDefinitions();
        TestDefinition loadUserDef = testDefitions.stream().filter(def -> def.getRelease().contains("rest-api")).findAny().get();

        // Create test
        TestDetails test = new TestDetails();
        test.setName("RestApiTest" + RandomStringUtils.randomAlphanumeric(5));
        test.setDescription("Test rest api driver.");
        test.setRelease(loadUserDef.getRelease());
        test.setSchema(Integer.valueOf(loadUserDef.getSchema()));

        client.createTest(test);
        client.updateTestProperty(test, "mongo.test.host", mongoHost + ":" + mongoPort);
        client.updateTestProperty(test, "alfresco.server", alfrescoServer);

        // Create test run
        TestRunDetails testRun = new TestRunDetails();
        testRun.setName("RestApiTestRun" + RandomStringUtils.randomAlphanumeric(5));
        testRun.setDescription("Test run for rest api driver.");

        client.createTestRun(test, testRun);

        // Schedule the test run
        TestRunSchedule testRunSchedule = new TestRunSchedule();
        testRunSchedule.setScheduled(System.currentTimeMillis());
        testRunSchedule.setVersion(0);

        client.scheduleTest(test, testRun, testRunSchedule);

        // Wait to complete test
        long starttime = System.currentTimeMillis();
        boolean testStatus = waitTestCompleted(starttime, 90000L, test, testRun);

        // Get test results
        TestRunSummary results = client.getTestRunSummary(test, testRun);

        // Check test results
        if(testStatus)
        {
            // Successful events should be more than the events scheduled for creation
            assertTrue("There are less than expected successful events", results.getResultsSuccess() > 5);
            assertTrue("The success rate is lower than expected", results.getSuccessRate() > 0.6);
        }
        else
        {
            fail("Testrun did not complete. The test result was: " +  results.getState());
        }
    }

    private boolean waitTestCompleted(long starttime, long maxtime, TestDetails testDetails, TestRunDetails testRunDetails)
    {
        String testRunState;
        long now;
        do
        {
            testRunState = client.getTestRunState(testDetails, testRunDetails);
            now = System.currentTimeMillis();
        } while (!TestRunState.COMPLETED.toString().equals(testRunState) && !TestRunState.STOPPED.toString().equals(testRunState)
                && (now - starttime < maxtime));

        return TestRunState.COMPLETED.toString().equals(testRunState) ? true : false;
    }
}
