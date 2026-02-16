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
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
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
}
