BM Server Scripts:
=================
@author Derek Hulley
@since 1.4

Modify the alfresco-benchmark-server.sh file according to your needs (look for @@).

Upload the following to your own ftp server:

    /software/selenium-server-standalone-2.31.0.jar
    /software/alfresco-benchmark-server.sh
    /software/alfresco-benchmark-server-*-exec.jar

Copy the services scripts to /etc/init.d

   cp Xvfb /etc/init.d
   cp bmserver /etc/init.d

Enable them as services:

   chmod +x /etc/init.d/Xvfb
   chmod +x /etc/init.d/bmserver
   chkconfig --add Xvfb
   chkconfig --add bmserver

Edit the scripts according to your environment.
Note that the 'bmserver' init script can be modified for AWS environments to so that initial wget
command issued to retrieve the code is supplied dynamically.  For install, set the following
as the user data:
   wget --ftp-user *** --ftp-password *** ftp://***/software/*
to get the software required directly from the ftp server.

Install:
   firefox
   X11 fonts
   Xvfb

Start the services manually:

   service Xvfb start
   server bmserver start

Check that the required processes are running:

   ps aux | grep selenium
   ps aux | grep Xvfb
   ps aux | grep alfresco

There should be:

   java -Xmx256M -jar selenium-server-standalone-2.31.0.jar -role hub ...
   java -Xmx6144M -Xms6144M -jar selenium-server-standalone-2.31.0.jar -role node  ...
   Xvfb :0 -screen 0 1024x768x24
   java -Devents.threads.count=6 -Dwebdrone.browserType=AlfrescoRemoteWebDriverFireFox -Xmx1024M -jar alfresco-benchmark-server-exec.jar -zkUri ... -zkPath ...