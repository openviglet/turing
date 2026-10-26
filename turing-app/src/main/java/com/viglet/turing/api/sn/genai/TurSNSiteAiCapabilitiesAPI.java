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

package com.viglet.turing.api.sn.genai;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.genai.TurSNChatDisabledReason;
import com.viglet.turing.genai.TurSNChatRouteResolver;
import com.viglet.turing.genai.catalog.TurCatalogCopilotService;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.dsl.eval.TurNLFacetParser;
import com.viglet.turing.system.TurLlmSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Which AI features a Semantic Navigation site can use, in one read (T1123).
 *
 * <p>Every answer comes from configuration, as {@code /chat/enabled} does: no
 * model is called, so a status screen can ask on every page load. Chat and the
 * answer engine read the same route since T1116, so they always agree.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@RestController
@RequestMapping("/api/sn/{siteName}/ai/capabilities")
@Tag(name = "Semantic Navigation AI capabilities", description = "Which AI features a site can use")
public class TurSNSiteAiCapabilitiesAPI {

    private final TurSNSearchProcess turSNSearchProcess;
    private final TurSNChatRouteResolver chatRouteResolver;
    private final TurDefaultAgentResolver defaultAgentResolver;
    private final TurNLFacetParser nlFacetParser;
    private final TurCatalogCopilotService catalogCopilotService;
    private final TurLlmSummaryService llmSummaryService;

    public TurSNSiteAiCapabilitiesAPI(TurSNSearchProcess turSNSearchProcess,
            TurSNChatRouteResolver chatRouteResolver, TurDefaultAgentResolver defaultAgentResolver,
            TurNLFacetParser nlFacetParser, TurCatalogCopilotService catalogCopilotService,
            TurLlmSummaryService llmSummaryService) {
        this.turSNSearchProcess = turSNSearchProcess;
        this.chatRouteResolver = chatRouteResolver;
        this.defaultAgentResolver = defaultAgentResolver;
        this.nlFacetParser = nlFacetParser;
        this.catalogCopilotService = catalogCopilotService;
        this.llmSummaryService = llmSummaryService;
    }

    /** One feature: whether it is on, and why not when it is off ({@code NONE} when on). */
    public record Capability(boolean enabled, String reason) {

        static Capability of(TurSNChatDisabledReason reason) {
            return new Capability(reason == TurSNChatDisabledReason.NONE, reason.name());
        }

        static Capability needsDefaultLlm(boolean available) {
            return of(available ? TurSNChatDisabledReason.NONE : TurSNChatDisabledReason.MISSING_DEFAULT_LLM);
        }
    }

    /**
     * Each feature of the site. {@code similarByVector} is the vector neighbour
     * search, off for a vectorless site even when its agent has RAG on.
     */
    public record Capabilities(String site, Capability chat, Capability answer, Capability nlFilters,
            Capability copilot, Capability similarByVector, Capability summaries) {
    }

    @Operation(summary = "Which AI features a Semantic Navigation site can use, without calling a model")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Capabilities capabilities(@PathVariable String siteName) {
        return turSNSearchProcess.getSNSite(siteName).map(site -> {
            TurSNSiteGenAi genAi = site.getTurSNSiteGenAi();
            Capability route = Capability.of(chatRouteResolver.resolve(genAi).reason());
            return new Capabilities(site.getName(), route, route,
                    Capability.needsDefaultLlm(nlFacetParser.isAvailable()),
                    Capability.needsDefaultLlm(catalogCopilotService.isAvailable()),
                    defaultAgentResolver.isRagReady(genAi) ? Capability.of(TurSNChatDisabledReason.NONE)
                            : new Capability(false, ragOffReason(genAi)),
                    Capability.needsDefaultLlm(llmSummaryService.isAvailable()));
        }).orElseThrow(() -> new TurNotFoundException("SN site not found: " + siteName));
    }

    /** Why vector similarity is off: the site opted out of vectors, or its agent cannot embed. */
    private String ragOffReason(TurSNSiteGenAi genAi) {
        if (genAi != null && genAi.getKnowledgeBaseMode() != null
                && !genAi.getKnowledgeBaseMode().needsVectorSetup()) {
            return "VECTORLESS";
        }
        TurSNChatDisabledReason reason = chatRouteResolver.resolve(genAi).reason();
        return reason == TurSNChatDisabledReason.NONE ? TurSNChatDisabledReason.RAG_DISABLED.name()
                : reason.name();
    }
}
