# Alfresco Benchmark Manager 
a.k.a Benchmark Server 

## What is it?
It is a spring boot app that coordinates and monitors the Alfresco Benchmark Drivers applications 
in order for a user to be able to create test definitions and test runs that would run against 
a configured Alfresco Content Services installation. 

It has the ability to create reports about test runs;

It has a set of REST APIs that control everything that the app can do.

It also has an UI app with AngularJS that the users can use to call those REST APIs. 

## How to start it?

Start with: 
```
mvn clean spring-boot:run -Dmongo.config.host=localhost
```
The default port is 9080 but you can control that by adding the ```-Dserver.port=ZZZZ``` to the command above;

Access the UI at: http://localhost:9080/alfresco-bm-manager

### Docker Compose

Alfresco Benchmark Manager can be started using docker-compose. To start it make sure Docker and Docker Compose are installed, then browse to **server\docker-compose**:
1. Set the MONGO_PORT in .env property file to choose the port which will be exposed for MongoDb
2. Set REGISTRY to the docker registry which contains the BM Manager image
2. Run:
```
docker-compose -f docker-compose-manager.yml up
```
This will start the Manager and MongoDB.

## Class/components description and start up details

1. org.alfresco.bm.manager.Application - spring boot entry point starts up and loads ```config/spring/app-context.xml```
and it also loads all the classes under: ```org.alfresco.bm.manager.api.v1``` that have the 
proper spring boot annotations for services/controllers: ```@RestController, @ControllerAdvice```; 

2. The classes under ```org.alfresco.bm.manager.api.v1``` constitute the **entire REST API** available in the bm-manager app; 
The REST APIs control everything the server does/can do: 
trigger create BM Test Definitions, 
trigger create BM Test Runs, 
start BM Test Runs, 
trigger report generation for BM Test Runs, 
get status for BM Test Runs 
and other actions;

3. The beans defined in the ```config/spring/app-context.xml``` file are also loaded 
_(they are loaded on the **bm-driver*** as well but some bean detect if they are on a driver or the manager and do different 
things based on this check. See Note bellow **_).
The beans in the ```config/spring/app-context.xml``` provide the following support: 
* properties management _(this will be explained further down)_;
* DAO service and connection management to mongo (these are started with the BM Config Mongo DB in this context):
 see package ```org.alfresco.bm.common.mongo```;_(see details below)_
* testService - see description blow
* testRunServices (class TestRunServiceCache)(this includes: resultService, eventService, sessionService, dataReportService but will be configured 
later/not at startup (but rather during/after BM Test Run execution) to connect to the specific BM Test Data Mongo DB) 
- see description blow
* logs (logService)
* lifecycleController - **unfortunately**(see [REPO-3741](https://issues.alfresco.com/jira/browse/REPO-3741))

**Note** ** Bean "test"(```org.alfresco.bm.driver.test.Test```) defined in ```config/spring/app-context.xml``` 
detects in this line:  
```
boolean isDriver = !release.toLowerCase().startsWith("alfresco-bm-manager");
```
that it is not running in the _driver_ context and therefore does not register any tasks to listen for new test runs available 
(scheduled) in the BM config mongo DB - the manger does not run tests.

### Component diagram and flow
See [bm-manager_component.png](bm-manager_component.png);
From top to bottom:

#### Interaction level
1. The users, using the Angular UI app included in bm-manager or a custom app that makes REST API calls, controls the bm-manager 
functionality;

#### REST API layer
1. The ```TestDefinitionRestAPI``` @RestController using the ```MongoTestDAO``` connects to the Config Mongo DB and ensures 
CRUD operations for:
   * Properties (default or specific) BM Test (Definitions, Runs)
2. The ```TestRestAPI``` @RestController is using the ```MongoTestDao``` and ```TestService``` class (that also uses 
the ```MongoTestDAO```) and the ```LogService``` to 
   * Ensures CRUD and monitor/control operations for: BM Test Definitions and BM Test Runs; 
   * This controller also uses the ```TestRunsServiceCache``` to ensure that when a BM Test Run is deleted from the Config Mongo DB, 
   the associated data from the BM Test Data Mongo DB is also deleted/cleared; See description for ```TestRunsServiceCache``` below. 
3. The ```StatusAPI``` @RestController bean uses the ```logService``` to get (live) logs and start up status for BM Test Runs
4. The ```ResultsRestAPI``` @RestController uses  the ```TestRunServiceCache``` bean and also the ```XLSXReport``` and ```CSVReport``` to:
   * generate reports
   * get events specific to a BM Test Run

#### Service layer
1. TestService controls/gets information about BM Test Runs;
2. LogService directly interrogates/writes to the BM Config Mongo DB the log messages; 
3. TestRunServiceCache (bean:testRunServices): This is rather a complex mapping service that stores for a given BM Test Run a spring context 
(build using the ```test-services-context.xml```) that holds the following services:
   1. EventService
   2. SessionService
   3. ResultService
   4. DataReportService

   Each of these 4 services (accessible through TestRunServiceCache) are loaded and configured with the exact configuration and properties used by a particular BM Test Runs; 
   This way, any code that wants to have data from the **BM Test Data Mongo DB** for a particular BM Test Run can do so. 
   This is very useful for code that wants to extract the reports, results and events for a certain BM Test Run.  
   
#### Reports layer
1. XLSXReport - creates XLSX report based on the ```TestRunServiceCache``` services for a particular BM Test Run;
2. CSVReport - creates CSV report based on the ```TestRunServiceCache``` services for a particular BM Test Run;

#### Mongo DAO Layer
1. MongoTestDAO - DAO operations to the BM Config Mongo DB

#### Mongo DB Layer
1. BM Config Mongo DB
2. BM Test Data Mongo DB


### Other details:
#### Mongo connections:
* **bm-manager** connects to the **config mongo DB**: see "configMongoClient" and "configMongoDB" beans;
* The bm-manager also connects to the bm test data mongo DB but only get the reports/results for a test run. 

#### Properties:
* Bean "appPlaceholderConfigurer" loads the "normal spring" properties for the bm-manager from the 
  * config/startup/app.properties: app.release, app.schema, app.inheritance, server.contextPath, server.port
  * config/startup/mongo.properties: **Note:** A default value for the mongo.config.host is not specified, 
to force the user to specify one when starting the bm-manager app up;
  * and any other : config/startup/*.properties 
  
* The bean "testDefaults" is not used in the bm-manager code. It is explained in the 
[bm-driver documentation](../bm-driver/README.md)

### Package structure:
* Classes under ```org.alfresco.bm.manager``` are the only one specific to the bm-manager. They include the bm-manager REST API and the report generation (csv and xlsx);
* The server does not use any of the classes under the "org.alfresco.bm.driver" package, but uses the "org.alfresco.bm.common" package (as so does the bm-drivers)

### Other info:
* There is a app.release and app.schema combination that should uniquely identify bm-manager and bm-drivers that are compatible 
with each other and can work well together. E.g: the same bm-driver (app.release), using different schemas (app.schema) can not run 
the same BM Test Runs. Each BM Test Run can only be assigned to a unique app.release  and app.schema combination and only those 
bm-driver that have that app.release and app.schema can run those tests. 

## Junit (integration) tests

Most of the classes (Rest Controllers, services, ...) have junits tests to ensure their functionality. 
The tests run by starting up spring contexts (that is why we classify them as junit integration tests), and use an embedded Mongo DB.