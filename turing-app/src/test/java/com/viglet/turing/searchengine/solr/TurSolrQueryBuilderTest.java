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

package com.viglet.turing.searchengine.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.solr.client.solrj.request.SolrQuery;
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
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.TurSNSiteFacetRangeEnum;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacet;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetItem;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFacetFieldSortEnum;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingCondition;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingExpression;
import com.viglet.turing.sn.api.TurSNSiteFieldExts;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.sn.TurSNFieldType;
import com.viglet.turing.sn.facet.TurSNFacetTypeContext;

/**
 * Tests for TurSolrQueryBuilder.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@ExtendWith(MockitoExtension.class)
class TurSolrQueryBuilderTest {

        @Mock
        private TurSNSiteFieldExts turSNSiteFieldExtRepository;

        @Mock
        private TurSESiteSearchConfig siteSearchConfig;

        @Mock
        private TurSNTargetingRules turSNTargetingRules;

        @Mock
        private com.viglet.turing.sn.api.TurSNSiteCustomFacets turSNSiteCustomFacetRepository;



        @Mock
        private com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshotService snapshotService;

        private TurSolrQueryBuilder builder() {
                return new TurSolrQueryBuilder(turSNSiteFieldExtRepository,
                                turSNSiteCustomFacetRepository,
                                siteSearchConfig,
                                turSNTargetingRules, new com.viglet.turing.sn.facet.TurSNEnabledFacets(turSNSiteFieldExtRepository, turSNSiteCustomFacetRepository, snapshotService));
        }

        private static TurSERankingExpression toSpi(TurSNRankingExpression expression,
                        TurSNRankingCondition... conditions) {
                return new TurSERankingExpression(expression.getName(), expression.getWeight(),
                                java.util.Arrays.stream(conditions)
                                                .map(c -> new TurSERankingCondition(c.getAttribute(),
                                                                switch (c.getCondition()) {
                                                                        case TurSNRankingCondition.IS_NOT -> TurSERankingCondition.Kind.IS_NOT;
                                                                        case TurSNRankingCondition.EXISTS -> TurSERankingCondition.Kind.EXISTS;
                                                                        default -> TurSERankingCondition.Kind.IS;
                                                                }, c.getValue()))
                                                .toList());
        }

        private static java.util.Map<String, com.viglet.turing.commons.se.field.TurSEFieldType> fieldTypes(
                        java.util.Collection<TurSNSiteFieldExt> fields) {
                java.util.Map<String, com.viglet.turing.commons.se.field.TurSEFieldType> types = new java.util.LinkedHashMap<>();
                fields.stream().filter(field -> field.getName() != null && field.getType() != null)
                                .forEach(field -> types.putIfAbsent(field.getName(), field.getType()));
                return types;
        }

        private TurSNSiteSearchContext contextFrom(TurSEParameters parameters,
                        TurSNSitePostParamsBean post) {
                TurSNConfig config = new TurSNConfig();
                config.setHlEnabled(true);
                return new TurSNSiteSearchContext("site", config, parameters, Locale.US,
                                URI.create("http://localhost/search"), post);
        }

        @Test
        void testPrepareQueryMLTConfiguresQueryWhenEnabled() {
                TurSNSite site = new TurSNSite();
                site.setMlt(1);
                TurSNSiteFieldExt mltField = TurSNSiteFieldExt.builder().name("body").build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndMltAndEnabled(site, 1, 1))
                                .thenReturn(List.of(mltField));

                TurSolrQueryBuilder builder = new TurSolrQueryBuilder(turSNSiteFieldExtRepository, turSNSiteCustomFacetRepository,
                                siteSearchConfig, turSNTargetingRules, new com.viglet.turing.sn.facet.TurSNEnabledFacets(turSNSiteFieldExtRepository, turSNSiteCustomFacetRepository, snapshotService));
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
                                .facet(1)
                                .enabled(1)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(com.viglet.turing.sn.persistence.model.field.TurSNSiteFacetFieldSortEnum.DEFAULT)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(facet));

                SolrQuery query = new SolrQuery();
                TurSNFilterParams params = TurSNFilterParams.builder().build();
                List<TurSNSiteFieldExt> facets = builder().prepareQueryFacet(site, query, params, (com.viglet.turing.commons.se.TurSEParameters) null);

                assertThat(facets).hasSize(1);
                assertThat(query.getBool("facet")).isTrue();
                assertThat(query.get("facet.limit")).isEqualTo("5");
                assertThat(query.getFacetFields()).isNotEmpty();
        }

        @Test
        void testPrepareQueryFacetIncludesCustomFacetWhenBaseFieldIsNotFacet() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(5);
                site.setFacetSort(null);

                TurSNSiteCustomFacetItem item = TurSNSiteCustomFacetItem.builder()
                                .label("101 - 500")
                                .rangeStart(new BigDecimal("101"))
                                .rangeEnd(new BigDecimal("500"))
                                .build();
                TurSNSiteCustomFacet customFacet = TurSNSiteCustomFacet.builder()
                                .name("price_range")
                                .items(new java.util.HashSet<>(List.of(item)))
                                .build();

                TurSNSiteFieldExt nonFacetField = TurSNSiteFieldExt.builder()
                                .id("field-1")
                                .name("id")
                                .facet(0)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();
                customFacet.setTurSNSiteFieldExt(nonFacetField);

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(nonFacetField));
                when(turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(any()))
                                .thenReturn(List.of(customFacet));

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> facets = builder().prepareQueryFacet(site, query,
                                TurSNFilterParams.builder().build(), (com.viglet.turing.commons.se.TurSEParameters) null);

                assertThat(facets).hasSize(1);
                assertThat(query.getFacetQuery()).isNotEmpty();
        }

        @Test
        void testPrepareQueryFacetSelectedCustomFacetUsesItsOwnNameForOrAndExclusion() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(5);
                site.setFacetSort(null);
                site.setFacetType(TurSNSiteFacetFieldEnum.OR);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);

                TurSNSiteCustomFacetItem item = TurSNSiteCustomFacetItem.builder()
                                .label("101 - 500")
                                .rangeStart(new BigDecimal("101"))
                                .rangeEnd(new BigDecimal("500"))
                                .build();

                TurSNSiteFieldExt nonFacetField = TurSNSiteFieldExt.builder()
                                .id("field-1")
                                .name("id")
                                .facet(0)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();
                TurSNSiteCustomFacet customFacet = TurSNSiteCustomFacet.builder()
                                .name("price_range")
                                .items(new java.util.HashSet<>(List.of(item)))
                                .turSNSiteFieldExt(nonFacetField)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(nonFacetField));
                when(turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(any()))
                                .thenReturn(List.of(customFacet));

                SolrQuery query = new SolrQuery();
                TurSNFilterParams params = TurSNFilterParams.builder()
                                .defaultValues(List.of("price_range:101 - 500"))
                                .operator(TurSNFilterQueryOperator.NONE)
                                .itemOperator(TurSNFilterQueryOperator.NONE)
                                .build();

                builder().prepareQueryFacet(site, query, params, (com.viglet.turing.commons.se.TurSEParameters) null);

                assertThat(query.getFacetQuery()).isNotEmpty();
                assertThat(List.of(query.getFacetQuery()))
                                .allMatch(facetQuery -> !facetQuery.contains("ex=_all_"));
        }

        @Test
        void testPrepareSolrQueryUsesCustomFacetSpecificOperatorsInsteadOfFieldOrGlobal() {
                TurSNSite site = new TurSNSite();
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNSiteCustomFacetItem item1 = TurSNSiteCustomFacetItem.builder()
                                .label("1 - 10")
                                .rangeStart(new BigDecimal("1"))
                                .rangeEnd(new BigDecimal("10"))
                                .build();
                TurSNSiteCustomFacetItem item2 = TurSNSiteCustomFacetItem.builder()
                                .label("11 - 20")
                                .rangeStart(new BigDecimal("11"))
                                .rangeEnd(new BigDecimal("20"))
                                .build();

                TurSNSiteCustomFacet customFacet = TurSNSiteCustomFacet.builder()
                                .name("price_range")
                                .facetType(TurSNSiteFacetFieldEnum.OR)
                                .facetItemType(TurSNSiteFacetFieldEnum.OR)
                                .items(new java.util.HashSet<>(List.of(item1, item2)))
                                .build();

                TurSNSiteFieldExt field = TurSNSiteFieldExt.builder()
                                .id("field-1")
                                .name("id")
                                .facetType(TurSNSiteFacetFieldEnum.AND)
                                .facetItemType(TurSNSiteFacetFieldEnum.AND)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();
                customFacet.setTurSNSiteFieldExt(field);

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(field));
                when(turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(any()))
                                .thenReturn(List.of(customFacet));
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledAndType(site, 1, 1,
                                com.viglet.turing.commons.se.field.TurSEFieldType.DATE))
                                .thenReturn(Collections.emptyList());
                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                params.setFq(List.of("price_range:1 - 10", "price_range:11 - 20"));
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getFilterQueries())
                                .hasSize(1)
                                .as("Filter query should contain OR operator")
                                .allMatch(fq -> fq.contains(" OR "))
                                .as("Filter query should contain range 1 TO 10")
                                .allMatch(fq -> fq.contains("id:[1 TO 10]"))
                                .as("Filter query should contain range 11 TO 20")
                                .allMatch(fq -> fq.contains("id:[11 TO 20]"));
        }

        @Test
        void testPrepareSolrQueryAppliesExactMatchAndTargetingRulesAndGroup() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(20);
                site.setExactMatch(1);
                site.setExactMatchField("title_exact");
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("\"hello world\"");
                searchParams.setRows(-1);
                searchParams.setGroup("type");
                TurSNSitePostParamsBean post = new TurSNSitePostParamsBean();
                post.setTargetingRules(List.of("segment:vip"));

                TurSEParameters parameters = new TurSEParameters(searchParams, post);
                TurSESpellCheckResult spell = new TurSESpellCheckResult(false, "");

                when(turSNTargetingRules.ruleExpression(com.viglet.turing.searchengine.solr.TurSNTargetingRuleMethod.AND,
                                List.of("segment:vip")))
                                .thenReturn("segment:\"vip\"");

                SolrQuery query = builder().prepareSolrQuery(contextFrom(parameters, post), site,
                                parameters, spell);

                assertThat(query.getQuery()).isEqualTo("title_exact:\"hello world\"");
                assertThat(query.get(GroupParams.GROUP)).isEqualTo("true");
                assertThat(query.get(GroupParams.GROUP_FIELD)).isEqualTo("type");
                assertThat(query.getFilterQueries()).contains("segment:\"vip\"");
                verify(turSNTargetingRules).ruleExpression(
                                com.viglet.turing.searchengine.solr.TurSNTargetingRuleMethod.AND,
                                List.of("segment:vip"));
        }

        @Test
        void testPrepareSolrQueryUsesCorrectedTextWhenAutoCorrectionEnabled() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(1);
                site.setSpellCheckFixes(1);
                site.setExactMatch(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

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

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());
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

        @Test
        void testPrepareSolrQueryAppliesSortAndDefaultRowsWhenNegative() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(0);
                site.setDefaultDateField("publishedDate");
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams firstParams = new TurSNSearchParams();
                firstParams.setQ("java");
                firstParams.setRows(-1);
                firstParams.setSort("createdAt:asc");
                TurSEParameters first = new TurSEParameters(firstParams, new TurSNSitePostParamsBean());

                SolrQuery firstQuery = builder().prepareSolrQuery(
                                contextFrom(first, new TurSNSitePostParamsBean()), site,
                                first, new TurSESpellCheckResult(false, ""));

                assertThat(first.getRows()).isEqualTo(10);
                assertThat(firstQuery.getSortField()).contains("createdAt asc");

                TurSNSearchParams newestParams = new TurSNSearchParams();
                newestParams.setQ("java");
                newestParams.setRows(10);
                newestParams.setSort("newest");
                TurSEParameters newest = new TurSEParameters(newestParams, new TurSNSitePostParamsBean());

                SolrQuery newestQuery = builder().prepareSolrQuery(
                                contextFrom(newest, new TurSNSitePostParamsBean()), site,
                                newest, new TurSESpellCheckResult(false, ""));

                assertThat(newestQuery.getSortField()).contains("publishedDate desc");

                TurSNSearchParams oldestParams = new TurSNSearchParams();
                oldestParams.setQ("java");
                oldestParams.setRows(10);
                oldestParams.setSort("oldest");
                TurSEParameters oldest = new TurSEParameters(oldestParams, new TurSNSitePostParamsBean());

                SolrQuery oldestQuery = builder().prepareSolrQuery(
                                contextFrom(oldest, new TurSNSitePostParamsBean()), site,
                                oldest, new TurSESpellCheckResult(false, ""));

                assertThat(oldestQuery.getSortField()).contains("publishedDate asc");
        }

        @Test
        void testPrepareSolrQueryFormatsDateRangeAndUnknownFacetInFilterQuery() {
                TurSNSite site = new TurSNSite();
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNSiteFieldExt enabledDate = TurSNSiteFieldExt.builder()
                                .name("publishDate")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE)
                                .facetRange(TurSNSiteFacetRangeEnum.MONTH)
                                .build();
                TurSNSiteFieldExt enabledCategory = TurSNSiteFieldExt.builder()
                                .name("category")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(enabledDate, enabledCategory));
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledAndType(site, 1, 1,
                                com.viglet.turing.commons.se.field.TurSEFieldType.DATE))
                                .thenReturn(List.of(enabledDate));
                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                params.setFq(List.of("publishDate:2024-01-01T00:00:00Z", "category:books", "other:value"));
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getFilterQueries()).hasSize(1);
                String fq = query.getFilterQueries()[0];
                assertThat(fq)
                                .contains("publishDate:[ 2024-01-01T00:00:00Z TO ")
                                .contains("category:\"books\"")
                                .contains("other:\"value\"");
        }

        @Test
        void testPrepareSolrQueryAddsBoostQueryIncludingRecentDatesExpression() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNSiteFieldExt dateField = TurSNSiteFieldExt.builder()
                                .name("published")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE)
                                .build();
                TurSNSiteFieldExt typeField = TurSNSiteFieldExt.builder()
                                .name("type")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();

                TurSNRankingExpression expression = new TurSNRankingExpression();
                expression.setId("expr-1");
                expression.setWeight(5.0f);

                TurSNRankingCondition dateCondition = new TurSNRankingCondition();
                dateCondition.setAttribute("published");
                dateCondition.setValue("asc");
                TurSNRankingCondition typeCondition = new TurSNRankingCondition();
                typeCondition.setAttribute("type");
                typeCondition.setValue("article");

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(List.of(dateField, typeField)));
                when(siteSearchConfig.rankingExpressions(any()))
                                .thenReturn(List.of(toSpi(expression, dateCondition, typeCondition)));

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getParams("bq")).isNotNull();
                assertThat(String.join(" ", query.getParams("bq"))).contains("_query_:");
                assertThat(String.join(" ", query.getParams("bq"))).contains("type:\"article\"");
        }

        @Test
        void testPrepareSolrQueryBoostsUpcomingDatesForward() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNRankingExpression expression = new TurSNRankingExpression();
                expression.setId("expr-upcoming");
                expression.setWeight(4.0f);
                TurSNRankingCondition upcoming = new TurSNRankingCondition();
                upcoming.setAttribute("eventEnd");
                upcoming.setValue("upcoming");

                when(siteSearchConfig.fieldTypes(any())).thenReturn(fieldTypes(List.of(
                                TurSNSiteFieldExt.builder().name("eventEnd")
                                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE).build())));
                when(siteSearchConfig.rankingExpressions(any()))
                                .thenReturn(List.of(toSpi(expression, upcoming)));

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                // T1219 — today or later on the recency curve, nearest most; the past scores 0.
                assertThat(query.getParams("bq")).containsExactly(
                                "(_query_:\"{!func}if(gte(ms(eventEnd,NOW/DAY),0),"
                                                + "recip(ms(eventEnd,NOW/DAY),3.16e-11,1,1),0)\")^4.0");
        }

        @Test
        void testPrepareSolrQueryBoostQueryNegatesIsNotCondition() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNRankingExpression expression = new TurSNRankingExpression();
                expression.setId("expr-not");
                expression.setWeight(3.0f);
                TurSNRankingCondition isNot = new TurSNRankingCondition();
                isNot.setAttribute("type");
                isNot.setValue("news");
                isNot.setCondition(TurSNRankingCondition.IS_NOT);

                when(siteSearchConfig.fieldTypes(any())).thenReturn(fieldTypes(List.of()));
                when(siteSearchConfig.rankingExpressions(any()))
                                .thenReturn(List.of(toSpi(expression, isNot)));

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                // T1070 — before, Is not wrote type:"news" and boosted exactly what it meant to bury.
                assertThat(query.getParams("bq")).containsExactly("((*:* -type:\"news\"))^3.0");
        }

        @Test
        void testPrepareSolrQueryOmitsIsNotOnRecencyForm() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNRankingExpression expression = new TurSNRankingExpression();
                expression.setId("expr-not-recent");
                expression.setWeight(3.0f);
                TurSNRankingCondition isNot = new TurSNRankingCondition();
                isNot.setAttribute("published");
                isNot.setValue("asc");
                isNot.setCondition(TurSNRankingCondition.IS_NOT);

                when(siteSearchConfig.fieldTypes(any())).thenReturn(fieldTypes(List.of(
                                TurSNSiteFieldExt.builder().name("published")
                                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE).build())));
                when(siteSearchConfig.rankingExpressions(any()))
                                .thenReturn(List.of(toSpi(expression, isNot)));

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getParams("bq")).isNull();
        }

        @Test
        void testPrepareSolrQueryAddsFieldWeightAsEdismaxBoostQuery() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                                TurSNSiteFieldExt.builder().name("title").searchWeight(3f).build(),
                                TurSNSiteFieldExt.builder().name("text").searchWeight(1f).build(),
                                TurSNSiteFieldExt.builder().name("summary").build()));

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("java");
                params.setRows(10);
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                // Weight 3 adds the query on title worth 2; weight 1 and no weight add nothing.
                assertThat(query.getParams("bq")).containsExactly("{!edismax qf='title^2.00' v=$q}");
        }

        @Test
        void testPrepareSolrQueryBoostQueryWritesExistsConditionAsRangeQuery() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNRankingExpression expression = new TurSNRankingExpression();
                expression.setId("expr-exists");
                expression.setWeight(2.0f);
                TurSNRankingCondition exists = new TurSNRankingCondition();
                exists.setAttribute("salaryMin");
                exists.setCondition(TurSNRankingCondition.EXISTS);

                when(siteSearchConfig.fieldTypes(any())).thenReturn(fieldTypes(List.of()));
                when(siteSearchConfig.rankingExpressions(any()))
                                .thenReturn(List.of(toSpi(expression, exists)));

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getParams("bq")).containsExactly("(salaryMin:[* TO *])^2.0");
        }

        @Test
        void testPrepareQueryFacetWithOneFacetHandlesDateRangeAndEntityPrefix() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(8);
                site.setFacetSort(null);
                site.setFacetType(TurSNSiteFacetFieldEnum.OR);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.OR);

                TurSNSiteFieldExt dateFacet = TurSNSiteFieldExt.builder()
                                .name("publishDate")
                                .snType(TurSNFieldType.SE)
                                .facet(1)
                                .enabled(1)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE)
                                .facetRange(TurSNSiteFacetRangeEnum.YEAR)
                                .facetSort(TurSNSiteFacetFieldSortEnum.COUNT)
                                .build();

                TurSNSiteFieldExt entityFacet = TurSNSiteFieldExt.builder()
                                .name("person")
                                .snType(TurSNFieldType.NER)
                                .facet(1)
                                .enabled(1)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.ALPHABETICAL)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(dateFacet, entityFacet));

                SolrQuery dateQuery = new SolrQuery();
                builder().prepareQueryFacetWithOneFacet(site, dateQuery, TurSNFilterParams.builder().build(),
                                "publishDate");
                assertThat(dateQuery.getParams("facet.range")).isNotNull();

                SolrQuery entityQuery = new SolrQuery();
                builder().prepareQueryFacetWithOneFacet(site, entityQuery, TurSNFilterParams.builder().build(),
                                "person");
                assertThat(entityQuery.getFacetFields()).isNotEmpty();
                assertThat(String.join(",", entityQuery.getFacetFields())).contains("person").doesNotContain("turing_entity");
                assertThat(entityQuery.get("f.person.facet.sort")).isEqualTo("index");
        }

        @Test
        void testCustomFacetQuerySeparatorConstant() {
                assertThat(TurSolrQueryBuilder.CUSTOM_FACET_QUERY_SEPARATOR).isEqualTo("::");
        }

        @Test
        void testPrepareQueryFacetReturnsFacetFields() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(10);
                site.setFacetSort(null);
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);

                TurSNSiteFieldExt f1 = TurSNSiteFieldExt.builder()
                                .name("category")
                                .facet(1)
                                .enabled(1)
                                .facetPosition(2)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.DEFAULT)
                                .build();
                TurSNSiteFieldExt f2 = TurSNSiteFieldExt.builder()
                                .name("type")
                                .facet(1)
                                .enabled(1)
                                .facetPosition(1)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.DEFAULT)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(f1, f2));

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> facets = builder().prepareQueryFacet(site, query,
                                TurSNFilterParams.builder().build(), (com.viglet.turing.commons.se.TurSEParameters) null);

                assertThat(facets).hasSize(2);
                assertThat(facets).extracting(TurSNSiteFieldExt::getName)
                                .containsExactlyInAnyOrder("category", "type");
                assertThat(query.getBool("facet")).isTrue();
        }

        /** T1250 — a facet the request leaves out is not asked of Solr. */
        @Test
        void testPrepareQueryFacetLeavesOutTheRequestsExcludedFacets() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(10);
                site.setFacetSort(null);
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);
                TurSNSiteFieldExt f1 = TurSNSiteFieldExt.builder().name("category").facet(1).enabled(1)
                                .facetPosition(2).type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.DEFAULT).build();
                TurSNSiteFieldExt f2 = TurSNSiteFieldExt.builder().name("year").facet(1).enabled(1)
                                .facetPosition(1).type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.DEFAULT).build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(f1, f2));
                com.viglet.turing.commons.sn.bean.TurSNSearchParams searchParams =
                                new com.viglet.turing.commons.sn.bean.TurSNSearchParams();
                searchParams.setExf(List.of("year"));

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> facets = builder().prepareQueryFacet(site, query,
                                TurSNFilterParams.builder().build(),
                                new com.viglet.turing.commons.se.TurSEParameters(searchParams));

                assertThat(facets).extracting(TurSNSiteFieldExt::getName).containsExactly("category");
                assertThat(String.join(",", query.getFacetFields())).doesNotContain("year");
        }

        @Test
        void testPrepareQueryFacetWithDisabledFacetReturnsEmpty() {
                TurSNSite site = new TurSNSite();
                site.setFacet(0);

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> facets = builder().prepareQueryFacet(site, query,
                                TurSNFilterParams.builder().build(), (com.viglet.turing.commons.se.TurSEParameters) null);

                assertThat(facets).isEmpty();
        }

        @Test
        void testPrepareSolrQueryWithNullSortUsesDefault() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("test");
                searchParams.setRows(10);
                searchParams.setSort(null);
                TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(parameters, new TurSNSitePostParamsBean()), site,
                                parameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getQuery()).isEqualTo("test");
        }

        @Test
        void testPrepareQueryMLTWithMltDisabledReturnsFieldsButDoesNotConfigureQuery() {
                TurSNSite site = new TurSNSite();
                site.setMlt(0);
                TurSNSiteFieldExt mltField = TurSNSiteFieldExt.builder().name("body").build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndMltAndEnabled(site, 1, 1))
                                .thenReturn(List.of(mltField));

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> result = builder().prepareQueryMLT(site, query);

                // Fields are always returned from the repository
                assertThat(result).containsExactly(mltField);
                // But the query is NOT configured with MLT params when mlt is disabled
                assertThat(query.get(MoreLikeThisParams.MLT)).isNull();
        }

        @Test
        void testPrepareSolrQueryWithoutExactMatchDoesNotModifyQuery() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setExactMatch(0);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("\"quoted query\"");
                searchParams.setRows(10);
                TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(parameters, new TurSNSitePostParamsBean()), site,
                                parameters, new TurSESpellCheckResult(false, ""));

                // Without exact match, the query should not be prefixed
                assertThat(query.getQuery()).isEqualTo("\"quoted query\"");
        }

        @Test
        void testPrepareSolrQueryExactMatchWithBlankFieldDoesNotModify() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setExactMatch(1);
                site.setExactMatchField("");
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("\"test\"");
                searchParams.setRows(10);
                TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(parameters, new TurSNSitePostParamsBean()), site,
                                parameters, new TurSESpellCheckResult(false, ""));

                // ExactMatchField is blank so exact match should NOT apply
                assertThat(query.getQuery()).isEqualTo("\"test\"");
        }

        @Test
        void testPrepareSolrQueryWithFacetSortCount() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(10);
                site.setFacetSort(com.viglet.turing.sn.persistence.model.TurSNSiteFacetSortEnum.COUNT);
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);

                TurSNSiteFieldExt facet = TurSNSiteFieldExt.builder()
                                .name("category")
                                .facet(1)
                                .enabled(1)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.DEFAULT)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(facet));

                SolrQuery query = new SolrQuery();
                builder().prepareQueryFacet(site, query, TurSNFilterParams.builder().build(), null);

                assertThat(query.get("facet.sort")).isEqualTo("count");
        }

        @Test
        void testPrepareSolrQueryWithFacetSortAlphabetical() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(10);
                site.setFacetSort(com.viglet.turing.sn.persistence.model.TurSNSiteFacetSortEnum.ALPHABETICAL);
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);

                TurSNSiteFieldExt facet = TurSNSiteFieldExt.builder()
                                .name("category")
                                .facet(1)
                                .enabled(1)
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetSort(TurSNSiteFacetFieldSortEnum.DEFAULT)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(facet));

                SolrQuery query = new SolrQuery();
                builder().prepareQueryFacet(site, query, TurSNFilterParams.builder().build(), null);

                assertThat(query.get("facet.sort")).isEqualTo("index");
        }

        @Test
        void testPrepareQueryHLWithMultipleFields() {
                TurSNSite site = new TurSNSite();
                site.setHlPre("<b>");
                site.setHlPost("</b>");

                TurSNSiteFieldExt hlField1 = TurSNSiteFieldExt.builder().name("title").build();
                TurSNSiteFieldExt hlField2 = TurSNSiteFieldExt.builder().name("body").build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndHlAndEnabled(site, 1, 1))
                                .thenReturn(List.of(hlField1, hlField2));

                TurSNSearchParams searchParams = new TurSNSearchParams();
                TurSEParameters parameters = new TurSEParameters(searchParams);
                TurSNConfig config = new TurSNConfig();
                config.setHlEnabled(true);
                TurSNSiteSearchContext context = new TurSNSiteSearchContext("site", config,
                                parameters, Locale.US, URI.create("http://localhost/search"));

                SolrQuery query = new SolrQuery();
                List<TurSNSiteFieldExt> result = builder().prepareQueryHL(site, query, context);

                assertThat(result).hasSize(2);
                assertThat(query.get(HighlightParams.FIELDS)).isEqualTo("title,body");
                assertThat(query.get(HighlightParams.SIMPLE_PRE)).isEqualTo("<b>");
                assertThat(query.get(HighlightParams.SIMPLE_POST)).isEqualTo("</b>");
        }

        @Test
        void testPrepareQueryFacetWithNullItemsPerFacetDoesNotSetFacet() {
                TurSNSite site = new TurSNSite();
                site.setFacet(1);
                site.setItemsPerFacet(null);

                TurSNSiteFieldExt facet = TurSNSiteFieldExt.builder()
                                .name("category")
                                .facet(1)
                                .enabled(1)
                                .build();

                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(facet));

                SolrQuery query = new SolrQuery();
                builder().prepareQueryFacet(site, query,
                                TurSNFilterParams.builder().build(), (com.viglet.turing.commons.se.TurSEParameters) null);

                // itemsPerFacet is null so wasFacetConfigured returns false
                assertThat(query.get("facet")).isNull();
        }

        @Test
        void testPrepareSolrQueryWithNonExactMatchQuotedQuery() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(10);
                site.setExactMatch(1);
                site.setExactMatchField("title_exact");
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                // Non-quoted query should NOT trigger exact match
                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("hello world");
                searchParams.setRows(10);
                TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(parameters, new TurSNSitePostParamsBean()), site,
                                parameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getQuery()).isEqualTo("hello world");
        }

        @Test
        void testPrepareSolrQueryWithPositiveRowsPerPageSetsRows() {
                TurSNSite site = new TurSNSite();
                site.setRowsPerPage(25);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams searchParams = new TurSNSearchParams();
                searchParams.setQ("test");
                searchParams.setRows(-1);
                TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

                builder().prepareSolrQuery(
                                contextFrom(parameters, new TurSNSitePostParamsBean()), site,
                                parameters, new TurSESpellCheckResult(false, ""));

                assertThat(parameters.getRows()).isEqualTo(25);
        }

        @Test
        void testPrepareSolrQueryWithAndFilterQueries() {
                TurSNSite site = new TurSNSite();
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNSiteFieldExt categoryField = TurSNSiteFieldExt.builder()
                                .name("category")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(categoryField));
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledAndType(site, 1, 1,
                                com.viglet.turing.commons.se.field.TurSEFieldType.DATE))
                                .thenReturn(Collections.emptyList());
                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                params.setFq(List.of("category:books"));
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                assertThat(query.getFilterQueries()).isNotEmpty();
                assertThat(query.getFilterQueries()[0]).contains("category:\"books\"");
        }

        @Test
        void testPrepareSolrQueryWrapsNegationFilterQueryWithMatchAll() {
                TurSNSite site = new TurSNSite();
                site.setFacetType(TurSNSiteFacetFieldEnum.AND);
                site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);
                site.setRowsPerPage(10);
                site.setSpellCheck(0);
                site.setSpellCheckFixes(0);

                TurSNSiteFieldExt topicField = TurSNSiteFieldExt.builder()
                                .name("temas-secundarios")
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .build();
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                                .thenReturn(List.of(topicField));
                when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledAndType(site, 1, 1,
                                com.viglet.turing.commons.se.field.TurSEFieldType.DATE))
                                .thenReturn(Collections.emptyList());
                when(siteSearchConfig.fieldTypes(any()))
                                .thenReturn(fieldTypes(Collections.emptyList()));
                when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

                TurSNSearchParams params = new TurSNSearchParams();
                params.setQ("query");
                params.setRows(10);
                params.setFq(List.of("temas-secundarios:Políticas Públicas",
                                "-id:/content/excluded-page"));
                TurSEParameters seParameters = new TurSEParameters(params, new TurSNSitePostParamsBean());

                SolrQuery query = builder().prepareSolrQuery(
                                contextFrom(seParameters, new TurSNSitePostParamsBean()), site,
                                seParameters, new TurSESpellCheckResult(false, ""));

                // T1233 — the exclusion is a filter query of its own, so it always
                // narrows and is never joined to a facet group.
                assertThat(query.getFilterQueries()).containsExactlyInAnyOrder(
                                "-id:\"/content/excluded-page\"",
                                query.getFilterQueries()[1]);
                assertThat(query.getFilterQueries()[1])
                                .contains("temas-secundarios:\"Políticas Públicas\"")
                                .doesNotContain("excluded-page");
        }

        // --- buildFilterQueryByOperator (via reflection) ---

        private static String invokeBuildFilterQueryByOperator(
                        com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum op,
                        String field, String start, String end) throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "buildFilterQueryByOperator", String.class,
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.class,
                                String.class, String.class);
                method.setAccessible(true);
                return (String) method.invoke(null, field, op, start, end);
        }

        @Test
        void testBuildFilterQueryBetweenInclusive() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.BETWEEN_INCLUSIVE,
                                "price", "10", "100"))
                                .isEqualTo("price:[10 TO 100]");
        }

        @Test
        void testBuildFilterQueryBetweenInclusiveWithNullStart() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.BETWEEN_INCLUSIVE,
                                "price", null, "100"))
                                .isEqualTo("price:[* TO 100]");
        }

        @Test
        void testBuildFilterQueryBetweenInclusiveWithNullEnd() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.BETWEEN_INCLUSIVE,
                                "price", "10", null))
                                .isEqualTo("price:[10 TO *]");
        }

        @Test
        void testBuildFilterQueryBetweenExclusive() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.BETWEEN_EXCLUSIVE,
                                "price", "10", "100"))
                                .isEqualTo("price:{10 TO 100}");
        }

        @Test
        void testBuildFilterQueryEqual() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.EQUAL,
                                "status", "active", null))
                                .isEqualTo("status:active");
        }

        @Test
        void testBuildFilterQueryEqualUsesEndWhenStartNull() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.EQUAL,
                                "status", null, "inactive"))
                                .isEqualTo("status:inactive");
        }

        @Test
        void testBuildFilterQueryEqualBothNullUsesWildcard() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.EQUAL,
                                "status", null, null))
                                .isEqualTo("status:*");
        }

        @Test
        void testBuildFilterQueryGreaterThanStrict() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.GREATER_THAN,
                                "price", "50", null))
                                .isEqualTo("price:{50 TO *]");
        }

        @Test
        void testBuildFilterQueryGreaterThanOrEqual() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.GREATER_THAN_OR_EQUAL,
                                "price", "50", null))
                                .isEqualTo("price:[50 TO *]");
        }

        @Test
        void testBuildFilterQueryLessThanStrict() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.LESS_THAN,
                                "price", null, "100"))
                                .isEqualTo("price:[* TO 100}");
        }

        @Test
        void testBuildFilterQueryLessThanOrEqual() throws Exception {
                assertThat(invokeBuildFilterQueryByOperator(
                                com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetOperatorEnum.LESS_THAN_OR_EQUAL,
                                "price", null, "100"))
                                .isEqualTo("price:[* TO 100]");
        }

        // --- escapeLocalParamValue (via reflection) ---

        @Test
        void testEscapeLocalParamValueEscapesBackslashAndQuote() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "escapeLocalParamValue", String.class);
                method.setAccessible(true);

                assertThat((String) method.invoke(null, "normal")).isEqualTo("normal");
                assertThat((String) method.invoke(null, "it's")).isEqualTo("it\\'s");
                assertThat((String) method.invoke(null, "back\\slash")).isEqualTo("back\\\\slash");
                assertThat((String) method.invoke(null, "both\\and'quote")).isEqualTo("both\\\\and\\'quote");
        }

        // --- addDoubleQuotesToValue (via reflection) ---

        @Test
        void testAddDoubleQuotesToValueWithKeyValue() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "addDoubleQuotesToValue", String.class);
                method.setAccessible(true);

                assertThat((String) method.invoke(null, "category:books")).isEqualTo("category:\"books\"");
        }

        @Test
        void testAddDoubleQuotesToValueWithoutColon() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "addDoubleQuotesToValue", String.class);
                method.setAccessible(true);

                assertThat((String) method.invoke(null, "plainvalue")).isEqualTo("\"plainvalue\"");
        }

        // --- queryWithoutExpression / withoutExpression (via reflection) ---

        @Test
        void testWithoutExpression() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "withoutExpression", String.class);
                method.setAccessible(true);

                assertThat((boolean) method.invoke(null, "simple")).isTrue();
                assertThat((boolean) method.invoke(null, "[1 TO 10]")).isFalse();
                assertThat((boolean) method.invoke(null, "(nested)")).isFalse();
                assertThat((boolean) method.invoke(null, "wild*")).isFalse();
                assertThat((boolean) method.invoke(null, "normal text")).isTrue();
        }

        @Test
        void testQueryWithoutExpression() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "queryWithoutExpression", String.class);
                method.setAccessible(true);

                assertThat((boolean) method.invoke(null, "field:value")).isTrue();
                assertThat((boolean) method.invoke(null, "(field:value)")).isFalse();
                assertThat((boolean) method.invoke(null, "field:[1 TO 10]")).isFalse();
                assertThat((boolean) method.invoke(null, "field:wild*")).isFalse();
        }

        // --- betweenSpaces (via reflection) ---

        @Test
        void testBetweenSpaces() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "betweenSpaces", String.class);
                method.setAccessible(true);

                assertThat((String) method.invoke(null, "AND")).isEqualTo(" AND ");
                assertThat((String) method.invoke(null, "OR")).isEqualTo(" OR ");
        }

        // --- getCustomFacetLocalParams (via reflection) ---

        @Test
        void testGetCustomFacetLocalParamsWithExclude() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "getCustomFacetLocalParams", String.class, String.class, boolean.class);
                method.setAccessible(true);

                String result = (String) method.invoke(null, "price_range", "0-100", true);
                assertThat(result)
                                .contains("key='price_range::0-100'")
                                .contains("ex=_all_");
        }

        @Test
        void testGetCustomFacetLocalParamsWithoutExclude() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "getCustomFacetLocalParams", String.class, String.class, boolean.class);
                method.setAccessible(true);

                String result = (String) method.invoke(null, "price_range", "0-100", false);
                assertThat(result)
                                .contains("key='price_range::0-100'")
                                .doesNotContain("ex=_all_");
        }

        // --- facetSortIsEmptyOrCount (via reflection) ---

        @Test
        void testFacetSortIsEmptyOrCount() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "facetSortIsEmptyOrCount", TurSNSite.class);
                method.setAccessible(true);

                TurSNSite nullSort = new TurSNSite();
                nullSort.setFacetSort(null);
                assertThat((boolean) method.invoke(null, nullSort)).isTrue();

                TurSNSite countSort = new TurSNSite();
                countSort.setFacetSort(com.viglet.turing.sn.persistence.model.TurSNSiteFacetSortEnum.COUNT);
                assertThat((boolean) method.invoke(null, countSort)).isTrue();

                TurSNSite alphaSort = new TurSNSite();
                alphaSort.setFacetSort(com.viglet.turing.sn.persistence.model.TurSNSiteFacetSortEnum.ALPHABETICAL);
                assertThat((boolean) method.invoke(null, alphaSort)).isFalse();
        }

        // --- isNerOrThesaurus (via reflection) ---

        // --- isDateRangeFacet (via reflection) ---

        @Test
        void testIsDateRangeFacet() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "isDateRangeFacet", TurSNSiteFieldExt.class);
                method.setAccessible(true);

                TurSNSiteFieldExt dateRangeField = TurSNSiteFieldExt.builder()
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE)
                                .facetRange(TurSNSiteFacetRangeEnum.MONTH)
                                .build();
                assertThat((boolean) method.invoke(null, dateRangeField)).isTrue();

                TurSNSiteFieldExt dateDisabledField = TurSNSiteFieldExt.builder()
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE)
                                .facetRange(TurSNSiteFacetRangeEnum.DISABLED)
                                .build();
                assertThat((boolean) method.invoke(null, dateDisabledField)).isFalse();

                TurSNSiteFieldExt dateNullRange = TurSNSiteFieldExt.builder()
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.DATE)
                                .facetRange(null)
                                .build();
                assertThat((boolean) method.invoke(null, dateNullRange)).isFalse();

                TurSNSiteFieldExt stringField = TurSNSiteFieldExt.builder()
                                .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING)
                                .facetRange(TurSNSiteFacetRangeEnum.MONTH)
                                .build();
                assertThat((boolean) method.invoke(null, stringField)).isFalse();
        }

        // --- isFacetEnabled (via reflection) ---

        @Test
        void testIsFacetEnabled() throws Exception {
                Method method = TurSolrQueryBuilder.class.getDeclaredMethod(
                                "isFacetEnabled", TurSNSiteFieldExt.class);
                method.setAccessible(true);

                TurSNSiteFieldExt enabled = TurSNSiteFieldExt.builder().facet(1).build();
                assertThat((boolean) method.invoke(null, enabled)).isTrue();

                TurSNSiteFieldExt disabled = TurSNSiteFieldExt.builder().facet(0).build();
                assertThat((boolean) method.invoke(null, disabled)).isFalse();
        }

        // ---- T1111: an exclusive-start range is an expression, never a phrase ----

        @Test
        void rangesOfEveryBracketShapeAreLeftUnquoted() {
                assertThat(TurSolrQueryBuilder.withoutExpression("{1800 TO 4000]")).isFalse();
                assertThat(TurSolrQueryBuilder.withoutExpression("{1800 TO 4000}")).isFalse();
                assertThat(TurSolrQueryBuilder.withoutExpression("[1800 TO 4000}")).isFalse();
                assertThat(TurSolrQueryBuilder.withoutExpression("[1800 TO 4000]")).isFalse();
                assertThat(TurSolrQueryBuilder.withoutExpression("educacao")).isTrue();
        }

        // ---- T1104: custom sort levels with a missing placement ----

        @Test
        void customSortLevelWithMissingLastIsPrecededByAnExistsSort() {
                // Levels arrive in position order from the SPI (T1310).
                var items = java.util.List.of(
                                new com.viglet.turing.searchengine.spi.TurSECustomSortItem("expiresAt",
                                                com.viglet.turing.searchengine.TurSNSiteCustomSortOrderEnum.ASC,
                                                com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum.LAST),
                                new com.viglet.turing.searchengine.spi.TurSECustomSortItem("publishedAt",
                                                com.viglet.turing.searchengine.TurSNSiteCustomSortOrderEnum.DESC, null));

                assertThat(TurSolrQueryBuilder.customSortClauses(items))
                                .extracting(c -> c.getItem() + " " + c.getOrder())
                                .containsExactly("if(exists(expiresAt),0,1) asc", "expiresAt asc",
                                                "publishedAt desc");
        }

        @Test
        void customSortLevelWithMissingFirstPutsTheUndatedFirst() {
                var items = java.util.List.of(new com.viglet.turing.searchengine.spi.TurSECustomSortItem("expiresAt",
                                com.viglet.turing.searchengine.TurSNSiteCustomSortOrderEnum.ASC,
                                com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum.FIRST));

                assertThat(TurSolrQueryBuilder.customSortClauses(items))
                                .extracting(c -> c.getItem() + " " + c.getOrder())
                                .containsExactly("if(exists(expiresAt),1,0) asc", "expiresAt asc");
        }

        // ---- T1103: semantic recall's id restriction ----

        @Test
        void idRestrictionIsItsOwnQuotedFilterQuery() {
                SolrQuery query = new SolrQuery();

                TurSolrQueryBuilder.prepareIdRestriction(
                                List.of("https://jobs/auxiliar-de-creche", "say \"hi\"\\x"), query);

                assertThat(query.getFilterQueries()).containsExactly(
                                "id:(\"https://jobs/auxiliar-de-creche\" OR \"say \\\"hi\\\"\\\\x\")");
        }

        @Test
        void noIdRestrictionAddsNoFilterQuery() {
                SolrQuery query = new SolrQuery();

                TurSolrQueryBuilder.prepareIdRestriction(null, query);
                TurSolrQueryBuilder.prepareIdRestriction(List.of(), query);

                assertThat(query.getFilterQueries()).isNull();
        }

        /** T1133 — the key's partition is its own quoted filter query. */
        @Test
        void partitionIsItsOwnQuotedFilterQuery() {
                SolrQuery query = new SolrQuery();

                TurSolrQueryBuilder.preparePartition(
                                new com.viglet.turing.commons.se.TurSEPartition("site_key", "a \"b\""), query);
                TurSolrQueryBuilder.preparePartition(null, query);

                assertThat(query.getFilterQueries()).containsExactly("site_key:\"a \\\"b\\\"\"");
        }
}
