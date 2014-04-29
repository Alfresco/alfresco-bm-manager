Benchmark UI Unit test
==============
The UI unit test covers the controllers, directives, cross browser compability, factory and services that make up the benchmark server ui.

prerequisite
----------
The test is run, using karma, test runner,  and jasmine(a BDD base framework).
To run karma you would first need to have nodejs, please setup nodejs if not already installed.

Setup nodejs
-----------
see http://nodejs.org/

Setting Karma
------------
To setup karma open a terminal and invoke the following command: npm install -g karma

The node package manager will proceed to install and set karma.

Running UI unit test
-----------------
Navigate from root of the project to src/test/js/config and type the following:
karma start karma.conf.js

This will launch the karma test run server on port 9876, and run the unit tests. The server can be left running, and is configured to listen to any code change which will invoke the unit tests.
The test configuration can be found in src/test/js/config/karma.config.js

