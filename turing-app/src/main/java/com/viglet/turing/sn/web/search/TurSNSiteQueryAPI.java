/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.web.search;

import org.apache.commons.lang3.LocaleUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.commons.sn.search.TurSNParamType;
import com.viglet.turing.searchengine.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sn/{siteName}/query")
@Tag(name = "Semantic Navigation DSL Query", description = "Semantic Navigation DSL Query API")
public class TurSNSiteQueryAPI {
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSearchEnginePluginFactory pluginFactory;

    public TurSNSiteQueryAPI(TurSNSiteRepository turSNSiteRepository,
            TurSearchEnginePluginFactory pluginFactory) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.pluginFactory = pluginFactory;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String turSNSiteSearchSelectGet(
            @PathVariable String siteName,
            @RequestParam(name = TurSNParamType.LOCALE) String localeRequest,
            @RequestBody String json) {
        return turSNSiteRepository.findByNameIgnoreCase(siteName)
                .flatMap(turSNSite -> pluginFactory.getPluginForSite(turSNSite)
                        .nativeJsonQuery(turSNSite, LocaleUtils.toLocale(localeRequest), json))
                .orElse("{}");
    }

}
