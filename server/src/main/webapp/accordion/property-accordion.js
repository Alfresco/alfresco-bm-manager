accordion = angular.module('property-accordion', []);
/**
 * Property accordion.
 *
 * To use accordion insert the following in the page:
 * <!-- Properties -->
 * <div ng-repeat="d in data.properties">
 *    <div property-accordion data="d"></div>
 * </div>
 * data is the array of property objects which holds 
 * the ui identifier, stated of accordion and array of properties.
 * The data structure should match the following:
 * {collapse:boolean, "properties":[],"uid":ui id}
 */
accordion.directive('propertyAccordion', function () {
    return {
        restrict: 'AE',
        scope: {prop: '=data' },
        templateUrl: function(elem, attrs) {
           return attrs.templateUrl || 'accordion/property-accordion.html'
        },
        link: function (scope, el, attrs) {        
            scope.toggleCollapsedStates = function(ind){
                if(ind.collapsed)
                {
                    ind.collapsed = false;
                    $("#property-" + ind.uid).collapse('show');
                } else {
                    ind.collapsed = true;
                    $("#property-" + ind.uid).collapse('hide');
                };
            }
        }
    };
});
