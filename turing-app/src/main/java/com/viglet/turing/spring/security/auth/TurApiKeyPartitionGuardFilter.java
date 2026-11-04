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
     * document read (both confined to the partition), autocomplete (suggested
     * from the partition's documents), spell-check (no correction), the
     * thesaurus' related terms, result clicks, the JSON imports (each
     * document is stamped with the partition), and (T1209) the site
     * configuration reads, the manifest and its plan, summaries, the AI
     * probes, the partition's own spotlights and the synonym reads, and the RAG
     * chat and answer, retrieved from the partition's chunks only.
     */
    static final List<Route> ENFORCED = List.of(
            new Route("GET", Pattern.compile(
                    "^/api/sn/[^/]+/search(?:/list|/locales|/sort-options|/similar|/document)?$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/search(?:/query|/list/query)?$")),
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/ac$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/ac/query$")),
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/[^/]+/(?:spell-check|related-terms)$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/click$")),
            // T1133 — the insight reports a connector reads, over its partition's logs.
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/metrics/(?:failed-searches|content-gaps|clicks)$")),
            // T1143 — the audience personas and content-fit read the agent's
            // catalog, never the index, so there is nothing a partition must confine.
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/personas$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/persona/[^/]+/content-fit$")),
            new Route("POST", Pattern.compile("^/api/sn/import(?:/bulk)?$")),
            // T1209 — the SN site configuration, read-only: the list and one site,
            // never the export, which carries every partition's content.
            new Route("GET", Pattern.compile("^/api/sn(?:/(?!export$)[^/]+)?$")),
            // T1209 — the manifest and its plan: the controller refuses a manifest
            // that would change the shared schema, so only a re-post goes through.
            new Route("POST", Pattern.compile("^/api/sn/manifest(?:/plan)?$")),
            // T1209 — summaries of the caller's own data, cached per partition.
            new Route("POST", Pattern.compile("^/api/v2/summary$")),
            // T1209 — the AI probes: each answers from configuration, and the
            // capabilities' vector probe says only whether the store holds a chunk,
            // so none returns a document. The copilot and the raw DSL _search stay
            // refused: the DSL suggesters read the whole index's vocabulary.
            new Route("GET", Pattern.compile(
                    "^/api/sn/[^/]+/(?:ai/capabilities|chat/enabled|copilot/available|summary/available)$")),
            // T1133 — the RAG chat and the answer: retrieval reads only the chunks
            // written with the partition, the turn runs without tools, and a site
            // whose chunks predate the partition field refuses until reindexed.
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/chat/conversation$")),
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/answer$")),
            // T1133 — natural-language filters: the values each filter is checked
            // against are read by a DSL aggregation confined to the partition.
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/nl-filters$")),
            // T1209 — spotlights: the API confines a partitioned key to its
            // partition's own, and a search shows them only to that partition.
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/spotlight(?:/[^/]+)?$")),
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/spotlight$")),
            new Route("PUT", Pattern.compile("^/api/sn/[^/]+/spotlight/[^/]+$")),
            new Route("DELETE", Pattern.compile("^/api/sn/[^/]+/spotlight/[^/]+$")),
            // T1209 — synonyms are one set per engine core, shared by every
            // partition: readable, never written (see SHARED).
            new Route("GET", Pattern.compile("^/api/sn/[^/]+/synonym(?:/[^/]+)?$")));

    /**
     * T1209 — routes a partitioned key is refused on for a reason of their own,
     * named in the 403 so a client can say why rather than "not yet".
     */
    static final List<Route> SHARED = List.of(
            new Route("POST", Pattern.compile("^/api/sn/[^/]+/synonym(?:/.*)?$")),
            new Route("PUT", Pattern.compile("^/api/sn/[^/]+/synonym/.*$")),
            new Route("DELETE", Pattern.compile("^/api/sn/[^/]+/synonym/.*$")));

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
        response.getWriter().write(shared(request.getMethod(), path(request))
                ? "{\"error\":\"This API key is bound to a partition of a shared site, and the site's synonyms "
                        + "are one set shared by every partition; change them with an unpartitioned key\"}"
                : "{\"error\":\"This API key is bound to a partition of a shared site, "
                        + "and this endpoint does not enforce partitions yet\"}");
    }

    /** T1209 — whether this request is refused because what it writes is shared by every partition. */
    static boolean shared(String method, String uri) {
        return SHARED.stream().anyMatch(route -> route.matches(method, uri));
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
