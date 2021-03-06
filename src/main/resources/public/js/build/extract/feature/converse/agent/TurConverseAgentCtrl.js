turingApp.controller('TurConverseAgentCtrl', [
	"$scope",
	"$http",
	"$window",
	"$state",
	"$stateParams",
	"$rootScope",
	"$translate",
	"turAPIServerService",
	"turConverseAgentResource",
	"Notification",
	function ($scope, $http, $window, $state, $stateParams, $rootScope, $translate, turAPIServerService, turConverseAgentResource, Notification) {
		$rootScope.$state = $state;
		$scope.agentId = $stateParams.agentId;
		$scope.tryText = "";
		$scope.agentResponse = null;
		$scope.agent = turConverseAgentResource.get({
			id: $scope.agentId
		});
		$scope.try = function () {
			$scope
				.$evalAsync($http
					.get(turAPIServerService.get().concat("/converse/agent/" + $scope.agentId + "/chat?q=" + $scope.tryText))
					.then(
						function (response) {
							$scope.agentResponse = response.data;
						}));

		}
		$scope.agentRebuild = function () {
			$scope
				.$evalAsync($http
					.get(turAPIServerService.get().concat("/converse/agent/" + $scope.agentId + "/rebuild"))
					.then(
						function (response) {
							Notification.warning($scope.agent.name + ' Agent was rebuilt.');
						}));

		}
	}]);