# Run BM on AWS with EKS deployed Alfresco

These are detailed instruction on what you need to do and what you need to check, in order to run the Alfresco Benchmarking on the
Alfresco Content Services deployed in AWS (with EKS).
For more details on how to deploy ACS with EKS see this poject: https://github.com/Alfresco/acs-deployment-aws

This page also contains information about BM test run results and how to interpret them 
( see section _BM test run results analysis_) and also information about how to get cost estimations in AWS for your ACS setup
(see section _Setup Cost Explorer_).

## Prerequisite

##### AWS access (account)

It has to have enough rights to let you access the deployed ACS EKS machines, access to create new EC2 machines and s3 buckets,
in the same region as your ACS EKS machines, access to the kubernetes system that runs ACS (kubernetes dashboard) etc.

Note: (optionally) See this tutorial for setting up the kubernetes dashboard: 
https://docs.aws.amazon.com/eks/latest/userguide/dashboard-tutorial.html

##### Alfresco target
We will need a target system to run against. For this tutorial we will use an Alfresco EKS deployment in AWS.
Follow the tutorial https://github.com/Alfresco/acs-deployment-aws to deploy Alfresco with Cloudformation templates in AWS EKS 
kubernetes cluster.

##### Access to quay.io under Alfresco organization to get the docker images for the BM code

##### EC2 machines (at least 2) to run the BM software. Access them with ssh

##### (optional) FTP location where to store sample files used by the BM drivers

## Preparation

### AWS Console (web UI)
1. Select the region where you will be working on;
2. Select CloudFormation and find out what cluster you want to test;
3. In the EC2 section you can filter and see the machines that run your kubernetes node(s). Main one being the one that 
ends with _"-cluster-worker-node"_
4. In the RDS section you can filter (by vpc name for example) and see the DB instances that are used for your cluster;

**Notes:** Both the EC2 and the RDS machines are good candidates to monitor (from the AWS Console, select the machine (EC2 or RDS)
and select the _Monitor_ tab below) in order to check the (almost realy time) load (CPU, mem, number of connections, 
IO, network) on the those machines.
 
This would give you a good indication if you can add( or need to add) more load from the BM drivers, in order to efficiently load 
the ACS installation;

### Check Alfresco before the benchmark

Check the target Alfresco is up and running fine before the benchmark;

**Alfresco**: https://<--AWS DNS entry for your server-->/alfresco/ e.g: https://testenv.dev.alfresco.me/alfresco/
1. Admin console: https://<--AWS DNS entry for your server-->/alfresco/s/enterprise/admin/admin-systemsummary
2. Lower on the admin-systemsummary page you can see how many users and user groups are on the system
e.g:
```
Users and Groups (Authorities in Default Zone)
Users: 14,348
Groups: 61,381
```
3. Validate the cluster https://<--AWS DNS entry for your server-->/alfresco/s/enterprise/admin/admin-clustering
4. Support tools:
   1. Check the active sessions: https://<--AWS DNS entry for your server-->/alfresco/s/enterprise/admin/admin-activesessions ; 
   You can see here the DB connections(not very accurate) and the logged users that have a valid session
   2. System performance: https://<--AWS DNS entry for your server-->/alfresco/s/enterprise/admin/admin-performance

**Note:** The problems with support tools pages is that 
* They are not very efficient -> a lot of async calls are made to the sever
* The calls are not always redirected to the same node in the cluster; Depends on your ingress/ngnix configuration;
* Even if they are always redirected to one node, it means you don't see the activity on the other nodes; 
You only monitor one node effectively;
* The number of DB connection is different(much lower) from the one reported in the AWS RDS page;

**REST APIs**
* Number of users: https://<--AWS DNS entry for your server-->/alfresco/api/-default-/public/alfresco/versions/1/people/ ;
```"totalItems": 14348,```
* Number of groups: https://<--AWS DNS entry for your server-->/alfresco/api/-default-/public/alfresco/versions/1/groups/ ;
_this seems to be to slow for me - possible bottleneck- it times out in the ngnix_
* Number of sites: https://testenv.dev.alfresco.me/alfresco/api/-default-/public/alfresco/versions/1/sites/
```"totalItems": 12267,```

**Share:**
https://<--AWS DNS entry for your server-->/share/page/

### (Optional) Setup Cost Explorer 
Make sure you have the billing (Cost Explorer) set up to track the tag with the name: "Name"; 
This will help you to get cost estimates for your cluster. Detailed instructions: 
https://github.com/Alfresco/acs-deployment-aws/tree/master/docs/costs 
**Note:** It takes about 24h to add a new tag name to be monitored by the AWS Cost Explorer;
The cost estimates are done for the previous day, so another 24h to consider if you want cost estimation. Plan accordingly.

