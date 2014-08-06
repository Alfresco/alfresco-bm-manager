            'use strict';
            /**
             * The testdef.js is a AngularJS lib that contains
             * the controllers, services, filters of the test def module.
             */

            /**
             * Service layer to interact with Test API.
             */
            angular.module('benchtest', ['ngResource'])

            /**
             * Test Def Service.
             */
            .factory('TestDefDetailService', function($resource) {
                return $resource('api/v1/test-defs/:name/:schema', {
                    name: '@name',
                    schema: '@schema'
                }, {
                    //Returns list of active test defs
                    getTestDef: {
                        method: 'GET',
                        params: {
                            name: 'name',
                            schema: 'schema'
                        }
                    }
                })
            }).value('version', '0.1')

            .factory('TestDefService', function($resource) {
                return $resource('api/v1/test-defs?:name', {
                    name: '@name'
                }, {
                    //Returns list of active test defs
                    getAllTestDefs: {
                        method: 'GET',
                        params: {
                            name: 'activeOnly=false'
                        },
                        isArray: true
                    },
                    getTestDefs: {
                        method: 'GET',
                        params: {
                            name: 'activeOnly=true'
                        },
                        isArray: true
                    },
                })
            }).value('version', '0.1')

            /**
             * Tests service.
             */
            .factory('TestService', function($resource) {
                return $resource("api/v1/tests/:id", {
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
                })
            }).value('version', '0.1')

            /**
             * Test properties service.
             */
            .factory('TestPropertyService', function($resource) {
                return $resource('api/v1/tests/:id/props/:propertyname', {
                    id: '@id',
                    propertyname: '@propertyname'
                }, {
                    update: {
                        method: 'PUT',
                        params: {
                            id: 'id',
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
             * Test Controllers
             */
             //Controller to list tests, delete test and edit test.
            .controller('ListTestsCtrl', ['$scope', 'TestService', 'ModalService',
                function($scope, TestService, ModalService) {
                    // Instantiate an object to store your scope data in (Best Practices)
                    $scope.data = {};

                    // callback for ng-click 'deleteTest':
                    $scope.deleteTest = function(testId) {
                        $scope.modal = {
                            display: true,
                            title: 'Delete test ' + testId,
                            message: 'Are you sure you want to delete ' + testId + ' ?',
                            buttonClose: "Cancel",
                            buttonOk: "Delete",
                            actionName: "doDeleteTest",
                            actionValue: [testId]
                        }
                        $scope.modal = ModalService.create($scope.modal);
                    }

                    $scope.doDeleteTest = function(name) {
                        TestService.deleteTest({
                            id: name
                        }, function(response){
                            $scope.data.tests = TestService.getTests()
                        });
                        $scope.data.tests = TestService.getTests();
                    }

                    $scope.data.tests = TestService.getTests();
                }
            ])


            /**
             * Controller to display test detail
             */
            .controller('TestDetailCtrl', ['$scope', '$location', 'TestService', 'TestPropertyService', 'UtilService',
                function($scope, $location, TestService, TestPropertyService, UtilService) {
                    $scope.data = {};
                    $scope.properties = [];
                    $scope.master = {}
                    var myname = $location.path().split('/');
                    var testname = myname[2];
                    TestService.getTest({
                        id: testname
                    }, function(response) {
                        $scope.data.test = response;
                        var result = UtilService.groupBy($scope.data.test.properties, function(item) {
                            return [item.group];
                        });
                        $scope.data.properties = result;
                        $scope.master = angular.copy($scope.data.test);
                    });

                    //callback for ng-click 'deleteTest':
                    $scope.deleteTest = function(testId) {
                        TestService.deleteTest({
                            id: testId
                        });
                        $location.path("/tests/");
                    };
                    $scope.reset = function() {
                        console.log($scope.master)
                        $scope.data.test = angular.copy($scope.master);
                    }

                    //callback for ng-click 'renameTest'
                    $scope.renameTest = function(name) {
                        var postData = {
                            "oldName": testname,
                            "name": name,
                            "version": $scope.data.test.version,
                            "description": $scope.data.test.description,
                            "release": $scope.data.test.release,
                            "schema": $scope.data.test.schema
                        };
                        $scope.updateTest(postData);
                    }


                    //callback for ng-click 'updateTestDesc':
                    $scope.updateTestDesc = function(desc) {
                        var testDescEditorEnabled = $scope.testDescEditorEnabled;
                        $scope.testDescEditorEnabled = !testDescEditorEnabled;
                        var postData = {
                            "oldName": testname,
                            "name": testname,
                            "version": $scope.data.test.version,
                            "description": desc,
                            "release": $scope.data.test.release,
                            "schema": $scope.data.test.schema
                        };
                        $scope.updateTest(postData);
                    }

                    //callback for ng-click 'updateTestSchema':
                    $scope.updateTestSchema = function(release, schema) {
                        var testVersionEditorEnabled = $scope.testVersionEditorEnabled;
                        $scope.testVersionEditorEnabled = !testVersionEditorEnabled;
                        var postData = {
                            "oldName": testname,
                            "name": testname,
                            "version": $scope.data.test.version,
                            "description": $scope.data.test.description,
                            "release": $scope.data.test.release,
                            "schema": $scope.data.test.schema
                        };
                        $scope.updateTest(postData);
                    }

                    //Updates the test and redirects to new page
                    $scope.updateTest = function(postData) {
                        TestService.updateTest({}, postData, function(res) {
                            for (var i in $scope.data.properties) {
                                if (propertyName === $scope.data.properties[i].name) {
                                    $scope.data.properties.splice(i, 1, res);
                                    break;
                                }
                            }
                        });

                        $location.path("/tests/" + postData.name + "/properties");
                    }

                    //-------------- Test properties crud ----------
                    // call back for update test property field
                    $scope.updateProperty = function(propertyName, propertyValue, propertyVersion) {
                        var propData = {
                            "version": propertyVersion,
                            "value": propertyValue
                        };
                        $scope.updateTestProperty(testname, propertyName, propData);
                    }

                    $scope.resetProperty = function(propertyName, propertyValue, propertyVersion) {
                        var restData = {
                            "version": propertyVersion,
                            "value": propertyValue
                        };
                        $scope.updateTestProperty(testname, propertyName, restData);
                    }
                    $scope.updateTestProperty = function(testname, propertyName, propData) {
                        TestPropertyService.update({
                                "id": testname,
                                "propertyname": propertyName
                            }, propData,
                            function(res) {
                                var redirect = "/tests/" + testname + "/properties";
                                //update modal with latest from backend
                                TestService.getTest({
                                    id: testname
                                }, function(response) {
                                    $scope.data.test = response;
                                    var result = UtilService.groupBy($scope.data.test.properties, function(item) {
                                        return [item.group];
                                    });
                                    $scope.data.properties = result;
                                    $scope.master = angular.copy($scope.data.test);
                                });
                                $location.path(redirect);
                            });
                    }
                }
            ])

            /**
             * Controller to create test detail
             */
            .controller('TestCreateCtrl', ['$scope', '$location', 'TestService', 'TestDefService',
                function($scope, $location, TestService, TestDefService) {
                    $scope.master = {};
                    $scope.defs = {};
                    $scope.nodefs = false;
                    TestDefService.getAllTestDefs(function(response) {
                        $scope.defs = response;
                        if ($scope.defs.length == 0) {
                            $scope.nodefs = true;
                        } else {
                            $scope.nodefs = false;
                        }
                    });
                    $scope.update = function(test) {
                        $scope.master = angular.copy(test);
                    };

                    $scope.reset = function() {
                        $scope.test = angular.copy($scope.master);
                    };

                    $scope.isUnchanged = function(test) {
                        return angular.equals(test, $scope.master);
                    };

                    $scope.createTest = function(test) {
                        if (test.def !== undefined) {
                            var def = test.def.split("-schema:");
                            var release = def[0];
                            var schema = def[1];
                            var postData = {
                                "name": test.name,
                                "description": test.description,
                                "release": release,
                                "schema": schema
                            };
                            TestService.saveTest({}, postData, function(res) {
                                if (res.name === postData.name) {
                                    $location.path("/tests/" + res.name + "/properties");
                                }
                            }, function error(error) {
                                $scope.hasError = true;
                                if (error.status == 500) {
                                    $scope.errorMsg = "The name already exists, please choose another unique name.";
                                } else {
                                    $scope.errorMsg = error.data.error;
                                }

                            });
                        } else {
                            $scope.hasError = true;
                            $scope.errorMsg = "Valid test version and schema is required.";
                        }
                    }
                    $scope.reset();
                }
            ])

            /**
             * Controller to list test defs.
             */
            .controller('TestDefListCtrl', ['$scope', 'TestDefService',
                function($scope, TestDefService) {
                    // Instantiate an object to store your scope data in (Best Practices)
                    $scope.data = {};
                    TestDefService.getAllTestDefs(function(response) {
                        $scope.data.tests = response;
                    });
                }
            ])
            /**
             * Controler to display test def detail.
             */
            .controller('TestDefDetailCtrl', ['$scope', '$location', 'TestDefDetailService',
                function($scope, $location, TestDefDetailService) {
                    $scope.data = {};
                    var path = $location.path();
                    var testDefName = path.replace("/testdefs/", "").split("/");

                    TestDefDetailService.getTestDef({
                        name: testDefName[0],
                        schema: testDefName[1]
                    }, function(response) {
                        $scope.data.item = response;
                    });
                }
            ])
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
            .controller('TestRunDetailCtrl', ['$scope',
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
                        $scope.data = response;
                        var result = UtilService.groupBy($scope.data.properties, function(item) {
                            return [item.group];
                        });
                        $scope.data.properties = result;
                        $scope.master = angular.copy($scope.data);
                        if (response.started > 0 || response.completed > 0) {
                            $scope.readOnly = true;
                        }
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
                    $scope.updateProperty = function(propertyName, propertyValue, propertyVersion) {
                        var propData = {
                            "version": propertyVersion,
                            "value": propertyValue
                        };
                        $scope.updateTestRunProperty(testname, runname, propertyName, propData);
                    }

                    $scope.resetProperty = function(propertyName, propertyVersion) {
                        var restData = {
                            "version": propertyVersion
                        };
                        $scope.updateTestRunProperty(testname, runname, propertyName, restData);
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
                                $scope.data.properties = result;
                            });
                    }
                }
            ])

            /*
             * Run summary controller
             */
            .controller('TestRunSummaryCtrl', ['$scope', '$location', '$timeout', 'TestRunService',
                function($scope, $location, $timeout, TestRunService) {
                    var timer;
                    $scope.mockData = [{
        "time": "Jan 2000",
        "value": "1394.46",
        "name": "search"
    }, {
        "time": "Feb 2000",
        "value": "1366.42",
        "name": "search"
    }, {
        "time": "Mar 2000",
        "value": "1498.58",
        "name": "search"
    }, {
        "time": "Apr 2000",
        "value": "1452.43",
        "name": "search"
    }, {
        "time": "May 2000",
        "value": "1420.6",
        "name": "search"
    }, {
        "time": "Jun 2000",
        "value": "1454.6",
        "name": "search"
    }, {
        "time": "Jul 2000",
        "value": "1430.83",
        "name": "search"
    }, {
        "time": "Aug 2000",
        "value": "1517.68",
        "name": "search"
    }, {
        "time": "Sep 2000",
        "value": "1436.51",
        "name": "search"
    }, {
        "time": "Oct 2000",
        "value": "1429.4",
        "name": "search"
    }, {
        "time": "Nov 2000",
        "value": "1314.95",
        "name": "search"
    }, {
        "time": "Dec 2000",
        "value": "1320.28",
        "name": "search"
    }, {
        "time": "Jan 2001",
        "value": "1366.01",
        "name": "search"
    }, {
        "time": "Feb 2001",
        "value": "1239.94",
        "name": "search"
    }, {
        "time": "Mar 2001",
        "value": "1160.33",
        "name": "search"
    }, {
        "time": "Apr 2001",
        "value": "1249.46",
        "name": "search"
    }, {
        "time": "May 2001",
        "value": "1255.82",
        "name": "search"
    }, {
        "time": "Jun 2001",
        "value": "1224.38",
        "name": "search"
    }, {
        "time": "Jul 2001",
        "value": "1211.23",
        "name": "search"
    }, {
        "time": "Aug 2001",
        "value": "1133.58",
        "name": "search"
    }, {
        "time": "Sep 2001",
        "value": "1040.94",
        "name": "search"
    }, {
        "time": "Oct 2001",
        "value": "1059.78",
        "name": "search"
    }, {
        "time": "Nov 2001",
        "value": "1139.45",
        "name": "search"
    }, {
        "time": "Dec 2001",
        "value": "1148.08",
        "name": "search"
    }, {
        "time": "Jan 2002",
        "value": "1130.2",
        "name": "search"
    }, {
        "time": "Feb 2002",
        "value": "1106.73",
        "name": "search"
    }, {
        "time": "Mar 2002",
        "value": "1147.39",
        "name": "search"
    }, {
        "time": "Apr 2002",
        "value": "1076.92",
        "name": "search"
    }, {
        "time": "May 2002",
        "value": "1067.14",
        "name": "search"
    }, {
        "time": "Jun 2002",
        "value": "989.82",
        "name": "search"
    }, {
        "time": "Jul 2002",
        "value": "911.62",
        "name": "search"
    }, {
        "time": "Aug 2002",
        "value": "916.07",
        "name": "search"
    }, {
        "time": "Sep 2002",
        "value": "815.28",
        "name": "search"
    }, {
        "time": "Oct 2002",
        "value": "885.76",
        "name": "search"
    }, {
        "time": "Nov 2002",
        "value": "936.31",
        "name": "search"
    }, {
        "time": "Dec 2002",
        "value": "879.82",
        "name": "search"
    }, {
        "time": "Jan 2003",
        "value": "855.7",
        "name": "search"
    }, {
        "time": "Feb 2003",
        "value": "841.15",
        "name": "search"
    }, {
        "time": "Mar 2003",
        "value": "848.18",
        "name": "search"
    }, {
        "time": "Apr 2003",
        "value": "916.92",
        "name": "search"
    }, {
        "time": "May 2003",
        "value": "963.59",
        "name": "search"
    }, {
        "time": "Jun 2003",
        "value": "974.5",
        "name": "search"
    }, {
        "time": "Jul 2003",
        "value": "990.31",
        "name": "search"
    }, {
        "time": "Aug 2003",
        "value": "1008.01",
        "name": "search"
    }, {
        "time": "Sep 2003",
        "value": "995.97",
        "name": "search"
    }, {
        "time": "Oct 2003",
        "value": "1050.71",
        "name": "search"
    }, {
        "time": "Nov 2003",
        "value": "1658.2",
        "name": "search"
    }, {
        "time": "Dec 2003",
        "value": "1111.92",
        "name": "search"
    }, {
        "time": "Jan 2004",
        "value": "1131.13",
        "name": "search"
    }, {
        "time": "Feb 2004",
        "value": "1144.94",
        "name": "search"
    }, {
        "time": "Mar 2004",
        "value": "1126.21",
        "name": "search"
    }, {
        "time": "Apr 2004",
        "value": "1107.3",
        "name": "search"
    }, {
        "time": "May 2004",
        "value": "1120.68",
        "name": "search"
    }, {
        "time": "Jun 2004",
        "value": "1140.84",
        "name": "search"
    }, {
        "time": "Jul 2004",
        "value": "1101.72",
        "name": "search"
    }, {
        "time": "Aug 2004",
        "value": "1104.24",
        "name": "search"
    }, {
        "time": "Sep 2004",
        "value": "1114.58",
        "name": "search"
    }, {
        "time": "Oct 2004",
        "value": "1130.2",
        "name": "search"
    }, {
        "time": "Nov 2004",
        "value": "1173.82",
        "name": "search"
    }, {
        "time": "Dec 2004",
        "value": "1211.92",
        "name": "search"
    }, {
        "time": "Jan 2005",
        "value": "1181.27",
        "name": "search"
    }, {
        "time": "Feb 2005",
        "value": "1203.6",
        "name": "search"
    }, {
        "time": "Mar 2005",
        "value": "1180.59",
        "name": "search"
    }, {
        "time": "Apr 2005",
        "value": "1156.85",
        "name": "search"
    }, {
        "time": "May 2005",
        "value": "1191.5",
        "name": "search"
    }, {
        "time": "Jun 2005",
        "value": "1191.33",
        "name": "search"
    }, {
        "time": "Jul 2005",
        "value": "1234.18",
        "name": "search"
    }, {
        "time": "Aug 2005",
        "value": "1220.33",
        "name": "search"
    }, {
        "time": "Sep 2005",
        "value": "1228.81",
        "name": "search"
    }, {
        "time": "Oct 2005",
        "value": "1207.01",
        "name": "search"
    }, {
        "time": "Nov 2005",
        "value": "1249.48",
        "name": "search"
    }, {
        "time": "Dec 2005",
        "value": "1248.29",
        "name": "search"
    }, {
        "time": "Jan 2006",
        "value": "1280.08",
        "name": "search"
    }, {
        "time": "Feb 2006",
        "value": "1280.66",
        "name": "search"
    }, {
        "time": "Mar 2006",
        "value": "1294.87",
        "name": "search"
    }, {
        "time": "Apr 2006",
        "value": "1310.61",
        "name": "search"
    }, {
        "time": "May 2006",
        "value": "1270.09",
        "name": "search"
    }, {
        "time": "Jun 2006",
        "value": "1270.2",
        "name": "search"
    }, {
        "time": "Jul 2006",
        "value": "1276.66",
        "name": "search"
    }, {
        "time": "Aug 2006",
        "value": "1303.82",
        "name": "search"
    }, {
        "time": "Sep 2006",
        "value": "1335.85",
        "name": "search"
    }, {
        "time": "Oct 2006",
        "value": "1377.94",
        "name": "search"
    }, {
        "time": "Nov 2006",
        "value": "1400.63",
        "name": "search"
    }, {
        "time": "Dec 2006",
        "value": "1418.3",
        "name": "search"
    }, {
        "time": "Jan 2007",
        "value": "1438.24",
        "name": "search"
    }, {
        "time": "Feb 2007",
        "value": "1406.82",
        "name": "search"
    }, {
        "time": "Mar 2007",
        "value": "1420.86",
        "name": "search"
    }, {
        "time": "Apr 2007",
        "value": "1482.37",
        "name": "search"
    }, {
        "time": "May 2007",
        "value": "1530.62",
        "name": "search"
    }, {
        "time": "Jun 2007",
        "value": "1503.35",
        "name": "search"
    }, {
        "time": "Jul 2007",
        "value": "1455.27",
        "name": "search"
    }, {
        "time": "Aug 2007",
        "value": "1473.99",
        "name": "search"
    }, {
        "time": "Sep 2007",
        "value": "1526.75",
        "name": "search"
    }, {
        "time": "Oct 2007",
        "value": "1549.38",
        "name": "search"
    }, {
        "time": "Nov 2007",
        "value": "1481.14",
        "name": "search"
    }, {
        "time": "Dec 2007",
        "value": "1468.36",
        "name": "search"
    }, {
        "time": "Jan 2008",
        "value": "1378.55",
        "name": "search"
    }, {
        "time": "Feb 2008",
        "value": "1330.63",
        "name": "search"
    }, {
        "time": "Mar 2008",
        "value": "1322.7",
        "name": "search"
    }, {
        "time": "Apr 2008",
        "value": "1385.59",
        "name": "search"
    }, {
        "time": "May 2008",
        "value": "1400.38",
        "name": "search"
    }, {
        "time": "Jun 2008",
        "value": "1280",
        "name": "navigate"
    }, {
        "time": "Jul 2008",
        "value": "1267.38",
        "name": "navigate"
    }, {
        "time": "Aug 2008",
        "value": "1282.83",
        "name": "navigate"
    }, {
        "time": "Sep 2008",
        "value": "1166.36",
        "name": "navigate"
    }, {
        "time": "Oct 2008",
        "value": "968.75",
        "name": "navigate"
    }, {
        "time": "Nov 2008",
        "value": "896.24",
        "name": "navigate"
    }, {
        "time": "Dec 2008",
        "value": "903.25",
        "name": "navigate"
    }, {
        "time": "Jan 2009",
        "value": "825.88",
        "name": "navigate"
    }, {
        "time": "Feb 2009",
        "value": "735.09",
        "name": "navigate"
    }, {
        "time": "Mar 2009",
        "value": "797.87",
        "name": "navigate"
    }, {
        "time": "Apr 2009",
        "value": "872.81",
        "name": "navigate"
    }, {
        "time": "May 2009",
        "value": "919.14",
        "name": "navigate"
    }, {
        "time": "Jun 2009",
        "value": "919.32",
        "name": "navigate"
    }, {
        "time": "Jul 2009",
        "value": "987.48",
        "name": "navigate"
    }, {
        "time": "Aug 2009",
        "value": "1020.62",
        "name": "navigate"
    }, {
        "time": "Sep 2009",
        "value": "1057.08",
        "name": "navigate"
    }, {
        "time": "Oct 2009",
        "value": "1036.19",
        "name": "navigate"
    }, {
        "time": "Nov 2009",
        "value": "1095.63",
        "name": "search"
    }, {
        "time": "Dec 2009",
        "value": "1115.1",
        "name": "search"
    }, {
        "time": "Jan 2010",
        "value": "1073.87",
        "name": "search"
    }, {
        "time": "Feb 2010",
        "value": "1104.49",
        "name": "search"
    }, {
        "time": "Mar 2010",
        "value": "1140.45",
        "name": "search"
    }];
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
            .controller('TestCopyCtrl', ['$scope', '$location', 'TestService',
                function($scope, $location, TestService) {
                    $scope.testname = $location.path().split('/')[2];
                    TestService.getTest({
                        id: $scope.testname
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
                            $scope.errorMsg = "New unique test name is required";
                        }
                    }

                    //callback for ng-click 'renameTest'
                    $scope.executeCopyTest = function(test) {
                        var postData = {
                            "copyOf": $scope.data.name,
                            "version": $scope.data.version,
                            "name": test.name
                        };

                        TestService.copyTest({}, postData, function(res) {
                                $scope.response = res;
                                $location.path("/tests/" + res.name);
                            },
                            function error(error) {
                                $scope.hasError = true;
                                if (error.status == 500) {
                                    $scope.errorMsg = "The name already exists, please choose another unique name.";
                                } else {
                                    $scope.errorMsg = error.data.error;
                                }
                                $scope.errorMsg = error.data.error;
                            });
                    };
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
