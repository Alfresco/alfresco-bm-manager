// Karma configuration
// Generated on Fri Jan 03 2014 16:46:56 GMT+0000 (GMT)

module.exports = function(config) {
  config.set({

    // base path, that will be used to resolve files and exclude
    basePath: '../../../',

    // frameworks to use
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
        'main/webapp/js/jquery/jquery-2.2.0.min.js',
        'main/webapp/js/angularjs/1.4.9/angular.js',
        'main/webapp/js/angularjs/1.4.9/angular-mocks.js',
        'main/webapp/js/angularjs/1.4.9/angular-resource.js',
        'main/webapp/js/angularjs/1.4.9/angular-route.js',
        'main/webapp/js/app.js',
        'main/webapp/js/breadcrumbs.js',
        'main/webapp/js/filter.js',
        'main/webapp/js/modal.js',
        'main/webapp/benchmark/**/*.js',
        'test/js/unit/*.js'
    ],

    // list of files to exclude
    exclude: [
    ],

    // test results reporter to use
    // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
    reporters: ['progress'],

    // web server port
    port: 9876,

    // enable / disable colors in the output (reporters and logs)
    colors: true,

    // level of loggingpossible values:.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel:  config.LG_DEBUG,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,

    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera (has to be installed with `npm install karma-opera-launcher`)
    // - Safari (only Mac; has to be installed with `npm install karma-safari-launcher`)
    // - PhantomJS
    // - IE (only Windows; has to be installed with `npm install karma-ie-launcher`)
    browsers: ['Firefox'],
    //browsers: ['Firefox','Chrome','Safari'],
    // If browser does not capture in given timeout [ms], kill it
    captureTimeout: 60000,

    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: false
  });
};