### Define the server load targets for your test
How many users/ sites/site members, files/folders, deleted folders/files you want on the system
Define what is the acceptable (avg)response time for your responsiveness BM tests (is an avg of of 300 ms ok for you); 

### Setup the EC2 machines for running the BM manager/mongo and the BM drivers
*Note:* It is a good idea to monitor these EC2 machines from the AWS Console (Monitor tab) to see the kind of load they are 
under while running the benchmark. 
You may want to create additional EC2 machines to run the BM tests from multiple drivers 
(not all BM drivers support this feature);  

**Set up the BM EC2 machines**

You will need to follow the steps below for at least one bm manager machine and one for the bm drivers:
1. Lunch new instance and select something like t3.xlarge(for the manager) or t3.2xlarge (for the drivers)
2. On the next page, select the VPC that you want (I selected the same one as the ACS deployment - using the internal network 
in the same VPC may help with network band/latency limitations)
3. Select the subnet that is public, so you can ssh into it; 
4. On the next page, depending on the BM you want to run, select how much storage you need. I picked 32 GB of General Purpose Storage
5. Add tags: name and author with proper values, so you can easily find them;
6. Set up proper SSH access restrictions, as see fit
7. Last, review the settings and use an existing key pair, or set up new ones
8. For the BM manager machine you will need to allow inbound access to 9080 port to access the UI

**Install docker**
https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html
```
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
# as ec2-user, the following command should work, after a disconnect and reconnect to ssh
docker info
```

**Install docker-compose**

See instructions: https://github.com/docker/compose/releases/tag/1.22.0

```
[ec2-user@ip-10-0-143-159 ~]$ sudo curl -L https://github.com/docker/compose/releases/download/1.22.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
[ec2-user@ip-10-0-143-159 ~]$ sudo chmod +x /usr/local/bin/docker-compose
[ec2-user@ip-10-0-143-159 ~]$ docker-compose version
docker-compose version 1.22.0, build f46880fe
```

**Login to quay.io**
1. Go to your account settings on the quay.io web page and access the "Generate Encrypted Password"; 
That will give you the encrypted password that you can use;
2. Go the ec2 machines and do ```docker login quay.io``` and it will ask you for the name and that encrypted password
3. You can test it with ```docker pull alfresco/alfresco-bm-manager```


### Run the benchmark manager

