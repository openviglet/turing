/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.viglet.turing.api.sn.search;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchLatestRequestBean;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSiteLocaleBean;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchBean;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.sn.search.TurSNParamType;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.TurSNUtils;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sn/{siteName}/search")
@Tag(name = "Semantic Navigation Search", description = "Semantic Navigation Search API")
public class TurSNSiteSearchAPI {
        private static final String NONE = "NONE";
        @Value("${turing.search.cache.enabled:false}")
        private boolean searchCacheEnabled;
        private final TurSNSearchProcess turSNSearchProcess;
        private final TurSNSiteRepository turSNSiteRepository;
        private final TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI;
        private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

        public TurSNSiteSearchAPI(TurSNSearchProcess turSNSearchProcess,
                        TurSNSiteRepository turSNSiteRepository,
                        TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI,
                        TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
                this.turSNSearchProcess = turSNSearchProcess;
                this.turSNSiteRepository = turSNSiteRepository;
                this.turSNSiteSearchCachedAPI = turSNSiteSearchCachedAPI;
                this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        }

        @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<List<Object>> turSNSiteSearchSelectListGet(
                        @PathVariable String siteName,
                        @ModelAttribute TurSNSearchParams turSNSearchParams,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_DEFAULT) List<String> filterQueriesDefault,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_AND) List<String> filterQueriesAnd,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_OR) List<String> filterQueriesOr,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_OPERATOR, defaultValue = NONE) TurSNFilterQueryOperator fqOperator,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_ITEM_OPERATOR, defaultValue = NONE) TurSNFilterQueryOperator fqItemOperator,
                        @RequestParam(required = false, name = TurSNParamType.LOCALE) String localeRequest,
                        HttpServletRequest request) {
                setSearchParams(turSNSearchParams, filterQueriesDefault, filterQueriesAnd, filterQueriesOr,
                                fqOperator, fqItemOperator, convertToLocale(localeRequest));
                if (!turSNSearchProcess.existsByTurSNSiteAndLanguage(siteName, turSNSearchParams.getLocale())) {
                        return notFoundResponse();
                }
                return turSNSiteRepository.findByName(siteName).map(site -> {

                        List<Object> termList = turSNSearchProcess
                                        .searchList(getTurSNSiteSearchContext(turSNSearchParams, request, site));
                        return new ResponseEntity<>(termList, HttpStatus.OK);
                }).orElse(notFoundResponse());
        }

        private Locale convertToLocale(String localeRequest) {
                return LocaleUtils.toLocale(localeRequest);
        }

        private TurSNSiteSearchContext getTurSNSiteSearchContext(
                        TurSNSearchParams turSNSearchParams, HttpServletRequest request, TurSNSite site) {
                TurSNConfig turSNConfig = getTurSNConfig(site);
                TurSNSiteSearchContext turSNSiteSearchContext = TurSNUtils.getTurSNSiteSearchContext(turSNConfig,
                                site.getName(), turSNSearchParams, request);
                return turSNSiteSearchContext;
        }

        @GetMapping
        public ResponseEntity<TurSNSiteSearchBean> turSNSiteSearchSelectGet(
                        @PathVariable String siteName,
                        @ModelAttribute TurSNSearchParams turSNSearchParams,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_DEFAULT) List<String> filterQueriesDefault,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_AND) List<String> filterQueriesAnd,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_OR) List<String> filterQueriesOr,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_OPERATOR, defaultValue = NONE) TurSNFilterQueryOperator fqOperator,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_ITEM_OPERATOR, defaultValue = NONE) TurSNFilterQueryOperator fqItemOperator,
                        @RequestParam(required = false, name = TurSNParamType.LOCALE) String localeRequest,
                        HttpServletRequest request) {
                setSearchParams(turSNSearchParams, filterQueriesDefault, filterQueriesAnd,
                                filterQueriesOr, fqOperator, fqItemOperator, convertToLocale(localeRequest));
                if (!turSNSearchProcess.existsByTurSNSiteAndLanguage(siteName, turSNSearchParams.getLocale())) {
                        return notFoundResponse();
                }
                return turSNSiteRepository.findByName(siteName)
                                .map(site -> executeGetSearch(siteName, turSNSearchParams, request, site))
                                .orElse(notFoundResponse());
        }

        private ResponseEntity<TurSNSiteSearchBean> executeGetSearch(String siteName,
                        TurSNSearchParams turSNSearchParams,
                        HttpServletRequest request,
                        TurSNSite site) {

                TurSNSiteSearchContext searchContext = getTurSNSiteSearchContext(turSNSearchParams, request, site);
                return searchCacheEnabled
                                ? executeSearchWithCache(request, searchContext)
                                : executeSearch(searchContext);
        }

        private void setSearchParams(TurSNSearchParams turSNSearchParams, List<String> filterQueriesDefault,
                        List<String> filterQueriesAnd, List<String> filterQueriesOr,
                        TurSNFilterQueryOperator fqOperator, TurSNFilterQueryOperator fqItemOperator,
                        Locale locale) {
                turSNSearchParams.setFq(filterQueriesDefault);
                turSNSearchParams.setFqAnd(filterQueriesAnd);
                turSNSearchParams.setFqOr(filterQueriesOr);
                turSNSearchParams.setFqOp(fqOperator);
                turSNSearchParams.setFqiOp(fqItemOperator);
                turSNSearchParams.setLocale(locale);
        }

        private <T> ResponseEntity<T> notFoundResponse() {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        private ResponseEntity<TurSNSiteSearchBean> executeSearch(TurSNSiteSearchContext turSNSiteSearchContext) {
                return new ResponseEntity<>(
                                turSNSearchProcess.search(turSNSiteSearchContext), HttpStatus.OK);
        }

        private ResponseEntity<TurSNSiteSearchBean> executeSearchWithCache(HttpServletRequest request,
                        TurSNSiteSearchContext turSNSiteSearchContext) {
                return new ResponseEntity<>(
                                turSNSiteSearchCachedAPI.searchCached(
                                                TurSNUtils.getCacheKey(turSNSiteSearchContext.getSiteName(), request),
                                                turSNSiteSearchContext),
                                HttpStatus.OK);
        }

        @PostMapping
        public ResponseEntity<TurSNSiteSearchBean> turSNSiteSearchSelectPost(
                        @PathVariable String siteName,
                        @ModelAttribute TurSNSearchParams turSNSearchParams,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_DEFAULT) List<String> filterQueriesDefault,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_AND) List<String> filterQueriesAnd,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_OR) List<String> filterQueriesOr,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_OPERATOR, defaultValue = NONE) TurSNFilterQueryOperator fqOperator,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_ITEM_OPERATOR, defaultValue = NONE) TurSNFilterQueryOperator fqItemOperator,
                        @RequestParam(required = false, name = TurSNParamType.LOCALE) String localeRequest,
                        @RequestBody TurSNSitePostParamsBean turSNSitePostParamsBean,
                        Principal principal,
                        HttpServletRequest request) {
                if (principal == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                setSearchParams(turSNSearchParams, filterQueriesDefault, filterQueriesAnd,
                                filterQueriesOr, fqOperator, fqItemOperator,
                                determineLocale(turSNSitePostParamsBean, localeRequest));
                if (!turSNSearchProcess.existsByTurSNSiteAndLanguage(siteName, turSNSearchParams.getLocale())) {
                        return notFoundResponse();
                }

                return turSNSiteRepository.findByName(siteName)
                                .map(site -> executePostSearch(turSNSearchParams, turSNSitePostParamsBean, request,
                                                site))
                                .orElse(notFoundResponse());
        }

        private Locale determineLocale(TurSNSitePostParamsBean turSNSitePostParamsBean, String localeRequest) {
                String localeStr = StringUtils.isNotBlank(turSNSitePostParamsBean.getLocale())
                                ? turSNSitePostParamsBean.getLocale()
                                : localeRequest;
                return convertToLocale(localeStr);
        }

        private ResponseEntity<TurSNSiteSearchBean> executePostSearch(
                        TurSNSearchParams turSNSearchParams,
                        TurSNSitePostParamsBean turSNSitePostParamsBean,
                        HttpServletRequest request,
                        TurSNSite site) {
                turSNSitePostParamsBean.setTargetingRules(
                                turSNSearchProcess.requestTargetingRules(turSNSitePostParamsBean.getTargetingRules()));
                TurSNSiteSearchContext searchContext = TurSNUtils.getTurSNSiteSearchContext(
                                getTurSNConfig(site), site.getName(), turSNSearchParams,
                                turSNSitePostParamsBean, request);
                if (searchCacheEnabled) {
                        return executeSearchWithCache(request, searchContext);
                } else {
                        return executeSearch(searchContext);
                }
        }

        private TurSNConfig getTurSNConfig(TurSNSite turSNSite) {
                TurSNConfig turSNConfig = new TurSNConfig();
                turSNConfig.setHlEnabled(TurSNUtils.isTrue(turSNSite.getHl()) && hasSiteHLFields(turSNSite));
                return turSNConfig;
        }

        private boolean hasSiteHLFields(TurSNSite turSNSite) {
                return !CollectionUtils.isEmpty(turSNSiteFieldExtRepository
                                .findByTurSNSiteAndHlAndEnabled(turSNSite, 1, 1));
        }

        @GetMapping("locales")
        public List<TurSNSiteLocaleBean> turSNSiteSearchLocale(@PathVariable String siteName) {
                return turSNSiteRepository.findByName(siteName).map(turSNSite -> {
                        try {
                                return turSNSearchProcess.responseLocales(turSNSite, new URI(
                                                String.format("/api/sn/%s/search", siteName)));
                        } catch (URISyntaxException e) {
                                log.error(e.getMessage(), e);
                        }
                        return getTurSNSiteLocaleBeans();
                }).orElse(Collections.emptyList());
        }

        private static @NotNull List<TurSNSiteLocaleBean> getTurSNSiteLocaleBeans() {
                return new ArrayList<>();
        }

        @PostMapping("latest")
        public ResponseEntity<List<String>> turSNSiteSearchLatestImpersonate(
                        @PathVariable String siteName,
                        @RequestParam(required = false, name = TurSNParamType.ROWS, defaultValue = "5") Integer rows,
                        @RequestParam(name = TurSNParamType.LOCALE) String locale,
                        @RequestBody Optional<TurSNSearchLatestRequestBean> turSNSearchLatestRequestBean,
                        Principal principal) {
                if (principal == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                return new ResponseEntity<>(turSNSearchProcess.latestSearches(siteName, locale,
                                isLatestImpersonate(turSNSearchLatestRequestBean, principal), rows), HttpStatus.OK);
        }

        private String isLatestImpersonate(
                        Optional<TurSNSearchLatestRequestBean> turSNSearchLatestRequestBean,
                        Principal principal) {
                if (turSNSearchLatestRequestBean.isPresent()
                                && turSNSearchLatestRequestBean.get().getUserId() != null) {
                        return turSNSearchLatestRequestBean.get().getUserId();
                } else {
                        return principal.getName();
                }
        }
}
