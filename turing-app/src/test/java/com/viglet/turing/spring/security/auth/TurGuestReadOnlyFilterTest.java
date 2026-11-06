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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * T1184 — the guest console's session reads and never writes.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurGuestReadOnlyFilterTest {

    private final TurGuestReadOnlyFilter filter = new TurGuestReadOnlyFilter();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "someone", null, java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/sn, true",
            "GET, /api/sn/site-1/field/ext, true",
            "GET, /api/v2/user/current, true",
            "HEAD, /api/llm, true",
            "GET, /api/v2/user, false",
            "GET, /api/v2/user/admin, false",
            "GET, /api/dev/token, false",
            "GET, /api/group/g1, false",
            "GET, /api/sn/site-1/export, false",
            "GET, /api/sn/export, false",
            "POST, /api/sn, false",
            "PUT, /api/sn/site-1, false",
            "DELETE, /api/llm/l1, false",
            "POST, /api/v2/summary, false",
            "POST, /api/sn/site-1/search, true",
            "POST, /api/sn/site-1/search/similar/query, true",
            "POST, /api/sn/site-1/search/duplicates/query, true",
            "POST, /api/sn/site-1/chat/conversation, true",
            "POST, /api/sn/site-1/chat/conversation/stream, false",
            "POST, /logout, true"
    })
    void readsPassAndWritesAreRefused(String method, String path, boolean allowed) {
        assertThat(TurGuestReadOnlyFilter.allows(method, path)).isEqualTo(allowed);
    }

    @Test
    void refusesAGuestWriteWithA403() throws Exception {
        signIn(TurGuestReadOnlyFilter.GUEST_AUTHORITY, "SN_VIEW");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("PUT", "/api/sn/site-1"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("read-only guest console");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void leavesEveryoneElseAlone() throws Exception {
        signIn("ROLE_ADMIN");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("PUT", "/api/sn/site-1"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(TurGuestReadOnlyFilter.isGuest(null)).isFalse();
        assertThat(TurGuestReadOnlyFilter.isGuest(UsernamePasswordAuthenticationToken.authenticated("g", null,
                List.of(new SimpleGrantedAuthority(TurGuestReadOnlyFilter.GUEST_AUTHORITY))))).isTrue();
    }
}
