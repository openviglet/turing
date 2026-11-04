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
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * T1184 — the guest console is read-only: a session signed in through
 * {@code /api/v2/guest/login} carries {@link #GUEST_AUTHORITY}, and every request
 * it makes that could change something is refused with 403. A read passes,
 * except the few that hand out what a stranger must not see (people, keys,
 * whole-site exports). The anonymous search and chat surfaces stay open, since
 * any visitor reaches them without signing in.
 *
 * <p>Failing closed is the point: a new endpoint is read-only for the guest by
 * its method, without anyone having to list it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurGuestReadOnlyFilter extends OncePerRequestFilter {

    /** The authority that marks a guest console session. */
    public static final String GUEST_AUTHORITY = "ROLE_GUEST";

    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");

    /** Reads a guest is refused: people, credentials and exports of a whole site. */
    static final List<Pattern> HIDDEN_READS = List.of(
            Pattern.compile("^/api/v2/user(?!/current$)(?:/.*)?$"),
            Pattern.compile("^/api/(?:v2/)?(?:group|role|privilege)(?:/.*)?$"),
            Pattern.compile("^/api/keycloak(?:/.*)?$"),
            Pattern.compile("^/api/dev/token(?:/.*)?$"),
            Pattern.compile("^/api/(?:v2/)?gateway(?:/.*)?$"),
            Pattern.compile("^/api/.*/export(?:/.*)?$"),
            Pattern.compile("^/h2(?:/.*)?$"));

    /** Writes any anonymous visitor may make anyway: the public search and chat of a site. */
    static final List<Pattern> PUBLIC_WRITES = List.of(
            Pattern.compile("^/api/sn/[^/]+/(?:search(?:/query|/list/query)?|ac/query|click"
                    + "|chat/conversation|nl-filters|copilot)$"),
            Pattern.compile("^/logout$"));

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {
        String path = path(request);
        if (!isGuest(SecurityContextHolder.getContext().getAuthentication())
                || allows(request.getMethod(), path)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("[GuestConsole] Refusing {} {} for the read-only guest", request.getMethod(), path);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"This is a read-only guest console: nothing can be saved "
                + "or changed here\"}");
    }

    static boolean isGuest(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .anyMatch(GUEST_AUTHORITY::equals);
    }

    /** Whether the guest may make this request. */
    static boolean allows(String method, String path) {
        if (READS.contains(method.toUpperCase())) {
            return HIDDEN_READS.stream().noneMatch(pattern -> pattern.matcher(path).matches());
        }
        return PUBLIC_WRITES.stream().anyMatch(pattern -> pattern.matcher(path).matches());
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
