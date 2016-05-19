'use strict';
var app = angular.module('benchmark', ['ngRoute','benchmark-test', 'd3benchmark', 'modal','benchmark-breadcrumbs']);

app.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.
     when('/home', {templateUrl: 'benchmark/dashboard.html',   controller:'ListTestsCtrl'}).
     when('/tests', {templateUrl: 'benchmark/dashboard.html',   controller:'ListTestsCtrl'}).
     when('/tests/create', {templateUrl: 'benchmark/test/create.html', controller: 'TestCreateCtrl'}).
     when('/tests/:testId/properties', {templateUrl: 'benchmark/test/property.html', controller: 'TestPropertyCtrl'}).
     when('/tests/:testId', {templateUrl: 'benchmark/run/index.html', controller: 'TestRunListCtrl'}).
     when('/tests/:testId/create', {templateUrl: 'benchmark/run/create.html', controller: 'TestRunCreateCtrl'}).
     when('/tests/:testId/copy', {templateUrl: 'benchmark/test/copy.html', controller: 'TestCopyCtrl'}).
     when('/tests/:testId/:runId', {templateUrl: 'benchmark/run/summary.html', controller: 'TestRunSummaryCtrl'}).
     when('/tests/:testId/:runId/copy', {templateUrl: 'benchmark/run/copy.html', controller: 'TestRunCopyCtrl'}).
     when('/tests/:testId/:runId/properties', {templateUrl: 'benchmark/run/property.html', controller: 'TestRunPropertyCtrl'}).
     when('/tests/:testId/:runId/status', {templateUrl: 'benchmark/run/status.html', controller: 'TestRunStateCtrl'}).
     when('/testdefs', {templateUrl: 'benchmark/list-testdefs.html', controller:'TestDefListCtrl'}).
     when('/testdefs/:testId/:schemaId', {templateUrl: 'benchmark/testdef-detail.html', controller: 'TestDefDetailCtrl'}).
     otherwise({redirectTo: '/home'});
}]);

app.filter('checkmark', function() {
    return function(input) {
        return input ? '\u2713' : '\u2718';
    };
});

