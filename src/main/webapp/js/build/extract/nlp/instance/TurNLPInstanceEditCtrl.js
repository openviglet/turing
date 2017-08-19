turingApp.controller('TurNLPInstanceEditCtrl', [
		"$scope",
		"$stateParams",
		"$state",
		"$rootScope",
		"$translate",
		"vigLocale",
		"turNLPInstanceResource",
		"turNLPVendorResource",
		"turLocaleResource",
		"$uibModal",
		function($scope, $stateParams, $state, $rootScope, $translate,
				vigLocale, turNLPInstanceResource, turNLPVendorResource,turLocaleResource,
				$uibModal) {

			$scope.vigLanguage = vigLocale.getLocale().substring(0, 2);
			$translate.use($scope.vigLanguage);

			$rootScope.$state = $state;
			$scope.locales = turLocaleResource.query();
			$scope.nlpVendors = turNLPVendorResource.query();
			$scope.nlp = turNLPInstanceResource.get({
				id : $stateParams.nlpInstanceId
			});

			$scope.nlpInstanceUpdate = function() {
				$scope.nlp.$update(function() {
					$state.go('nlp.instance');
				});
			}
			$scope.nlpInstanceDelete = function() {

				var $ctrl = this;

				var modalInstance = $uibModal.open({
					animation : true,
					ariaLabelledBy : 'modal-title',
					ariaDescribedBy : 'modal-body',
					templateUrl : 'templates/modal/turDeleteInstance.html',
					controller : 'ModalDeleteInstanceCtrl',
					controllerAs : '$ctrl',
					size : null,
					appendTo : undefined,
					resolve : {
						instanceName : function() {
							return $scope.nlp.title;
						}
					}
				});

				modalInstance.result.then(function(removeInstance) {
					$scope.removeInstance = removeInstance;
					$scope.nlp.$delete(function() {
						$state.go('nlp.instance');
					});
				}, function() {
					// Selected NO
				});
			}

		} ]);