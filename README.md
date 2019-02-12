### Alfresco Benchmark Manager

Important note: This (and all the other alfresco-bm-* ) projects do not compile if you don't have access to alfresco nexus internal maven repositories. Follow https://issues.alfresco.com/jira/browse/REPO-3886 to get an update for when the required TAS-RESTAPI library will be made public as well, making this project compilable by anyone.


This project provides a management application and a supporting library for development of highly scalable, easy-to-run Java-based load and benchmark tests.  Maven and Java development patterns are employed so that load tests can be included in automated build plans; both for the product they are testing but also to prevent regressions in the tests.

More information on the <a href="https://community.alfresco.com/docs/DOC-6235">Alfresco Community Site</a>
* Videos related to the <a href="https://www.youtube.com/watch?v=CXFH_1lFvsk&list=PLktNOqTikHe_Uy6UNIic0U_ga44XK0voi">Benchmark Framework 2.0</a>
* Videos prepared for <a href="http://summit.alfresco.com">Alfresco Summit 2014</a> showing <a href="https://www.youtube.com/watch?v=_8w5TxjBgh4&list=PLktNOqTikHe8wXFvWnV8s7TbTlV4K2flf">a CMIS load test in action</a>

### Get the code

Git:

    git clone https://github.com/Alfresco/alfresco-bm-manager.git
    cd alfresco-bm-manager

Subversion:

    svn checkout https://github.com/Alfresco/alfresco-bm-manager.git
    cd alfresco-bm-manager
    
### Prerequisites
There are a few components we need before we can kick off any tests.

#### 1. Java
Before you can start any form of testing with the Alfresco Benchmark Framework, you need to install Java SDK version 1.7.0_51 or later on the Benchmark Management Manager host and on each host running the Benchmark Driver Server.

#### 2. Maven
Maven is used as the build tool so make sure you have Apache Maven 3 installed. 

#### 3. MongoDB
There also needs to be an instance of MongoDB, version 2.6.3 or later. The Benchmark Framework servers expects the port number to be *27017*.  
If you want to use docker: ```docker run -p 27017:27017 mongo```  
Additionally, installing **Robo 3T** (MongoDB GUI) is helpfull to see the database connection.

### Start Alfresco Benchmark Driver and run Test Suite

1.Build

    mvn clean install

2.Check availability of MongoDB server

    mongo \<mongo-host\>    
    exit

3.Start server

    cd server   
    mvn clean spring-boot:run -Dmongo.config.host=\<mongo-host\>

4.Start sample load driver

    cd ../sample    
    mvn clean spring-boot:run -Dmongo.config.host==\<mongo-host\> 

5.Access server UI

    Browse to http://localhost:9080/alfresco-bm-manager

6.Create a Test

    Click [+] if not presented with "Create Test" options.  
    Fill in test details:   
        - Test Name: MyFirstTest01  
        - Test Description: Getting started 
        - Test Definition: alfresco-benchmark-bm-sample-xxx     
    Click "Ok".
 
7.Edit test properties

    It is a requirement that all test runs get told where to store the generated results.   
    Change property "mongo.test.host" to your \<mongo-host\>  
    Click: "MyFirstTest01" on top left

8.Create a Test Run

    Click [+] if not presented with "Create Test Run" options.  
    Fill in test run details:   
        - Test run name: 01     
    Click "Ok".

9.Start the Test Run

    Click "Play" button next to Test Run "01".  
    The progress bar will auto-refresh as the test run completion estimate changes.

10.Download results

    At any time - usually when the test run completes - click through on the test run.  
    Click the download button and open the CSV file in a spreadsheet.
    
### Create an archetype
Please use [this guide](sample/building-the-archetype.md) to create an archetype and start the new project.

### Release bm-manager project
Please use [this guide](docs/ReleaseProcess.md) for release process.

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.
