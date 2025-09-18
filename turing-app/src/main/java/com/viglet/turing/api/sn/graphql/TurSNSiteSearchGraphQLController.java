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

package com.viglet.turing.api.sn.graphql;

import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchBean;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.TurSNSearchProcess;

import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL Controller for Semantic Navigation Search API.
 * 
 * @author Alexandre Oliveira
 * @since 0.3.6
 */
@Slf4j
@Controller
public class TurSNSiteSearchGraphQLController {

    private final TurSNSearchProcess turSNSearchProcess;
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    public TurSNSiteSearchGraphQLController(TurSNSearchProcess turSNSearchProcess,
            TurSNSiteRepository turSNSiteRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.turSNSearchProcess = turSNSearchProcess;
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    @QueryMapping
    public TurSNSiteSearchBean siteSearch(@Argument String siteName,
            @Argument TurSNSearchParamsInput searchParams,
            @Argument String locale) {

        log.debug("GraphQL siteSearch called with siteName: {}, params: {}, locale: {}", 
                  siteName, searchParams, locale);

        // Convert GraphQL input to internal search params
        TurSNSearchParams turSNSearchParams = convertToTurSNSearchParams(searchParams, locale);
        
        // Get locale, prioritizing the direct locale parameter over searchParams locale
        String localeToUse = StringUtils.isNotBlank(locale) ? locale : turSNSearchParams.getLocale();
        if (StringUtils.isBlank(localeToUse)) {
            localeToUse = "en"; // Default locale
        }
        
        Locale localeObj = LocaleUtils.toLocale(localeToUse);
        
        // Check if site exists and has the specified language
        if (!turSNSearchProcess.existsByTurSNSiteAndLanguage(siteName, localeObj)) {
            log.warn("Site {} with locale {} not found or not supported", siteName, localeToUse);
            return new TurSNSiteSearchBean();
        }

        return turSNSiteRepository.findByName(siteName)
                .map(site -> {
                    TurSNSiteSearchContext turSNSiteSearchContext = getTurSNSiteSearchContext(
                            siteName, turSNSearchParams, localeObj, site);
                    return turSNSearchProcess.search(turSNSiteSearchContext);
                })
                .orElse(new TurSNSiteSearchBean());
    }

    private TurSNSearchParams convertToTurSNSearchParams(TurSNSearchParamsInput input, String locale) {
        TurSNSearchParams params = new TurSNSearchParams();
        
        if (input != null) {
            params.setQ(StringUtils.defaultIfBlank(input.getQ(), "*"));
            params.setP(input.getP() != null ? input.getP() : 1);
            params.setRows(input.getRows() != null ? input.getRows() : -1);
            params.setSort(StringUtils.defaultIfBlank(input.getSort(), "relevance"));
            params.setGroup(input.getGroup());
            params.setNfpr(input.getNfpr() != null ? input.getNfpr() : 1);
            params.setFq(input.getFq());
            params.setFqAnd(input.getFqAnd());
            params.setFqOr(input.getFqOr());
            params.setFl(input.getFl());
            
            // Convert String operators to enum values
            if (StringUtils.isNotBlank(input.getFqOp())) {
                try {
                    params.setFqOp(TurSNFilterQueryOperator.valueOf(input.getFqOp().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    params.setFqOp(TurSNFilterQueryOperator.NONE);
                }
            }
            
            if (StringUtils.isNotBlank(input.getFqiOp())) {
                try {
                    params.setFqiOp(TurSNFilterQueryOperator.valueOf(input.getFqiOp().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    params.setFqiOp(TurSNFilterQueryOperator.NONE);
                }
            }
            
            // Set locale from input if available, otherwise use the direct locale parameter
            String inputLocale = StringUtils.isNotBlank(input.getLocale()) ? input.getLocale() : locale;
            params.setLocale(inputLocale);
        }
        
        return params;
    }

    private TurSNSiteSearchContext getTurSNSiteSearchContext(String siteName,
            TurSNSearchParams turSNSearchParams, Locale locale, TurSNSite site) {
        
        TurSNConfig turSNConfig = getTurSNConfig(site);
        
        // Create a basic search context - simplified version of the original method
        TurSNSiteSearchContext context = new TurSNSiteSearchContext();
        context.setSiteName(siteName);
        context.setLocale(locale);
        context.setTurSNConfig(turSNConfig);
        context.setTurSNSearchParams(turSNSearchParams);
        
        return context;
    }

    private TurSNConfig getTurSNConfig(TurSNSite turSNSite) {
        TurSNConfig turSNConfig = new TurSNConfig();
        turSNConfig.setHlEnabled(turSNSite.getHl() == 1 && hasSiteHLFields(turSNSite));
        return turSNConfig;
    }

    private boolean hasSiteHLFields(TurSNSite turSNSite) {
        return !CollectionUtils.isEmpty(turSNSiteFieldExtRepository
                .findByTurSNSiteAndHlAndEnabled(turSNSite, 1, 1));
    }
}