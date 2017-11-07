var turingApp=angular.module("turingApp","ngCookies ngResource ngAnimate ngSanitize ui.router ui.bootstrap pascalprecht.translate".split(" "));turingApp.controller("TurAlertCtrl",["$scope","turNotificationService",function(a,b){a.alerts=b.notifications;a.closeAlert=function(a){b.notifications.splice(a,1)}}]);
turingApp.controller("ModalDeleteInstanceCtrl",["$uibModalInstance","instanceName",function(a,b){var e=this;e.removeInstance=!1;e.instanceName=b;e.ok=function(){e.removeInstance=!0;a.close(e.removeInstance)};e.cancel=function(){e.removeInstance=!1;a.dismiss("cancel")}}]);
turingApp.config(["$stateProvider","$urlRouterProvider","$locationProvider","$translateProvider",function(a,b,e,c){c.useSanitizeValueStrategy("escaped");c.translations("en",{NLP_EDIT:"Edit NLP",NLP_EDIT_SUBTITLE:"Change the NLP Settings",NAME:"Name",DESCRIPTION:"Description",VENDORS:"Vendors",HOST:"Host",PORT:"Port",SETTINGS_SAVE_CHANGES:"Save Changes",INTERNAL_NAME:"Internal Name",SENTENCE:"Sentence",SENTENCES:"Sentences",CATEGORY:"Category"});c.translations("pt",{NLP_EDIT:"Editar o NLP",NLP_EDIT_SUBTITLE:"Altere as configura\u00e7\u00f5es do NLP",
NAME:"Nome",DESCRIPTION:"Descri\u00e7\u00e3o",VENDORS:"Produtos",HOST:"Host",PORT:"Porta",SETTINGS_SAVE_CHANGES:"Salvar Altera\u00e7\u00f5es",INTERNAL_NAME:"Nome Interno",SENTENCE:"Senten\u00e7a",SENTENCES:"Senten\u00e7as",CATEGORY:"Categoria"});c.fallbackLanguage("en");b.otherwise("/home");a.state("home",{url:"/home",templateUrl:"templates/home.html",controller:"TurHomeCtrl",data:{pageTitle:"Home | Viglet Turing"}}).state("ml",{url:"/ml",templateUrl:"templates/ml/ml.html",data:{pageTitle:"Machine Learning | Viglet Turing"}}).state("ml.instance",
{url:"/instance",templateUrl:"templates/ml/ml-instance.html",controller:"TurMLInstanceCtrl",data:{pageTitle:"Machine Learnings | Viglet Turing"}}).state("ml.instance-new",{url:"/instance/new",templateUrl:"templates/ml/ml-instance-new.html",controller:"TurMLInstanceNewCtrl",data:{pageTitle:"New Machine Learning Instance | Viglet Turing"}}).state("ml.instance-edit",{url:"/instance/:mlInstanceId",templateUrl:"templates/ml/ml-instance-edit.html",controller:"TurMLInstanceEditCtrl",data:{pageTitle:"Edit Machine Learning | Viglet Turing"}}).state("ml.model",
{url:"/model",templateUrl:"templates/ml/model/ml-model.html",controller:"TurMLModelCtrl",data:{pageTitle:"Machine Learning Models | Viglet Turing"}}).state("ml.datagroup",{url:"/datagroup",templateUrl:"templates/ml/data/group/ml-datagroup.html",controller:"TurMLDataGroupCtrl",data:{pageTitle:"Machine Learning Data Groups | Viglet Turing"}}).state("ml.datagroup-new",{url:"/datagroup/new",templateUrl:"templates/ml/data/group/ml-datagroup-new.html",controller:"TurMLDataGroupNewCtrl",data:{pageTitle:"New Data Group | Viglet Turing"}}).state("ml.datagroup-edit",
{url:"/datagroup/:mlDataGroupId",templateUrl:"templates/ml/data/group/ml-datagroup-edit.html",controller:"TurMLDataGroupEditCtrl",data:{pageTitle:"Edit Data Group | Viglet Turing"}}).state("ml.datagroup-edit.category",{url:"/category",templateUrl:"templates/ml/data/group/ml-datagroup-category.html",controller:"TurMLDataGroupCategoryCtrl",data:{pageTitle:"Data Group Categories | Viglet Turing"}}).state("ml.datagroup-edit.category-edit",{url:"/category/:mlCategoryId",templateUrl:"templates/ml/category/ml-category-edit.html",
controller:"TurMLCategoryEditCtrl",data:{pageTitle:"Edit Category | Viglet Turing"}}).state("ml.datagroup-edit.category-edit.sentence",{url:"/sentence",templateUrl:"templates/ml/category/ml-category-sentence.html",controller:"TurMLCategorySentenceCtrl",data:{pageTitle:"Edit Category | Viglet Turing"}}).state("ml.datagroup-edit.data",{url:"/data",templateUrl:"templates/ml/data/group/ml-datagroup-data.html",controller:"TurMLDataGroupDataCtrl",data:{pageTitle:"Data Group Documents | Viglet Turing"}}).state("ml.datagroup-edit.data-edit",
{url:"/data/:mlDataId",templateUrl:"templates/ml/data/ml-data-edit.html",controller:"TurMLDataEditCtrl",data:{pageTitle:"Edit Data | Viglet Turing"}}).state("ml.datagroup-edit.sentence",{url:"/sentence",templateUrl:"templates/ml/data/group/ml-datagroup-sentence.html",controller:"TurMLDataGroupSentenceCtrl",data:{pageTitle:"Data Group Sentences | Viglet Turing"}}).state("ml.datagroup-edit.sentence-edit",{url:"/sentence/:mlSentenceId",templateUrl:"templates/ml/sentence/ml-sentence-edit.html",controller:"TurMLDataGroupSentenceEditCtrl",
data:{pageTitle:"Edit Sentence | Viglet Turing"}}).state("ml.datagroup-edit.model",{url:"/model",templateUrl:"templates/ml/data/group/ml-datagroup-model.html",controller:"TurMLDataGroupModelCtrl",data:{pageTitle:"Data Group Models | Viglet Turing"}}).state("ml.datagroup-edit.model-edit",{url:"/model/:mlModelId",templateUrl:"templates/ml/model/ml-model-edit.html",controller:"TurMLDataGroupModelEditCtrl",data:{pageTitle:"Edit Model | Viglet Turing"}}).state("ml.datagroup-edit.data-edit.sentence",{url:"/sentence",
templateUrl:"templates/ml/data/ml-data-sentence.html",controller:"TurMLDataSentenceCtrl",data:{pageTitle:"Edit Data | Viglet Turing"}}).state("converse",{url:"/converse",templateUrl:"templates/converse/converse.html",data:{pageTitle:"Converse | Viglet Turing"}}).state("converse.intent",{url:"/intent",templateUrl:"templates/converse/converse-intent.html",controller:"TurConverseIntentCtrl",data:{pageTitle:"Converse Intents | Viglet Turing"}}).state("converse.entity",{url:"/intent",templateUrl:"templates/converse/converse-entity.html",
controller:"TurConverseEntityCtrl",data:{pageTitle:"Converse Entity | Viglet Turing"}}).state("converse.training",{url:"/training",templateUrl:"templates/converse/converse-training.html",controller:"TurConverseTrainingCtrl",data:{pageTitle:"Converse Training | Viglet Turing"}}).state("converse.prebuilt-agent",{url:"/prebuiltagent",templateUrl:"templates/converse/converse-prebuilt-agent.html",controller:"TurConversePreBuiltAgentCtrl",data:{pageTitle:"Converse Prebuilt Agents | Viglet Turing"}}).state("storage",
{url:"/storage",templateUrl:"templates/storage/storage.html",data:{pageTitle:"Storage | Viglet Turing"}}).state("storage.instance",{url:"/instance",templateUrl:"templates/storage/storage-instance.html",controller:"TurStorageInstanceCtrl",data:{pageTitle:"Storages | Viglet Turing"}}).state("storage.mgmt",{url:"/mgmt",templateUrl:"templates/storage/mgmt/storage-mgmt.html",controller:"TurStorageMgmtCtrl",data:{pageTitle:"Storages | Viglet Turing"}}).state("storage.mgmt-child",{url:"/mgmt?path",templateUrl:"templates/storage/mgmt/storage-mgmt-child.html",
controller:"TurStorageMgmtCtrl",data:{pageTitle:"Storages | Viglet Turing"}}).state("se",{url:"/se",templateUrl:"templates/se/se.html",data:{pageTitle:"Search Engine | Viglet Turing"}}).state("se.instance",{url:"/instance",templateUrl:"templates/se/se-instance.html",controller:"TurSEInstanceCtrl",data:{pageTitle:"Search Engines | Viglet Turing"}}).state("se.instance-new",{url:"/instance/new",templateUrl:"templates/se/se-instance-new.html",controller:"TurSEInstanceNewCtrl",data:{pageTitle:"New Search Engine Instance | Viglet Turing"}}).state("se.instance-edit",
{url:"/instance/:seInstanceId",templateUrl:"templates/se/se-instance-edit.html",controller:"TurSEInstanceEditCtrl",data:{pageTitle:"Edit Search Engine | Viglet Turing"}}).state("sn",{url:"/sn",templateUrl:"templates/sn/sn.html",data:{pageTitle:"Semantic Navigation | Viglet Turing"}}).state("sn.site",{url:"/site",templateUrl:"templates/sn/site/sn-site.html",controller:"TurSNSiteCtrl",data:{pageTitle:"Semantic Navigation Sites | Viglet Turing"}}).state("sn.site-new",{url:"/site/new",templateUrl:"templates/sn/site/sn-site-new.html",
controller:"TurSNSiteNewCtrl",data:{pageTitle:"New Semantic Navigation Site | Viglet Turing"}}).state("sn.site-edit",{url:"/site/:snSiteId",templateUrl:"templates/sn/site/sn-site-edit.html",controller:"TurSNSiteEditCtrl",data:{pageTitle:"Edit Semantic Navigation Site | Viglet Turing"}}).state("sn.site-edit.field",{url:"/field",templateUrl:"templates/sn/site/sn-site-field.html",controller:"TurSNSiteFieldCtrl",data:{pageTitle:"Semantic Navigation Site Fields | Viglet Turing"}}).state("sn.site-edit.facet",
{url:"/facet",templateUrl:"templates/sn/site/sn-site-facet.html",controller:"TurSNSiteFacetCtrl",data:{pageTitle:"Semantic Navigation Site Facets | Viglet Turing"}}).state("sn.site-edit.hl",{url:"/hl",templateUrl:"templates/sn/site/sn-site-hl.html",controller:"TurSNSiteHLCtrl",data:{pageTitle:"Semantic Navigation Site Highlighting | Viglet Turing"}}).state("sn.site-edit.ui",{url:"/ui",templateUrl:"templates/sn/site/sn-site-ui.html",controller:"TurSNSiteUICtrl",data:{pageTitle:"Semantic Navigation Site Appearance | Viglet Turing"}}).state("sn.ad",
{url:"/ad",templateUrl:"templates/sn/sn-ad.html",controller:"TurSNAdvertisingCtrl",data:{pageTitle:"Semantic Navigation Advertising | Viglet Turing"}}).state("nlp",{url:"/nlp",templateUrl:"templates/nlp/nlp.html",data:{pageTitle:"NLP | Viglet Turing"}}).state("nlp.instance",{url:"/instance",templateUrl:"templates/nlp/nlp-instance.html",controller:"TurNLPInstanceCtrl",data:{pageTitle:"NLPs | Viglet Turing"}}).state("nlp.instance-new",{url:"/instance/new",templateUrl:"templates/nlp/nlp-instance-new.html",
controller:"TurNLPInstanceNewCtrl",data:{pageTitle:"New NLP Instance | Viglet Turing"}}).state("nlp.instance-edit",{url:"/instance/:nlpInstanceId",templateUrl:"templates/nlp/nlp-instance-edit.html",controller:"TurNLPInstanceEditCtrl",data:{pageTitle:"Edit NLP | Viglet Turing"}}).state("nlp.validation",{url:"/validation",templateUrl:"templates/nlp/nlp-validation.html",controller:"TurNLPValidationCtrl",data:{pageTitle:"NLP Validation | Viglet Turing"}}).state("nlp.entity",{url:"/entity",templateUrl:"templates/nlp/entity/nlp-entity.html",
controller:"TurNLPEntityCtrl",data:{pageTitle:"NLP Entities | Viglet Turing"}}).state("nlp.entity-import",{url:"/entity/import",templateUrl:"templates/nlp/entity/nlp-entity-import.html",data:{pageTitle:"Import Entity | Viglet Turing"}}).state("nlp.entity-edit",{url:"/entity/:nlpEntityId",templateUrl:"templates/nlp/entity/nlp-entity-edit.html",controller:"TurNLPEntityEditCtrl",data:{pageTitle:"Edit Entity | Viglet Turing"}}).state("nlp.entity-edit.term",{url:"/term",templateUrl:"templates/nlp/entity/nlp-entity-term.html",
data:{pageTitle:"Entity Terms | Viglet Turing"}})}]);turingApp.directive("fileModel",["$parse",function(a){return{restrict:"A",link:function(b,e,c){var d=a(c.fileModel).assign;e.bind("change",function(){b.$apply(function(){d(b,e[0].files[0])})})}}}]);turingApp.directive("convertToNumber",function(){return{require:"ngModel",link:function(a,b,e,c){c.$parsers.push(function(a){return parseInt(a,10)});c.$formatters.push(function(a){return""+a})}}});
turingApp.service("turNotificationService",["$http",function(a){this.notifications=[];this.addNotification=function(a){this.notifications.push({msg:a})}}]);turingApp.service("fileUpload",["$http",function(a){this.uploadFileToUrl=function(b,e){var c=new FormData;c.append("file",b);return a.post(e,c,{transformRequest:angular.identity,headers:{"Content-Type":void 0}})}}]);
turingApp.service("turAPIServerService",["$http","$location","$cookies",function(a,b,e){var c=b.protocol(),d=b.host();b=b.port();var f=c+"://"+d+":"+b+"/api";console.log(f);this.get=function(){if(null!=e.get("turAPIServer"))return e.get("turAPIServer");a({method:"GET",url:f}).then(function(a){e.put("turAPIServer",f)},function(a){e.put("turAPIServer","http://localhost:2700/api")});return f}}]);
turingApp.factory("turLocaleResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/locale/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.factory("vigLocale",["$window",function(a){return{getLocale:function(){var b=a.navigator;return angular.isArray(b.languages)&&0<b.languages.length?b.languages[0].split("-").join("_"):(b.language||b.browserLanguage||b.systemLanguage||b.userLanguage||"").split("-").join("_")}}}]);
turingApp.controller("TurSEInstanceEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turSEInstanceResource","turSEVendorResource","turLocaleResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k,l,m){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.locales=k.query();a.seVendors=h.query();a.se=g.get({id:b.seInstanceId});a.seInstanceUpdate=function(){a.se.$update(function(){l.addNotification('Search Engine Instance "'+a.se.title+
'" was saved.')})};a.seInstanceDelete=function(){m.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.se.title}}}).result.then(function(b){a.removeInstance=b;a.deletedMessage='Search Engine Instance "'+a.se.title+'" was deleted.';a.se.$delete(function(){l.addNotification(a.deletedMessage);e.go("se.instance")})},
function(){})}}]);turingApp.controller("TurSEInstanceNewCtrl",["$scope","$state","$rootScope","$translate","vigLocale","turSEInstanceResource","turSEVendorResource","turLocaleResource","turNotificationService",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=d.getLocale().substring(0,2);c.use(a.vigLanguage);e.$state=b;a.locales=h.query();a.seVendors=g.query();a.se={enabled:0};a.seInstanceSave=function(){f.save(a.se,function(){k.addNotification('Search Engine Instance "'+a.se.title+'" was created.');b.go("se.instance")})}}]);
turingApp.controller("TurSEInstanceCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turSEInstanceResource",function(a,b,e,c,d,f,g){d.$state=c;a.ses=g.query()}]);turingApp.factory("turSEInstanceResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/se/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.factory("turSEVendorResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/se/vendor/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurHomeCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turAPIServerService",function(a,b,e,c,d,f,g){createServerAPICookie=g.get();a.accesses=null;d.$state=c}]);
turingApp.controller("TurStorageMgmtCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turStorageMgmtResource","$stateParams",function(a,b,e,c,d,f,g,h){d.$state=c;a.currPath=h.path+"/";console.log("Teste1");console.log(c.params.path);a.getFullPath=function(a){return h.path+"/"+a};0>=h.path.length?(a.rootPath=!0,a.filesAndDirs=g.query()):(a.rootPath=!1,a.filesAndDirs=g.get({id:h.path}))}]);
turingApp.factory("turStorageMgmtResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/storage/hadoop/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.controller("TurStorageInstanceCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turStorageInstanceResource",function(a,b,e,c,d,f,g){d.$state=c;a.ses=g.query()}]);
turingApp.factory("turStorageInstanceResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/storage/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.controller("TurSNAdvertisingCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);turingApp.controller("TurSNSiteUICtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);
turingApp.controller("TurSNSiteHLCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);turingApp.controller("TurSNSiteFieldCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);
turingApp.controller("TurSNSiteNewCtrl",["$scope","$state","$rootScope","$translate","vigLocale","turSNSiteResource","turSEInstanceResource","turNLPInstanceResource","turNotificationService",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=d.getLocale().substring(0,2);c.use(a.vigLanguage);e.$state=b;a.snSite={};a.seInstances=g.query({},function(){angular.forEach(a.seInstances,function(b,c){1==b.selected&&(b.title=b.title,a.snSite.turSEInstance=b)})});a.nlpInstances=h.query({},function(){angular.forEach(a.nlpInstances,
function(b,c){1==b.selected&&(b.title=b.title,a.snSite.turNLPInstance=b)})});a.snSiteSave=function(){f.save(a.snSite,function(){k.addNotification('Semantic Navigation Site "'+a.snSite.name+'" was created.');b.go("sn.site")})}}]);turingApp.controller("TurSNSiteCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turSNSiteResource",function(a,b,e,c,d,f,g){d.$state=c;a.snSites=g.query()}]);
turingApp.controller("TurSNSiteEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turSNSiteResource","turSEInstanceResource","turNLPInstanceResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k,l,m){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.seInstances=h.query();a.nlpInstances=k.query();a.snSite=g.get({id:b.snSiteId});a.snSiteUpdate=function(){a.snSite.$update(function(){l.addNotification('Semantic Navigation Site "'+
a.snSite.name+'" was saved.')})};a.snSiteDelete=function(){m.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){l.addNotification('Semantic Navigation Site "'+a.snSite.name+'" was deleted.');return a.snSite.name}}}).result.then(function(b){a.removeInstance=b;a.snSite.$delete(function(){e.go("sn.site")})},
function(){})}}]);turingApp.factory("turSNSiteResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/sn/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.controller("TurSNSiteFacetCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);
turingApp.controller("TurMLCategoryNewCtrl",["$uibModalInstance","category","turMLCategoryResource","turNotificationService",function(a,b,e,c){var d=this;d.removeInstance=!1;d.category=b;d.ok=function(){e.save(d.category,function(b){c.addNotification('Category "'+b.name+'" was created.');a.close(b)})};d.cancel=function(){a.dismiss("cancel")}}]);
turingApp.controller("TurMLCategorySentenceCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","$uibModal","turMLDataSentenceResource","turNotificationService",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.sentenceNew=function(){a.sentence={dataGroupId:b.mlDataGroupId,turMLCategoryId:b.mlCategoryId};a.categoryId={dataGroupId:b.mlDataGroupId};g.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",
templateUrl:"templates/ml/sentence/ml-sentence-new.html",controller:"TurMLSentenceNewCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{sentence:function(){return a.sentence}}}).result.then(function(a){},function(){})}}]);
turingApp.controller("TurMLCategoryEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLCategoryResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.category=g.get({id:b.mlCategoryId});a.mlCategoryUpdate=function(){a.category.$update(function(){h.addNotification('Category "'+a.category.name+'" was saved.')})};a.mlCategoryDelete=function(){k.open({animation:!0,ariaLabelledBy:"modal-title",
ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.category.name}}}).result.then(function(b){a.removeInstance=b;a.deletedMessage='Category "'+a.category.name+'" was deleted.';a.category.$delete(function(){h.addNotification(a.deletedMessage);e.go("ml.datagroup")})},function(){})}}]);
turingApp.factory("turMLCategoryResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/category/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurMLSentenceNewCtrl",["$uibModalInstance","sentence","turMLDataGroupSentenceResource","turNotificationService",function(a,b,e,c){var d=this;d.removeInstance=!1;d.dataGroupId=b.dataGroupId;console.log(b.turMLCategoryId);d.sentence=b;d.ok=function(){delete b.dataGroupId;e.save({dataGroupId:d.dataGroupId},d.sentence,function(b){c.addNotification('Sentence "'+b.sentence+'" was created.');a.close(b)})};d.cancel=function(){a.dismiss("cancel")}}]);
turingApp.controller("TurMLModelCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turMLModelResource",function(a,b,e,c,d,f,g){d.$state=c;a.mlModels=g.query()}]);turingApp.factory("turMLModelResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/model/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.controller("TurMLInstanceCtrl",["$scope","$state","$rootScope","$translate","turMLInstanceResource",function(a,b,e,c,d){e.$state=b;a.mls=d.query()}]);
turingApp.controller("TurMLInstanceNewCtrl",["$scope","$state","$rootScope","$translate","vigLocale","turMLInstanceResource","turMLVendorResource","turLocaleResource","turNotificationService",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=d.getLocale().substring(0,2);c.use(a.vigLanguage);e.$state=b;a.locales=h.query();a.mlVendors=g.query();a.ml={enabled:0};a.mlInstanceSave=function(){f.save(a.ml,function(){k.addNotification('Machine Learning Instance "'+a.ml.title+'" was created.');b.go("ml.instance")})}}]);
turingApp.factory("turMLInstanceResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurMLInstanceEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLInstanceResource","turMLVendorResource","turLocaleResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k,l,m){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.locales=k.query();a.mlVendors=h.query();a.ml=g.get({id:b.mlInstanceId});a.mlInstanceUpdate=function(){a.ml.$update(function(){l.addNotification('Machine Learning Instance "'+
a.ml.title+'" was saved.')})};a.mlInstanceDelete=function(){m.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.ml.title}}}).result.then(function(b){a.removeInstance=b;a.deletedMessage='Machine Learning Instance "'+a.ml.title+'" was deleted.';a.ml.$delete(function(){l.addNotification(a.deletedMessage);
e.go("ml.instance")})},function(){})}}]);turingApp.controller("TurMLDataGroupCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turMLDataGroupResource",function(a,b,e,c,d,f,g){d.$state=c;a.mlDataGroups=g.query()}]);
turingApp.controller("TurMLDataGroupCategoryCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupCategoryResource","$uibModal",function(a,b,e,c,d,f,g,h){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.mlDataGroupCategories=g.query({dataGroupId:b.mlDataGroupId});a.categoryNew=function(){a.category={};h.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/ml/category/ml-category-new.html",
controller:"TurMLCategoryNewCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{category:function(){return a.category}}}).result.then(function(a){delete a.turDataGroupCategories;delete a.turDataSentences;turMLDataGroupCategory={};turMLDataGroupCategory.turMLCategory=a;g.save({dataGroupId:b.mlDataGroupId},turMLDataGroupCategory)},function(){})}}]);
turingApp.controller("TurMLDataGroupEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupResource","turNotificationService","$uibModal","$http","turAPIServerService",function(a,b,e,c,d,f,g,h,k,l,m){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.dataGroup=g.get({id:b.mlDataGroupId});a.dataGroupSave=function(){a.dataGroup.$update(function(){h.addNotification('Data Group "'+a.dataGroup.name+'" was saved.')})};a.generateModel=function(){l.get(m.get().concat("/ml/data/group/"+
b.mlDataGroupId+"/model/generate")).then(function(b){h.addNotification('"'+a.dataGroup.name+'" model was generated.');a.results=b.data},function(a){})};a.dataGroupDelete=function(){k.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.dataGroup.name}}}).result.then(function(b){a.removeInstance=b;
a.deletedMessage='Data Group "'+a.dataGroup.name+'" was deleted.';a.dataGroup.$delete(function(){h.addNotification(a.deletedMessage);e.go("ml.datagroup")})},function(){})}}]);
turingApp.controller("TurMLDataGroupNewCtrl",["$scope","$http","$window","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupResource","turNotificationService",function(a,b,e,c,d,f,g,h,k,l){a.vigLanguage=h.getLocale().substring(0,2);g.use(a.vigLanguage);f.$state=d;a.mlDataGroupId=c.mlDataGroupId;a.dataGroup={};a.dataGroupSave=function(){k.save(a.dataGroup,function(){l.addNotification('Data Group "'+a.dataGroup.name+'" was created.');d.go("ml.datagroup")})}}]);
turingApp.controller("TurMLDataGroupSentenceEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupSentenceResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.sentence=g.get({dataGroupId:b.mlDataGroupId,id:b.mlSentenceId});a.mlSentenceUpdate=function(){a.sentence.$update({dataGroupId:b.mlDataGroupId},function(){h.addNotification('Sentence "'+a.sentence.sentence+
'" was saved.')})};a.mlSentenceDelete=function(){k.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.sentence.sentence}}}).result.then(function(c){a.removeInstance=c;a.deletedMessage='Sentence "'+a.sentence.sentence+'" was deleted.';a.sentence.$delete({dataGroupId:b.mlDataGroupId},function(){h.addNotification(a.deletedMessage);
e.go("ml.datagroup")})},function(){})}}]);
turingApp.controller("TurMLDataGroupSentenceCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupSentenceResource","$uibModal","turNotificationService",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.mlDataGroupSentences=g.query({dataGroupId:b.mlDataGroupId});a.sentenceUpdate=function(a){g.update({dataGroupId:b.mlDataGroupId,id:a.id},a,function(){k.addNotification('Sentence "'+a.sentence.substring(0,20)+
'..." was saved.')})};a.sentenceNew=function(){a.sentence={dataGroupId:b.mlDataGroupId};h.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/ml/sentence/ml-sentence-new.html",controller:"TurMLSentenceNewCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{sentence:function(){return a.sentence}}}).result.then(function(a){},function(){})}}]);
turingApp.controller("TurMLDataGroupModelEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupModelResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.model=g.get({dataGroupId:b.mlDataGroupId,id:b.mlModelId});a.mlModelUpdate=function(){a.model.$update({dataGroupId:b.mlDataGroupId},function(){h.addNotification('Model "'+a.model.model+'" was saved.')})};a.mlModelDelete=
function(){k.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.model.model}}}).result.then(function(b){a.removeInstance=b;a.deletedMessage='Model "'+a.model.model+'" was deleted.';a.model.$delete(function(){h.addNotification(a.deletedMessage);e.go("ml.datagroup")})},function(){})}}]);
turingApp.controller("TurMLDataGroupModelCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupModelResource","$uibModal",function(a,b,e,c,d,f,g,h){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.mlDataGroupModels=g.query({dataGroupId:b.mlDataGroupId});a.modelNew=function(){a.model={dataGroupId:b.mlDataGroupId};h.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/ml/model/ml-model-new.html",
controller:"TurMLModelNewCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{model:function(){return a.model}}}).result.then(function(a){},function(){})}}]);
turingApp.controller("TurMLDataGroupDataCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataGroupDataResource","$uibModal",function(a,b,e,c,d,f,g,h){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.mlDataGroupDatas=g.query({dataGroupId:b.mlDataGroupId});a.uploadDocument=function(){a.data={};a.data.datagroupId=b.mlDataGroupId;h.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/ml/data/ml-document-upload.html",
controller:"TurMLDataNewCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{data:function(){return a.data}}}).result.then(function(a){},function(){})}}]);turingApp.factory("turMLDataGroupModelResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/group/:dataGroupId/model/:id"),{id:"@id",dataGroupId:"@dataGroupId"},{update:{method:"PUT"}})}]);
turingApp.factory("turMLDataGroupResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/group/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.factory("turMLDataGroupCategoryResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/group/:dataGroupId/category/:id"),{id:"@id",dataGroupId:"@dataGroupId"},{update:{method:"PUT"}})}]);
turingApp.factory("turMLDataGroupDataResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/group/:dataGroupId/data/:id"),{id:"@id",dataGroupId:"@dataGroupId"},{update:{method:"PUT"}})}]);turingApp.factory("turMLDataGroupSentenceResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/group/:dataGroupId/sentence/:id"),{id:"@id",dataGroupId:"@dataGroupId"},{update:{method:"PUT"}})}]);
turingApp.controller("TurMLDataNewCtrl",["$uibModalInstance","data","fileUpload","turNotificationService","turAPIServerService",function(a,b,e,c,d){var f=this;f.myFile=null;f.removeInstance=!1;f.data=b;f.ok=function(){var g=f.myFile,h=d.get().concat("/ml/data/group/"+b.datagroupId+"/data/import");e.uploadFileToUrl(g,h).then(function(b){c.addNotification(b.data.turData.name+'" file was uploaded.');a.close(b)})};f.cancel=function(){f.removeInstance=!1;a.dismiss("cancel")}}]);
turingApp.factory("turMLDataSentenceResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/sentence/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurMLDataEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turMLDataResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.data=g.get({id:b.mlDataId});a.dataSave=function(){delete a.data.turDataGroupSentences;a.data.$update(function(){h.addNotification('Data "'+a.data.name+'" was saved.')})};a.dataDelete=function(){k.open({animation:!0,ariaLabelledBy:"modal-title",
ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.data.name}}}).result.then(function(b){a.removeInstance=b;a.deletedMessage='Data "'+a.data.name+'" was deleted.';a.data.$delete(function(){h.addNotification(a.deletedMessage);e.go("ml.datagroup")})},function(){})}}]);
turingApp.factory("turMLDataResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/data/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurMLDataSentenceCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","$uibModal","turMLDataGroupCategoryResource","turMLDataGroupSentenceResource","turNotificationService",function(a,b,e,c,d,f,g,h,k,l){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.sentenceUpdate=function(a){k.update({dataGroupId:b.mlDataGroupId,id:a.id},a,function(){l.addNotification('Sentence "'+a.sentence.substring(0,20)+'..." was saved.')})}}]);
turingApp.factory("turMLVendorResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/ml/vendor/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.controller("TurNLPEntityEditCtrl",["$scope","$http","$window","$stateParams","$state","$rootScope","$translate","vigLocale","turNLPEntityResource",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=h.getLocale().substring(0,2);g.use(a.vigLanguage);f.$state=d;a.nlpEntityId=c.nlpEntityId;a.entity=k.get({id:a.nlpEntityId})}]);
turingApp.controller("TurNLPEntityCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turNLPEntityResource",function(a,b,e,c,d,f,g){d.$state=c;a.entities=g.query()}]);turingApp.factory("turNLPEntityResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/entity/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurNLPInstanceCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turNLPInstanceResource",function(a,b,e,c,d,f,g){d.$state=c;a.nlps=g.query()}]);
turingApp.controller("TurNLPInstanceEditCtrl",["$scope","$stateParams","$state","$rootScope","$translate","vigLocale","turNLPInstanceResource","turNLPVendorResource","turLocaleResource","turNotificationService","$uibModal",function(a,b,e,c,d,f,g,h,k,l,m){a.vigLanguage=f.getLocale().substring(0,2);d.use(a.vigLanguage);c.$state=e;a.locales=k.query();a.nlpVendors=h.query();a.nlp=g.get({id:b.nlpInstanceId});a.nlpInstanceUpdate=function(){a.nlp.$update(function(){l.addNotification('NLP Instance "'+a.nlp.title+
'" was saved.')})};a.nlpInstanceDelete=function(){m.open({animation:!0,ariaLabelledBy:"modal-title",ariaDescribedBy:"modal-body",templateUrl:"templates/modal/turDeleteInstance.html",controller:"ModalDeleteInstanceCtrl",controllerAs:"$ctrl",size:null,appendTo:void 0,resolve:{instanceName:function(){return a.nlp.title}}}).result.then(function(b){a.removeInstance=b;a.deletedMessage='NLP Instance "'+a.nlp.title+'" was deleted.';a.nlp.$delete(function(){l.addNotification(a.deletedMessage);e.go("nlp.instance")})},
function(){})}}]);turingApp.factory("turNLPInstanceResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/nlp/:id"),{id:"@id"},{update:{method:"PUT"}})}]);
turingApp.controller("TurNLPInstanceNewCtrl",["$scope","$state","$rootScope","$translate","vigLocale","turNLPInstanceResource","turNLPVendorResource","turLocaleResource","turNotificationService",function(a,b,e,c,d,f,g,h,k){a.vigLanguage=d.getLocale().substring(0,2);c.use(a.vigLanguage);e.$state=b;a.locales=h.query();a.nlpVendors=g.query();a.nlp={enabled:0};a.nlpInstanceSave=function(){f.save(a.nlp,function(){k.addNotification('NLP Instance "'+a.nlp.title+'" was created.');b.go("nlp.instance")})}}]);
turingApp.controller("TurNLPValidationCtrl",["$scope","$http","$window","$state","$rootScope","$translate","turNLPInstanceResource","turAPIServerService",function(a,b,e,c,d,f,g,h){a.results=null;a.text=null;a.nlpmodel=null;d.$state=c;a.nlps=g.query({},function(){angular.forEach(a.nlps,function(b,c){1==b.selected&&(a.nlpmodel=b.id)})});a.changeView=function(c){text={text:a.text};c=JSON.stringify(text);b.post(h.get().concat("/nlp/"+a.nlpmodel+"/validate"),c).then(function(b){a.results=b.data},function(a){})}}]);
turingApp.factory("turNLPVendorResource",["$resource","turAPIServerService",function(a,b){return a(b.get().concat("/nlp/vendor/:id"),{id:"@id"},{update:{method:"PUT"}})}]);turingApp.controller("TurConverseEntityCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);turingApp.controller("TurConverseIntentCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);
turingApp.controller("TurConverseTrainingCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);turingApp.controller("TurConversePreBuiltAgentCtrl",["$scope","$http","$window","$state","$rootScope","$translate",function(a,b,e,c,d,f){d.$state=c}]);