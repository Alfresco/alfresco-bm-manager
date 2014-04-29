'use strict';

describe('Modal Unit Test:', function(){
    beforeEach(module('modal'));
    var service;
    it('should return current version', inject(function(version) {
        expect(version).toEqual('0.1');
    }));

    describe('Modal Service', function () {
        beforeEach(inject(function(ModalService){
            service = ModalService;
        }))
         //check to see if it has the expected function
         it('should have a create modal function', function () {
            expect(angular.isFunction(service.create)).toBe(true);
        });
        it('should return default modal object', function (){
            expect(service.title).toBe('Title');
            expect(service.display).toBe(false);
            expect(service.message).toBe('Message');
            expect(service.buttonOk).toBe('Confirm');
            expect(service.buttonClose).toBe('Cancel');
            expect(service.actionName).not.toBeDefined;
            expect(service.actionValue).not.toBeDefined;
        });
        //check to see if it does what it's supposed to do.
        it('should create modal object', function (){
            var myModal = {
            display : true,
            title: 'Delete test ' + 1,
            message: 'Are you sure you want to delete ' + 1 + ' ?',
            buttonOk: "Delete",
            buttonClose: "Cancel",
            actionName: "doDeleteTest",
            actionValue: [1]
            }
            service.create(myModal);

            expect(service.title).toBe(myModal.title);
            expect(service.display).toBe(myModal.display);
            expect(service.message).toBe(myModal.message);
            expect(service.buttonOk).toBe(myModal.buttonOk);
            expect(service.buttonClose).toBe(myModal.buttonClose);
            expect(service.actionName).toBe(myModal.actionName);
            expect(service.actionValue).toBe(myModal.actionValue);
        });
    });
});
