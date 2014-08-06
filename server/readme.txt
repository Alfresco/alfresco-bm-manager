Developing in Eclipse:
---------------------

Make sure you set the Maven repository location in Eclipse:
    Window > Preferences > Java > Build Path > Classpath Variables: M2_REPO=[probably home/.m2/repository]
Run
    mvn dependency:sources
    mvn eclipse:eclipse
Import the parent and module poms into Eclipse

Building in Maven:
-----------------

Checkout master or tag of 'alfresco-benchmark'
Run
    mvn clean install
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

Start the BM Server:
> cd server
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

Start the BM Sample Test:
> cd ../sample
> mvn tomcat7:run -Dmongo.config.host=localhost
The sample test driver will run at
   http://localhost:9081/alfresco-benchmark-bm-sample
There is no UI for the test driver and, although it has all the APIs, there is no
direct communication between the server application (UI) and the test drivers.
You can start any number of instances of the sample test driver on other
machines.

Check that the sample started OK
   GET: <bm-sample>/api/v1/status/startup

The sample test will have registered and initialized the test details.  Retrieve the test definitions:
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
   
To retrieve a csv stream of the test run results:
   GET:   <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/results/csv
   
To retrieve results for charting (time-series):
   GET:   <bm-server>/api/v1/tests/SAMPLE1/runs/RUN01/results/ts
   Content-Type:application/json
   {
      "fromTime":<ms since Epoch. Optional and will be rebased to near the first event, if necessary.>,
      "timeUnit":<The units of the 'reportPeriod' value.  Default: 'SECONDS'.>,
      "reportPeriod":<How often to report statistics, expressed as a number of 'timeUnits'.  Default 1.>,
      "smoothing":<Number of x-axis values to include in smoothing curve.  Default 1.>,
      "chartOnly":<Whether or not to include only data interesting to end-users.  Default 'true'.>
   }
   Return JSON example for {reportPeriod:10, timeUnit:"seconds", smoothing:2}
   [
      { "time" : 1406839320000 , "events" : [ { "name" : "process" , "median" : 34.87096774193548 , "min" : 10.0, "max" : 60.0 , "stdDev" : 15.637789319012697 , "num" : 62 , "numPerSec" : 3.1 , "fail" : 13 , "failPerSec" : 0.65} , { "name" : "scheduleProcesses" , "median" : 7.0 , "min" : 7.0 , "max" : 7.0 , "stdDev" : 0.0 , "num" : 1 , "numPerSec" : 0.05 , "fail" : 0 , "failPerSec" : 0.0}]} ,
      { "time" : 1406839330000 , "events" : [ { "name" : "process" , "median" : 36.13664596273292 , "min" : 10.0, "max" : 60.0 , "stdDev" : 14.265560317776668 , "num" : 161 , "numPerSec" : 8.05 , "fail" : 40 , "failPerSec" : 2.0} , { "name" : "scheduleProcesses" , "median" : 5.5 , "min" : 4.0 , "max" : 7.0 , "stdDev" : 2.1213203435596424 , "num" : 2 , "numPerSec" : 0.1 , "fail" : 0 , "failPerSec" : 0.0}]} ,
      { "time" : 1406839340000 , "events" : [ { "name" : "process" , "median" : 35.63768115942029 , "min" : 10.0 , "max" : 60.0 , "stdDev" : 14.266464187847879 , "num" : 138 , "numPerSec" : 6.9 , "fail" : 37 , "failPerSec" : 1.85} , { "name" : "scheduleProcesses" , "median" : 4.0 , "min" : 4.0 , "max" : 4.0 , "stdDev" : 0.0 , "num" : 1 , "numPerSec" : 0.05 , "fail" : 0 , "failPerSec" : 0.0}]}
]