// Validation service - inject into your controller and call "ValidationService.validate(property)" 
app.service('ValidationService', function(){
    var validators = {
        
        // main entry point for validation
        validate:function(property){
            // reset the validation values first
            property.validationFail = false;
            property.validationMessage = "";
            
            // check 'choice'
            var choiceValidated = false;
            if (typeof property.choice != 'undefined'){
                // convert JSON string to object/array and validate 
                validators.choiceCollectionValidator(property, JSON.parse(property.choice));
                choiceValidated = true;
            }
            
            // check if "validation" property is set, if not, use the type validation
            if (typeof property.validation != 'undefined'){
                switch (property.validation.toLowerCase()){
                    case "type":
                        validators.typeValidator(property);
                        break;
                    
                    default:
                        property.validationFail = true;
                        property.validationMessage = "Internal error: unknown validation type '" + property.validation + "'";
                        break;
                }
            }
            else{
                // if choice collection was validated OK and no special validation was assigned, we're done
                if (!choiceValidated){
                    // no "special" validation selected - so fall-back to type validation
                    validators.typeValidator(property);
                }
            } 
        },
        
        // checks if "value" is empty string
        isEmpty:function(value){
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
        },
        
        // test name validation - only alpha-numeric values and the underscore are allowed values
        // returns null if everything is OK or a string error message if not. 
        isValidTestName:function(testName){
            // check empty
            if (validators.isEmpty(testName) ){
                return "Please enter a test name!";
            }
            
            // validate RegEx
            var regex = new RegExp( "[a-zA-Z][a-zA-Z0-9_]*" );
            if (testName.match(regex) != testName){
                return "Test names must start with a letter and contain only letters, numbers or underscores!";
            }
            
            return null;
        },

        // test run name validation - only alpha-numeric values and the underscore are allowed values
        // returns null if everything is OK or a string error message if not. 
        isValidTestRunName:function(testRunName){
            // check empty
            if (validators.isEmpty(testRunName) ){
                return "Please enter a test run name!";
            }
            
            // validate RegEx
            var regex = new RegExp( "[a-zA-Z0-9][a-zA-Z0-9_]*" );
            if (testRunName.match(regex) != testRunName){
                return "Test run names must contain only letters, numbers or underscores and start with a number or letter!";
            }
            
            return null;
        },
        
        // validation by "type" (int | decimal | boolean | string) of the property
        typeValidator:function(property){
            if (typeof property.type == 'undefined'){
                property.validationFail = true;
                property.validationMessage = "Internal configuration error: the property '" + property.title + "' has no type!";
            }
            else{
            	if (typeof property.value == 'undefined'){
                    property.validationMessage = "Undefined value.";
            		return true;
            	}
                switch (property.type.toLowerCase()){
                    case "string":
                        validators.stringValidator(property);
                        break;
                        
                    case "int":
                        validators.intValidator(property);
                        break;
                        
                    case "decimal":
                        validators.decimalValidator(property);
                        break;
                        
                    case "boolean":
                        validators.booleanValidator(property);
                        break;
                        
                    default:
                        property.validationFail = true;
                        property.validationMessage = "Internal configuration error: the property '" + property.title + "' has an unknown 'type': '" + property.type + "'";
                        break;
                }
            }
        },
        
        // string validation
        // min: minimum length (optional)
        // max: maximum length (optional)
        // regex: regular expression (optional)
        stringValidator:function(property){
        	// accept other property
        	try{
        		if (validators.isAnotherProperty(property.value)){
        			return;
        		}
        	}catch(err){alert("String Validation: " + err);};
        	// check 'min'
            var min = 0;
            if (typeof property.min != 'undefined'){
                // if a minimum value is defined the string must NOT be empty
                min = property.min;
            }
            if (typeof property.value == 'string'){
                // check 'min'
                if (property.value.length < min){
                    property.validationFail = true;
                    property.validationMessage = "Please enter at least " + min + " char(s)";
                    return;
                }
                // check 'max'
                if (typeof property.max != 'undefined'){
                    if (property.value.length > property.max){
                        property.validationFail = true;
                        property.validationMessage = "The value entered is too long. Enter max " + property.max + " char(s)";
                        return;
                    }
                }
                // check 'regex'
                if (typeof property.regex != 'undefined'){
                    var value = property.value;
                    var regex = new RegExp( property.regex );
                    if (value.match(regex) != value){
                        property.validationFail = true;
                        property.validationMessage = "The value entered doesn't match the regular expression '" + property.regex + "'";
                    }
                }
            }
            else{
                if (min > 0){
                    property.validationFail = true;
                    property.validationMessage = "Please enter a string value";
                }
            }
        },
        
        // integer validation
        intValidator:function(property){
        	// accept other property
        	try{
        		if (validators.isAnotherProperty(property.value)){
        			return;
        		}
        	}catch(err){alert(err);};
            if (typeof property.value == 'number' || (typeof property.value == 'string' && property.value.length > 0)) {
                // don't accept numbers that are not 'int' ...
                if ( property.value%1 == 0){
                 // 'min'
                    if (typeof property.min != 'undefined'){
                        if (parseInt(property.value, 10) < parseInt(property.min, 10)){
                            property.validationFail = true;
                            property.validationMessage = "The value entered is too small, min. value is " + property.min;
                            return;
                        }
                    }
                    // 'max'
                    if (typeof property.max != 'undefined'){
                        if (parseInt(property.value, 10) > parseInt(property.max, 10)){
                            property.validationFail = true;
                            property.validationMessage = "The value entered is too large, max. value is " + property.max;
                            return;
                        }
                    }
                    return;
                }
            }

            // neither a number, nor a string with at least one char
            property.validationFail = true;
            property.validationMessage = (typeof property.value); //"Please enter an integer value";
        },
        
        // decimal validation
        decimalValidator:function(property){
        	// accept other property
        	try{
        		if (validators.isAnotherProperty(property.value)){
        			return;
        		}
        	}catch(err){alert(err);};
            // accept any number
        	if (Number.isNaN(parseFloat(property.value))){
                property.validationFail = true;
                property.validationMessage = "Please enter a decimal value.";
                return;        		
        	}
        	if (typeof property.value == 'number' || (typeof property.value == 'string' && property.value.length > 0)) {
        		var value = parseFloat(property.value);
        		if ("" + value !== property.value)
        		{
        			// the entered value may be a float so do a regular expression test
        			var val = property.value;
        			var regex = new RegExp( "[-+]?[0-9]*\\.?[0-9]+" );
                    if (val.match(regex) != val){
	                    property.validationFail = true;
	                    property.validationMessage = "Please enter a decimal value. ";
	                    return;
                    }
        		}
        		
	            // 'min'
	            if (typeof property.min != 'undefined'){
	                if (parseFloat(property.value) < parseFloat(property.min)){
	                    property.validationFail = true;
	                    property.validationMessage = "The value entered is too small, min. value is " + property.min;
	                    return;
	                }
	            }
	            // 'max'
	            if (typeof property.max != 'undefined'){
	                if (parseFloat(property.value) > parseFloat(property.max)){
	                    property.validationFail = true;
	                    property.validationMessage = "The value entered is too large, max. value is " + property.max;
	                    return;
	                }
	            }
        	}
        },
        
        // boolean validation
        booleanValidator:function(property){
            if (typeof property.value != 'boolean'){
                // also accept strings 'true' and 'false'
                if (typeof property.value == 'string'){
                    if (property.value.toLowerCase() == 'true' || property.value.toLowerCase() == 'false'){
                        return;
                    }
                }
                try{
                	if (!validators.isAnotherProperty(property.value)){
                		property.validationFail = true;
                		property.validationMessage = "Please enter 'true' or 'false'";
                	}
                }catch(err){alert(err);};
            }
        },
        
        // validation if property.value is part of the values array
        choiceCollectionValidator:function(property, values){
            if (typeof property.value != 'undefined' && typeof values != 'undefined'){
                for (var i = 0; i < values.length; i++) {
                    if (values[i] == property.value) {
                        return;
                    }
                }
            }
            try{
            	if (!validators.isAnotherProperty(property.value) && !validators.isEmpty(property.value)){
            		property.validationFail = true;
            		property.validationMessage = "Please use one of: " + values;
            	}
            }catch(err){alert(err);};
        },
        
        // checks if propValue is ${xyz.xyz}
        isAnotherProperty:function(propValue){
        	if (typeof propValue == 'undefined'){
        		return false;
        	}
        	var regex = new RegExp( "\\${[a-zA-Z][a-z.A-Z]*}" );
        	if (propValue.match(regex) != propValue){
        		return false;
        	}
        	return true;
        }
    };
    return validators;
});

