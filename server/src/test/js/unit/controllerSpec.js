'use strict';
describe('Benchmark controller unit test', function(){
    var $scope = null;
    var ctrl = null;

    beforeEach(module('benchmark-breadcrumbs'));
    beforeEach(inject(function($rootScope){
            $scope = $rootScope.$new();
    }));
    //Tests
    describe("controller test", function(){
        beforeEach(inject(function($controller) {
            ctrl = $controller('BreadcrumbCtrl', {$scope: $scope});
        }));
        it('contoller should be defined',  inject(function($controller) {
            expect(ctrl).toBeDefined();
        }));
    });
});
