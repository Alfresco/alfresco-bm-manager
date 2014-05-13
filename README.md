### alfresco-benchmark

### Get the code

Git:

    git clone https://github.com/derekhulley/alfresco-benchmark.git
    cd alfresco-benchmark

Subversion:

    svn checkout https://github.com/derekhulley/alfresco-benchmark.git
    cd alfresco-benchmark

### Use Maven

1.Build

    mvn clean install

2.Check availability of MongoDB server

    mongo \<mongo-host\>    
    exit

3. Start server

    cd server   
    mvn tomcat7:run -Dmongo.config.host=\<mongo-host\>

4. Start sample load driver

    cd ../sample    
    mvn tomcat7:run -Dmongo.config.host=\<mongo-host\> 

5. Access server UI

    Browse to http://localhost:9080/alfresco-benchmark-server

6. Create a Test

    Click [+] if not presented with "Create Test" options.  
    Fill in test details:   
        - Test Name: MyFirstTest01  
        - Test Description: Getting started 
        - Test Definition: alfresco-benchmark-bm-sample-xxx     
    Click "Ok".
 
7. Edit test properties

    It is a requirement that all test runs get told where to store the generated results.   
    Change property "mongo.test.host" to your \<mongo-host\>  
    Click: "MyFirstTest01" on top left

8. Create a Test Run

    Click [+] if not presented with "Create Test Run" options.  
    Fill in test run details:   
        - Test run name: 01     
    Click "Ok".

9. Start the Test Run

    Click "Play" button next to Test Run "01".  
    The progress bar will auto-refresh as the test run completion estimate changes.

10. Download results

    At any time - usually when the test run completes - click through on the test run.  
    Click the download button and open the CSV file in a spreadsheet.