/**
 * General utility service that provides:
 * Group elements in an array.
 * @return {[type]} [description]
 */
app.service('UtilService',function(){
    return {
        /**
         * Group results from an array.
         * To use service and method, inject the service and call it as follow
         * var result = UtilService.groupBy($scope.thearray, function(item) {
         *                 return [item.group];//the property to group by
         *              });
         * @param  {[type]} array [description]
         * @param  {[type]} f     [description]
         * @return {[type]}       [description]
         */
        groupBy:function(array, f){
        var groups = {};
        var count = 0;
        array.forEach(function(o) {
            var group = JSON.stringify(f(o));
            groups[group] = groups[group] || [];
            groups[group].push(o);});
            var result = Object.keys(groups).map(function(group) { 
                count ++;
                return {
                    "uid" : count,
                    "collapsed" : true,
                    "properties" : groups[group]}; 
                });
            return result;
        }
        
    }
});

app.directive('ngFocus', [function() {
  var FOCUS_CLASS = "ng-focused";
  return {
    restrict: 'A',
    require: 'ngModel',
    link: function(scope, element, attrs, ctrl) {
      ctrl.$focused = false;
      element.bind('focus', function(evt) {
        element.addClass(FOCUS_CLASS);
        scope.$apply(function() {ctrl.$focused = true;});
      }).bind('blur', function(evt) {
        element.removeClass(FOCUS_CLASS);
        scope.$apply(function() {ctrl.$focused = false;});
      });
    }
  }
}]);

app.controller('TestPropertiesFilterCtrl', ['$scope', TestPropertiesFilterCtrl]);
function TestPropertiesFilterCtrl($scope) { 
			angular.element(document).ready(function () {
				var properties = {
						showAll : function (value) {
							var sufix = (" ng-hide"===value) ? "" : " ng-hide";
							var propertyGroups = document.getElementsByClassName("test list-group" + sufix);
							for (var i=0, max=propertyGroups.length; i < max; i++) {
								propertyGroups[i].className="test list-group" + value;
							}
						}
				}
				var filterValue = document.getElementById('filter_value');
				document.getElementById('filter').addEventListener('input', function() {
					//this will get all property groups that are hidden
					if (!this.value) {
						filterValue.innerHTML = "";
						properties.showAll(" ng-hide");
						return;
					}
					filterValue.innerHTML = ".filterable:not([filter-value*=\"" + this.value.toLowerCase() + "\"]) { display: none; }";
					properties.showAll("");
				});
			});
};