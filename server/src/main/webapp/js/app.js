'use strict';
angular.module('benchmark', ['ngRoute','benchmark-test', 'd3benchmark', 'modal','benchmark-breadcrumbs'])
.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
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
}])

.filter('checkmark', function() {
    return function(input) {
        return input ? '\u2713' : '\u2718';
    };
})

// Validation service - inject into your controller and call "ValidationService.validate(property)" 
.service('ValidationService', function(){
    var validators = {
        
        // main entry point for validation
        validate:function(property){
            // reset the validation values first
            property.validationFail = false;
            property.validationMessage = "";
            
            // check if "validation" property is set, if not, use the type validation
            if (typeof property.validation != 'undefined'){
                if (property.validation.toLowerCase() == "type"){
                    validators.typeValidator(property);
                }
                // TODO V2 extension: append other validation like "host, URL, ..."
            }
            else{
                // no "special" validation selected - so fall-back to type validation
                validators.typeValidator(property);
            } 
        },
  
        // validation by "type" (int | decimal | boolean | string) of the property
        typeValidator:function(property){
            if (typeof property.type == 'undefined'){
                property.validationFail = true;
                property.validationMessage = "Internal configuration error: the property '" + property.title + "' has no type!";
            }
            else{
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
        stringValidator:function(property){
            /*
            // TODO - just some test code
            if (property.value == "Test"){
                property.validationFail = true;
                property.validationMessage = "Don't use the value 'Test'!";
            }
            */
        },
        intValidator:function(property){
            // TODO
        },
        decimalValidator:function(property){
            // TODO
        },
        booleanValidator:function(property){
            // TODO
        }
    };
    return validators;
})
/**
 * General utility service that provides:
 * Group elements in an array.
 * @return {[type]} [description]
 */
.service('UtilService',function(){
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
})
.directive('ngFocus', [function() {
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