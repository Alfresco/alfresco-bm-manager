'use strict';
angular.module('benchmark', ['ngRoute','benchtest', 'd3benchmark', 'modal','breadcrumbs'])
.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.
     when('/home', {templateUrl: 'testdef/list-tests.html',   controller:'ListTestsCtrl'}).
     when('/tests', {templateUrl: 'testdef/list-tests.html',   controller:'ListTestsCtrl'}).
     when('/tests/create', {templateUrl: 'testdef/test-create.html', controller: 'TestCreateCtrl'}).
     when('/tests/:testId/properties', {templateUrl: 'testdef/test-detail.html', controller: 'TestDetailCtrl'}).
     when('/tests/:testId', {templateUrl: 'testdef/list-runs.html', controller: 'TestRunListCtrl'}).
     when('/tests/:testId/create', {templateUrl: 'testdef/run-create.html', controller: 'TestRunCreateCtrl'}).
     when('/tests/:testId/copy', {templateUrl: 'testdef/test-copy.html', controller: 'TestCopyCtrl'}).
     when('/tests/:testId/:runId', {templateUrl: 'testdef/run-summary.html', controller: 'TestRunSummaryCtrl'}).
     when('/tests/:testId/:runId/copy', {templateUrl: 'testdef/run-copy.html', controller: 'TestRunCopyCtrl'}).
     when('/tests/:testId/:runId/properties', {templateUrl: 'testdef/run-detail.html', controller: 'TestRunDetailCtrl'}).
     when('/tests/:testId/:runId/status', {templateUrl: 'testdef/run-result.html', controller: 'TestRunStateCtrl'}).
     when('/testdefs', {templateUrl: 'testdef/list-testdefs.html', controller:'TestDefListCtrl'}).
     when('/testdefs/:testId/:schemaId', {templateUrl: 'testdef/testdef-detail.html', controller: 'TestDefDetailCtrl'}).
     otherwise({redirectTo: '/home'});
}])
.filter('checkmark', function() {
    return function(input) {
        return input ? '\u2713' : '\u2718';
    };
})
/**
 * General utility service the provides:
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
        array.forEach(function(o) {
            var group = JSON.stringify(f(o));
            groups[group] = groups[group] || [];
            groups[group].push(o);});
        return Object.keys(groups).map(function(group) {
            return groups[group];
             })
        }
    }
});
