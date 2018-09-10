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

public class IntegrationTests
{
    //@Value("${alfresco.server}")
    private String alfrescoServer;

    //@Value("${mongo.test.host}")
    private String mongoHost;

    private RestTestClient client;

    @Before
    public void setUp()
    {
        client = new RestTestClient();
        alfrescoServer = "172.31.142.102";
        mongoHost = "172.31.142.102:27017";
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
        client.updateTestProperty(test, "mongo.test.host", mongoHost);
        client.updateTestProperty(test, "alfresco.server", alfrescoServer);
        client.updateTestProperty(test, "user.numberOfUsers", "" + 50);

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
        boolean testStatus = waitTestCompleted(starttime, 60000L, test, testRun);

        // Get test results
        TestRunSummary results = client.getTestRunSummary(test, testRun);

        // Check test results
        if(testStatus)
        {
            // Successful events should be more than the events scheduled for creation
            assertTrue(results.getResultsSuccess() > 5);
            assertTrue(results.getSuccessRate() > 0.8);
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
