# Alfreco Benchmark driver 
This is a description of how a generic bm-driver works 

## What is it?
It is a spring boot, with a lot of legacy classic spring (xml context dependent), app that can run the actual code from events
that constitute the real work that a BM Test Run does. 
There is no UI and no REST API. 
The entire functionality relies on the data found in the BM Config Mongo DB. There is no direct network communication between the bm-manager
and the bm-driver applications.
It uses a BM Test Data Mongo DB to store BM Test specific Data: events, event results, session data...    
The separate data base is also used for security reasons, so test data could possibly be stored in a more secure environment;

## How to start it?

Start with: 
```
mvn clean spring-boot:run -Dmongo.config.host=localhost
```
The default port is 908X, where X is dependent on the specific driver default. 
You can control the port by adding: ```-Dserver.port=ZZZZ``` to the command above;

**A docker(docker-compose) version will also be available soon;**


## Class/components description and start up details

### Important dependency
The bm-driver si dependent on the bm-manager library and reuses most of its code. bm-driver logic relies some "smart" if statements, 
some properties (files), extra context files and on multiple spring contexts reloads in the bm-manager code to ensure that 
the code acts as a bm-driver.
The advantage of this architecture is that a lot of the code is reused. The main disadvantage is that is is difficult to ramp up
and understand how it all works when you want to debug it.



### Startup Flow

1. A class (with @SpringbootApplication annotation usually) loads the same ```classpath:config/spring/app-context.xml``` , 
see sample: ```org.alfresco.bm.SampleMBFDriverApplication```, just like the bm-manager; This means that everything described in the
[manager startup code](../bm-manager/README.md) is still true for the bm-driver, **Except** for the ```@RestController```s that 
in the bm-driver case are no longer scanned and loaded.

