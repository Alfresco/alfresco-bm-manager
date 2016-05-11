'use strict';
    /**
     * The benchmark-test.js is a AngularJS lib that contains
     * the controllers, services, filters of benchmark test run module.
     */

    /**
     * Service layer to interact with Test API.
     */
    var bmRun = angular.module('benchmark-run', ['ngResource']);

    /**
     * Test run properties service.
     */
    bmRun.factory('TestRunPropertyService', function($resource) 
    {
        return $resource
        (
            'api/v1/tests/:id/runs/:runname/props/:propertyname', 
            {
                id: '@id',
                propertyname: '@propertyname'
            }, 
            {
                'update': {
                    method: 'PUT',
                    params: 
                    {
                        id: 'id',
                        runname: 'runname',
                        propertyname: 'propertyname'
                    }
                }, 
                'delete': {
                    method: 'DELETE',
                    headers: 
                    {
                        'Content-Type': 'application/json'
                    },
                    params: 
                    {
                        id: 'id',
                        propertyname: 'propertyname'
                    }
                }
            }
        )
    }).value('version', '0.1');

    /**
     * Test run service.
     * Get run detail http://localhost:9080/alfresco-benchmark-server/api/v1/tests/Bench_13/runs/run_1
     * Stop run /api/v1/tests/SAMPLE1/runs/RUN01/terminate
     * Copy test run /api/v1/tests/SAMPLE1/runs
     */
    bmRun.factory('TestRunService', function($resource) {
        return $resource('api/v1/tests/:id/runs/:runname/:param1/:param2', {
            id: '@id',
            runname: '@runname',
            filterEventName: '@filterEventName',
            filterSuccess: '@filterSuccess',
            numberOfResults: '@numberOfResults'
        }, {
            getTestRuns: {
                method: 'GET',
                params: {
                    id: 'id'
                },
                isArray: true
            },
            getTestRun: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname'
                }
            },
            getTestRunSummary: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'summary'
                }
            },
            saveTestRun: {
                method: 'POST',
                params: {
                    id: 'id'
                }
            },
            deleteTestRun: {
                method: 'DELETE',
                params: {
                    id: 'id',
                    runname: 'runname'
                }
            },
            startTestRun: {
                method: 'POST',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'schedule'
                }
            },
            stopTestRun: {
                method: 'POST',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'terminate'
                }
            },
            updateTestRun: {
                method: 'PUT',
                params: {
                    id: 'id'
                }
            },
            getRunState: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'state'
                }
            },
            copyTest: {
                method: 'POST',
                params: {
                    id: 'id'
                }
            },
            getRunResult: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'results',
                    param2: 'ts'
                },
                isArray: true
            },
            getResultEventNames: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'results',
                    param2: 'eventNames'
                },
                isArray: true
            },
            getAllEventsFilterName: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'results',
                    param2: 'allEventsFilterName'
                },
                isArray: true
            },
            getEventDetails: {
                method: 'GET',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'results',
                    param2: 'eventResults'
                },
                isArray: true
            },
            importProps : {
            	method: 'POST',
                params: {
                    id: 'id',
                    runname: 'runname',
                    param1: 'importProps',
                }
            }
        })
    }).value('version', '0.1');
    
    /**
     * Show all logs /api/v1/status/logs
     */
    bmRun.factory('TestShowLogsService', function($resource) {
        return $resource('api/v1/status/logs', {}, {
            getAllLogs: {
                method: 'GET',
                params: {
                	driverId: 'driverId',
                	test: 'test',
                	run: 'run'
                },
                isArray: true
            }
        })
    }).value('version', '0.1');
    
        /**
     * Tests service.
     */
    bmRun.factory('TestService', function($resource) {
        return $resource("api/v1/tests/:id/:param", {
            id: '@id'
        }, {
            //Define methods on the TestService Object
            getTests: {
                method: 'GET',
                isArray: true
            },
            getTest: {
                method: 'GET',
                params: {
                    id: 'id'
                }
            },
            saveTest: {
                method: 'POST'
            },
            updateTest: {
                method: 'PUT'
            },
            copyTest: {
                method: 'POST'
            },
            deleteTest: {
                method: 'DELETE',
                params: {
                    id: 'id'
                }
            },
            getDrivers: {
                method: 'GET',
                isArray: true,
                params: {
                    id: 'id',
                    param:'drivers'
                }
            }
        })
    }).value('version', '0.1');
    
    /**
     * List test runs controller
     */
    bmRun.controller('TestRunListCtrl', ['$scope', '$location', '$window', '$timeout', 'TestRunService', 'ModalService', 'TestService',
        function($scope, $location, $window, $timeout, TestRunService, ModalService, TestService) {
            var timer;
            $scope.data = {};
            $scope.data.runs = {};
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            $scope.data.testname = names[0];
            $scope.drivers = [];
            $scope.hasError = false;
            $scope.error = {};
            $scope.error.msg = "";
            $scope.file = "";
            
            //loads the driver properties
            $scope.loadDrivers = function(thetestname){
                TestService.getDrivers({
                    id: thetestname
                }, function(response) {
                    //Strip $ as it prevents value from appearing
                    var jsonStr = JSON.stringify(response);
                    jsonStr = jsonStr.replace(/\$/g, '').replace('_id','ID');
                    $scope.drivers = JSON.parse(jsonStr); 
                });
            };
            
         // checks if drivers are present
            $scope.hasDrivers = function(thetestname){
                if ($scope.drivers){
                    return $scope.drivers.length > 0;
                }
                return false;
            };
            
            // checks if at least one driver is present
            $scope.checkDriver = function(thetestname){
                $scope.error.msg = "";
                $scope.hasError = false;
                $scope.loadDrivers(thetestname);
                if($scope.drivers){
                    if ($scope.drivers.length < 1){
                        // show HTML error message 
                        $scope.error.msg = "Unable to start test '" + thetestname + "': there is no driver present";
                        $scope.hasError = true;
                        return false;
                    }
                }
                return true;
            };
            /**
             * Get data gets test run collection from back-end.
             */
            $scope.getData = function() {
                // load initial drivers
                $scope.loadDrivers($scope.data.testname);
                
                TestRunService.getTestRuns({
                    id: $scope.data.testname
                }, function(response) {
                    var foundRunning = false;
                    var runs = response;
                    for (var i = 0; i < runs.length; i++) {
                        var run = runs[i];
                        if (run.state == 'NOT_SCHEDULED') {
                            run.hasStarted = false;
                        } else {
                            run.hasStarted = true;
                        }
                        if ("COMPLETED" == run.state || "STOPPED" == run.state) {
                            run.run = true;
                        } else {
                            run.run = false;
                        }
                        //foundRunning if has running test
                        var stillrunning = !run.run
                        if (run.hasStarted && stillrunning) {
                            foundRunning = true;
                        }
                    }
                    if (!foundRunning) {
                        $timeout.cancel(timer);
                    }
                    $scope.data.runs = runs;
                });
            };
            //Prepare data for initial page load.
            $scope.getData();

            //Refresh data every 2.5 seconds.
            $scope.poll = function() {
                timer = $timeout(function() {
                    $scope.getData();
                    $scope.poll();
                }, 2500);
            };
            $scope.poll();

            //Cancel timer action
            $scope.$on(
                "$destroy",
                function(event) {
                    $timeout.cancel(timer);
                }
            );

            // import properties
            $scope.importProperties = function(runname, testname){
            	$scope.importPropsData = {
                        display: true,
                        title: 'Import run properties: ' + runname,
                        message: "Select JSON property file to upload",
                        upload : true,
                        buttonClose: "Cancel",
                        buttonOk: "Import",
                        actionName: "doImportProperties",
                        actionValue: [runname, testname]
                    };
                $scope.modal = ModalService.create($scope.importPropsData);
            };
            
            // callback from modal to store the file content
            $scope.storeFile = function(file){
            	$scope.file = file;
            };
			
			// callback from modal to trigger import
			$scope.doImportProperties = function(runname, testname) {
				// create transfer to RestAPI
				var json = {
						"version" : 0,
	                    "value": $scope.file.toString()
	                };
				TestRunService.importProps({
                    id: testname,
                    runname: runname
                }, json, function(response) {
                	var result = response;
                	// if all OK - reload page
                	if (result.result.toString() == "OK"){
                		$window.location.reload();
                	}
                	else{
	                	try
	                	{
	                		$scope.importWarnErrorData = {
	                                display: true,
	                                title: 'Import result',
	                                message: result.msg,
	                                upload : false,
	                                buttonOk: "OK",
	                                actionName: "doFinishImport",
	                                actionValue: [runname, testname],
	                                hideButtonClose : true
	                            };
	                		$scope.modal = ModalService.create($scope.importWarnErrorData);
	                	}
	                	catch(err){/*alert(err);*/};
                	}
                })
			};

			// finish import on warn or error and navigate to property edit
			$scope.doFinishImport = function(runname, testname){
				var url = "#/tests/" + testname + "/" + runname + "/properties";
				$window.location = url;
				// final reload to ensure clean forms
				$window.location.reload();
			}
			
            // Call back to delete run
            $scope.deleteRun = function(runname, testname) {
                $scope.modal = {
                    display: true,
                    title: 'Delete run ' + runname,
                    message: 'Are you sure you want to delete ' + runname + ' ?',
                    buttonClose: "Cancel",
                    buttonOk: "Delete",
                    actionName: "doDeleteRun",
                    actionValue: [testname, runname]
                };
                $scope.modal = ModalService.create($scope.modal);
            };

            //Call back from modal to perform delete.
            $scope.doDeleteRun = function(testname, runname) {
                TestRunService.deleteTestRun({
                    id: testname,
                    runname: runname
                }, function(response) {
                    TestRunService.getTestRuns({
                        id: testname
                    }, function(response) {
                        $scope.data.runs = response;
                    });
                })
            };
            //call back to start run
            $scope.startRun = function(testname, index) {
                // check if driver present
                if (!$scope.checkDriver(testname)){
                    return;
                };
                
                var run = $scope.data.runs[index];
                var version = run.version;
                var currentdate = new Date();
                var time = currentdate.valueOf();
                var json = {
                    "version": version,
                    "scheduled": time,
                }
                TestRunService.startTestRun({
                    id: $scope.data.testname,
                    runname: run.name
                }, json, function(response) {
                    run = response;
                    run.hasStarted = true;
                    $scope.data.runs[index] = run;
                    $scope.poll();
                })
            };
            //call back to stop run
            $scope.stopRun = function(runname, testname, index) {
                TestRunService.stopTestRun({
                    id: $scope.data.testname,
                    runname: runname
                }, {}, function(response) {
                    $location.path("/tests/" + testname);
                });
            }
        }

    ]);

    /**
     * Controller to create test run
     */
    bmRun.controller('TestRunCreateCtrl', ['$scope', '$location', '$window', 'TestRunService', 'ValidationService',
        function($scope, $location, $window, TestRunService, ValidationService) {
            $scope.master = {};
            $scope.testname = {};
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            $scope.testname = names[0];
            $scope.runs = [];
            $scope.errorMsg = null;

            TestRunService.getTestRuns({
                    id: $scope.testname
                }, function(response) {
                    $scope.runs = response; 
                });

            $scope.reset = function() {
                $scope.test = angular.copy($scope.master);
                if($scope.runs < 1){
                    $window.location.hash = "#/";
                    $window.location.reload();
                }else{
                    $location.path("/tests/" + $scope.testname); 
                }
            };

            // validates the test name
            $scope.validateName = function(testRunName){
            	try{
                $scope.errorMsg = ValidationService.isValidTestRunName(testRunName);
            	}catch(err){
            		alert(err);
            	}
            };
            
            $scope.createTestRun = function(testrun) {
                var postData = {
                    "name": testrun.name,
                    "description": testrun.description
                };

                TestRunService.saveTestRun({
                        id: $scope.testname
                    }, postData,
                    function success(res) {
                        if (res.name === postData.name) {
                            var url = "#/tests/" + $scope.testname
                            //If the URL are same do a reload on the page
                            if ($window.location.hash === url) {
                                $window.location.reload();
                            } else {
                                $window.location.hash = url;
                            }
                        }
                    },
                    function error(errorVal) {
                        $scope.hasError = true;
                        if (errorVal.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = errorVal.data.error;
                        }
                    });
            }
        }
    ]);

    /**
     * Test run property controller
     */
    bmRun.controller('TestRunPropertyCtrl', ['$scope',
        '$location',
        'TestRunService',
        'TestRunPropertyService',
        'UtilService',
        'ValidationService',
        function($scope,
            $location,
            TestRunService,
            TestRunPropertyService,
            UtilService,
            ValidationService) {
            $scope.data = {};
            $scope.master = {};
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            var testname = names[0];
            var runname = names[1];

            $scope.readOnly = false;
            $scope.data.testname = testname;
            $scope.data.runname = runname;
            $scope.nameErrorMessage = null;

            TestRunService.getTestRun({
                id: $scope.data.testname,
                runname: $scope.data.runname
            }, function(response) {
                var redirect = "/tests/" + testname + "/" + runname +"/properties";
                $scope.data = response;
                $scope.data.properties.forEach(function(item){
                    if(item.value == "undefined"){
                        item.value = item['default'];
                    }
                });
                var result = UtilService.groupBy($scope.data.properties, function(item) {
                    return [item.group];
                });
                $scope.data.properties = result;
                $scope.master = angular.copy($scope.data);
                if (response.started > 0 || response.completed > 0) {
                    $scope.readOnly = true;
                }
                $location.path(redirect);
            })

            $scope.reset = function() {
                $scope.data = angular.copy($scope.master);
            }
            
            // Validates the name entered by the user
            $scope.validateRunName = function(){          
                $scope.nameErrorMessage = ValidationService.isValidTestRunName($scope.data.name);
            }

            // called for each key press  in the BM name editor:
            // returns true if to continue edit
            // returns false if edit is done. 
            $scope.doKeyPressName = function(event){
                if (event.keyCode == 13){
                    // ENTER - allowed only if no validation error message is present
                    if (null == $scope.nameErrorMessage){
                        $scope.updateRunName($scope.data.name);
                        return false;
                    }
                }
                else if (event.keyCode == 27){
                    // ESC
                    $scope.reset();
                    return false;
                }
                return true;
            }

            // called for each key press  in the BM test description editor:
            // returns true if to continue edit
            // returns false if edit is done. 
            $scope.doKeyPressDesc = function(event){
                if (event.keyCode == 13){
                    // ENTER
                    $scope.updateRunDesc($scope.data.description);
                    return false;
                }
                else if (event.keyCode == 27){
                    // ESC
                    $scope.reset();
                    return false;
                }
                return true;
            }
            
            //-------------- Test run properties CRUD ----------
            //call back for update run
            $scope.updateRunName = function(name) {
                var json = {
                    "oldName": runname,
                    "version": $scope.data.version,
                    "name": name,
                    "description": $scope.data.description
                }
                $scope.updateTestRun(json);
            }

            $scope.cancelEdit = function(item) {
             // restore
                item.value = item['cancelValue'];
            }

            //call back for updating run description
            $scope.updateRunDesc = function(description) {
                var json = {
                    "oldName": runname,
                    "version": $scope.data.version,
                    "name": runname,
                    "description": description
                }
                $scope.updateTestRun(json);
                // increase version number for there will be no reload of page on update
                // of the version number ... else next save will fail ...
                $scope.data.version = $scope.data.version + 1;
            }

            $scope.updateTestRun = function(data) {
                TestRunService.updateTestRun({
                    "id": testname
                }, data, function(response) {

                    $scope.runNameEditorEnabled = false;
                    $scope.runDescEditorEnabled = false;
                    $location.path("/tests/" + testname + "/" + response.name + "/properties");
                });
            };
            // call back for update test property field
            $scope.updateProperty = function(item) {
                var propData = {
                    "version": item.version,
                    "value": item.value
                };
                $scope.updateTestRunProperty(testname, runname, item.name, propData);
                item.version = item.version + 1;
            }

            $scope.resetProperty = function(item) {
                var restData = {
                    "version": item.version
                };
                $scope.updateTestRunProperty(testname, runname, item.name, restData);
                item.value = item['default'];
                item.version = 0;
            }
            
            // checks whether value is empty or not
            $scope.isEmpty = function(value){
                if (typeof value == undefined){
                    return true;
                }
                if (value == null){
                    return true;
                }
                var testString = "" + value; 
                if (testString == ""){
                    return true;
                }
                return false;
            }
            
            // copy the value from default if value is empty
            $scope.setInitialValue = function(item){
                if ($scope.isEmpty(item.value)){
                    item.value = item['default'];
                }

                // store for cancel
                item['cancelValue'] = item.value;
            }
            
            // called for each key press - returns true if to continue edit
            // returns false if edit is done. 
            $scope.doKeyPress = function(event, item){
                if (event.keyCode == 13){
                    // ENTER
                    $scope.updateProperty(item);
                    return false;
                }
                else if (event.keyCode == 27){
                    // ESC
                    $scope.cancelEdit(item);
                    return false;
                }
                return true;
            }
            
            // validates the property
            $scope.validate = function(itemProperty){
                ValidationService.validate(itemProperty);
            }
                        
            // checks whether the property item has a choice collection
            $scope.hasChoice = function(itemProperty){
                if (itemProperty.type.toLowerCase() == 'boolean'){
                    // a boolean value has implicit 'true' and 'false' only ...
                    return true;
                }
                    
                if (typeof itemProperty.choice != 'undefined'){
                    if(JSON.parse(itemProperty.choice).length > 0){
                        return true;
                    }
                }
                return false;
            }
            
            // returns the choice collection of a property item or null 
            $scope.getChoiceCollection = function(itemProperty){
                // check boolean first ...
                if (itemProperty.type.toLowerCase() == 'boolean'){
                    var choices= ["true", "false"];
                    return choices; 
                }
                if (typeof itemProperty.choice != 'undefined'){
                    return JSON.parse(itemProperty.choice);
                }
                return null;
            }
            
            $scope.updateTestRunProperty = function(testname, runname, propertyName, propData) {
                TestRunPropertyService.update({
                        "id": testname,
                        "runname": runname,
                        "propertyname": propertyName
                    }, propData,
                    function(res) {
                        var collection = [];
                        collection = collection.concat.apply(collection, $scope.data.properties);
                        for (var i in collection) {
                            if (propertyName === collection[i].name) {
                                collection.splice(i, 1, res);
                                break;
                            }
                        }
                        var result = UtilService.groupBy(collection, function(item) {
                            return [item.group];
                        });
                    });
            }
            $scope.toggleCollapsedStates = function(ind){
                if(ind.collapsed)
                {
                    ind.collapsed = false;
                } else {
                    ind.collapsed = true;
                };
            }
            $scope.attentionRequired = false;

            $scope.attentionReq = function(item) {
                if (typeof item.value != 'undefined'){
                    //for the case when we don't have a value and default '--' one exists
                    if (angular.isUndefined(item.value) && item['default'].indexOf('--') > -1)
                    {
                        $scope.attentionRequired = true;
                        $scope.attentionMessage = "* {" + item.group + " / " +item.name + "}: A value must be set.";
                        return true;
                    }
    
                    var isAttentionReq = item.value.indexOf('--') > -1;
                    if (isAttentionReq)
                    {
                        $scope.attentionRequired = true;
                        $scope.attentionMessage = "* {" + item.group + " / " +item.name + "}: A value must be set.";
                    }
                    return isAttentionReq;
                }
                return false;
            }
        }
    ]);

    /*
     * Run summary controller
     */
    bmRun.controller('TestRunSummaryCtrl', ['$scope', '$location', '$timeout', 'TestRunService', 'TestShowLogsService', 'ModalService', 'TestService',
        function($scope, $location, $timeout, TestRunService, TestShowLogsService, ModalService, TestService) {
            var timer;
            var timerEvents = null;
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            $scope.summary = {};
            $scope.logs = [];
            $scope.testname = names[0];
            $scope.runname = names[1];
            $scope.mockData = []; //Implement call to get charts
            $scope.hasError = false;
            $scope.errorMsg = "";
          
            // stores the driver properties 
            $scope.drivers = [];            
            
         // gets the driver properties
            $scope.loadDrivers = function(){
                TestService.getDrivers({
                            id: $scope.testname
                        }, function(response) {
                            //Strip $ as it prevents value from appearing
                            var jsonStr = JSON.stringify(response);
                            jsonStr = jsonStr.replace(/\$/g, '').replace('_id','ID');
                            $scope.drivers = JSON.parse(jsonStr); 
                        });
            };
            
            // checks if drivers are present
            $scope.hasDrivers = function(){
                if ($scope.drivers){
                    return $scope.drivers.length > 0;
                }
                return false;
            };
            
            // checks if drivers are present and notifies if not. 
            $scope.checkDrivers = function(){
                $scope.hasError = false;
                $scope.loadDrivers();
                if ($scope.drivers.length < 1){
                    // show HTML error message
                    $scope.hasError = true;
                    $scope.errorMsg = "No driver present, unable to start test run '" + $scope.testname + "." + $scope.runname;
                    return false;
                }
                return true;
            };
            
            // TODO
         // import properties
            $scope.importProperties = function(runname, testname){
            	$scope.importPropsData = {
                        display: true,
                        title: 'Import run properties: ' + runname,
                        message: "Select JSON property file to upload",
                        upload : true,
                        buttonClose: "Cancel",
                        buttonOk: "Import",
                        actionName: "doImportProperties",
                        actionValue: [runname, testname]
                    };
                $scope.modal = ModalService.create($scope.importPropsData);
            };
            
            // callback from modal to store the file content
            $scope.storeFile = function(file){
            	$scope.file = file;
            };
			
			// callback from modal to trigger import
			$scope.doImportProperties = function(runname, testname) {
				// create transfer to RestAPI
				var json = {
						"version" : 0,
	                    "value": $scope.file.toString()
	                };
				TestRunService.importProps({
                    id: testname,
                    runname: runname
                }, json, function(response) {
                	var result = response;
                	// if all OK - reload page
                	if (result.result.toString() == "OK"){
                		$window.location.reload();
                	}
                	else{
	                	try
	                	{
	                		$scope.importWarnErrorData = {
	                                display: true,
	                                title: 'Import result',
	                                message: result.msg,
	                                upload : false,
	                                buttonOk: "OK",
	                                actionName: "doFinishImport",
	                                actionValue: [runname, testname],
	                                hideButtonClose : true
	                            };
	                		$scope.modal = ModalService.create($scope.importWarnErrorData);
	                	}
	                	catch(err){/*alert(err);*/};
                	}
                })
			};

			// finish import on warn or error and navigate to property edit
			$scope.doFinishImport = function(runname, testname){
				var url = "#/tests/" + testname + "/" + runname + "/properties";
				$window.location = url;
				// final reload to ensure clean forms
				$window.location.reload();
			}            
            
            $scope.getSummary = function() {
                //initial chart display.
                $scope.summary.result = [0, 100];
                $scope.summary.total = 0;
                TestRunService.getTestRunSummary({
                    id: $scope.testname,
                    runname: $scope.runname
                }, function(response) {
                    var da = response
                    $scope.summary = da;
                    //Stop polling.
                    if (da.progress == 1) {
                        $timeout.cancel(timer);
                    }
                    $scope.summary.progress = da.progress * 100;
                    $scope.summary.result = [da.resultsSuccess, da.resultsFail];
                });
            }

            //Call back to delete run
            $scope.deleteRun = function() {
                $scope.modal = {
                    display: true,
                    title: 'Delete run ' + $scope.runname,
                    message: 'Are you sure you want to delete ' + $scope.runname + ' ?',
                    buttonClose: "Cancel",
                    buttonOk: "Delete",
                    actionName: "doDeleteRun",
                    actionValue: [$scope.testname, $scope.runname]
                }
                $scope.modal = ModalService.create($scope.modal);
            }

            //Call back from modal to perform delete.
            $scope.doDeleteRun = function() {
                TestRunService.deleteTestRun({
                    id: $scope.testname,
                    runname: $scope.runname
                }, function(response) {
                    $location.path("/tests/" + $scope.testname);                    
                })
            };
            //call back to start run
            $scope.startRun = function() {
                if (!$scope.checkDrivers()){
                    return;
                }          
                var version = $scope.summary.version;
                var currentdate = new Date();
                var time = currentdate.valueOf();
                var json = {
                    "version": version,
                    "scheduled": time,
                }
                TestRunService.startTestRun({
                    id: $scope.testname,
                    runname: $scope.runname
                }, json, function(response) {
                    $scope.hasStarted = true;
                })
            };
            
          //call back to stop run
            $scope.stopRun = function() {
                TestRunService.stopTestRun({
                    id: $scope.testname,
                    runname: $scope.runname
                }, {}, function(response) {
                    $scope.hasStarted = false;
                });
            }
            
            $scope.errorLogs = 0;
            // get test logs only - 2016-01-29 fkb: match at least test and run
            $scope.getTestLogs = function() {
            	TestShowLogsService.getAllLogs({
                    driverId: null,
                    test: $scope.testname,
                    run: $scope.runname
                }, function(response) {
                    var logs = [];
                    $scope.errorLogs = 0;
                    for (var i = 0; i < response.length; i++) {
                        var log = response[i];
                        if (log.t == $scope.testname && log.tr ==  $scope.runname) {
                            logs.push(log);
                            if(log.level == 4 || log.level == 5) {
                            	$scope.errorLogs = $scope.errorLogs + 1;
                            }
                        }
                    }
                    $scope.logs = logs;
                });
            }  
           
            // column sorting for logs
            $scope.columnSort = { sortColumn: 'time.$date', reverse: true };   
            // set new sort column or reverse order if clicked on the same column
            $scope.setSortColumn = function(sortColumnName)
            {
                if (sortColumnName)
                {
                    if ($scope.columnSort.sortColumn == sortColumnName)
                    {
                        $scope.columnSort.reverse = !$scope.columnSort.reverse;
                    }
                    else
                    {
                        $scope.columnSort.sortColumn = sortColumnName;
                    }
                }
            };
            
            // column sorting for event details
            $scope.eventColumnSort = { sortColumn: 'time.$date', reverse: true };
            // set new sort column or reverse order if clicked on the same column
            $scope.setEventSortColumn = function(sortColumnName)
            {
                if (sortColumnName)
                {
                    if ($scope.eventColumnSort.sortColumn == sortColumnName)
                    {
                        $scope.eventColumnSort.reverse = !$scope.eventColumnSort.reverse;
                    }
                    else
                    {
                        $scope.eventColumnSort.sortColumn = sortColumnName;
                    }
                }
            };
            
            // stores the number of events to get from the server
            $scope.numEvents = 25;
            
            // possible number of events to retrieve
            $scope.numEventValues = [5, 10, 15, 20, 25, 50, 100];
            
            // selected auto refresh
            $scope.autoRefresh = "off";
            
            // values for auto-refresh
            $scope.autoRefreshValues = ["off", "3 sec", "5 sec", "10sec", "15 sec", "30 sec", "60 sec", "90 sec"];
            
            // update auto-refresh of events
            $scope.selectRefresh = function(value){
                $scope.autoRefresh=value;
                var time = parseInt(value);
                if (time > 0){
                    time = time * 1000;
                    $scope.doAutoRefresh(time);
                }
                else{
                    // cancel timer
                    if (null != timerEvents){
                        $timeout.cancel(timerEvents);
                        timerEvents = null;
                    }
                }
            } 
            
            // auto refreh events
            $scope.doAutoRefresh=function(time){
                if (null != timerEvents){
                    $timeout.cancel(timerEvents);
                    timerEvents = null;
                }
                
                timerEvents = $timeout(function() {
                	$scope.getEventNames(); // update names for there may be new ones
                    $scope.getEvents();
                    $scope.doAutoRefresh(time);
                }, time);
            }
            
            // selects number of events and updates 
            $scope.selectNumEvents = function(num){
                $scope.numEvents = num;
                $scope.getEvents();
            };
            
            // checks if the number is selected or not
            $scope.isSelectedNumEvents = function(num){
                if (num){
                    if ($scope.numEvents == num){
                        return true;
                    }
                }
                return false;
            }
            
            // event filter
            $scope.eventFilters = ["All", "Success", "Failed"];
            $scope.selectedEventFilter = "All";
            $scope.selectEventFilter = function(filterName){
                $scope.selectedEventFilter = filterName;
                $scope.getEvents();
            }
            $scope.isSelectedFilter = function(filterName){
                if (filterName){
                    if ($scope.selectedEventFilter == filterName){
                        return true;
                    }
                }
                return false;
            }
            
            // filter by event names 
            $scope.eventNames = [];
            $scope.selectedEventName = null;
            $scope.selectEventName = function(eventName){
                $scope.selectedEventName = eventName;
                $scope.getEvents();
            };
            
            // gets available event names for the test - run
            $scope.getEventNames = function(){   
                TestRunService.getResultEventNames({
                    id: $scope.testname,
                    runname: $scope.runname
                }, function(response) {
                    var evn = [];
                    for (var i = 0; i < response.length; i++) {
                        var ev = response[i];
                        evn.push(ev);
                    }
                    // store the event names
                    $scope.eventNames = evn;
                    
                    // inital set the (All Events) text
                    if ($scope.selectedEventName == null){
	                    TestRunService.getAllEventsFilterName({
	                        id: $scope.testname,
	                        runname: $scope.runname
	                    }, function(response) {
	                        if (response.length == 1){
	                            $scope.selectedEventName = response[0];
	                        }
	                    });
                    }
                });
            };
            
            // checks if the given event name is selected
            $scope.isSelectedEvent = function(eventName){
              if (eventName){
                  if ($scope.selectedEventName == eventName){
                      return true;
                  }
              }
              return false;
            };
            
            // stores the events 
            $scope.events = [];
            
            // get events from server
            $scope.getEvents = function(){
                TestRunService.getEventDetails({
                    id: $scope.testname,
                    runname: $scope.runname,
                    filterEventName: $scope.selectedEventName,
                    filterSuccess: $scope.selectedEventFilter,
                    numberOfResults: $scope.numEvents
                }, function(response) {
                    var evn = [];
                    for (var i = 0; i < response.length; i++) {
                        var ev = response[i];
                        evn.push(ev);
                    }
                    // store the event names
                    $scope.events = evn;
                });
            };
            
            // get event names from result service
            $scope.getEventNames();
            
            //Get the summary now!
            $scope.getSummary();
            
            // initial load drivers
            $scope.loadDrivers();
            
            //Refresh data every 2.5 seconds.
            $scope.summaryPoll = function() {
                timer = $timeout(function() {
                    $scope.getSummary();
                    $scope.summaryPoll();
                }, 2500);
            };
            $scope.summaryPoll();
            $scope.getTestLogs();
            $scope.getEvents();
            
            //Cancel timer action
            $scope.$on(
                "$destroy",
                function(event) {
                    $timeout.cancel(timer);
                    if (null != timerEvents){
                        $timeout.cancel(timerEvents);
                        timerEvents = null;
                    }
                }
            );
        }
    ]);
    
    /*
     * Copy test form controller
     */
    bmRun.controller('TestRunCopyCtrl', ['$scope', '$location', 'TestRunService', 'ValidationService',
        function($scope, $location, TestRunService, ValidationService) {
            $scope.testname = $location.path().split('/')[2];
            $scope.runname = $location.path().split('/')[3];
            TestRunService.getTestRun({
                id: $scope.testname,
                runname: $scope.runname
            }, function(response) {
                $scope.data = response;
            });
            $scope.master = {};
            $scope.errorMsg = null;
            
            // validates the test run name
            $scope.validateName = function(testRunName){
                $scope.errorMsg = ValidationService.isValidTestRunName(testRunName);
            };

            $scope.update = function(test) {
                $scope.master = angular.copy(test);
            };

            $scope.reset = function() {
                $scope.test = angular.copy($scope.master);
            };

            $scope.isUnchanged = function(test) {
                return angular.equals(test, $scope.master);
            };

            $scope.copyTest = function(test) {
                if (test.name !== undefined && test.name !== $scope.testname) {
                    $scope.executeCopyTest(test);
                } else {
                    $scope.hasError = true;
                    $scope.errorMsg = "New unique test run name is required";
                }
                $scope.reset();
            }

            //callback for ng-click 'renameTest'
            $scope.executeCopyTest = function(test) {
                var postData = {
                    "copyOf": $scope.data.name,
                    "version": $scope.data.version,
                    "name": test.name
                };

                TestRunService.copyTest({
                        id: $scope.testname
                    }, postData, function(res) {
                        $scope.response = res;
                        $location.path("/tests/" + $scope.testname);
                    },
                    function error(errorVal) {
                        $scope.hasError = true;
                        if (errorVal.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = errorVal.data.error;
                        }
                    });
            };
        }
    ]);
