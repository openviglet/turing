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
package com.viglet.turing.searchengine.solr;

import com.viglet.turing.searchengine.TurSEInstanceEndpoints;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Component;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import com.viglet.turing.searchengine.solr.source.TurSolrInstanceSource;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Alexandre Oliveira
 * @since 0.3.5
 */
@Component
@Slf4j
public class TurSolrInstanceProcess {

    private final com.viglet.turing.searchengine.TurSEInstanceEndpoints seInstanceEndpoints;
    private final ConcurrentHashMap<String, SolrClient> clientCache = new ConcurrentHashMap<>();
    private final TurSESiteSearchConfig siteSearchConfig;
    private final TurSolrInstanceSource solrInstanceSource;

    public TurSolrInstanceProcess(
            TurSESiteSearchConfig siteSearchConfig,
            TurSolrInstanceSource solrInstanceSource,
            com.viglet.turing.searchengine.TurSEInstanceEndpoints seInstanceEndpoints) {
        this.seInstanceEndpoints = seInstanceEndpoints;
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "connection");
        System.setProperty("jdk.httpclient.HttpClient.version", "HTTP_1_1");
        this.siteSearchConfig = siteSearchConfig;
        this.solrInstanceSource = solrInstanceSource;
    }

    private Optional<TurSolrInstance> getSolrClient(TurSEInstance turSEInstance, String core) {
        return clientFor(resolveEndpoint(turSEInstance != null ? turSEInstance.getEndpointUrl() : null), core);
    }

    private Optional<TurSolrInstance> clientFor(String endpoint, String core) {
        // T1247 — one URL, a node list served with load balancing and failover, or a
        // ZooKeeper ensemble whose cluster state the client follows.
        TurSolrEndpoints endpoints = TurSolrEndpoints.parse(endpoint);
        if (endpoints == null || !endpoints.isHttp()) {
            log.debug("Skipping Solr client init for non-HTTP endpoint: {}", endpoint);
            return Optional.empty();
        }
        SolrClient solrClient = clientCache.computeIfAbsent(endpoints.key(), key -> {
            log.info("Starting Solr connection pool for {}", key);
            return endpoints.createClient();
        });
        // T1247b — indexing on nodes of its own, when the endpoint names an update side.
        SolrClient updateClient = endpoints.hasUpdateSide()
                ? clientCache.computeIfAbsent(endpoints.key() + "#update", key -> {
                    log.info("Starting Solr update connection pool for {}", endpoints.update().key());
                    return endpoints.createUpdateClient();
                })
                : null;

        try {
            // A ZooKeeper endpoint's raw-HTTP calls go to a live node of the cluster.
            TurSolrInstance solrInstance = new TurSolrInstance(solrClient, updateClient,
                    URI.create(endpoints.baseUrl(solrClient)).toURL(), core);
            return Optional.of(solrInstance);
        } catch (MalformedURLException | IllegalStateException e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }

    public Optional<TurSolrInstance> initSolrInstance(String siteName, Locale locale) {
        // T1310 - the site's SE instance and core come through the site search SPI.
        return siteSearchConfig.siteCore(siteName, locale).flatMap(siteCore -> clientFor(
                resolveEndpoint(TurSEInstanceEndpoints.ofInstance(seInstanceEndpoints, siteCore.seInstanceId())),
                siteCore.core()));
    }

    public Optional<TurSolrInstance> initSolrInstance(TurSEInstance turSEInstance, String core) {
        return this.getSolrClient(turSEInstance, core);
    }

    public Optional<TurSolrInstance> initSolrInstance(TurSNSiteLocale turSNSiteLocale) {
        return this.getSolrClient(turSNSiteLocale.getTurSNSite(), turSNSiteLocale);

    }

    private Optional<TurSolrInstance> getSolrClient(TurSNSite turSNSite, TurSNSiteLocale turSNSiteLocale) {
        // T1337 — only the endpoint is needed, and it is cached by SE instance id.
        return clientFor(resolveEndpoint(TurSEInstanceEndpoints.ofInstance(seInstanceEndpoints,
                turSNSite == null ? null : turSNSite.getTurSEInstanceId())),
                turSNSiteLocale.getCore());
    }

    private String resolveEndpoint(String instanceEndpoint) {
        if (solrInstanceSource != null && solrInstanceSource.isReadOnly()) {
            String configured = solrInstanceSource.getConfiguredEndpoint();
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
        }
        return instanceEndpoint;
    }

    @PreDestroy
    public void shutdown() {
        clientCache.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                /* ignore */ }
        });
    }
}