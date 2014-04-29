#!/bin/bash

set -e

let cpus=`cat /proc/cpuinfo | grep processor | wc -l`
let browsers=$cpus*2
let mem=$cpus*1024
xmx="$mem"M

# Start Selenium Grid
nohup java -Xmx256M -jar selenium-server-standalone-2.28.0.jar -role hub -DPOOL_MAX=$browsers &
DISPLAY=:0 nohup java -Xmx$xmx -Xms$xmx -jar selenium-server-standalone-2.28.0.jar -role node -hub http://localhost:4444/grid/register -maxSession $browsers -browser browserName=firefox,platform=ANY,maxInstances=$browsers -port 5559 &

for bmserver in alfresco-benchmark-server-*-exec.jar
do
   nohup java -Devents.threads.count=$cpus -Dwebdrone.browserType=AlfrescoRemoteWebDriverFireFox -Xmx1024M -jar $bmserver -zkUri bmserver.dt01:2181 -zkPath /alfresco/aws 2>$bmserver.log&
done
