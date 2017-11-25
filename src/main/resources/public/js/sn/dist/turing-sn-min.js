var turingSNApp=angular.module("turingSNApp","ngCookies ngResource ngAnimate ngSanitize ui.router ui.bootstrap pascalprecht.translate angularMoment".split(" "));
turingSNApp.config(["$stateProvider","$urlRouterProvider","$locationProvider","$translateProvider",function(b,a,d,c){c.useSanitizeValueStrategy("escaped");d.html5Mode(!0);c.translations("en",{REMOVE:"Remove",FIRST:"First",LAST:"LAST",PREVIOUS:"Previous",NEXT:"Next",SEARCH:"Search",SEARCH_FOR:"Search for",NO_RESULTS_FOUND:"No results found",APPLIED_FILTERS:"Applied Filters",FOUND:"Found",RESULTS_FOR_THE_TERM:"results for the term"});c.translations("pt",{REMOVE:"Remover",FIRST:"Primeiro",LAST:"\u00daltimo",
PREVIOUS:"Anterior",NEXT:"Pr\u00f3ximo",SEARCH:"Pesquisar",SEARCH_FOR:"Pesquisar por",NO_RESULTS_FOUND:"Nenhum resultado encontrado",APPLIED_FILTERS:"Filtros Aplicados",FOUND:"Encontrados",RESULTS_FOR_THE_TERM:"resultados para o termo"});c.fallbackLanguage("en")}]);
turingSNApp.service("turAPIServerService",["$http","$location","$cookies",function(b,a,d){var c=a.protocol(),g=a.host();a=a.port();var e=c+"://"+g+":"+a+"/api";console.log(e);this.get=function(){if(null!=d.get("turAPIServer"))return d.get("turAPIServer");b({method:"GET",url:e}).then(function(b){d.put("turAPIServer",e)},function(b){d.put("turAPIServer","http://localhost:2700/api")});return e}}]);
turingSNApp.factory("vigLocale",["$window",function(b){return{getLocale:function(){var a=b.navigator;return angular.isArray(a.languages)&&0<a.languages.length?a.languages[0].split("-").join("_"):(a.language||a.browserLanguage||a.systemLanguage||a.userLanguage||"").split("-").join("_")}}}]);
turingSNApp.controller("TurSNMainCtrl",["$scope","$http","$window","$state","$rootScope","$translate","$location","turSNSearch","amMoment","vigLocale",function(b,a,d,c,g,e,f,h,k,l){b.vigLanguage=l.getLocale().substring(0,2);e.use(b.vigLanguage);k.changeLocale("en");b.total=0;a=f.path().trim();a.endsWith("/")&&(a=a.substring(0,a.length-1));a=a.split("/");b.turSiteName=a[a.length-1];b.init=function(){b.turQuery=f.search().q;b.turPage=f.search().p;b.turLocale=f.search()._setlocale;b.turSort=f.search().sort;
b.turFilterQuery=f.search()["fq[]"];b.initParams(b.turQuery,b.turPage,b.turLocale,b.turSort,b.turFilterQuery)};b.initParams=function(a,c,d,e,f){if(null==a||0==a.trim().length)a="*";h.search(b.turSiteName,a,c,d,e,f).then(function(a){b.total=a.data.queryContext.count;b.results=a.data.results.document;b.pages=a.data.pagination;b.facets=a.data.widget.facet;b.facetsToRemove=a.data.widget.facetToRemove},function(b){})};b.initURL=function(a,c,d,e,f){h.searchURL(a,c,d,e,f).then(function(a){b.total=a.data.queryContext.count;
b.results=a.data.results.document;b.pages=a.data.pagination;b.facets=a.data.widget.facet;b.facetsToRemove=a.data.widget.facetToRemove},function(b){})};b.init();g.$state=c;b.turRedirect=function(a){b.initURL(a)};b.replaceUrlSearch=function(a){return urlFormatted=a.replace("/api/sn/"+b.turSiteName+"/search","/sn/"+b.turSiteName)}}]);
turingSNApp.factory("turSNSearch",["$http","turAPIServerService",function(b,a){return{search:function(d,c,g,e,f,h){c={params:{q:c,p:g,_setlocale:e,sort:f,"fq[]":h},headers:{Accept:"application/json"}};return b.get(a.get().concat("/sn/"+d+"/search"),c)},searchURL:function(d){urlFormatted=d.replace("/api/","/");return b.get(a.get().concat(urlFormatted),{headers:{Accept:"application/json"}})}}}]);