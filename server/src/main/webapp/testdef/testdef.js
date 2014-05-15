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
                        "date": 20111002,
                        "search": 58,
                        "Login": 59.9,
                        "Logout": 67.7
                    }, {
                        "date": 20111003,
                        "search": 53.3,
                        "Login": 59.1,
                        "Logout": 69.4
                    }, {
                        "date": 20111004,
                        "search": 55.7,
                        "Login": 58.8,
                        "Logout": 68
                    }, {
                        "date": 20111005,
                        "search": 64.2,
                        "Login": 58.7,
                        "Logout": 72.4
                    }, {
                        "date": 20111006,
                        "search": 58.8,
                        "Login": 57,
                        "Logout": 77
                    }, {
                        "date": 20111007,
                        "search": 57.9,
                        "Login": 56.7,
                        "Logout": 82.3
                    }, {
                        "date": 20111008,
                        "search": 61.8,
                        "Login": 56.8,
                        "Logout": 78.9
                    }, {
                        "date": 20111009,
                        "search": 69.3,
                        "Login": 56.7,
                        "Logout": 68.8
                    }, {
                        "date": 20111010,
                        "search": 71.2,
                        "Login": 60.1,
                        "Logout": 68.7
                    }, {
                        "date": 20111011,
                        "search": 68.7,
                        "Login": 61.1,
                        "Logout": 70.3
                    }, {
                        "date": 20111012,
                        "search": 61.8,
                        "Login": 61.5,
                        "Logout": 75.3
                    }, {
                        "date": 20111013,
                        "search": 63,
                        "Login": 64.3,
                        "Logout": 76.6
                    }, {
                        "date": 20111014,
                        "search": 66.9,
                        "Login": 67.1,
                        "Logout": 66.6
                    }, {
                        "date": 20111015,
                        "search": 61.7,
                        "Login": 64.6,
                        "Logout": 68
                    }, {
                        "date": 20111016,
                        "search": 61.8,
                        "Login": 61.6,
                        "Logout": 70.6
                    }, {
                        "date": 20111017,
                        "search": 62.8,
                        "Login": 61.1,
                        "Logout": 71.1
                    }, {
                        "date": 20111018,
                        "search": 60.8,
                        "Login": 59.2,
                        "Logout": 70
                    }, {
                        "date": 20111019,
                        "search": 62.1,
                        "Login": 58.9,
                        "Logout": 61.6
                    }, {
                        "date": 20111020,
                        "search": 65.1,
                        "Login": 57.2,
                        "Logout": 57.4
                    }, {
                        "date": 20111021,
                        "search": 55.6,
                        "Login": 56.4,
                        "Logout": 64.3
                    }, {
                        "date": 20111022,
                        "search": 54.4,
                        "Login": 60.7,
                        "Logout": 72.4
                    }, {
                        "date": 20111023,
                        "search": 54.4,
                        "Login": 65.1,
                        "Logout": 72.4
                    }, {
                        "date": 20111024,
                        "search": 54.8,
                        "Login": 60.9,
                        "Logout": 72.5
                    }, {
                        "date": 20111025,
                        "search": 57.9,
                        "Login": 56.1,
                        "Logout": 72.7
                    }, {
                        "date": 20111026,
                        "search": 54.6,
                        "Login": 54.6,
                        "Logout": 73.4
                    }, {
                        "date": 20111027,
                        "search": 54.4,
                        "Login": 56.1,
                        "Logout": 70.7
                    }, {
                        "date": 20111028,
                        "search": 42.5,
                        "Login": 58.1,
                        "Logout": 56.8
                    }, {
                        "date": 20111029,
                        "search": 40.9,
                        "Login": 57.5,
                        "Logout": 51
                    }, {
                        "date": 20111030,
                        "search": 38.6,
                        "Login": 57.7,
                        "Logout": 54.9
                    }, {
                        "date": 20111031,
                        "search": 44.2,
                        "Login": 55.1,
                        "Logout": 58.8
                    }, {
                        "date": 20111101,
                        "search": 49.6,
                        "Login": 57.9,
                        "Logout": 62.6
                    }, {
                        "date": 20111102,
                        "search": 47.2,
                        "Login": 64.6,
                        "Logout": 71
                    }, {
                        "date": 20111103,
                        "search": 50.1,
                        "Login": 56.2,
                        "Logout": 58.4
                    }, {
                        "date": 20111104,
                        "search": 40.1,
                        "Login": 50.5,
                        "Logout": 45.1
                    }]
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
