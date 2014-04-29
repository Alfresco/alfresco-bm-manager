Creating a Benchmark Server Image on Windows
============================================
@author Derek Hulley
@since 1.4

Create a directory to hold the benchmark server software

   mkdir c:\bmserver

Download build the desired server version(s).
Download selenium-server-standalone-2.31.0.jar
Download "NSSM - the Non-Sucking Service Manager": http://nssm.cc/download/?page=download

If you are running Selenium-based tests (e.g. alfresco-benchmark-bm-share):
Install:
   FireFox (or whichever browser you with to use)
Install, as a service, a Selenium grid and hub to run using:
   java -Xmx1024M -jar c:\bmserver\selenium-server-standalone-2.31.0.jar -role hub -DPOOL_MAX=50 -newSessionWaitTimeout 5000 -timeout 120
   java -Xmx1024M -jar c:\bmserver\selenium-server-standalone-2.31.0.jar -role node -hub http://localhost:4444/grid/register -maxSession 32 -browser browserName=firefox,platform=ANY,maxInstances=50 -port 5559 
Tune the maximum number of browsers to be 4 x CPUs as a start.
The actual number of browsers that can be supported will depend on how heavily they are used.

Install, as a service, the Alfresco Benchmark Server to run using:
   java -Devents.threads.count=<4xCPUs> -Dwebdrone.browserType=AlfrescoRemoteWebDriverFireFox -Xmx1024M -jar c:\bmserver\<bmserver-exec.jar> -zkUri <zkServer>:2181 -zkPath <zkPath>
Any test or server-specific properties can be set on the command line and cannot be altered by any test settings.
Set thread count to be 4 x CPUs as a start.    

HINT: Although the service install can use java.exe as the program to execute, it is more convenient to
      create a simple batch file that changes to the directory where the jar is and calls the method directly.
      This allows the output logs to appear with the jars rather than in obscure places like the user's home directory.
      As an additional benefit, modifying the execution parameters is simpler as the service won't need to be
      reinstalled.