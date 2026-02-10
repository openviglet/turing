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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.se.result.TurSEResult;
import com.viglet.turing.sn.TurSNFieldProcess;
import com.viglet.turing.sn.field.TurSNSiteFieldService;
import com.viglet.turing.sn.tr.TurSNTargetingRules;

/**
 * Unit tests for TurSolr.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSolrTest {

    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    @Mock
    private TurSNTargetingRules turSNTargetingRules;

    @Mock
    private TurSNSiteFieldService turSNSiteFieldService;

    @Mock
    private TurSNRankingExpressionRepository turSNRankingExpressionRepository;

    @Mock
    private TurSNRankingConditionRepository turSNRankingConditionRepository;

    @Mock
    private TurSNSiteRepository turSNSiteRepository;

    @Mock
    private TurSNFieldProcess turSNFieldProcess;

    @Mock
    private HttpJdkSolrClient httpJdkSolrClient;

    @Mock
    private SolrClient solrClient;

    private TurSolrInstance turSolrInstance;

    @BeforeEach
    void setUp() throws MalformedURLException {
        turSolrInstance = new TurSolrInstance(httpJdkSolrClient, URI.create("http://localhost:8983/solr").toURL(),
                "core");
        turSolrInstance.setSolrClient(solrClient);
    }

    @Test
    void testAddAWildcardInQuery() {
        SolrQuery query = new SolrQuery().setQuery("test");

        TurSolr.addAWildcardInQuery(query);

        assertThat(query.getQuery()).isEqualTo("test*");
    }

    @Test
    void testEnabledWildcardNoResults() {
        TurSNSite turSNSite = mock(TurSNSite.class);
        when(turSNSite.getWildcardNoResults()).thenReturn(1);

        assertThat(TurSolr.enabledWildcardNoResults(turSNSite)).isTrue();

        when(turSNSite.getWildcardNoResults()).thenReturn(null);
        assertThat(TurSolr.enabledWildcardNoResults(turSNSite)).isFalse();
    }

    @Test
    void testIsNotQueryExpressionReturnsTrueForSimpleQuery() {
        SolrQuery query = new SolrQuery().setQuery("simple");

        assertThat(TurSolr.isNotQueryExpression(query)).isTrue();
    }

    @Test
    void testCreateTurSEResultFromDocument() {
        SolrDocument document = new SolrDocument();
        document.addField("id", "100");
        document.addField("title", "Sample");

        TurSEResult result = TurSolr.createTurSEResultFromDocument(document);

        assertThat(result.getFields())
                .containsEntry("id", "100")
                .containsEntry("title", "Sample");
    }

    @Test
    void testCommitSkippedWhenDisabled() throws Exception {
        TurSolr turSolr = buildTurSolr(false);

        turSolr.commit(turSolrInstance);

        verify(solrClient, never()).commit();
    }

    @Test
    void testCommitInvokedWhenEnabled() throws Exception {
        TurSolr turSolr = buildTurSolr(true);

        turSolr.commit(turSolrInstance);

        verify(solrClient, times(1)).commit();
    }

    private TurSolr buildTurSolr(boolean commitEnabled) {
        return new TurSolr(commitEnabled, 500,
                turSNSiteFieldExtRepository,
                turSNTargetingRules,
                turSNSiteFieldService,
                turSNRankingExpressionRepository,
                turSNRankingConditionRepository,
                turSNSiteRepository,
                turSNFieldProcess);
    }
}
