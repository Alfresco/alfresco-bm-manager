'use strict';
    /**
     * The benchmark-test.js is a AngularJS library that contains
     * the controllers, services, filters of benchmark test module.
     */

    /**
     * Service layer to interact with Test API.
     */
    var bmTest = angular.module('benchmark-test', ['ngResource','benchmark-run']);

    /**
     * Test Def Service.
     */
    bmTest.factory('TestDefDetailService', function($resource) {
        return $resource('api/v1/test-defs/:name/:schema', {
            name: '@name',
            schema: '@schema'
        }, {
            //Returns list of active test definitions
            getTestDef: {
                method: 'GET',
                params: {
                    name: 'name',
                    schema: 'schema'
                }
            }
        })
    }).value('version', '0.1');

    bmTest.factory('TestDefService', function($resource) {
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
    }).value('version', '0.1');

    /**
     * Tests service.
     */
    bmTest.factory('TestService', function($resource) {
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
     * Test properties service.
     */
    bmTest.factory('TestPropertyService', function($resource) {
        return $resource('api/v1/tests/:id/props/:propertyname', {
            id: '@id',
            propertyname: '@propertyname'
        }, {
            'update': {
                method: 'PUT',
                params: {
                    id: 'id',
                    propertyname: 'propertyname'
                }
            },
            'delete': {
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
    }).value('version', '0.1');

    

    /**
     * Test Controllers
     */
     //Controller to list tests, delete test and edit test.
    bmTest.controller('ListTestsCtrl', ['$scope', '$location', '$timeout','TestService', 'ModalService',
        function($scope, $location, $timeout, TestService, ModalService) {
            // Instantiate an object to store your scope data in (Best Practices)
            $scope.data = {};
            
            // loads the number of drivers for the test
            $scope.getNumDrivers = function(test){
            	try{
	            	TestService.getDrivers({
	                    id: test.name
	                }, function(response) {
	                    test.driver = response.length + " Driver"; 
	                });
            	}
            	catch(err){
            		test.driver = "No Driver";
            	}
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
            
            // callback from modal '$scope.deleteTest': delete test OK
            $scope.doDeleteTest = function(name) {
                TestService.deleteTest({
                    id: name
                }, function(response){
                    $scope.data.tests = TestService.getTests();
                });
                $scope.data.tests = TestService.getTests();
            }
            
            $scope.data.tests = TestService.getTests();
        }
    ]);


    /**
     * Controller to display test detail
     */
    bmTest.controller('TestPropertyCtrl', ['$scope', '$location', 'TestService', 'TestPropertyService', 'UtilService','ModalService', 'ValidationService',
        function($scope, $location, TestService, TestPropertyService, UtilService, ModalService, ValidationService) {
            $scope.data = {};
            $scope.properties = [];
            $scope.master = {};
            $scope.drivers = {"collapsed":true};
            $scope.drivers.prop = [];
            $scope.attentionRequired=false;
            var myname = $location.path().split('/');
            var testname = myname[2];
            $scope.nameErrorMessage = null;

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
                        item.value = item['default'];
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
                // V2.1: only call back-end if name was altered ...
                if (name != testname){
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
                // increase version number for there will be no reload of page on update
                // of the version number ... else next save will fail ...
                $scope.data.test.version = $scope.data.test.version + 1;
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
                var result = TestService.updateTest({}, postData, function(res) {});
                // V2.1 only update location if name was altered
                if (result.name != result.oldName){
                    $location.path("/tests/" + postData.name + "/properties");
                }
                return result;
            }

            //-------------- Test properties crud ----------
            // call back for update test property field
            $scope.updateProperty = function(item) {
                var propData = {
                    "version": item.version,
                    "value": item.value
                };
                $scope.updateTestProperty(testname, item, propData);
                item.version = item.version + 1; 
                // TODO - check if update was successful and update UI if not
            }

            $scope.cancelEdit = function(item) {
                // restore
                item.value = item['cancelValue'];
            }

            $scope.resetProperty = function(item) {
                var restData = {
                    "version": item.version
                };
                $scope.updateTestProperty(testname, item, restData);
                item.value = item['default'];
                item.version = 0;
            }
            
            // checks whether value is empty or not
            $scope.isEmpty = function(value){
                if (typeof value == 'undefined'){
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
            
            // called for each key press  in the property editor:
            // returns true if to continue edit
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
            
            // called for each key press  in the BM name editor:
            // returns true if to continue edit
            // returns false if edit is done. 
            $scope.doKeyPressName = function(event){
                if (event.keyCode == 13){
                    // ENTER - allowed only if no validation error message is present
                    if (null == $scope.nameErrorMessage){
                        $scope.renameTest($scope.data.test.name);
                        return false;
                    }
                }
                else if (event.keyCode == 27){
                    // ESC
                    $scope.cancelRename();
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
                    $scope.updateTestDesc($scope.data.test.description);
                    return false;
                }
                else if (event.keyCode == 27){
                    // ESC
                    $scope.cancelDesc();
                    return false;
                }
                return true;
            }
            
            // Validates the name entered by the user
            $scope.validateName = function(){
                $scope.nameErrorMessage = ValidationService.isValidTestName($scope.data.test.name);
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
            
            $scope.updateTestProperty = function(testname, item, propData) {
                TestPropertyService.update({
                        "id": testname,
                        "propertyname": item.name
                    }, propData,
                    function(res) {
                        var redirect = "/tests/" + testname + "/properties";
                        //update modal with latest from back-end
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
    ]);

    /**
     * Controller to create test detail
     */
    bmTest.controller('TestCreateCtrl', ['$scope', '$location', 'TestService', 'TestDefService', 'ValidationService',
        function($scope, $location, TestService, TestDefService, ValidationService) {
            $scope.master = {};
            $scope.defs = {};
            $scope.nodefs = false;
            $scope.showActiveTests = true;
            $scope.errorMsg = null;
            
            $scope.showActiveTestDefs = function(value) {
                if (value == true) {
                    TestDefService.getTestDefs(function(response) {
                    var uniqueDefs = [];    //this will contain the unique list of final custom test defs
                    var isReleaseFound = function (releaseName) {
                          for(var i = 0; i < uniqueDefs.length; i++){
                            if(uniqueDefs[i]["release"]==releaseName){
                                uniqueDefs[i]["count"] +=1;
                                return true;
                            }
                          }
                          return false;
                    };
                    
                    for(var i = 0; i < response.length; i++){
                        if (!isReleaseFound(response[i]["release"]))
                        {
                            response[i]["count"]= 1;
                            uniqueDefs.push(response[i]);         
                        }
                    }
                    
                    $scope.defs = uniqueDefs;
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

            // validates the test name
            $scope.validateName = function(testName){
                $scope.errorMsg = ValidationService.isValidTestName(testName);
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
                    }, function error(errorVal) {
                        $scope.hasError = true;
                        if (errorVal.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = errorVal.data.error;
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
    ]);

    /**
     * Controller to list test definitions.
     */
    bmTest.controller('TestDefListCtrl', ['$scope', 'TestDefService',
        function($scope, TestDefService) {
            // Instantiate an object to store your scope data in (Best Practices)
            $scope.data = {};
            TestDefService.getAllTestDefs(function(response) {
                $scope.data.tests = response;
            });
        }
    ]);
    
    /**
     * Controller to display test definition detail.
     */
    bmTest.controller('TestDefDetailCtrl', ['$scope', '$location', 'TestDefDetailService',
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
    ]);
    
    /*
     * Copy test form controller
     */
    bmTest.controller('TestCopyCtrl', ['$scope', '$location', 'TestService', 'TestDefService', 'ValidationService',
        function($scope, $location, TestService, TestDefService, ValidationService) {
            $scope.testname = $location.path().split('/')[2];
            TestService.getTest({
                id: $scope.testname
            }, function(response) {
                $scope.data = response;
            });
            $scope.master = {};
            $scope.defs = {};
            $scope.nodefs = false;
            $scope.showActiveTests = true;
            $scope.errorMsg = null;
            
            $scope.showActiveTestDefs = function(value) {
                if (value == true) {
                    TestDefService.getTestDefs(function(response) {
                    var uniqueDefs = [];    //this will contain the unique list of final custom test defs
                    var isReleaseFound = function (releaseName) {
                          for(var i = 0; i < uniqueDefs.length; i++){
                            if(uniqueDefs[i]["release"]==releaseName){
                                uniqueDefs[i]["count"] +=1;
                                return true;
                            }
                          }
                          return false;
                    };
                    
                    for(var i = 0; i < response.length; i++){
                        if (!isReleaseFound(response[i]["release"]))
                        {
                            response[i]["count"]= 1;
                            uniqueDefs.push(response[i]);         
                        }
                    }
                    
                    $scope.defs = uniqueDefs;
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
            $scope.showActiveTestDefs(true);

            // validation of user entries
            $scope.validateName = function(runName){
                $scope.errorMsg = ValidationService.isValidTestName(runName);
            }
            
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
                if (test.def !== undefined) {
                    var def = test.def.split("-schema:");
                    var release = def[0];
                    var schema = def[1];
                    var postData = {
                        "copyOf": $scope.data.name,
                        "version": $scope.data.version,
                        "release": release,
                        "schema": schema,
                        "name": test.name
                    };
                    TestService.saveTest({}, postData, function(res) {
                        if (res.name === postData.name) {
                            $location.path("/tests/" + res.name + "/properties");
                        }
                    }, function error(errorVal) {
                        $scope.hasError = true;
                        if (errorVal.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = errorVal.data.error;
                        }
                    });
                } else {
                    $scope.hasError = true;
                    $scope.errorMsg = "Valid test version and schema is required.";
                }

                TestService.copyTest({}, postData, function(res) {
                        $scope.response = res;
                        $location.path("/tests/" + res.name);
                    },
                    function error(errorVal) {
                        $scope.hasError = true;
                        if (errorVal.status == 500) {
                            $scope.errorMsg = "The name already exists, please choose another unique name.";
                        } else {
                            $scope.errorMsg = errorVal.data.error;
                        }
                        $scope.errorMsg = errorVal.data.error;
                    });
            };
        }
    ]);