2. This means that all the beans defined in the ```config/spring/app-context.xml``` are still available in the bm-driver, which is 
fine because it means we have the necessary services and DAOs in the bm-driver as well, that communicate with the BM Config Mongo DB.
This means that we are reusing the code (even though we don'r really need all the methods in the bm-driver logic); 
Here is a reminder of what beans we have in the ```config/spring/app-context.xml```: 
* properties management _(this will be explained further down for the bm-driver)_;
* DAO service and connection management to mongo (these are started with the BM Config Mongo DB in this context):
 see package ```org.alfresco.bm.common.mongo```;_(see details below)_
* testService - see description on the [bm-manager doc](../bm-manager/README.md); This connects to the BM Config Mongo DB
* testRunServices (class TestRunServiceCache)(I don't think this is used in the bm-driver context, as the services from the 
test-services-context.xml file are loaded directly by the TestRun.start() context load from the test-common-context.xml) 
- see description on the [bm-manager doc](../bm-manager/README.md) if you want to understand what this is used for on the 
bm-maanger side
* logs (logService)
* lifecycleController - **unfortunately**(see [REPO-3741](https://issues.alfresco.com/jira/browse/REPO-3741))

**Note** ** Bean "test"(```org.alfresco.bm.driver.test.Test```) defined in ```config/spring/app-context.xml``` 
detects in this line:  
```
boolean isDriver = !release.toLowerCase().startsWith("alfresco-bm-manager");
```
that it is running in the _bm-driver_ context and triggers the start of main waiting loop(entry point) of the logic 
that a bm-driver app does; 
We will take a look at this in the next section:   

### Logic flow
As we said the "test"(```org.alfresco.bm.driver.test.Test```) that starts at bm-driver start up detects that it is running as a 
bm-driver and triggers the following actions:
```
    // The core BM Server application does NOT drive anything
    boolean isDriver = !release.toLowerCase().startsWith("alfresco-bmf-manager");
    if (isDriver)
    {
         // Ensure that there is a representation of the test in the DB
         initTestDefaults();
   
         // Register this driver
         registerDriver();
         
         // Store driver details
         refreshRegistrationTask.run();
         // Make sure we keep it refreshed
         Timer timer = new Timer("TestDriverPing-" + release + "-" + schema, true);
         timer.schedule(refreshRegistrationTask, 0L, TestDriverPingTask.PING_TIMEOUT/2);
            
         // Create monitors for all test runs of interest
         testRunPingTask.run();
         // Keep it refreshed
         timer = new Timer("TestRunPing-" + release + "-" + schema, true);
         timer.schedule(testRunPingTask, testRunMonitorPeriod, testRunMonitorPeriod);
        }
``` 
1. ```initTestDefaults()``` this loads the default properties (```testDefaults```) that this bm-driver will use. 
This is done based on the app.inheritence property. This also saves the default properties that are applicable for this bm-driver 
in the  BM Config Mongo DB ('test.defs' collection)- see section **Properties** below 
2. ```registerDriver();``` this registers this bm-driver (based on app.release and app.schema) in the BM Config Mongo DB 
(under the 'test.drivers' collection) -> this is the only way for the bm-manager to know what bm-drivers are available to run tests
3. ```refreshRegistrationTask``` is registered to run from time to time (TestDriverPingTask.PING_TIMEOUT/2) to refresh -> 
This means that from time to time this dm-driver updates its ping time in the 'test.drivers' BM Config Mongo DB. 
The bm-manager uses this information to detect the "alive" drivers.
4. ```testRunPingTask``` **this is the most important task** that the bm-driver does: This thread (```TestRunPingTask```) queries 
the BM Config Mongo DB for the presence of new scheduled BM Test Runs that it can execute 
```testDAO.getTests(release, schema, testsSkip, 100)```: it looks into the 'test.runs' collection for test runs with:
   * scheduled state 
   * bm-driver compatibility  

When it does find such a BM Test Run it creates an instance of ```org.alfresco.bm.driver.test.TestRun``` and calls the check state 
method: 
```
    // Call each monitor to get it to check itself
    for (TestRun testRun : testRuns.values())
    {
        testRun.checkState();
    }
``` 

```org.alfresco.bm.driver.test.TestRun.checkState``` is a huge method that does orchestrates the entire flow loop of a BM Test Run;
Let's look at some of the important things it does:

1. ```testRun.checkState()``` first check the state of the BM Test Run, and the first time it (the first bm-driver of this kind)
 goes through this code the state would be **SCHEDULED**. If another instance of this bm-driver is started, it may detect that 
 the state of the BM Test Run is actually **STARTED**; Either case, the code calls 
 ```org.alfresco.bm.driver.test.TestRun.start()``` method;
   1. This _start()_ method prepares a new xml based spring context (with a parent to the existing, usually springboot context, so 
      all the beans from the parent context can still be accessed). This xml spring context starts from a hardcoded file:
      ```PATH_TEST_CONTEXT = "classpath:config/spring/test-context.xml";```
   2. This ```test-context.xml``` file is usually inside the bm-driver and it usually includes a reference to the 
   ```classpath:config/spring/test-common-context.xml"``` file, that is found in the bm-manager code.
      * the ```test-context.xml``` contains everything that is specific to that particular bm-driver spring context, and the most
      important thing here would be a **complete/finite** set of beans named like this: "event.<word>"; There should be a bean 
      **event.start** that specifies how the bm-driver should start handling the start event of a BM Test Run: it usually does a 
      redirect to another event name. All the other beans named "event.<word>" define a 
      [state machine](https://en.wikipedia.org/wiki/Finite-state_machine) that guides the bm-driver through a set of events that 
      either produce other events or redirect to other events or do some logic (and may not produce any follow up events). All is 
      driven by these bean names and the convention around the names. Each of these beans are implemented by 
         * producers(```org.alfresco.bm.driver.event.producer.EventProducer```) or 
         * processors(```org.alfresco.bm.driver.event.EventProcessor```) of events in that bm-driver 
      Each of these beans (should) implement an abstract producer/processor class that is spring context aware and it will self 
      register into the eventProducers/eventProcessors maps so that the framework can find them; see:
         * ```org.alfresco.bm.driver.event.AbstractEventProcessor.register()``` or
         * ```org.alfresco.bm.driver.event.producer.AbstractEventProducer.register()``` ;
   3. The ```test-common-context.xml``` file also loads the ```"classpath:config/spring/test-services-context.xml"``` file  that 
   loads and connects to the BM Test Data Mongo DB but also directly loads all the services used to interact with the 
   BM Test Data Mongo DB:
      * eventService
      * resultService
      * sessionService
      * dataReportService (not sure this is used on the bm-driver context)
   4. The ```test-common-context.xml``` also loads some other services:
      * These two are very important for mapping the beans that can handle certain events to the event names they can process:
         * eventProducers ```org.alfresco.bm.driver.event.producer.EventProducerRegistry```
         * eventProcessors ```org.alfresco.bm.driver.event.EventProcessorRegistry```  
      * Bean "eventController" class: ```org.alfresco.bm.driver.event.EventController``` is an important controller class - 
      will be discussed later, see below
      * testRunService - discussed later, see below;
      * some "completionEstimator" beans 
    5. Then the start method reads all the properties stored in the BM Config Mongo DB for this BM Test Run (these are a 
    combination/merge of the properties files loaded by the ```testDefaults``` with the "app.inheritance" variable and any other 
    override for this BM Test Run done at the BM Test Definition level or BM Test Run level ). All these properties are given to 
    xml spring context before the load. 
    6. Any system variables that would override any of the properties values are also appended
    7. This new context is finally started
    8. Immediately after the start of the context an **event.start** event is put into the **<test_definition>.<test_run>.events**
     collection in the BM Test Data Mongo DB
2. After the "testrun" context is started, the checkState() method can advance through the BM Test Runs states (until completion),
executing all the events that are produced or have to be processed; This is done by the EventController (description below) by 
the ```TestRun``` class calling ```updateDriverIds()``` method thus marking this bm-driver a valid processor for events on the 
events queue;
3. ```EventController``` the javadoc explains it pretty well and feel free to go through the 
```org.alfresco.bm.driver.event.EventController.runImpl()``` method as well;

```
/**
 * A <i>master</i> controlling thread that ensures that:
 * <ul>
 *  <li>reads events from the queue</li>
 *  <li>checks out threads to process events</li>
 *  <li>monitors event processors</li>
 *  <li>records event executions</li>
 *  <li>handles exceptions e.g. events that take too long to process</li>
 * </ul>
 * Calls from the {@link LifecycleListener} control the execution phases and,
 * when the events run out, the application context is notified to shut down.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class EventController implements LifecycleListener, ApplicationContextAware, Runnable
```
The important part in the _runImpl()_ method:
```
// Find the processor for the event
EventProcessor processor = getProcessor(event);
            
// Schedule it
EventWork work = new EventWork(
                    driverId, testRunFqn,
                    event,
                    driverIds,
                    processor, eventProducers,
                    eventService, resultService, sessionService,
                    logService);
try
{
    // Grabbing an event automatically applies a short-lived lock to prevent
    // any other drivers from grabbing the same event before the event is locked
    // for execution.
    executor.execute(work);
}
```
As you can see the EventWork class is the wrapper for the actual work the event processor for an event does. 
It gets scheduled for execution according to the settings specified to the bm-driver/event ( number of threads, scheduled time, 
delays,...)

### Other components and services:

The progress of the test runs is calculated using the ```CompletionEstimator``` classes. These measure the completion ratio based on
the number of completed sessions, on the number of a specific type of event or based on elapsed time.

TestRunService (not to be confused with TestRunServices) is used to log messages in the database for a specific test run.

#### Entire component diagram:

See [bm-driver_component.png](bm-driver_component.png);

#### Samples:

E.g. test.driver entry:
```
{
    "_id" : ObjectId("5b6abf4cfa3adc399c6169df"),
    "release" : "alfresco-users-load-bmf-driver-3.0-SNAPSHOT",
    "schema" : 9,
    "ipAddress" : "127.0.0.1",
    "hostname" : "127.0.0.1",
    "contextPath" : "/",
    "capabilities" : {
        "system" : [ 
            "java"
        ]
    },
    "ping" : {
        "time" : ISODate("2018-08-08T10:00:44.555Z"),
        "expires" : ISODate("2018-08-08T15:45:18.794Z")
    }
}
```

### Properties:
* Just like on the server: bean "appPlaceholderConfigurer" loads the "normal spring" properties from the 
   * config/startup/app.properties: app.release, app.schema, app.inheritance, server.contextPath, server.port
   * config/startup/mongo.properties: A default value for the mongo.config.host is not specified to force the user to specify 
one when starting the bm-driver app up 
   * and any other : config/startup/*.properties
* Plus at startup: ```testDefaults``` is a special implementation of properties that uses the ```app.inheritence``` property 
(e.g: _app.inheritance=SAMPLE,COMMON,FILES,FILES_FTP,FILES_LOCAL_) loaded from the "normal" properties, used to decide "at run/BM Test Run time" 
what properties will be saved as the defaults properties for that particular drivers(and schema) 
   * When the driver starts up: see ```org.alfresco.bm.driver.test.Test#initTestDefaults``` that 
   calls ```org.alfresco.bm.driver.test.TestDefaults#getPropertiesList```; 
   * These ```testDefaults``` values are loaded from various files under classpath ```*:config/defaults/*.properties``` 
   (including: config/**defaults**/events.properties, config/**defaults**/mongo.properties( these mongo.properties refers to the 
   BM Test Data Mongo DB, 
   **not** the BM Config Mongo DB)
   * This mechanism allows a bm-driver (and schema) to inherit default properties for various event types and common functionality, 
    but also to override any specify new properties for its custom needs;  
       * E.g. 1: probably all drivers will use a mongo db to store the specific BM Test Data, therefore it is simpler to keep a single 
    template with the all the properties needed in one place; 
       * E.g. 2: some tests will need to know where they can find a list of users already created in Alfresco, 
       therefore it is sensible to consider a default set of settings for this functionality that would suite most use cases, 
       but also allow other drivers to override these settings


## Junit (integration) tests

The tests can be split into two categories: standard junits that test the methods (e.g ```org.alfresco.bm.user.CreateUsersWithRestV1APITest```
from alfresco-bm-load-users which tests the setting of user groups based on chance) and junit integration tests that are loading spring contexts.
Junits that test the whole test run process are using BMTestRunner (e.g ```UsersLoadBMFDriverTest.runDefaultSignup``` or ```BMDataLoadTest.testBasic```)

### BMTestRunner
The ```org.alfresco.bm.common.util.junit.tools.BMTestRunner``` class is used to execute a test run with TestProperties and MongoHost passed as arguments.
The method ```BMTestRunner.run``` creates a test, a test run and schedules the test to be run using the BM Manager's Rest API. While the test runs it checks
if the test has started or if it is hanging and finally if it is completed.
