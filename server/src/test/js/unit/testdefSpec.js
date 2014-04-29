'use strict'

describe('Test Service Unit test'
, function(){
    beforeEach(module("benchtest"));
    var service;
    beforeEach(inject(function(TestService){
       service = TestService;
    }));
    describe('version', function() {
        it('should return current version', inject(function(version) {
          expect(version).toEqual('0.1');
      }));
    });
    describe("getTests", function(){
        it("should return an array of tests", inject(function($httpBackend){
            $httpBackend.expect('GET','api/v1/tests').
            respond([
                {
                 "_id" : { "$oid" : "529c6e5003644d0abd143895"},
                  "description" : "SAP Benchmark test" ,
                  "name" : "Bench13"
                  , "release" : "alfresco-benchmark-bm-sample-2.0-SNAPSHOT"
                  , "schema" : 1
                  , "version" : 2}
                  , { "_id" : { "$oid" : "52cbe3f6036467580c9351f7"}
                  , "name" : "assad"
                  , "version" : 0
                  , "description" : "asd"
                  , "release" : "alfresco-benchmark-bm-sample-2.0-SNAPSHOT"
                  , "schema" : 1}
                  , { "_id" : { "$oid" : "52cbe47c036467580c9351f8"}
                  , "name" : "asdas"
                  , "version" : 0
                  , "description" : "asdasd"
                  , "release" : "alfresco-benchmark-bm-sample-2.0-SNAPSHOT"
                  , "schema" : 1}]);
            expect(service.getTests()).toBeDefined();
        }));
    });
});
