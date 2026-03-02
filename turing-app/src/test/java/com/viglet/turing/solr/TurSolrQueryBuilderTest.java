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

package com.viglet.turing.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;
import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingExpression;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.sn.facet.TurSNFacetTypeContext;
import com.viglet.turing.sn.tr.TurSNTargetingRules;

/**
 * Unit tests for TurSolrQueryBuilder.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSolrQueryBuilderTest {

        @Mock
        private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

        @Mock
        private TurSNRankingExpressionRepository turSNRankingExpressionRepository;

        @Mock
        private TurSNRankingConditionRepository turSNRankingConditionRepository;

        @Mock
        private TurSNTargetingRules turSNTargetingRules;

        private TurSolrQueryBuilder builder() {
                return new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNRankingExpressionRepository, turSNRankingConditionRepository,
                                turSNTargetingRules);
        }

        private TurSNSiteSearchContext contextFrom(TurSEParameters parameters,
                        TurSNSitePostParamsBean post) {
                TurSNConfig config = new TurSNConfig();
                config.setHlEnabled(true);
                return new TurSNSiteSearchContext("site", config, parameters, Locale.US,
                                URI.create("http://localhost/search"), post);
        }

        @Test
        void testHasGroupWhenGroupIsPresent() {
                TurSolrQueryBuilder builder = new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNRankingExpressionRepository, turSNRankingConditionRepository, turSNTargetingRules);
                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setGroup("group");
                TurSEParameters parameters = new TurSEParameters(
                                searchParams);

                assertThat(builder.hasGroup(parameters)).isTrue();
        }

        @Test
        void testHasGroupWhenGroupIsMissing() {
                TurSolrQueryBuilder builder = new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNRankingExpressionRepository, turSNRankingConditionRepository, turSNTargetingRules);
                TurSNSearchParams searchParams = new TurSNSearchParams();
                TurSEParameters parameters = new TurSEParameters(
                                searchParams);

                assertThat(builder.hasGroup(parameters)).isFalse();
        }

        @Test
        void testGetFqFieldsExtractsKeys() {
                TurSolrQueryBuilder builder = new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNRankingExpressionRepository, turSNRankingConditionRepository, turSNTargetingRules);
                TurSNFilterParams params = TurSNFilterParams.builder()
                                .defaultValues(List.of("category:books", "type:article"))
                                .and(List.of("author:john"))
                                .or(List.of("format:pdf"))
                                .build();

                List<String> keys = builder.getFqFields(params);

                assertThat(keys).containsExactly("category", "type", "author", "format");
        }

        @Test
        void testGetFacetTypeAndFacetItemTypeValuesUsesSiteDefaults() {
                TurSNSite site = new TurSNSite();
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.OR);
                TurSNFilterParams params = TurSNFilterParams.builder()
                                .operator(TurSNFilterQueryOperator.NONE)
                                .itemOperator(TurSNFilterQueryOperator.NONE)
                                .build();

                TurSNFacetTypeContext context = new TurSNFacetTypeContext(site, params);

                assertThat(TurSolrQueryBuilder.getFacetTypeAndFacetItemTypeValues(context))
                                .isEqualTo("AND-OR");
        }

        @Test
        void testGetFacetFieldsInFilterQueryFiltersEnabledFacets() {
                TurSNSite site = new TurSNSite();
                TurSNFilterParams params = TurSNFilterParams.builder()
                                .defaultValues(List.of("category:books", "other:1"))
                                .build();
                TurSNSiteFieldExt categoryFacet = TurSNSiteFieldExt.builder().name("category").build();
                TurSNSiteFieldExt typeFacet = TurSNSiteFieldExt.builder().name("type").build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabled(site, 1, 1))
                                .thenReturn(List.of(categoryFacet, typeFacet));

                TurSolrQueryBuilder builder = new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNRankingExpressionRepository, turSNRankingConditionRepository, turSNTargetingRules);
                TurSNFacetTypeContext context = new TurSNFacetTypeContext(site, params);

                List<String> fields = builder.getFacetFieldsInFilterQuery(context);

                assertThat(fields).containsExactly("category");
        }

        @Test
        void testPrepareQueryMLTConfiguresQueryWhenEnabled() {
                TurSNSite site = new TurSNSite();
                site.setMlt(1);
                TurSNSiteFieldExt mltField = TurSNSiteFieldExt.builder().name("body").build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndMltAndEnabled(site, 1, 1))
                                .thenReturn(List.of(mltField));

                TurSolrQueryBuilder builder = new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNRankingExpressionRepository, turSNRankingConditionRepository, turSNTargetingRules);
                SolrQuery query = new SolrQuery();

                List<TurSNSiteFieldExt> result = builder.prepareQueryMLT(site, query);

                assertThat(result).containsExactly(mltField);
                assertThat(query.getBool(MoreLikeThisParams.MLT)).isTrue();
                assertThat(query.get(MoreLikeThisParams.SIMILARITY_FIELDS)).isEqualTo("body");
        }

        @Test
        void testPrepareQueryMLTDoesNothingWhenDisabledOrNoFields() {
                TurSNSite site = new TurSNSite();
                site.setMlt(0);
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndMltAndEnabled(site, 1, 1))
                                .thenReturn(Collections.emptyList());

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> result = builder().prepareQueryMLT(site, query);

                assertThat(result).isEmpty();
                assertThat(query.get(MoreLikeThisParams.MLT)).isNull();
        }

        @Test
        void testPrepareQueryHLEnabledAndDisabled() {
                TurSNSite site = new TurSNSite();
                site.setHlPre("<mark>");
                site.setHlPost("</mark>");
                TurSNSiteFieldExt hlField = TurSNSiteFieldExt.builder().name("content").build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndHlAndEnabled(site, 1, 1))
                                .thenReturn(List.of(hlField));

                TurSNSearchParams searchParams = new TurSNSearchParams();
                TurSEParameters parameters = new TurSEParameters(searchParams);
                TurSNConfig config = new TurSNConfig();
                config.setHlEnabled(true);
                TurSNSiteSearchContext enabledContext = new TurSNSiteSearchContext("site", config,
                                parameters, Locale.US, URI.create("http://localhost/search"));

                SolrQuery enabledQuery = new SolrQuery();
                builder().prepareQueryHL(site, enabledQuery, enabledContext);

                assertThat(enabledQuery.getBool(HighlightParams.HIGHLIGHT)).isTrue();
                assertThat(enabledQuery.get(HighlightParams.FIELDS)).isEqualTo("content");

                config.setHlEnabled(false);
                TurSNSiteSearchContext disabledContext = new TurSNSiteSearchContext("site", config,
                                parameters, Locale.US, URI.create("http://localhost/search"));
                SolrQuery disabledQuery = new SolrQuery();
                builder().prepareQueryHL(site, disabledQuery, disabledContext);
                assertThat(disabledQuery.get(HighlightParams.FIELDS)).isNull();
        }

        @Test
        void testPrepareQueryFacetConfiguresFacetFields() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(5);
                site.setFacetSort(null);
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);

                TurSNSiteFieldExt facet = TurSNSiteFieldExt.builder()
                                .name("category")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldSortEnum.DEFAULT)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabled(site, 1, 1))
                                .thenReturn(List.of(facet));

                SolrQuery query = new SolrQuery();
                TurSNFilterParams params = TurSNFilterParams.builder().build();
                List<TurSNSiteFieldExt> facets = builder().prepareQueryFacet(site, query, params);

                assertThat(facets).hasSize(1);
                assertThat(query.getBool("facet")).isTrue();
                assertThat(query.get("facet.limit")).isEqualTo("5");
                assertThat(query.getFacetFields()).isNotEmpty();
        }

        @Test
        void testPrepareSolrQueryAppliesExactMatchAndTargetingRulesAndGroup() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(20);
                site.setExactMatch(1);
                site.setExactMatchField("title_exact");
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(turSNSiteFieldExtRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(Collections.emptyList());
                when(turSNRankingExpressionRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(Collections.<TurSNRankingExpression>emptySet());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("\"hello world\"");
                searchParams.setRows(-1);
                searchParams.setGroup("type");
                TurSNSitePostParamsBean post = new TurSNSitePostParamsBean();
                post.setTargetingRules(List.of("segment:vip"));

                TurSEParameters parameters = new TurSEParameters(searchParams, post);
                TurSESpellCheckResult spell = new TurSESpellCheckResult(false, "");

                when(turSNTargetingRules.ruleExpression(eq(com.viglet.turing.sn.tr.TurSNTargetingRuleMethod.AND),
                                eq(List.of("segment:vip"))))
                                .thenReturn("segment:\"vip\"");

                SolrQuery query = builder().prepareSolrQuery(contextFrom(parameters, post), site,
                                parameters, spell);

                assertThat(query.getQuery()).isEqualTo("title_exact:\"hello world\"");
                assertThat(query.get(GroupParams.GROUP)).isEqualTo("true");
                assertThat(query.get(GroupParams.GROUP_FIELD)).isEqualTo("type");
                assertThat(query.getFilterQueries()).contains("segment:\"vip\"");
                verify(turSNTargetingRules).ruleExpression(
                                com.viglet.turing.sn.tr.TurSNTargetingRuleMethod.AND,
                                List.of("segment:vip"));
        }

        @Test
        void testPrepareSolrQueryUsesCorrectedTextWhenAutoCorrectionEnabled() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(1);
                site.setSpellCheckFixes(1);
                site.setExactMatch(0);

                when(turSNSiteFieldExtRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(Collections.emptyList());
                when(turSNRankingExpressionRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(Collections.<TurSNRankingExpression>emptySet());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("helo");
                searchParams.setP(1);
                searchParams.setRows(10);
                searchParams.setNfpr(0);
                TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

                TurSESpellCheckResult spell = new TurSESpellCheckResult(true, "hello");

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(parameters, new TurSNSitePostParamsBean()), site,
                                parameters, spell);

                assertThat(query.getQuery()).isEqualTo("hello");
        }

        @Test
        void testPrepareSolrQueryTargetingRulesWithConditionBuildsFilterQuery() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);
                site.setExactMatch(0);

                when(turSNSiteFieldExtRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(Collections.emptyList());
                when(turSNRankingExpressionRepository.findByTurSNSite(any(), eq(site)))
                                .thenReturn(Collections.<TurSNRankingExpression>emptySet());
                when(turSNTargetingRules.andMethod(List.of("ruleA"))).thenReturn("ruleA");
                when(turSNTargetingRules.orMethod(List.of("ruleB"))).thenReturn("ruleB");

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("news");
                searchParams.setRows(10);
                TurSNSitePostParamsBean post = new TurSNSitePostParamsBean();
                post.setTargetingRulesWithCondition(new HashMap<>() {
                        {
                                put("user:1", List.of("ruleA"));
                        }
                });
                post.setTargetingRulesWithConditionOR(new HashMap<>() {
                        {
                                put("user:1", List.of("ruleB"));
                        }
                });

                TurSEParameters parameters = new TurSEParameters(searchParams, post);

                SolrQuery query = builder().prepareSolrQuery(contextFrom(parameters, post), site,
                                parameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getFilterQueries()).isNotEmpty();
                assertThat(String.join(" ", query.getFilterQueries())).contains("user:1");
        }
}
