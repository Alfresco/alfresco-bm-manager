# Goals for the revamped Alfresco Benchmark Framework

Most of the requirements for this rework of the Alfresco Benchmark Framework are captured in the 
[PRODEL-611](https://issues.alfresco.com/jira/browse/PRODDEL-611) and 
[related Epics](https://issues.alfresco.com/jira/issues/?jql=project%20%3D%20REPO%20AND%20fixVersion%20%3D%20%22VB%3A%20ACS%20Load%20Tests%20on%20AWS%22).
Make sure you give them a good read.

This document is intended to clear up some questions about what we want to do and challenge the old way of doing things. 

## Questions
### What are the use cases we should target?
I think these are captured in the epics in Jira, and we can't make any more predictions on what kind of use cases we should target.

The **initial focus** is to get the *bm-manager* and a few *bm-drivers* (minimal set of required functionality: load users, load data(files), 
test the responsiveness of the system) working with the recommended Alfresco V1 Rest API. All this done with docker (docker-compose) 
so it would be easy to run some initial benchmarking on AWS machines against a reference ACS deployed in AWS in order to get a 
ballpark figure about the costs of running ACS in AWS, but also start monitoring the performance of ACS and identifying bottlenecks
in performance.

Just to do all that, we need to do a few modification to the old framework to work with docker and we need to rewrite/create 
a few new bm-drivers that would work with V1 Rest API from Alfresco.
 
### What is the output and how is the output used to compare results?

For the **initial focus**, as described above, the existing reports (formats and data) are considered adequate.
They include details about the success and failure number/rate; time statistics for each event type and some graphical charts.
We will consider improving this in the future epics.

### How easy is to get the output results and compare the results?

Although, for the **initial focus** a lot of the configuration work is done manually, someone could still run an identical/similar
test against 2 or more ACS installations and download the XLSX report and compare the results (manually).

In future epics/iterations we will try to add more features that would facilitate this.

### What is out of scope? needs to be documented- at least what we know at this stage that it is out of scope.

For the **initial focus** everything that is not targeted towards getting ACS initial (ballpark) costs estimations on AWS and 
monitoring and identifying performance problems in ACS, should be considered out of scope.


