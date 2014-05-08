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
                            var runs = response;
                            for (var i = 0; i < runs.length; i++) {
                                var run = runs[i];
                                if (run.state == 'NOT_SCHEDULED') {
                                    run.hasStarted = false;
                                } else {
                                    run.hasStarted = true;
                                }
                                if ("COMPLETED" == run.state || "STOPED" == run.state) {
                                    run.run = true;
                                } else {
                                    run.run = false;
                                }
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
                        })
                    }
                    //call back to stop run
                    $scope.stopRun = function(runname, testname, index) {
                        TestRunService.stopTestRun({
                            id: $scope.data.testname,
                            runname: runname
                        }, json, function(response) {
                            $location.path("/tests/" + testname);
                        })
                    }
                }

            ])

            /**
             * Controller to create test run
             */
            .controller('TestRunCreateCtrl', ['$scope', '$location', 'TestRunService',
                function($scope, $location, TestRunService) {
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
                                    $location.path("/tests/" + $scope.testname);
                                    TestRunService.getTestRuns({
                                        id: $scope.testname
                                    }, function(response) {
                                        $scope.data.runs = response;
                                    });
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

            /**
             * Test run status controller
             */
            .controller('TestRunStateCtrl', ['$scope', '$location', 'TestRunService',
                function($scope, $location, TestRunService) {
                    $scope.data = {};
                    var path = $location.path();
                    var names = path.replace("/tests/", "").split("/");
                    var testname = names[0];
                    var runname = names[1];
                    $scope.data.testname = testname;
                    $scope.data.runname = runname;
                    TestRunService.getRunState({
                        id: testname,
                        runname: runname,
                        more: "state"
                    }, function(response) {
                        console.log(response);
                        $scope.data.scheduled = response.scheduled;
                        $scope.data.started = response.started;
                        $scope.data.duration = response.duration;
                        $scope.data.resultsTotal = response.resultsTotal;
                        $scope.data.resultsFail = response.resultsFail;
                        $scope.data.successRate = response.successRate;
                        $scope.data.progress = response.progress;
                    });
                }
            ])
            /*
             * Run summary controller
             */
            .controller('TestRunSummaryCtrl', ['$scope', '$location', 'TestRunService',
                function($scope, $location, TestRunService) {
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
                            $scope.summary.progress = da.progress * 100;
                            $scope.summary.result = [da.resultsSuccess, da.resultsFail];
                        });
                    }
                    //Get the summary now!
                    $scope.getSummary();
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