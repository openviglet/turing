turingSNApp.config([
		'$stateProvider',
		'$urlRouterProvider',
		'$locationProvider',
		'$translateProvider',
		function($stateProvider, $urlRouterProvider, $locationProvider,
				$translateProvider) {
			$translateProvider.useSanitizeValueStrategy('escaped');
			$locationProvider.html5Mode(true);
			$translateProvider.translations('en', {
				REMOVE : "Remove",
				FIRST: "First",
				LAST: "LAST",
				PREVIOUS: "Previous",
				NEXT: "Next",
				SEARCH: "Search",
				SEARCH_FOR: "Search for",
				NO_RESULTS_FOUND:"No results found",
				APPLIED_FILTERS: "Applied Filters",
				FOUND: "Found", 
				RESULTS_FOR_THE_TERM: "results for the term"
			});
			$translateProvider.translations('pt', {
				REMOVE : "Remover",
				FIRST: "Primeiro",
				LAST: "Último",
				PREVIOUS: "Anterior",
				NEXT: "Próximo",
				SEARCH: "Pesquisar",
				SEARCH_FOR: "Pesquisar por",
				NO_RESULTS_FOUND: "Nenhum resultado encontrado",
				APPLIED_FILTERS: "Filtros Aplicados",
				FOUND: "Encontrados", 
				RESULTS_FOR_THE_TERM: "resultados para o termo"

			});
			
			$translateProvider.fallbackLanguage('en');
			
		/*	$urlRouterProvider.otherwise('/sn/search');
			$stateProvider
					.state('search', {
						url : '/sn/search',
						templateUrl : 'sn/templates/home.html',
						controller : 'TurSNMainCtrl',
						data : {
							pageTitle : 'Home | Viglet Turing'
						}
					})*/
		} ]);