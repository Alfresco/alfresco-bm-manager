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
            .attr('id','donut-chart')
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
                        //clear any charts that may exists.
                        svg.text("");
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
            // var parseDate = d3.time.format("%Y-%m-%d %H:%M:%S").parse;
            var parseDate = d3.time.format("%b %Y").parse;
            //Colors for event lines.
            var colors = new Array();
            colors = d3.scale.category20();
            var margin = {
                top: 20,
                right: 80,
                bottom: 110,
                left: 40
            },
                mini_margin = {
                    top: scope.height-70,
                    right: 80,
                    bottom: 40,
                    left: 10
                },

                width = scope.width - margin.left - margin.right;
                var height = scope.height - margin.top - margin.bottom;
                var mini_height = scope.height - mini_margin.top - mini_margin.bottom;

            var legend_margin = {
                top: 35,
                right: 80,
                bottom: 40,
                left: width + 60
            },
                legend_height = 50;

            // Using time range instead of scale to display date and time on axis.
            var x = d3.time.scale().range([0, width]),
                mini_x = d3.time.scale().range([0, width]),
                xAxis2 = d3.svg.axis().scale(mini_x).orient("bottom"),
                y = d3.scale.linear().range([height, 0]),
                mini_y = d3.scale.linear().range([mini_height, 0]),
                line = d3.svg.line()
                    .interpolate("linear")
                    .x(function(d) {
                        return x(d.time);
                    })
                    .y(function(d) {
                        return y(d.value);
                    }),
                brush = d3.svg.brush()
                    .x(mini_x)
                    .on("brush", brushed);
            //Prepare data
            data = data.map(function(d) {
                d.time = parseDate(d.time);
                d.value = +d.value;
                return d;
            });
            //nest data by event
            data = d3.nest().key(function(d) {
                return d.name;
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
                    vis: 0,
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
            // prepare data end 
            var xAxis = d3.svg.axis()
                .scale(x)
                .tickSize(-height)
                .tickPadding(10)
                .tickSubdivide(true)
                .orient("bottom");

            var yAxis = d3.svg.axis()
                .scale(y)
                .tickPadding(10)
                .tickSize(-width)
                .tickSubdivide(true)
                .orient("left");

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
                .attr("height",scope.height)
                .attr("width",scope.width);

            //Main chart view
            var chart = svg.append("g")
                .attr("class", "chart")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height - 100 + margin.top + margin.bottom)
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            //focus of chart view 
            var focus = chart.append("g")
                .attr("class", "focus")
            //Evnet line chart plotter
            focus.append("g")
                .selectAll("path")
                .data(data)
                .enter().append("path")
                .attr("class", "events")
                .attr("d", function(ci) {
                    ci.line = this;
                    var res = line(ci.values);
                    return res
                })
                .style("stroke", function(d) {
                    if (d.vis == 1) {
                        return colors(d.name);
                    }
                });
            //X axis
            focus.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")") //adds the x grids
            .call(xAxis);
            //Y axis
            focus.append("g")
                .attr("class", "y axis")
                .call(yAxis);

            //------------- start brush slider
            var line2 = d3.svg.line()
                .x(function(d) {
                    return mini_x(d.time);
                })
                .y(function(d) {
                    return mini_y(d.value);
                })
                .interpolate('basis');


            var brush = d3.svg.brush() //for slider bar at the bottom
            .x(mini_x)
                .on("brush", brushed);
            //Attach to drawing window.
            focus.attr("clip-path", "url(#clip)");
            //Chart with brush zoom view.
            chart.append("defs").append("clipPath")
                .attr("id", "clip")
                .attr("width", 500)
                .attr("x", -100)
                .append("rect")
                .attr("width", width + 1)
                .attr("height", height + 40);

            //Populate axis for Zoom brush domains
            mini_x.domain(x.domain());
            mini_y.domain(y.domain());
            //The brush control.
            var slider = svg.append("g")
                .attr("class", "slider")
                .attr("transform", "translate(40," + mini_margin.top + ")");

            //Event line plotter for the brush view
            slider.append("g")
                .attr("class", "events")
                .selectAll("path")
                .data(data)
                .enter().append("path")
                .attr("d", function(minidata) {
                    minidata.line = this;
                    var res = line2(minidata.values);
                    return res
                });
            //Brushed view x axis 
            slider.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + mini_height + ")")
                .call(xAxis2);

            //Brushed view y axis 
            slider.append("g")
                .attr("class", "x brush")
                .call(brush)
                .selectAll("rect")
                .attr("y", -6)
                .attr("height", mini_height + 7);

            //Brush, zoom feature.

            function brushed() {
                x.domain(brush.empty() ? mini_x.domain() : brush.extent());
                focus.selectAll('.events').attr('d', function(d) {
                    return line(d.values);
                });
                focus.select(".x.axis").call(xAxis);
            }

            //Start events legend
            var legend = svg.append("g")
                .attr("class", "legend")
                .attr("transform", "translate(" + legend_margin.left + "," + legend_margin.top + ")");
            // Legend box used in developement to aid in layout.
            // var legend_box = legend.append("g").append("rect")
            //     .attr("class", "legend")
            //     .attr("width", 100)
            //     .attr("height", legend_height);
            //---------------------
            var legendLabel = legend.selectAll(".legendLabel")
                .data(data)
                .enter().append("g")
                .attr("class", "legendLabel");

            legendLabel.append("text")
                .attr("class", "fundNameLabel")
                .attr("x", 40)
                .attr("y", function(d) {
                    return getEventId(d.name) * 35;
                })
                .text(function(d) {
                    return d.name;
                })
                .attr("font-family", "sans-serif")
                .attr("font-size", "10px")

            //legendLabel select or dis-select btn
            legendLabel.append("rect")
                .attr("height", 10)
                .attr("width", 25)
                .attr("y", function(d) {
                    return getEventId(d.name) * 35 - 8;
                })
                .attr("stroke", function(d) {
                    return colors(d.name);
                })
                .attr("fill", function(d) {
                    if (d.vis == "1") {
                        return colors(d.name);
                    } else {
                        return "white";
                    }
                })
            //Event toggle action
            .on("click", function(d) {
                if (d.vis == "1") {
                    d.vis = "0";
                } else {
                    d.vis = "1";
                }

                focus.select(".y.axis").call(yAxis);

                legendLabel.select("path").transition() //update curve 
                .attr("d", function(d) {
                    if (d.vis == "1") {
                        return line(d.values);
                    } else {
                        return null;
                    }
                })

                legendLabel.select("rect") //update legend 
                .attr("fill", function(d) {
                    if (d.vis == "1") {
                        return colors(d.name);
                    } else {
                        return "white";
                    }
                });
                //Find event line and recolor it
                svg.selectAll(".events").style("stroke", function(d) {
                    if (d.vis == "1") {
                        return colors(d.name);
                    }
                });
            });

            //end events legend
            //this function is mainly for accessing the colors

            function getEventId(fundName) {
                for (var i = 0; i < data.length; i++) {
                    if (data[i].name == fundName)
                        return i;
                }
            }
        }
    }
});
