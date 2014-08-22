'use strict';
angular.module('benchmark', ['ngRoute','benchmark-test', 'd3benchmark', 'modal','benchmark-breadcrumbs'])
.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.
     when('/home', {templateUrl: 'benchmark/test/index.html',   controller:'ListTestsCtrl'}).
     when('/tests', {templateUrl: 'benchmark/test/index.html',   controller:'ListTestsCtrl'}).
     when('/tests/create', {templateUrl: 'benchmark/test/create.html', controller: 'TestCreateCtrl'}).
     when('/tests/:testId/properties', {templateUrl: 'benchmark/test/property.html', controller: 'TestPropertyCtrl'}).
     when('/tests/:testId', {templateUrl: 'benchmark/run/index.html', controller: 'TestRunListCtrl'}).
     when('/tests/:testId/create', {templateUrl: 'benchmark/run/create.html', controller: 'TestRunCreateCtrl'}).
     when('/tests/:testId/copy', {templateUrl: 'benchmark/run/copy.html', controller: 'TestCopyCtrl'}).
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
});
