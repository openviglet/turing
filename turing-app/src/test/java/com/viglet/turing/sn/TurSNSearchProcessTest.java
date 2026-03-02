/*
 * Copyright (C) 2016-2025 the original author or authors.
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

package com.viglet.turing.sn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.spotlight.TurSNSpotlightProcess;
import com.viglet.turing.solr.TurSolrInstanceProcess;
import com.viglet.turing.solr.TurSolrQueryBuilder;

/**
 * Unit tests for TurSNSearchProcess.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSNSearchProcessTest {

        @Mock
        private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

        @Mock
        private TurSNSiteFieldExtFacetRepository turSNSiteFieldExtFacetRepository;

        @Mock
        private TurSNSiteRepository turSNSiteRepository;

        @Mock
        private TurSNSiteLocaleRepository turSNSiteLocaleRepository;

        @Mock
        private TurSolrInstanceProcess turSolrInstanceProcess;

        @Mock
        private TurSNSpotlightProcess turSNSpotlightProcess;

        @Mock
        private TurSNSiteMetricAccessRepository turSNSiteMetricAccessRepository;

        @Mock
        private TurSearchEnginePluginFactory searchEnginePluginFactory;

        @Mock
        private TurSolrQueryBuilder turSolrQueryBuilder;

        @Test
        void testGetSNSiteReturnsOptional() {
                TurSNSite site = new TurSNSite();
                site.setName("site");
                when(turSNSiteRepository.findByName("site")).thenReturn(Optional.of(site));

                TurSNSearchProcess process = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                false, searchEnginePluginFactory, turSolrQueryBuilder);

                assertThat(process.getSNSite("site")).contains(site);
        }

        @Test
        void testExistsByTurSNSiteAndLanguage() {
                TurSNSite site = new TurSNSite();
                when(turSNSiteRepository.findByName("site")).thenReturn(Optional.of(site));
                when(turSNSiteLocaleRepository.existsByTurSNSiteAndLanguage(site, Locale.US))
                                .thenReturn(true);

                TurSNSearchProcess process = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                false, searchEnginePluginFactory, turSolrQueryBuilder);

                assertThat(process.existsByTurSNSiteAndLanguage("site", Locale.US)).isTrue();
        }

        @Test
        void testLatestSearchesReturnsTerms() {
                TurSNSite site = new TurSNSite();
                when(turSNSiteRepository.findByName("site")).thenReturn(Optional.of(site));
                TurSNSiteMetricAccessTerm term1 = new TurSNSiteMetricAccessTerm("one", Instant.now());
                TurSNSiteMetricAccessTerm term2 = new TurSNSiteMetricAccessTerm("two", Instant.now());
                when(turSNSiteMetricAccessRepository.findLatestSearches(eq(site), eq("en"), eq("user"),
                                any(PageRequest.class))).thenReturn(List.of(term1, term2));

                TurSNSearchProcess process = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                false, searchEnginePluginFactory, turSolrQueryBuilder);

                List<String> results = process.latestSearches("site", "en", "user", 10);

                assertThat(results).containsExactly("one", "two");
        }

        @Test
        void testRequestTargetingRulesFormatsValues() {
                TurSNSearchProcess process = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                true, searchEnginePluginFactory, turSolrQueryBuilder);

                List<String> result = process.requestTargetingRules(
                                List.of("segment:vip", "price:[10 TO 20]", "rawRule"));

                assertThat(result).contains("segment:\"vip\"");
                assertThat(result).contains("rawRule");
                assertThat(result).doesNotContain("price:[10 TO 20]");
        }

        @Test
        void testResponseLocalesBuildsLocaleLinks() {
                TurSNSearchProcess process = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                true, searchEnginePluginFactory, turSolrQueryBuilder);
                TurSNSite site = new TurSNSite();

                TurSNSiteLocale localeEn = new TurSNSiteLocale();
                localeEn.setLanguage(Locale.US);
                TurSNSiteLocale localePt = new TurSNSiteLocale();
                localePt.setLanguage(new Locale("pt", "BR"));

                when(turSNSiteLocaleRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(List.of(localeEn, localePt));

                var locales = process.responseLocales(site, URI.create("http://localhost/search?q=test"));

                assertThat(locales).hasSize(2);
                assertThat(locales.get(0).getLink()).contains("locale=");
                assertThat(locales.get(1).getLink()).contains("locale=");
        }

        @Test
        void testPopulateMetricsSavesWhenEnabledAndQueryIsNotWildcard() {
                TurSNSearchProcess process = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                true, searchEnginePluginFactory, turSolrQueryBuilder);

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("java");
                searchParams.setLocale(Locale.US);
                TurSNSitePostParamsBean post = new TurSNSitePostParamsBean();
                post.setUserId("user-1");
                post.setTargetingRules(List.of("segment:vip"));
                post.setPopulateMetrics(true);

                TurSNSiteSearchContext context = new TurSNSiteSearchContext("site", new TurSNConfig(),
                                new TurSEParameters(searchParams, post), Locale.US,
                                URI.create("http://localhost/search?q=java"), post);

                process.populateMetrics(new TurSNSite(), context, 3);

                verify(turSNSiteMetricAccessRepository).save(any());
        }

        @Test
        void testPopulateMetricsSkipsWhenDisabledOrWildcard() {
                TurSNSearchProcess processDisabled = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                false, searchEnginePluginFactory, turSolrQueryBuilder);

                TurSNSearchParams searchParamsWildcard = new TurSNSearchParams();
                searchParamsWildcard.setQ("*");
                TurSNSitePostParamsBean post = new TurSNSitePostParamsBean();
                post.setPopulateMetrics(true);
                TurSNSiteSearchContext contextWildcard = new TurSNSiteSearchContext("site", new TurSNConfig(),
                                new TurSEParameters(searchParamsWildcard, post), Locale.US,
                                URI.create("http://localhost/search?q=*"), post);

                processDisabled.populateMetrics(new TurSNSite(), contextWildcard, 0);

                verify(turSNSiteMetricAccessRepository, never()).save(any());

                TurSNSearchProcess processEnabled = new TurSNSearchProcess(turSNSiteFieldExtRepository,
                                turSNSiteFieldExtFacetRepository, turSNSiteRepository, turSNSiteLocaleRepository,
                                turSolrInstanceProcess, turSNSpotlightProcess, turSNSiteMetricAccessRepository,
                                true, searchEnginePluginFactory, turSolrQueryBuilder);

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("java");
                TurSNSitePostParamsBean postNoMetrics = new TurSNSitePostParamsBean();
                postNoMetrics.setPopulateMetrics(false);
                TurSNSiteSearchContext contextNoMetrics = new TurSNSiteSearchContext("site", new TurSNConfig(),
                                new TurSEParameters(searchParams, postNoMetrics), Locale.US,
                                URI.create("http://localhost/search?q=java"), postNoMetrics);

                processEnabled.populateMetrics(new TurSNSite(), contextNoMetrics, 0);

                verify(turSNSiteMetricAccessRepository, never()).save(any());
        }
}
