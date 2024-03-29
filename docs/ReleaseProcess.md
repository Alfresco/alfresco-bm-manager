### How we work? How do we release?

We have two type of projects : 
* benchmark manager: [alfresco-bm-manager](https://github.com/Alfresco/alfresco-bm-manager)
* benchmark drivers:
   * [alfresco-bm-load-users](https://github.com/Alfresco/alfresco-bm-load-users)
   * [alfresco-bm-load-data](https://github.com/Alfresco/alfresco-bm-load-data)
   * [alfresco-bm-rest-api](https://github.com/Alfresco/alfresco-bm-rest-api)

The manager is completely independent and it can be released independently, at any time.

The drivers are dependent on the manager code and should always use the latest (stable released) version of [alfresco-bm-manager](https://github.com/Alfresco/alfresco-bm-manager). 

All benchmark projects are always released off the master branch: latest and greatest.

*Note:* 
* We can do builds on branches other than master (that will also publish docker images) and we can do releases based on this branches, but this should be only used for development/testing purposes and should not be referenced on master branches of any of the bm-projects.

### Benchmark docker images
The only tag needed to uniquely identify any release, would be the release.version or project.version. 
The latest tag is generated by the master branch of each project and pushed to quay.io.

### Release
* Components for the release are updated as required (e.g. no snapshots versions allowed).
* From Bamboo, start a build on **master branch** and wait for it to be green.
* Check that the latest docker image was pushed to quay.io and wait for it to be security scanned. If there are security problems, these need to be raised and addressed with the PO.
* Once the security scan for the docker image is green, press the release stage and override the 2 variables:
**release.version** and **development.version**.
   * If the current version is 3.0.0-SNAPSHOT, then it will be:
      * release.version=3.0.0
      * development.version=3.0.1-SNAPSHOT
* The Git packaging project will be updated to the release.version, released, tagged, and then updated to development.version just like any other maven (release) project. 
* A docker image is build and pushed to quay.io. Locate the docker images in quay.io (alfresco-bm-(manager)/(load-users)/(load-data)/(rest-api)) with the tag exactly as the specified by release.version.

All components are released and uploaded to Nexus using their released versions.
Older releases for alfresco-bm-manager can be found [here](https://github.com/AlfrescoBenchmark/alfresco-benchmark/releases).
