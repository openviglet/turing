turingApp.controller('TurSNSiteFieldCtrl', [
		"$scope",
		"$http",
		"$window",
		"$state",
		"$rootScope",
		"$translate",
		function($scope, $http, $window, $state, $rootScope, $translate) {
			$rootScope.$state = $state;
		} ]);