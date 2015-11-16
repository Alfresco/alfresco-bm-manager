'use strict';
var breadcrumb = angular.module('benchmark-breadcrumbs', []);

breadcrumb.factory('BreadcrumbsFactory', ['$rootScope', '$location',
        function($rootScope, $location) {
            var breadcrumbs = [];
            var factory = {};
            //we want to update breadcrumbs only when a route is actually changed
            //as $location.path() will get updated imediatelly (even if route change fails!)
            $rootScope.$on('$routeChangeSuccess', function(event, current) {
                var pathElements = $location.path().split('/'),
                    result = [],
                    i;
                var breadcrumbPath = function(index) {
                    return '/' + (pathElements.slice(0, index + 1)).join('/');
                };
                pathElements.shift();
                for (i = 0; i < pathElements.length; i++) {
                    var isActive = ((i == pathElements.length - 1) ? "active" : "standard");
                    result.push({
                        name: pathElements[i],
                        path: breadcrumbPath(i),
                        active: isActive
                    });
                }
                breadcrumbs = result;
            });
            factory.getAll = function() {
                return breadcrumbs;
            };
            factory.getFirst = function() {
                return breadcrumbs[0] || {};
            };
            factory.getLast = function() {
                return breadcrumbs[breadcrumbs.length-1] || {};
            };
            return factory;
        }
    ]);

breadcrumb.controller('BreadcrumbCtrl', function($scope, BreadcrumbsFactory) {
    $scope.breadcrumbs = BreadcrumbsFactory;
});
