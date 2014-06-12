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
            'data': '='
        },
        restrict: 'E',
        link: link
    };

    function link(scope, element) {
        // the d3 bits

        // var color = d3.scale.category20();
        // Colors of the donut.
        var color = d3.scale.ordinal().range(["#323949", "#e7e7e7"]);
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
            .attr('class', 'progress-meter')
            .attr('transform', 'translate(' + width / 2 + ',' + height / 2 + ')');
        //add label in the middle of the donut.

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

            if (typeof newData != "undefined") {
                //Add label to middle of donut.
                if (newData[0] != "0") {
                    var total = newData[0] + newData[1];
                    var label = svg.select('#donut-label');
                    if (typeof label !== 'undefined') {
                        svg.insert("text", "g")
                            .text(total)
                            .attr("id", "donut-label")
                            .attr("class", "bold")
                            .attr("text-anchor", "middle")
                            .attr("dy", ".35em");
                    }
                }

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
                //Update label
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
})
/**
 * Line chart directive, replaces line tag with an svg generated line chart.
 * To use as follow:
 * <line data="mockData" width="430" height="260"   x="xFunction()" y="yFunction()"></line>
 */
d3Benchmark.directive('line', function() {
    return {
        restrict: 'EA',
        scope: {
            data: '=',
            width: '=',
            height: '=',
            id: '@',
        },

        link: function link(scope, element) {
            var data = scope.data;
            var margin = {
                top: 30,
                right: 40,
                bottom: 100,
                left: 50
            },
                width = 600,
                height = 300;

            var parseDate = d3.time.format("%Y-%m-%d %H:%M:%S").parse;
            // var parseDate = d3.time.format("%Y-%m-%d").parse;
            // Using time range instead of scale to display date and time on axis.
            var x = d3.time.scale().range([0, width]);
            var xAxis = d3.svg.axis().scale(x).orient("bottom");
            var y = d3.scale.linear().range([height, 0]);
            var yAxis = d3.svg.axis().scale(y).orient("left");
            var color = d3.scale.category10();
            var line = d3.svg.line()
                .interpolate("linear")
                .x(function(d) {
                    return x(d.time);
                })
                .y(function(d) {
                    return y(d.value);
                });

            data = data.map(function(d) {
                d.time = parseDate(d.time);
                d.value = +d.value;
                return d;
            });
            //nest data by event
            data = d3.nest().key(function(d) {
                return d.series;
            }).entries(data);

            //rename key property to name
            data = data.map(function(z) {
                if (z.hasOwnProperty("key")) {
                    z.name = z.key;
                    delete z.key;
                }
                return z;
            })
            var data = data.map(function(d) {
                var event = {
                    name: d.name,
                    values: null
                };
                event.values = d.values.map(function(f) {
                    return {
                        "event": event,
                        "time": f.time,
                        "value": f.value
                    };
                });
                return event;
            });

            var voronoi = d3.geom.voronoi()
                .x(function(d) {
                    return x(d.time);
                })
                .y(function(d) {
                    return y(d.value);
                })
                .clipExtent([
                    [-margin.left, -margin.top],
                    [width + margin.right, height + margin.bottom]
                ]);


            x.domain([d3.min(data, function(d) {
                    return d3.min(d.values, function(d) {
                        return d.time;
                    });
                }),
                d3.max(data, function(d) {
                    return d3.max(d.values, function(d) {
                        return d.time;
                    });
                })
            ]);
            y.domain([0, d3.max(data, function(d) {
                return d3.max(d.values, function(d) {
                    return d.value;
                });
            })]);

            //Angularjs replace html tag
            var el = element[0];
            var svg = d3.select(el).append("svg")
            // var svg = d3.select("body").append("svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
            //paint xaxis and rotate label to 65 degrees


            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis)
                .selectAll("text")
                .style("text-anchor", "end")
                .attr("dx", "-.8em")
                .attr("dy", ".15em")

            .attr("transform", function(d) {
                return "rotate(-65)"
            })
                .append("text")
                .attr("y", margin.bottom / 1.5)
                .attr("x", width / 2)
                .text("Date Taken");;


            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", -margin.top * 2.5)
                .attr("x", -height / 2)
                .attr("dy", ".71em")
                .style("text-anchor", "middle")
                .text("Y");

            svg.append("g")
                .attr("class", "cities")
                .selectAll("path")
                .data(data)
                .enter().append("path")
                .attr("d", function(ci) {
                    ci.line = this;
                    var res = line(ci.values);
                    return res
                });

            var focus = svg.append("g")
                .attr("transform", "translate(-100,-100)")
                .attr("class", "focus");

            focus.append("circle")
                .attr("r", 3.5);

            focus.append("text")
                .attr("y", -10);

            var voronoiGroup = svg.append("g")
                .attr("class", "voronoi");

            voronoiGroup.selectAll("path")
                .data(voronoi(d3.nest()
                    .key(function(d) {
                        return x(d.time) + "," + y(d.value);
                    })
                    .rollup(function(v) {
                        return v[0];
                    })
                    .entries(d3.merge(
                        data.map(function(d) {
                            return d.values;
                        })))
                    .map(function(d) {
                        return d.values;
                    })))
                .enter().append("path")
                .attr("d", function(d) {
                    return "M" + d.join("L") + "Z";
                })
                .datum(function(d) {
                    return d.point;
                })
                .on("mouseover", mouseover)
                .on("mouseout", mouseout);

            d3.select("#show-voronoi")
                .property("disabled", false)
                .on("change", function() {
                    voronoiGroup.classed("voronoi--show", this.checked);
                });

            function mouseover(d) {
                d3.select(d.event.line).classed("event--hover", true);
                d.event.line.parentNode.appendChild(d.event.line);
                focus.attr("transform", "translate(" + x(d.time) + "," + y(d.value) + ")");
                focus.select("text").text(d.event.name);
            }

            function mouseout(d) {
                d3.select(d.event.line).classed("event--hover", false);
                focus.attr("transform", "translate(-100,-100)");
            }
        }
    }
});
