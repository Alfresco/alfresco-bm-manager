'use strict';
describe('Benchmark controller unit test', function(){
    var $scope = null;
    var ctrl = null;

    beforeEach(module('benchmark'));
    beforeEach(inject(function($rootScope){
            $scope = $rootScope.$new();
    }));
    //Tests
    describe("NavCtrl controller test", function(){
        beforeEach(inject(function($controller,$location) {
            $location.path('http://localhost:9080/alfresco-benchmark-server/#/tests');
            ctrl = $controller('NavCtrl', {$scope: $scope});
        }));
        //Test start
        it('contoller should be defined',  inject(function($controller) {
            expect(ctrl).toBeDefined();
        }));
        it('should have 1 navigation button in config',function(){
            expect($scope.items.length).toBe(1);
        })
        it('should display active false as were not in home page', function(){
            expect($scope.isActive('home')).toBe(false);
        });
        it('should display active as we are on tests', function(){
            expect($scope.isActive('tests')).toBe(true);
        });
    });
});
