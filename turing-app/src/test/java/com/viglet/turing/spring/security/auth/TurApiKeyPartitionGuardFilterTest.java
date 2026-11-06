/*
 * Copyright (C) 2016-2026 the original author or authors.
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
package com.viglet.turing.spring.security.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.viglet.turing.persistence.model.dev.token.TurDevToken;

/**
 * T1133 — a partitioned key reaches only the endpoints that enforce partitions.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurApiKeyPartitionGuardFilterTest {

    private final TurApiKeyPartitionGuardFilter filter = new TurApiKeyPartitionGuardFilter();

    @ParameterizedTest
    @CsvSource({
            "GET, /api/sn/shared/search, true",
            "POST, /api/sn/shared/search/query, true",
            "POST, /api/sn/shared/search/list/query, true",
            "GET, /api/sn/shared/search/locales, true",
            "POST, /api/sn/shared/click, true",
            "POST, /api/sn/import, true",
            "POST, /api/sn/import/bulk, true",
            "GET, /api/sn/shared/ac, true",
            "POST, /api/sn/shared/ac/query, true",
            "GET, /api/sn/shared/en_US/related-terms, true",
            "GET, /api/sn/shared/search/similar, true",
            "GET, /api/sn/shared/search/document, true",
            "GET, /api/sn/shared/search/duplicates, true",
            "POST, /api/sn/shared/search/similar/query, true",
            "POST, /api/sn/shared/search/duplicates/query, true",
            "GET, /api/sn/shared/en_US/spell-check, true",
            "POST, /api/sn/shared/chat, false",
            "POST, /api/sn/import/zip, false",
            "POST, /api/sn/deindex, false",
            "GET, /api/sn/site-1/metrics/clicks, true",
            "GET, /api/sn/site-1/metrics/failed-searches, true",
            "GET, /api/sn/site-1/metrics/top-terms/today/10, false",
            "GET, /api/sn/shared/personas, true",
            "POST, /api/sn/shared/persona/p1/content-fit, true",
            "GET, /api/dev/token, false",
            "GET, /api/sn, true",
            "GET, /api/sn/site-1, true",
            "PUT, /api/sn/site-1, false",
            "DELETE, /api/sn/site-1, false",
            "GET, /api/sn/export, false",
            "GET, /api/sn/site-1/export, false",
            "POST, /api/sn/manifest, true",
            "POST, /api/sn/manifest/plan, true",
            "POST, /api/sn/manifest/derive, false",
            "POST, /api/v2/summary, true",
            "GET, /api/sn/site-1/synonym, true",
            "GET, /api/sn/site-1/synonym/s1, true",
            "POST, /api/sn/site-1/synonym, false",
            "POST, /api/sn/site-1/synonym/apply, false",
            "DELETE, /api/sn/site-1/synonym/s1, false",
            "GET, /api/sn/site-1/spotlight, true",
            "GET, /api/sn/site-1/spotlight/sp1, true",
            "POST, /api/sn/site-1/spotlight, true",
            "PUT, /api/sn/site-1/spotlight/sp1, true",
            "DELETE, /api/sn/site-1/spotlight/sp1, true",
            "GET, /api/sn/site-1/ranking-expression, false",
            "GET, /api/sn/shared/ai/capabilities, true",
            "GET, /api/sn/shared/chat/enabled, true",
            "GET, /api/sn/shared/copilot/available, true",
            "GET, /api/sn/site-1/summary/available, true",
            "GET, /api/sn/shared/answer, true",
            "GET, /api/sn/shared/answer/share/a1, false",
            "POST, /api/sn/shared/chat/conversation, true",
            "POST, /api/sn/shared/chat/conversation/stream, false",
            "POST, /api/sn/shared/nl-filters, true",
            "POST, /api/sn/shared/_search, false",
            "POST, /api/sn/shared/copilot, true",
            "POST, /api/sn/shared/copilot/kit, true",
            "GET, /api/sn/shared/chat, false"
    })
    void allowsOnlyTheEndpointsThatEnforceThePartition(String method, String uri, boolean allowed) {
        assertThat(TurApiKeyPartitionGuardFilter.allows(method, uri)).isEqualTo(allowed);
    }

    @Test
    void refusesAPartitionedKeyWithA403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sn/shared/chat");
        TurApiKeyPartitionContext.bind(request, partitioned());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    /** T1209 — a synonym write is refused for what it is: the set every partition shares. */
    @Test
    void refusesASynonymWriteNamingTheSharedSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sn/site-1/synonym");
        TurApiKeyPartitionContext.bind(request, partitioned());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("shared by every partition");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void passesARequestWithoutAPartitionedKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sn/shared/ac");
        TurApiKeyPartitionContext.bind(request, new TurDevToken());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    static TurDevToken partitioned() {
        TurDevToken token = new TurDevToken();
        token.setPartitionField("site_key");
        token.setPartitionValue("a.example.org");
        return token;
    }
}
