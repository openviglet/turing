/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.spring.security.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.viglet.turing.commons.se.TurSEPartition;

/**
 * T1210 — a partitioned key's authentication carries its partition, so a
 * request that replays the session the key opened stays inside it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurApiKeyPartitionContextTest {

    private static final TurSEPartition A = new TurSEPartition("site_key", "a.example.org");

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aReplayedSessionKeepsThePartitionTheKeyAuthenticatedWith() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sn/shared/search/query");
        authenticate(new TurApiKeyAuthenticationDetails(new MockHttpServletRequest(), A));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(TurApiKeyPartitionContext.of(request)).contains(A);
        assertThat(TurApiKeyPartitionContext.current()).contains(A);
    }

    @Test
    void anAuthenticationWithoutAPartitionHasNone() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        authenticate(null);

        assertThat(TurApiKeyPartitionContext.of(request)).isEmpty();
    }

    @Test
    void theGuardRefusesAReplayedSessionOnARouteThatDoesNotEnforcePartitions() throws Exception {
        authenticate(new TurApiKeyAuthenticationDetails(new MockHttpServletRequest(), A));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dev/token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new TurApiKeyPartitionGuardFilter().doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void theDetailsCompareByTheirPartition() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(new TurApiKeyAuthenticationDetails(request, A))
                .isEqualTo(new TurApiKeyAuthenticationDetails(request, A))
                .isNotEqualTo(new TurApiKeyAuthenticationDetails(request,
                        new TurSEPartition("site_key", "b.example.org")));
        assertThat(Optional.of(new TurApiKeyAuthenticationDetails(request, A).getPartition())).contains(A);
    }

    private static void authenticate(Object details) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("user", null,
                List.of());
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
