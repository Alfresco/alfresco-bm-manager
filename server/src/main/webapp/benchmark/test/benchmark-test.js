'use strict';
    /**
     * The benchmark-test.js is a AngularJS lib that contains
     * the controllers, services, filters of benchmark test module.
     */

    /**
     * Service layer to interact with Test API.
     */
    angular.module('benchmark-test', ['ngResource','benchmark-run'])

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
     * Test Controllers
     */
     //Controller to list tests, delete test and edit test.
    .controller('ListTestsCtrl', ['$scope', '$location', '$timeout','TestService', 'ModalService',
        function($scope, $location, $timeout, TestService, ModalService) {
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
    .controller('TestPropertyCtrl', ['$scope', '$location', 'TestService', 'TestPropertyService', 'UtilService','ModalService',
        function($scope, $location, TestService, TestPropertyService, UtilService, ModalService) {
            $scope.data = {};
            $scope.properties = [];
            $scope.master = {};
            $scope.drivers = {"collapsed":true};
            $scope.drivers.prop = [];
            $scope.attentionRequired=false;
            var myname = $location.path().split('/');
            var testname = myname[2];

            TestService.getDrivers({
                id: testname
            }, function(response) {
                //Strip $ as it prevents value from appearing
                var jsonStr = JSON.stringify(response);
                jsonStr = jsonStr.replace(/\$/g, '').replace('_id','ID');
                $scope.drivers.prop = JSON.parse(jsonStr); 
            });

            TestService.getTest({
                id: testname
            }, function(response) {
                $scope.data.test = response;
                //check default value if missing create one from default.
                $scope.data.test.properties.forEach(function(item){
                    if(item.value == undefined){
                        item.value = item.default;
                    }
                });
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

            $scope.cancelRename = function(){
                $scope.data.test.name = $scope.master.name;
            }

            $scope.cancelDesc = function(){
                $scope.data.test.description = $scope.master.description;
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
                TestService.updateTest({}, postData, function(res) {});
                $location.path("/tests/" + postData.name + "/properties");
            }

            //-------------- Test properties crud ----------
            // call back for update test property field
            $scope.updateProperty = function(item) {
                var propData = {
                    "version": item.version,
                    "value": item.value
                };
                $scope.updateTestProperty(testname, item, propData);
                var scope = $scope.data.properties;
                $compile(content.contents(scope));
            }

            $scope.cancelEdit = function(item) {
                // item.newvalue = item.value;
            }

            $scope.resetProperty = function(item) {
                item.version;
                item.newvalue = "";
                var restData = {
                    "version": item.version,
                    "value": item.default
                };
                $scope.updateTestProperty(testname, item, restData);
                item.value = item.default;
            }

            $scope.updateTestProperty = function(testname, item, propData) {
                TestPropertyService.update({
                        "id": testname,
                        "propertyname": item.name
                    }, propData,
                    function(res) {
                        var redirect = "/tests/" + testname + "/properties";
                        //update modal with latest from backend
                        TestService.getTest({
                            id: testname
                        }, function(response) {
                            $scope.data.test = response;
                            var result = UtilService.groupBy($scope.data.test.properties, function(data) {
                                return [data.group];
                            });
                            var expand = item.group;
                            // $scope.data.properties = result;
                            $scope.master = angular.copy($scope.data.test);
                        });
                    });
            }

            $scope.toggleCollapsedStates = function(ind){
                if(ind.collapsed)
                {
                    ind.collapsed = false;
                    // $("#property-" + ind.uid).collapse('show');
                } else {
                    ind.collapsed = true;
                    // $("#property-" + ind.uid).collapse('hide');
                };
            }

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
                $location.path("/tests/");
            }

            $scope.attentionReq = function(item) {
                var isAttentionReq = item.value.indexOf('--') > -1;
                if (isAttentionReq)
                {
                    $scope.attentionRequired = true;
                    $scope.attentionMessage = "* {" + item.group + " / " +item.name + "}: A value must be set.";
                }
                return isAttentionReq;
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
            $scope.showActiveTests = true;
            
            $scope.showActiveTestDefs = function(value) {
                if (value == true) {
                    TestDefService.getTestDefs(function(response) {
                    $scope.defs = response;
                    if ($scope.defs.length == 0) {
                        $scope.nodefs = true;
                    } else {
                        $scope.nodefs = false;
                    }
                   }); 
                } else {
                    TestDefService.getAllTestDefs(function(response) {
                    $scope.defs = response;
                    if ($scope.defs.length == 0) {
                        $scope.nodefs = true;
                    } else {
                        $scope.nodefs = false;
                    }
                   }); 
                };
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
            $scope.showActiveTestDefs(true);
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
    ]);
