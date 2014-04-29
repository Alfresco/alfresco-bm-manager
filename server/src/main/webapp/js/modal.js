'use strict';
angular.module('modal', [])
/* Factory that creates the modal json */
.factory('ModalService', function() {
    return {
        display: false,
        title: "Title",
        message: "Message",
        buttonOk: "Confirm",
        buttonClose: "Cancel",

        create: function(modal) {
            this.message = modal.message,
            this.title = modal.title,
            this.buttonClose = modal.buttonClose,
            this.buttonOk = modal.buttonOk,
            this.display = modal.display,
            this.actionName = modal.actionName,
            this.actionValue = modal.actionValue
        },
    }
}).value('version', '0.1')
/**
 *  Modal message directive with an inner controller to handle basic button events.
 *  Cancel button closes the modal and the confirm button calls on the next action.
 *  This directive <modal modal="data"></modal> needs to be placed within
 *  the html tags that holds the ng-controller inorder to call the parent controller's function.
 *  As the modal can be reused with other controller we configure the object defining the modal with
 *  action name and parameters. This is used to determine the parent function and perform a call
 *  to complete the confirm action.
 */
.directive('modal', function() {
    return {
        restrict: 'E',
        scope: {
            data: '='
        },
        replace: true,
        controller: function($scope, ModalService) {
            $scope.api = ModalService;
            $scope.modal = $scope.api;
            $scope.$watch('api.display', toggledisplay)
            $scope.show = false;

            /* call back to close or cancel modal */
            $scope.cancel = function() {
                $scope.modal.display = false;
                toggledisplay();
            };

            /* call back to confirm action, finds the action from parent controller
             * and calls with the test name param.
             */
            $scope.confirm = function(actionName, actionValue) {
                var fn = $scope.$parent[actionName]
                $scope.modal.display = false;
                toggledisplay();
                fn.apply(fn, actionValue);
            };

            function toggledisplay() {
                $scope.show = !$scope.show && $scope.modal.display;
                if ($scope.show) {
                    $('#theModal').modal();
                } else {
                    $('#theModal').modal('hide');
                }
            }
        },
        template: '<div class="modal fade" id="theModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" data-backdrop="static" data-keyboard="false">' + '   <div class="modal-dialog">' + '       <div class="modal-content">' + '           <div class="modal-header">' + '               <button type="button" id="modal-close" ng-click="cancel()" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' + '               <h4 class="modal-title" id="modal-title">{{modal.title}}</h4>' + '           </div>' + '           <div class="modal-body" id="modal-message">{{modal.message}}</div>' + '               <div class="modal-footer">' + '                   <button type="button" id="modal-btn-close" ng-click="cancel()" class="btn btn-default" data-dismiss="modal">{{modal.buttonClose}}</button>' + '                   <button type="button" id="modal-btn-confirm" ng-click="confirm(modal.actionName, modal.actionValue)" class="btn btn-dark">{{modal.buttonOk}}</button>' + '               </div>' + '           </div>' + '       </div>' + '   </div>' + '</div>'
    }
});
