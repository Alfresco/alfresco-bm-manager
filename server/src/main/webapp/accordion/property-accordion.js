accordion = angular.module('property-accordion', []);
accordion.directive('propertyAccordion', function () {
    return {
        restrict: 'AE',
        scope: {prop: '=data' },
        templateUrl: function(elem, attrs) {
           return attrs.templateUrl || 'accordion/property-accordion.html'
        },
        link: function (scope, el, attrs) {
            scope.panelBaseId = attrs.panelbaseid;
            // scope.panelId = attrs.collapsepanelid;
        
            $(document).ready(function(){
                angular.forEach(scope.prop, function(collapsed){
                    if (collapsed)
                    {
                        $("#property-" + scope.panelBaseId + " >> ul").collapse('show');
                    }
                });
            });
        
            scope.toggleCollapsedStates = function(ind){
                console.log(ind.collapsed);
                if(ind.collapsed)
                {
                    ind.collapsed = false;
                    $("#property-" + ind.uid + " >> ul").collapse('show');
                } else {
                    ind.collapsed = true;
                    $("#property-" + ind.uid + " >> ul").collapse('hide');
                };
console.log(ind.collapsed);
            }
        }
    };
});
