Developing in Eclipse:
---------------------

Run
    mvn dependency:sources
    mvn eclipse:eclipse
Import the generated project file into Eclipse
Make sure you set the Maven repository location in Eclipse:
    Window > Preferences > Java > Build Path > Classpath Variables: M2_REPO=[probably home/.m2/repository]
Install ZooKeeper client plugin
    Help > Install New Software > http://www.massedynamic.org/eclipse/updates/


Building in Maven:
-----------------

Run
    mvn clean install
Test
    mvn test
Generate all artifacts
    mvn -DperformRelease=true -Dmaven.test.skip=true clean install


Running with Tomcat7:
--------------------

Deploy to existing Tomcat7:
mvn tomcat7:deploy

Start Tomcat7:
mvn tomcat7:run -Dmongo.config.host=localhost:27017

API Walkthrough:
---------------

Install MongoDB (local or remote)

Checkout benchmark/server/HEAD
Start the BM Server:
> mvn tomcat7:run -Dmongo.config.host=localhost:27017
The Mongo connection details must be supplied; there is no default.

The server will run Tomcat and install itself to:
   http://localhost:9080/alfresco-benchmark-server
All API references will be relative to this root (<bm-server>)

Check that the server started OK
   GET: <bm-server>/api/v1/status/startup

There should be no test definitions available:
   GET: <server>/api/v1/test-defs
Nor will there be any test, either:
   GET: <bm-server>/api/v1/tests
   
Checkout benchmark/tests/bm-sample/HEAD
Start the BM Sample Test:
> mvn tomcat7:run -Dmongo.config.host=localhost
The test will run at
   http://localhost:9081/alfresco-benchmark-bm-sample
There is no UI for the test but it has all the APIs

Check that the sample started OK
   GET: <bm-sample>/api/v1/status/startup

The sample test will have registered the test details.  Retrieve the test definitions:
   GET: <bm-server>/api/v1/test-defs?activeOnly=true

Shutdown the BM Sample application with CTRL-C.  Check the test defintions:
   GET: <bm-server>/api/v1/test-defs?activeOnly=true
There will be nothing now.  However, the test definition now exists and can be used:
   GET: <bm-server>/api/v1/test-defs?activeOnly=false
You will see the sample test definition.
Typically, an end-user will not interested in the test definitions except when it comes to
creating a test.  Restart the BM Sample application.
> mvn tomcat7:run -Dmongo.config.host=localhost

Let's see what tests exist:
   GET: <bm-server>/api/v1/tests
Nothing.  So let's use the sample test definition and create a new test:
   POST: <bm-server>/api/v1/tests
   Content-Type:application/json
   {
      "name":"SAMPLE1",
      "description":"A first sample",
      "release":"alfresco-benchmark-bm-sample-2.0-SNAPSHOT",
      "schema":"1"
   }
Test names are unique, case-sensitive and quite restrictive.  Try using "SAMPLE 2" (add a space).
The response body contains all property definitions for the newly created test,
which has inherited everything from the test definition.  This can now be fetched from the test
directly as well:
   GET: <bm-server>/api/v1/tests/SAMPLE1

To update a test, the existing name and version are required:
   PUT: <bm-server>/api/v1/tests
   Content-Type:application/json
   {
      "oldName":"SAMPLE1",
      "version":"0",
      "name":"RENAMED_SAMPLE1",
      "description":"A renamed sample",
      "release":"alfresco-benchmark-bm-sample-2.0-SNAPSHOT",
      "schema":"1"
   }
Rename it back
   PUT: <bm-server>/api/v1/tests
   Content-Type:application/json
   {
      "oldName":"RENAMED_SAMPLE1",
      "version":"1",
      "name":"SAMPLE1",
      "description":"Back to the original name",
      "release":"alfresco-benchmark-bm-sample-2.0-SNAPSHOT",
      "schema":"1"
   }

To copy a test:
   POST: <bm-server>/api/v1/tests
   Content-Type:application/json
   {
      "copyOf":"SAMPLE1",
      "version":"2",
      "name":"COPIED_SAMPLE1"
   }
   
To delete a test, just the name is required:
   DELETE: <bm-server>/api/v1/tests/COPIED_SAMPLE1

To override a test property value:
   PUT: <bm-server>/api/v1/tests/SAMPLE1/props/proc.user
   Content-Type:application/json
   {
      "version":"0",
      "value":"Bob"
   }

To remove a test property value override:
   PUT: <bm-server>/api/v1/tests/SAMPLE1/props/proc.user
   Content-Type:application/json
   {
      "version":"1",
      "value":null
   }

Give the test the default location for storing test-specific data:
   PUT: <bm-server>/api/v1/tests/SAMPLE1/props/mongo.test.host
   Content-Type:application/json
   {
      "version":"0",
      "value":"localhost:27017"
   }

Test run names are unique within the parent test i.e. the combinationn '<test>.<run>'
is unique.  The same naming restrictions apply to the test run name as apply to the
test name except that they can start with numbers.
To list the current test runs:
   GET: <bm-server>/api/v1/tests/SAMPLE1/runs

Create a test run:
   POST: <bm-server>/api/v1/tests/SAMPLE1/runs
   {
      "name":"RUN01",
      "description":"Test run 01 by Fred"
   }

Get the test run:
   GET: <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01

To update a test run, the existing name and version are required:
   PUT: <bm-server>/api/v1/tests/SAMPLE1/runs
   Content-Type:application/json
   {
      "oldName":"RUN01",
      "version":"0",
      "name":"RENAMED_RUN01",
      "description":"A renamed test run"
   }
Rename it back
   PUT: <bm-server>/api/v1/tests/SAMPLE1/runs
   Content-Type:application/json
   {
      "oldName":"RENAMED_RUN01",
      "version":"1",
      "name":"RUN01",
      "description":"A test run"
   }

To copy a test run:
   POST: <bm-server>/api/v1/tests/SAMPLE1/runs
   Content-Type:application/json
   {
      "copyOf":"RUN01",
      "version":"2",
      "name":"COPIED_RUN01"
   }
   
To delete a test run, just the name is required:
   DELETE: <bm-server>/api/v1/tests/SAMPLE1/runs/COPIED_RUN01

Override a property value for the run:
   PUT: <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/props/proc.user
   Content-Type:application/json
   {
      "version":"0",
      "value":"Sam"
   }

To remove a test run property value override:
   PUT: <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/props/proc.user
   Content-Type:application/json
   {
      "version":"1",
      "value":null
   }
   
Once all the necessary properties have been set, the test run can be scheduled.
The actual start time is dependent on the availability of the test execution applications.

To request a test run execution:
   POST:  <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/schedule
   Content-Type:application/json
   {
      "version":"2",
      "scheduled":"<ms since Epoch>"
   }

The test run progress summary is present on all test runs.
To retrieve the test run summary i.e. the run details without all the associated properties:
   GET: <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/summary
   
The request the test run state (STARTED, COMPLETED, etc):
   GET: <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/state
   
To request test run termination:
   POST:  <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/terminate
   