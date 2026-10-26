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
            "GET, /api/sn/shared/ac, false",
            "GET, /api/sn/shared/search/similar, false",
            "GET, /api/sn/shared/search/document, false",
            "GET, /api/sn/shared/en_US/spell-check, false",
            "POST, /api/sn/shared/chat, false",
            "POST, /api/sn/import/zip, false",
            "POST, /api/sn/deindex, false",
            "GET, /api/sn/site-1/metrics/clicks, false",
            "GET, /api/dev/token, false"
    })
    void allowsOnlyTheEndpointsThatEnforceThePartition(String method, String uri, boolean allowed) {
        assertThat(TurApiKeyPartitionGuardFilter.allows(method, uri)).isEqualTo(allowed);
    }

    @Test
    void refusesAPartitionedKeyWithA403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sn/shared/ac");
        TurApiKeyPartitionContext.bind(request, partitioned());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
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
