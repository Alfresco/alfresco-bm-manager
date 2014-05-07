'use strict';
// create module for custom directives to display d3.js based charts
var d3Benchmark = angular.module('d3benchmark', []);
/**
 * Directive that displays the donutChart.
 * Sample useage:
 * <donut-chart data="[50,50]"></donut-chart>
 * </di>
 * @return {[type]} [description]
 */
d3Benchmark.directive('donutChart', function() {
    // isolate scope
    return {
        scope: {
            'data': '=',
            'onClick': '&'
        },
        restrict: 'E',
        link: link
    };

    function link(scope, element) {
        // the d3 bits
        var color = d3.scale.category10();
        var el = element[0];
        var width = el.clientWidth;
        var height = el.clientHeight;
        var min = Math.min(width, height);
        var pie = d3.layout.pie().sort(null);
        var arc = d3.svg.arc()
            .outerRadius(min / 2 * 0.9)
            .innerRadius(min / 2 * 0.5);

        var svg = d3.select(el).append('svg')
            .attr({
                width: width,
                height: height
            })
            .append('g')
            .attr('transform', 'translate(' + width / 2 + ',' + height / 2 + ')');

        svg.on('mousedown', function(d) {
            // yo angular, the code in this callback might make a change to the scope!
            // so be sure to apply $watch's and catch errors.
            scope.$apply(function() {
                if (scope.onClick) scope.onClick();
            });
        });

        function arcTween(a) {
            // see: http://bl.ocks.org/mbostock/1346410
            var i = d3.interpolate(this._current, a);
            this._current = i(0);
            return function(t) {
                return arc(i(t));
            };
        }

        // add the <path>s for each arc slice
        var arcs = svg.selectAll('path.arc').data(pie(scope.data))
            .enter().append('path')
            .attr('class', 'arc')
            .style('stroke', 'white')
            .attr('fill', function(d, i) {
                return color(i)
            })
        // store the initial angles
        .each(function(d) {
            return this._current = d
        });

        // our data changed! update the arcs, adding, updating, or removing
        // elements as needed
        scope.$watch('data', function(newData, oldData) {
            if(typeof newData != "undefined"){
            var data = newData.slice(0); // copy
            var duration = 500;
            var PI = Math.PI;
            while (data.length < oldData.length) data.push(0);
            arcs = svg.selectAll('.arc').data(pie(data));
            arcs.transition().duration(duration).attrTween('d', arcTween);
            // transition in any new slices
            arcs.enter().append('path')
                .style('stroke', 'white')
                .attr('class', 'arc')
                .attr('fill', function(d, i) {
                    return color(i)
                })
                .each(function(d) {
                    this._current = {
                        startAngle: 2 * PI - 0.001,
                        endAngle: 2 * PI
                    }
                })
                .transition().duration(duration).attrTween('d', arcTween);
            // transition out any slices with size = 0
            arcs.filter(function(d) {
                return d.data === 0
            })
                .transition()
                .duration(duration)
                .each(function(d) {
                    d.startAngle = 2 * PI - 0.001;
                    d.endAngle = 2 * PI;
                })
                .attrTween('d', arcTween).remove();
        }
        });
    }
});
/**
 * Directive that displays bar chart.
 * Useage:
 * <bars data="50,40,40"></bars>
 *
 * @param  {[type]} $parse [description]
 * @return {[type]}        [description]
 */
d3Benchmark.directive('bars', function($parse) {
    return {
        restrict: 'E',
        replace: true,
        template: '<div id="chart"></div>',
        link: function(scope, element, attrs) {
            var data = attrs.data.split(','),
                chart = d3.select('#chart')
                    .append("div").attr("class", "chart")
                    .selectAll('div')
                    .data(data).enter()
                    .append("div")
                    .transition().ease("elastic")
                    .style("width", function(d) {
                        return d + "%";
                    })
                    .text(function(d) {
                        return d + "%";
                    });
        }
    };
});
