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
package com.viglet.turing.solr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.stereotype.Component;

import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.persistence.repository.system.TurConfigVarRepository;
import com.viglet.turing.properties.TurConfigProperties;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Alexandre Oliveira
 * @since 0.3.5
 */
@Slf4j
@Component
public class TurSolrInstanceProcess {
    private final TurConfigVarRepository turConfigVarRepository;
    private final TurSEInstanceRepository turSEInstanceRepository;
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSolrCache turSolrCache;
    private final TurConfigProperties turConfigProperties;
    private final Map<String, HttpJdkSolrClient> solrClientCache;

    public TurSolrInstanceProcess(TurConfigVarRepository turConfigVarRepository,
            TurSEInstanceRepository turSEInstanceRepository,
            TurSNSiteLocaleRepository turSNSiteLocaleRepository,
            TurSNSiteRepository turSNSiteRepository,
            TurSolrCache turSolrCache,
            TurConfigProperties turConfigProperties,
            Map<String, HttpJdkSolrClient> solrClientCache) {

        this.turConfigVarRepository = turConfigVarRepository;
        this.turSEInstanceRepository = turSEInstanceRepository;
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSolrCache = turSolrCache;
        this.turConfigProperties = turConfigProperties;
        this.solrClientCache = solrClientCache;
    }

    private Optional<TurSolrInstance> getSolrClient(TurSNSite turSNSite, TurSNSiteLocale turSNSiteLocale) {
        return getSolrClient(turSNSite.getTurSEInstance(), turSNSiteLocale.getCore());
    }

    private Optional<TurSolrInstance> getSolrClient(TurSEInstance turSEInstance, String core) {
        String urlString = String.format("http://%s:%s/solr/%s",
                turSEInstance.getHost(),
                turSEInstance.getPort(),
                core);

        if (turSolrCache.isSolrCoreExists(urlString)) {
            HttpJdkSolrClient httpSolrClient = solrClientCache.computeIfAbsent(urlString,
                    url -> new HttpJdkSolrClient.Builder(url)
                            .withConnectionTimeout(turConfigProperties.getSolr().getTimeout(), TimeUnit.MILLISECONDS)
                            .withIdleTimeout(turConfigProperties.getSolr().getTimeout(), TimeUnit.MILLISECONDS)
                            .build());

            try {
                return Optional.of(
                        new TurSolrInstance(httpSolrClient, URI.create(urlString).toURL(), core));
            } catch (MalformedURLException e) {
                log.error(e.getMessage(), e);
            }
        }
        return Optional.empty();
    }

    @PreDestroy
    public void closeClients() {
        solrClientCache.values().forEach(client -> {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Error closing Solr client", e);
            }
        });
    }

    public Optional<TurSolrInstance> initSolrInstance(String siteName, Locale locale) {
        return turSNSiteRepository.findByName(siteName).flatMap(turSNSite -> this.initSolrInstance(turSNSite, locale));

    }

    private Optional<TurSolrInstance> initSolrInstance(TurSNSite turSNSite, Locale locale) {
        TurSNSiteLocale turSNSiteLocale = turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(turSNSite, locale);
        if (turSNSiteLocale != null) {
            return this.initSolrInstance(turSNSiteLocale);
        } else {
            log.warn("{} site with {} locale not found", turSNSite.getName(), locale);
            return Optional.empty();
        }
    }

    public Optional<TurSolrInstance> initSolrInstance(TurSEInstance turSEInstance, String core) {
        return this.getSolrClient(turSEInstance, core);
    }

    public Optional<TurSolrInstance> initSolrInstance(TurSNSiteLocale turSNSiteLocale) {
        return this.getSolrClient(turSNSiteLocale.getTurSNSite(), turSNSiteLocale);

    }

    public Optional<TurSolrInstance> initSolrInstance() {
        return turConfigVarRepository.findById("DEFAULT_SE")
                .flatMap(turConfigVar -> turSEInstanceRepository.findById(turConfigVar.getValue())
                        .map(turSEInstance -> getSolrClient(turSEInstance, "turing")))
                .orElse(turSEInstanceRepository.findAll().stream().findFirst()
                        .map(turSEInstance -> getSolrClient(turSEInstance, "turing")).orElse(null));
    }

}
