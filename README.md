### Alfresco Benchmark Framework Manager

This project provides a management application and a supporting library for development of highly scalable, easy-to-run Java-based load and benchmark tests.  Maven and Java development patterns are employed so that load tests can be included in automated build plans; both for the product they are testing but also to prevent regressions in the tests.

More information on the <a href="https://community.alfresco.com/docs/DOC-6235">Alfresco Community Site</a>
* Videos related to the <a href=https://www.youtube.com/watch?v=CXFH_1lFvsk&list=PLktNOqTikHe_Uy6UNIic0U_ga44XK0voi>Benchmark Framework 2.0</a>
* Videos prepared for <a href="http://summit.alfresco.com">Alfresco Summit 2014</a> showing <a href="https://www.youtube.com/watch?v=_8w5TxjBgh4&list=PLktNOqTikHe8wXFvWnV8s7TbTlV4K2flf">a CMIS load test in action</a>

### Get the code

Git:

    git clone https://github.com/Alfresco/alfresco-bmf-manager.git
    cd alfresco-bmf-manager

Subversion:

    svn checkout https://github.com/Alfresco/alfresco-bmf-manager.git
    cd alfresco-bmf-manager

### Use Maven

1.Build

    mvn clean install

2.Check availability of MongoDB server

    mongo \<mongo-host\>    
    exit

3.Start server

    cd server   
    mvn tomcat7:run -Dmongo.config.host=\<mongo-host\>

4.Start sample load driver

    cd ../sample    
    mvn tomcat7:run -Dmongo.config.host=\<mongo-host\> 

5.Access server UI

    Browse to http://localhost:9080/alfresco-benchmark-server

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

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.
