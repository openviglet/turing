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

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * T1133 — a key bound to a partition of a shared SN site reaches only the
 * endpoints that enforce the partition; every other request it makes is refused
 * with 403. Failing closed is what keeps a partitioned key safe while endpoints
 * learn the partition one at a time: an endpoint that does not apply it yet
 * cannot leak another partition's documents, because the key never gets there.
 *
 * <p>Requests without a partitioned key are untouched.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Slf4j
@Component
public class TurApiKeyPartitionGuardFilter extends OncePerRequestFilter {

    /** A method and a path the partition is enforced on. */
    record Route(String method, Pattern path) {

        boolean matches(String requestMethod, String uri) {
            return method.equalsIgnoreCase(requestMethod) && path.matcher(uri).matches();
        }
    }

    /**
     * Where a partitioned key may go: search (its results are filtered to the
     * partition), its configuration reads, similar documents and the stored
     * document read (both confined to the partition), result clicks, and the
     * JSON imports (each document is stamped with the partition).
     */
    static final List<Route> ENFORCED = List.of(
            new Route("GET", Pattern.compile(
                    "^/api/sn/[^/]+/search(?:/list|/locales|/sort-options|/similar|/document)?$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/search(?:/query|/list/query)?$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/click$")),
            new Route("POST", Pattern.compile("^/api/sn/import(?:/bulk)?$")));

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (TurApiKeyPartitionContext.of(request).isEmpty() || allows(request.getMethod(), path(request))) {
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("[PartitionGuard] Refusing a partitioned key on {} {}", request.getMethod(),
                request.getRequestURI());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"This API key is bound to a partition of a shared site, "
                + "and this endpoint does not enforce partitions yet\"}");
    }

    /** Whether a partitioned key may make this request. */
    static boolean allows(String method, String uri) {
        return ENFORCED.stream().anyMatch(route -> route.matches(method, uri));
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
