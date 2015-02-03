/* 
Filter rows marked as "filterable" based on filter-value data searched in "filtre" input
This will also expand and collapse the "test list-groups" in order to view all the filtered values
*/ 

angular.module('Filter', []);

function TestProperties($scope) {
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
			//this will get all property groups that are hiddend    
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
