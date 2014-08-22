'use strict';
    /**
     * The benchmark-test.js is a AngularJS lib that contains
     * the controllers, services, filters of benchmark test run module.
     */

    /**
     * Service layer to interact with Test API.
     */
    angular.module('benchmark-run', ['ngResource'])

    /**
     * Test run properties service.
     */
    .factory('TestRunPropertyService', function($resource) {
        return $resource('api/v1/tests/:id/runs/:runname/props/:propertyname', {
            id: '@id',
            propertyname: '@propertyname'
        }, {
            update: {
                method: 'PUT',
                params: {
                    id: 'id',
                    runname: 'runname',
                    propertyname: 'propertyname'
                }
            },
            delete: {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                },
                params: {
                    id: 'id',
                    propertyname: 'propertyname'
                }
            }
        })
    }).value('version', '0.1')

    /**
     * Test run service.
     * Get run detail http://localhost:9080/alfresco-benchmark-server/api/v1/tests/Bench_13/runs/run_1
     * Stop run /api/v1/tests/SAMPLE1/runs/RUN01/terminate
     * Copy test run /api/v1/tests/SAMPLE1/runs
     */
    .factory('TestRunService', function($resource) {
        return $resource('api/v1/tests/:id/runs/:runname/:more', {
            id: '@id',
            runname: '@runname'
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
                    more: 'summary'
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
                    more: 'schedule'
                }
            },
            stopTestRun: {
                method: 'POST',
                params: {
                    id: 'id',
                    runname: 'runname',
                    more: 'terminate'
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
                    more: 'state'
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
                    more: 'results/ts'
                }
            },
        })
    }).value('version', '0.1')
/**
     * List test runs controller
     */
    .controller('TestRunListCtrl', ['$scope', '$location', '$timeout', 'TestRunService', 'ModalService',
        function($scope, $location, $timeout, TestRunService, ModalService) {
            var timer;
            $scope.data = {};
            $scope.data.runs = {};
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            $scope.data.testname = names[0];
            /**
             * Get data gets test run collection from backend.
             */
            $scope.getData = function() {

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
            //Prepare data for inital page load.
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

            //Call back to delete run
            $scope.deleteRun = function(runname, testname) {
                $scope.modal = {
                    display: true,
                    title: 'Delete run ' + runname,
                    message: 'Are you sure you want to delete ' + runname + ' ?',
                    buttonClose: "Cancel",
                    buttonOk: "Delete",
                    actionName: "doDeleteRun",
                    actionValue: [testname, runname]
                }
                $scope.modal = ModalService.create($scope.modal);
            }

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
            }
            //call back to start run
            $scope.startRun = function(testname, index) {
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
            }
            //call back to stop run
            $scope.stopRun = function(runname, testname, index) {
                TestRunService.stopTestRun({
                    id: $scope.data.testname,
                    runname: runname
                }, {}, function(response) {
                    $location.path("/tests/" + testname);
                })
            }
        }

    ])

    /**
     * Controller to create test run
     */
    .controller('TestRunCreateCtrl', ['$scope', '$location', '$window', 'TestRunService',
        function($scope, $location, $window, TestRunService) {
            $scope.master = {};
            $scope.testname = {};
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            $scope.testname = names[0];

            $scope.update = function(testrun) {
                $scope.master = angular.copy(testrun);
            };

            $scope.reset = function() {
                $scope.test = angular.copy($scope.master);
            };

            $scope.isUnchanged = function(testrunrun) {
                return angular.equals(testrun, $scope.master);
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
                            //If the url are same do a reload on the page
                            if ($window.location.hash === url) {
                                $window.location.reload();
                            } else {
                                $window.location.hash = url;
                            }
                        }
                    },
                    function error(error) {
                        $scope.hasError = true;
                        if (error.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = error.data.error;
                        }
                    })
            }

            $scope.reset = function() {
                $scope.run = angular.copy($scope.master);
            };
            $scope.reset();
        }
    ])

    /**
     * Test run detail controller
     */
    .controller('TestRunPropertyCtrl', ['$scope',
        '$location',
        'TestRunService',
        'TestRunPropertyService',
        'UtilService',
        function($scope,
            $location,
            TestRunService,
            TestRunPropertyService,
            UtilService) {
            $scope.data = {};
            $scope.master = {};
            var path = $location.path();
            var names = path.replace("/tests/", "").split("/");
            var testname = names[0];
            var runname = names[1];

            $scope.readOnly = false;
            $scope.data.testname = testname;
            $scope.data.runname = runname;

            TestRunService.getTestRun({
                id: $scope.data.testname,
                runname: $scope.data.runname
            }, function(response) {
                var redirect = "/tests/" + testname + "/" + runname +"/properties";
                $scope.data = response;
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

            //-------------- Test properties crud ----------
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
            //call back for updating run description
            $scope.updateRunDesc = function(description) {
                var json = {
                    "oldName": runname,
                    "version": $scope.data.version,
                    "name": runname,
                    "description": description
                }
                $scope.updateTestRun(json);
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
            }

            $scope.resetProperty = function(item) {
                var restData = {
                    "version": item.version
                };
                $scope.updateTestRunProperty(testname, runname, item.name, restData);
                item.value = item.default;
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
        }
    ])

    /*
     * Run summary controller
     */
    .controller('TestRunSummaryCtrl', ['$scope', '$location', '$timeout', 'TestRunService',
        function($scope, $location, $timeout, TestRunService) {
            var timer;
            $scope.mockData = []; //Implement call to get charts
            $scope.getSummary = function() {
                var path = $location.path();
                var names = path.replace("/tests/", "").split("/");
                $scope.summary = {};
                $scope.testname = names[0];
                //inital chart display.
                $scope.summary.result = [0, 100];
                $scope.summary.total = 0;
                TestRunService.getTestRunSummary({
                    id: names[0],
                    runname: names[1]
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
            //Get the summary now!
            $scope.getSummary();

            //Refresh data every 2.5 seconds.
            $scope.summaryPoll = function() {
                timer = $timeout(function() {
                    $scope.getSummary();
                    $scope.summaryPoll();
                }, 2500);
            };
            $scope.summaryPoll();

            //Cancel timer action
            $scope.$on(
                "$destroy",
                function(event) {
                    $timeout.cancel(timer);
                }
            );
        }
    ])
/*
     * Copy test form controller
     */
    .controller('TestRunCopyCtrl', ['$scope', '$location', 'TestRunService',
        function($scope, $location, TestRunService) {
            $scope.testname = $location.path().split('/')[2];
            $scope.runname = $location.path().split('/')[3];
            TestRunService.getTestRun({
                id: $scope.testname,
                runname: $scope.runname
            }, function(response) {
                $scope.data = response;
            });
            $scope.master = {};

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
                    function error(error) {
                        $scope.hasError = true;
                        if (error.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = error.data.error;
                        }
                    });
            };
        }
    ]);