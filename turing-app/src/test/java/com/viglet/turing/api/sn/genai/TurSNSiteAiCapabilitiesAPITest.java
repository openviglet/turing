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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.genai.TurSNChatDisabledReason;
import com.viglet.turing.genai.TurSNChatRouteResolver;
import com.viglet.turing.genai.TurSNSiteVectorProbe;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.genai.catalog.TurCatalogCopilotService;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNKnowledgeBaseMode;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.dsl.eval.TurNLFacetParser;
import com.viglet.turing.system.TurLlmSummaryService;

/**
 * T1123 — one probe answers every AI feature of a site from configuration.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteAiCapabilitiesAPITest {

    @Mock
    private TurSNSearchProcess turSNSearchProcess;
    @Mock
    private TurSNChatRouteResolver chatRouteResolver;
    @Mock
    private TurDefaultAgentResolver defaultAgentResolver;
    @Mock
    private TurNLFacetParser nlFacetParser;
    @Mock
    private TurCatalogCopilotService catalogCopilotService;
    @Mock
    private TurLlmSummaryService llmSummaryService;
    @Mock
    private TurSNSiteVectorProbe vectorProbe;
    @InjectMocks
    private TurSNSiteAiCapabilitiesAPI api;

    private static TurSNSite site(TurSNSiteGenAi genAi) {
        TurSNSite site = new TurSNSite();
        site.setName("Sample");
        site.setTurSNSiteGenAi(genAi);
        return site;
    }

    @Test
    void aSiteOnTheDefaultAgentHasEveryFeatureOn() {
        when(turSNSearchProcess.getSNSite("Sample")).thenReturn(Optional.of(site(null)));
        when(chatRouteResolver.resolve(null)).thenReturn(new TurSNChatRouteResolver.Decision(
                TurSNChatRouteResolver.Route.RAG, TurSNChatDisabledReason.NONE));
        when(defaultAgentResolver.isRagReady(null)).thenReturn(true);
        when(nlFacetParser.isAvailable()).thenReturn(true);
        when(catalogCopilotService.isAvailable()).thenReturn(true);
        when(llmSummaryService.isAvailable()).thenReturn(true);

        TurSNSiteAiCapabilitiesAPI.Capabilities capabilities = api.capabilities("Sample");

        assertThat(capabilities.site()).isEqualTo("Sample");
        assertThat(capabilities.chat().enabled()).isTrue();
        assertThat(capabilities.answer()).isEqualTo(capabilities.chat());
        assertThat(capabilities.nlFilters().enabled()).isTrue();
        assertThat(capabilities.similarByVector().reason()).isEqualTo("NONE");
    }

    @Test
    void anOffFeatureNamesItsReason() {
        TurSNSiteGenAi vectorless = new TurSNSiteGenAi();
        vectorless.setKnowledgeBaseMode(TurSNKnowledgeBaseMode.VECTORLESS_STRUCTURED);
        when(turSNSearchProcess.getSNSite("Sample")).thenReturn(Optional.of(site(vectorless)));
        when(chatRouteResolver.resolve(vectorless)).thenReturn(new TurSNChatRouteResolver.Decision(
                TurSNChatRouteResolver.Route.UNAVAILABLE, TurSNChatDisabledReason.MISSING_DEFAULT_LLM));
        when(defaultAgentResolver.isRagReady(vectorless)).thenReturn(false);
        when(nlFacetParser.isAvailable()).thenReturn(false);
        when(catalogCopilotService.isAvailable()).thenReturn(false);
        when(llmSummaryService.isAvailable()).thenReturn(false);

        TurSNSiteAiCapabilitiesAPI.Capabilities capabilities = api.capabilities("Sample");

        assertThat(capabilities.answer().enabled()).isFalse();
        assertThat(capabilities.answer().reason()).isEqualTo("MISSING_DEFAULT_LLM");
        assertThat(capabilities.nlFilters().reason()).isEqualTo("MISSING_DEFAULT_LLM");
        assertThat(capabilities.similarByVector().reason()).isEqualTo("VECTORLESS");
    }

    @Test
    void aReadySiteWithNoVectorsAsksForAReindexAndKeepsChat() {
        // T1151 — configured for RAG, but nothing of the site reached the store.
        TurSNSite site = site(null);
        TurAIAgent agent = new TurAIAgent();
        when(turSNSearchProcess.getSNSite("Sample")).thenReturn(Optional.of(site));
        when(chatRouteResolver.resolve(null)).thenReturn(new TurSNChatRouteResolver.Decision(
                TurSNChatRouteResolver.Route.RAG, TurSNChatDisabledReason.NONE));
        when(defaultAgentResolver.isRagReady(null)).thenReturn(true);
        when(defaultAgentResolver.resolveEffectiveAgent(null)).thenReturn(agent);
        when(vectorProbe.probe(site, agent)).thenReturn(TurSNSiteVectorProbe.Presence.ABSENT);

        TurSNSiteAiCapabilitiesAPI.Capabilities capabilities = api.capabilities("Sample");

        assertThat(capabilities.similarByVector().enabled()).isFalse();
        assertThat(capabilities.similarByVector().reason()).isEqualTo(TurSNSiteAiCapabilitiesAPI.NOT_EMBEDDED);
        assertThat(capabilities.chat().enabled()).isTrue();
    }

    @Test
    void anUnknownSiteIsNotFound() {
        when(turSNSearchProcess.getSNSite("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.capabilities("ghost")).isInstanceOf(TurNotFoundException.class);
    }
}
