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

package com.viglet.turing.api.sn.console;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.sn.bean.TurSNFieldExtCheck;
import com.viglet.turing.api.sn.bean.TurSolrCoreExists;
import com.viglet.turing.api.sn.bean.TurSolrFieldCore;
import com.viglet.turing.api.sn.bean.TurSolrFieldStatus;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;
import com.viglet.turing.plugins.se.TurSEFieldSchema;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.spring.utils.TurPersistenceUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sn/{siteId}/field/check")
@Tag(name = "Semantic Navigation Field Check Ext", description = "Semantic Navigation Field Check Ext API")
public class TurSNSiteFieldCheckAPI {
        private final TurSNSiteRepository turSNSiteRepository;
        private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
        private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
        private final TurSearchEnginePluginFactory pluginFactory;

        public TurSNSiteFieldCheckAPI(TurSNSiteRepository turSNSiteRepository,
                        TurSNSiteFieldExtRepository turSNSiteFieldExtRepository,
                        TurSNSiteLocaleRepository turSNSiteLocaleRepository,
                        TurSearchEnginePluginFactory pluginFactory) {
                this.turSNSiteRepository = turSNSiteRepository;
                this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
                this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
                this.pluginFactory = pluginFactory;
        }

        @Operation(summary = "Semantic Navigation Site Field Ext Check List")
        @GetMapping
        public TurSNFieldExtCheck turSNSiteFieldExtCheckList(@PathVariable String siteId) {
                return turSNSiteRepository.findById(siteId).map(turSNSite -> TurSNFieldExtCheck.builder()
                                .cores(coresExist(turSNSite))
                                .fields(fieldsCheck(turSNSite))
                                .build())
                                .orElse(TurSNFieldExtCheck.builder().build());
        }

        private List<TurSolrFieldStatus> fieldsCheck(TurSNSite turSNSite) {
                List<TurSolrFieldStatus> fieldsExist = new ArrayList<>();
                TurSEInstance turSEInstance = turSNSite.getTurSEInstance();
                List<TurSNSiteLocale> siteLocales = turSNSiteLocaleRepository.findByTurSNSite(turSNSite);
                // T1243 — the site's engine answers, not Solr directly.
                TurSearchEnginePlugin plugin = pluginFactory.getPluginForSite(turSNSite);

                turSNSiteFieldExtRepository
                                .findByTurSNSite(TurPersistenceUtils.orderByNameIgnoreCase(), turSNSite)
                                .forEach(turSNSiteFieldExt -> fieldsExist.add(validateField(turSNSiteFieldExt,
                                                plugin, turSEInstance, siteLocales)));

                return fieldsExist;
        }

        private TurSolrFieldStatus validateField(TurSNSiteFieldExt turSNSiteFieldExt, TurSearchEnginePlugin plugin,
                        TurSEInstance turSEInstance, List<TurSNSiteLocale> siteLocales) {
                List<TurSolrFieldCore> cores = new ArrayList<>();
                boolean allCoresValid = true;

                for (TurSNSiteLocale locale : siteLocales) {
                        TurSolrFieldCore coreStatus = validateFieldInCore(turSNSiteFieldExt, locale, plugin,
                                        turSEInstance);
                        cores.add(coreStatus);
                        if (!coreStatus.isCorrect()) {
                                allCoresValid = false;
                        }
                }

                return TurSolrFieldStatus.builder()
                                .id(turSNSiteFieldExt.getId())
                                .externalId(turSNSiteFieldExt.getExternalId())
                                .name(turSNSiteFieldExt.getName())
                                .facetIsCorrect(isFacetConfigValid(turSNSiteFieldExt))
                                .cores(cores)
                                .correct(allCoresValid)
                                .build();
        }

        private TurSolrFieldCore validateFieldInCore(TurSNSiteFieldExt turSNSiteFieldExt,
                        TurSNSiteLocale locale, TurSearchEnginePlugin plugin, TurSEInstance turSEInstance) {
                String coreName = locale.getCore();

                // Engines without a schema (Lucene, Elasticsearch): fields are always valid
                if (!plugin.hasFieldSchema()) {
                        return TurSolrFieldCore.builder()
                                        .name(coreName)
                                        .exists(true)
                                        .typeIsCorrect(true)
                                        .multiValuedIsCorrect(true)
                                        .correct(true)
                                        .build();
                }

                return plugin.fieldSchema(turSEInstance, coreName, turSNSiteFieldExt.getName())
                                .map(schema -> buildFieldCore(coreName, turSNSiteFieldExt, schema))
                                .orElseGet(() -> buildMissingFieldCore(coreName));
        }

        private TurSolrFieldCore buildFieldCore(String coreName, TurSNSiteFieldExt turSNSiteFieldExt,
                        TurSEFieldSchema schema) {
                boolean multiValuedIsCorrect = schema.multiValued() == (turSNSiteFieldExt.getMultiValued() == 1);
                boolean typeIsCorrect = schema.hasType(turSNSiteFieldExt.getType());
                boolean isCorrect = multiValuedIsCorrect && typeIsCorrect;

                return TurSolrFieldCore.builder()
                                .name(coreName)
                                .exists(true)
                                .type(schema.engineType())
                                .typeIsCorrect(typeIsCorrect)
                                .multiValued(schema.multiValued())
                                .multiValuedIsCorrect(multiValuedIsCorrect)
                                .correct(isCorrect)
                                .build();
        }

        private TurSolrFieldCore buildMissingFieldCore(String coreName) {
                return TurSolrFieldCore.builder()
                                .name(coreName)
                                .exists(false)
                                .typeIsCorrect(false)
                                .multiValuedIsCorrect(false)
                                .correct(false)
                                .build();
        }

        private boolean isFacetConfigValid(TurSNSiteFieldExt turSNSiteFieldExt) {
                return turSNSiteFieldExt.getFacet() != 1 || !turSNSiteFieldExt.getType().equals(TurSEFieldType.TEXT);
        }
        private List<TurSolrCoreExists> coresExist(TurSNSite turSNSite) {
                List<TurSolrCoreExists> coresExist = new ArrayList<>();
                var plugin = pluginFactory.getPluginForSite(turSNSite);
                turSNSiteLocaleRepository.findByTurSNSite(turSNSite)
                                .forEach(turSNSiteLocale -> coresExist.add(TurSolrCoreExists.builder()
                                                .name(turSNSiteLocale.getCore())
                                                .exists(plugin.indexExists(turSNSite.getTurSEInstance(),
                                                                turSNSiteLocale.getCore()))
                                                .build()));
                return coresExist;
        }
}