On the bm manager ec2 machine where you want to run the bm manager and mongo db (This mongo db will be referred to as the config mongo DB)
1. Get this file https://github.com/Alfresco/alfresco-bm-manager/blob/master/server/docker-compose/docker-compose-manager.yml 
into a file on your file system. You can rename it to docker-compose.yml;
2. Get this file https://github.com/Alfresco/alfresco-bm-manager/blob/master/server/docker-compose/.env as well, 
next to the docker-compose.yml
3. Edit it like this (take the ip of the system with ```ifconfig```): 
```
[ec2-user@ip-10-0-143-159 manager]$ ll -a
total 16
drwxrwxr-x 2 ec2-user ec2-user 4096 Oct  2 16:29 .
drwx------ 6 ec2-user ec2-user 4096 Oct  2 16:29 ..
-rw-rw-r-- 1 ec2-user ec2-user  432 Oct  2 16:14 docker-compose.yml
-rw-rw-r-- 1 ec2-user ec2-user  205 Oct  2 16:29 .env
[ec2-user@ip-10-0-143-159 manager]$ cat .env
ALFRESCO_BM_MANAGER_TAG=latest
ALFRESCO_BM_LOAD_USERS_TAG=latest
ALFRESCO_BM_LOAD_DATA_TAG=latest
ALFRESCO_BM_REST_API_TAG=latest
MONGO_TAG=latest
MONGO_HOST=10.0.143.159
MONGO_PORT=27017
REGISTRY=quay.io/
[ec2-user@ip-10-0-143-159 manager]$ cat docker-compose.yml
version: "3"
# This is the docker compose for Alfresco Benchmark Manager

services:
    alfresco-bm-manager:
        image: ${REGISTRY}alfresco/alfresco-bm-manager:${ALFRESCO_BM_MANAGER_TAG}
        environment:
            JAVA_OPTS : "
                -Dmongo.config.host=mongo
                "
        ports:
            - 9080:9080

    mongo:
        image: mongo:${MONGO_TAG}
        ports:
            - ${MONGO_PORT}:27017

```
4. Start it up ```docker-compose up``` (if you renamed the file to docker-compose.yml)
5. Make sure that you configure the inbound rules for this EC2 machine to accept TCP traffic on port 9080, then you can access 
the UI of the BM Manager.(e.g: http://_bm-manager-ec2_.compute-1.amazonaws.com:9080/alfresco-bm-manager/ )
6. Make sure the (one of the ) IP(s) of this machine is accessible from the bm driver ec2 machines, add firewall (inbound rules)
as see fit; If you used the same VPC and the private internal network ip, you should not have to configure anything else. 


### Run the benchmark drivers

On the ec2 machine(s) where you want to run the drivers:
1. Get this file https://github.com/Alfresco/alfresco-bm-manager/blob/master/server/docker-compose/docker-compose-drivers.yml 
into a file on your file system. You can rename it to docker-compose.yml ;
2. Get this file https://github.com/Alfresco/alfresco-bm-manager/blob/master/server/docker-compose/.env as well, 
next to the docker-compose.yml ;
3. Edit it like this (**this should be identical to the .env found on the bm manager machine (to be clear: use the IP of 
the bm manager machine in both cases);** make sure that ip is accessible from the machine(s) of the drivers):   
```
[ec2-user@ip-10-0-132-47 drivers]$ cat .env
ALFRESCO_BM_MANAGER_TAG=latest
ALFRESCO_BM_LOAD_USERS_TAG=latest
ALFRESCO_BM_LOAD_DATA_TAG=latest
ALFRESCO_BM_REST_API_TAG=latest
MONGO_TAG=latest
MONGO_HOST=10.0.143.159
MONGO_PORT=27017
REGISTRY=quay.io/
[ec2-user@ip-10-0-132-47 drivers]$ cat docker-compose.yml
version: "3"
# This is the docker compose for Alfresco Benchmark drivers

services:
    alfresco-bm-load-users:
        image: ${REGISTRY}alfresco/alfresco-bm-load-users:${ALFRESCO_BM_LOAD_USERS_TAG}
        environment:
            JAVA_OPTS : "
                -Dmongo.config.host=${MONGO_HOST}:${MONGO_PORT}
                -Dserver.port=9082
                "

    alfresco-bm-load-data:
        image: ${REGISTRY}alfresco/alfresco-bm-load-data:${ALFRESCO_BM_LOAD_DATA_TAG}
        environment:
            JAVA_OPTS : "
                -Dmongo.config.host=${MONGO_HOST}:${MONGO_PORT}
                -Dserver.port=9083
                "

    alfresco-bm-rest-api:
        image: ${REGISTRY}alfresco/alfresco-bm-rest-api:${ALFRESCO_BM_REST_API_TAG}
        environment:
            JAVA_OPTS : "
                -Dmongo.config.host=${MONGO_HOST}:${MONGO_PORT}
                -Dserver.port=9084
                "
```
**Note:** You could set up another separate mongo DB that would hold all the test data(results) that would be more secured, 
in case it contains sensitive data. So this is a good time to do that; You will use this in the UI ( or REST API ) when you 
define new test definitions or test runs. This will be referred to as the test results mongo db. If you don't have client
sensitive data, you can use the same config mongo DB from the bm manager ec2 machine;

4. Start them up with ```docker-compose up```
5. To check if it worked out, go to the bm-manager UI and try to create a new test definition 
(e.g: http://_bm-manager-ec2_.compute-1.amazonaws.com:9080/alfresco-bm-manager/#/tests/create) and at the end, in the combo box,
 you should see all 3 bm drivers available. 

## Run the benchmarking

Keep in mind that running any benchmarking is not always simple or always successful. Sometimes the aim is to see the limits of a
system (that would mean it will go down eventually). You need to monitor all the machines and all the logs in order to understand
some parts of what is happening while the systems operate at the limit of their performance.

You may need to experiment with the settings a bit, and find the best options that work for your setup or for the purpose you want
to do the benchmark; 
* if you just want to test the limits the ACS server with requests, you will do a certain setup; 
* if you want to prove that it can scale (up and down) the pods in a kubernetes setup (when that features will be available),
 you will probably use a number of tests with different settings);

It is assumed here that you know the basic concepts for benchmarking a system and basic statistical terms present in the 
results of the tests; Refer to the old docs for more details: https://github.com/Alfresco/alfresco-bm-manager/tree/master/docs/old  

**Note:** It may be a good idea to start up small (small number of users/threads/sites....) just to ensure the system is 
running and reachable;

#### General Settings considerations:

**Events and Threads:** The events and threads number should not really be tweaked, unless you know what your are doing. These are
meant to be a limitation (break) so not to overwork the bm driver machine. The events will execute as fast as possible in the limit
of the max number of threads configured here, BUT the way we control the desired event stream is by
the event producers (controlled by properties like siteLoad.maxActiveLoaders, or delays(user create delay, site create delay...))  

**Mongo DB connection details:** Change the ip and port of the mongo db where the test results are kept.
You could reuse the one that holds the test settings details (main one we started on the bm manager ec2 machine) if you 
don't have (plan to have) sensitive client data in the test; So add the (accessible) IP of the mongo db  (eg: 10.0.143.159:27017)
 
**Alfresco Server Details**: Change the admin user and password as set in the ACS installation, and set the URL to point to
the correct location of the server; 
```
Alfresco host    testenv.dev.alfresco.me
Alfresco URL     https://testenv.dev.alfresco.me
Alfresco port    443
Scheme           https
```
or
```
Alfresco host   testenv.dev.alfresco.me
Alfresco URL    https://testenv.dev.alfresco.me/
Alfresco port   443
```
_Note:_ The _Alfresco host_ property is very important as it is used in the mongo DB to save the mirror lists (users, sites..), 
used in most of the other tests;

**Test Controls**: It is very important to estimate the time you want to allocate for the test. Specify the duration of the 
tests (in seconds, minutes, hours, days...), otherwise, the test will stop at exactly that specified duration even if not all events
got a chance to be processed; 

**Note:**
* After staring the tests, you can monitor the events progress, from the UI of the bm-manager (you will need to manually refresh 
the list of events and logs tabs) 
* _See section "BM test run results analysis" for more details on what you need to do while the test is running 
and after it had finished;_ 


### Load Users:

First create the load users test:

* You should edit any other settings that you think it is required for your BM test (like the number or users you want to create);


### Load Data:

Using the users created earlier (that are also saved in the mongo db users mirror list) and the load-data bm-driver, next we will 
create some sites, site members, folders and files. We will also simulate a percentage of deleted folders (from the ones created).
 
So, create a new test definition and modify the properties:

1. Test Files: specify the details for an accessible FTP location
2. All the other settings for site names, files, folders, percentage of deleted folders....
3. Edit siteLoad.maxActiveLoaders if your ec2 driver machine and alfresco system can handle more concurrent load


### Responsiveness tests

This driver is mean to check the responsiveness of an Alfresco system, by running scenarios that simulate what users would do in
and Alfresco system (reading files, updating properties, searching for stuff and deleting stuff)
These scenarios will be improved in new stories;

The scenarios *can be executed* by multiple drivers (in parallel), just make sure you point all of them to the same config mongoDB

1. Select proper weights as desired for the load scenarios available
2. Select sessionBatchSize and sessionCount values for your test;
3. Modify any other parameter you want


## BM test run results analysis

During the test run, or after the test run, you can download csv (simple) and .xlsx (more complex) reports with data about the 
BM test run. 

* The first page contains a summary of the BM test run (up to that point in time) including all the events type that 
run and how many of them were successful, and time it took to run them. 

**Note:** Keep in mind that the events response time will vary depending on the settings you have made to the test run and they 
do not usually represent "atomic" operations: E.g.: The "create site files event" actually creates a number of files in a folder 
(this number is a property of the test run) and the time indicated there in the report is for the creation of all those files, 
not just 1 file.

* The second sheet in the xlsx report contains all the properties defined in that BM Test run - This is very useful to review and
to have as reference in case you want to define similar tests on some other ACS system.

* All the other sheets in the excel report will contain "time series" data about each such event, and few graphs indicating 
response time and failure rate 

**Note:** Different events could/will run at different times during the BM test run,(e.g: "prepare sites" events will only 
run at the beginning ( usually the first 5% of the total test run time) of the BM Test run and will create/prepare in the test run 
results mongo DB instance all the sites metadata that will be created in alfresco, while the "create site" event will run some 
time after that, then the "create site members" will run for a while, then "create folders" and "create site files"). 
So don't expect an even distribution of all the events throughout the time of BM Test run;

### Monitor the BM test Run

While the BM test runs you should monitor the load on all the (EC2 processing) machines used/affected by the BM Run.
See the sections : Preparation -> _AWS Console (web UI)_ and 
_Check the target Alfresco is up and running fine before the benchmark_ for some tips on what needs monitoring.
Additionally, monitor the alfresco logs and the kubernetes dashboard (see section below)

#### Setup Kubernetes cluster access
There are 2 ways:
##### ssh through the bastion server
This options is without UI;
See this tutorial: https://github.com/Alfresco/acs-deployment-aws/blob/master/docs/bastion_access.md 


##### Setup kubectl proxy locally to access the kubernetes dashboard UI

This is the official documentation: https://docs.aws.amazon.com/eks/latest/userguide/dashboard-tutorial.html



