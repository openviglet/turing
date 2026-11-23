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
package com.viglet.turing.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.sn.api.TurSNSites;
import com.viglet.turing.system.TurGlobalSettingsService;

/**
 * Unit tests for the T622 default-agent fallback.
 */
@ExtendWith(MockitoExtension.class)
class TurDefaultAgentResolverTest {

    @Mock
    private TurGlobalSettingsService globalSettingsService;
    @Mock
    private TurAIAgentRepository turAIAgentRepository;
    @Mock
    private TurSNSites turSNSiteRepository;

    private final InMemoryAgentLlmInstances agentLlms = new InMemoryAgentLlmInstances();
    @org.mockito.Spy
    private TurAgentLlmInstances agentLlmInstances = agentLlms.service();

    @InjectMocks
    private TurDefaultAgentResolver resolver;

    @Test
    void siteAgentOfCarriesTheTitleRagFlagAndLlmInstancesInIdOrder() {
        // T1322h — what sn reads about its site's agent, through TurSNSiteAgents.
        com.viglet.turing.llm.persistence.model.TurLLMInstance second =
                new com.viglet.turing.llm.persistence.model.TurLLMInstance();
        second.setId("llm-b");
        com.viglet.turing.llm.persistence.model.TurLLMInstance first =
                new com.viglet.turing.llm.persistence.model.TurLLMInstance();
        first.setId("llm-a");
        TurAIAgent agent = ragReadyAgent();
        agent.setTitle("Support agent");
        agentLlms.attach(agent, second, first);
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, agent);

        var view = resolver.siteAgentOf(genAi).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(view.title()).isEqualTo("Support agent");
        org.assertj.core.api.Assertions.assertThat(view.ragEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(view.llmInstances()).containsExactly(first, second);
    }

    @Test
    void defaultAgentIsTheGlobalDefaultAsSnReadsItOrEmpty() {
        TurAIAgent defaultAgent = ragReadyAgent();
        defaultAgent.setTitle("Default");
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("def-1");
        when(turAIAgentRepository.findById("def-1")).thenReturn(Optional.of(defaultAgent));

        org.assertj.core.api.Assertions.assertThat(resolver.defaultAgent()).get()
                .extracting(com.viglet.turing.sn.api.TurSNSiteAgent::title).isEqualTo("Default");

        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("");
        org.assertj.core.api.Assertions.assertThat(resolver.defaultAgent()).isEmpty();
    }

    @Test
    void siteAgentOfIsEmptyWithoutAnAgentAndIgnoresTheDefault() {
        org.assertj.core.api.Assertions.assertThat(resolver.siteAgentOf(new TurSNSiteGenAi())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(resolver.siteAgentOf(null)).isEmpty();
        verifyNoInteractions(globalSettingsService);
    }

    @Test
    void siteOwnAgentWinsAndNoDefaultLookupHappens() {
        TurAIAgent siteAgent = new TurAIAgent();
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, siteAgent);

        assertSame(siteAgent, resolver.resolveEffectiveAgent(genAi));
        // The site declares its own agent, so the global default is never touched.
        verifyNoInteractions(globalSettingsService);
    }

    @Test
    void fallsBackToDefaultWhenBindingHasNoAgent() {
        TurAIAgent defaultAgent = new TurAIAgent();
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("def-1");
        when(turAIAgentRepository.findById("def-1")).thenReturn(Optional.of(defaultAgent));

        assertSame(defaultAgent, resolver.resolveEffectiveAgent(new TurSNSiteGenAi()));
    }

    @Test
    void fallsBackToDefaultWhenBindingIsNull() {
        TurAIAgent defaultAgent = new TurAIAgent();
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("def-1");
        when(turAIAgentRepository.findById("def-1")).thenReturn(Optional.of(defaultAgent));

        assertSame(defaultAgent, resolver.resolveEffectiveAgent(null));
    }

    @Test
    void returnsNullWhenNoDefaultConfigured() {
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("");

        assertNull(resolver.resolveEffectiveAgent(new TurSNSiteGenAi()));
    }

    @Test
    void returnsNullWhenDefaultAgentIdDangling() {
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("gone");
        when(turAIAgentRepository.findById("gone")).thenReturn(Optional.empty());

        assertNull(resolver.resolveEffectiveAgent(null));
    }

    /** T1315 — the binding names its agent by id; the repository answers it. */
    private void bind(TurSNSiteGenAi genAi, TurAIAgent agent) {
        agent.setId("own-1");
        genAi.setTurAIAgentId("own-1");
        org.mockito.Mockito.lenient().when(turAIAgentRepository.findById("own-1"))
                .thenReturn(Optional.of(agent));
    }

    private static TurAIAgent ragReadyAgent() {
        TurAIAgent agent = new TurAIAgent();
        agent.setEnabled(1);
        agent.setRagEnabled(true);
        return agent;
    }

    @Test
    void ragEnabledAgentIdsKeepsOnlyEnabledAgentsWithRagOn() {
        // T1322h — the site listing's flag, answered for sn through TurSNSiteAgents.
        TurAIAgent ready = ragReadyAgent();
        ready.setId("ready");
        TurAIAgent disabled = ragReadyAgent();
        disabled.setId("disabled");
        disabled.setEnabled(0);
        TurAIAgent noRag = ragReadyAgent();
        noRag.setId("no-rag");
        noRag.setRagEnabled(false);
        java.util.Set<String> ids = java.util.Set.of("ready", "disabled", "no-rag", "gone");
        when(turAIAgentRepository.findAllById(ids)).thenReturn(java.util.List.of(ready, disabled, noRag));

        org.assertj.core.api.Assertions.assertThat(resolver.ragEnabledAgentIds(ids))
                .containsExactly("ready");
    }

    @Test
    void ragEnabledAgentIdsReadsNothingForNoIds() {
        org.assertj.core.api.Assertions.assertThat(resolver.ragEnabledAgentIds(java.util.Set.of())).isEmpty();
        verifyNoInteractions(turAIAgentRepository);
    }

    @Test
    void isRagReadyTrueForRagReadyOwnAgent() {
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, ragReadyAgent());

        assertTrue(resolver.isRagReady(genAi));
        verifyNoInteractions(globalSettingsService);
    }

    @Test
    void isRagReadyFalseWhenOwnAgentDisabled() {
        TurAIAgent agent = ragReadyAgent();
        agent.setEnabled(0);
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, agent);

        assertFalse(resolver.isRagReady(genAi));
    }

    @Test
    void isRagReadyFalseWhenOwnAgentRagOff() {
        TurAIAgent agent = ragReadyAgent();
        agent.setRagEnabled(false);
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, agent);

        assertFalse(resolver.isRagReady(genAi));
    }

    @Test
    void isRagReadyFallsBackToRagReadyDefaultAgent() {
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("def-1");
        when(turAIAgentRepository.findById("def-1")).thenReturn(Optional.of(ragReadyAgent()));

        assertTrue(resolver.isRagReady(new TurSNSiteGenAi()));
    }

    @Test
    void isRagReadyFalseWhenNoOwnAgentAndNoDefault() {
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("");

        assertFalse(resolver.isRagReady(null));
    }

    @Test
    void isRagReadyFalseWhenVectorlessStructuredEvenWithRagReadyOwnAgent() {
        // T790 — the site's own agent is RAG-ready, but the site explicitly opts out
        // of vectors via VECTORLESS_STRUCTURED: no vector-dependent surface is ready.
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, ragReadyAgent());
        genAi.setKnowledgeBaseMode(
                com.viglet.turing.sn.persistence.model.genai.TurSNKnowledgeBaseMode.VECTORLESS_STRUCTURED);

        assertFalse(resolver.isRagReady(genAi));
        // Short-circuits on the mode before ever resolving/looking up an agent.
        verifyNoInteractions(globalSettingsService, turAIAgentRepository);
    }

    @Test
    void isRagReadyFalseWhenVectorlessStructuredEvenWithRagReadyDefaultAgent() {
        // The exact scenario reported: a search-only seed site (no own agent) would
        // inherit the RAG-ready Default AI Agent — but VECTORLESS_STRUCTURED overrides
        // that, so it never becomes RAG-ready (and never embeds). Mode is checked
        // first, so the default lookup is short-circuited.
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        genAi.setKnowledgeBaseMode(
                com.viglet.turing.sn.persistence.model.genai.TurSNKnowledgeBaseMode.VECTORLESS_STRUCTURED);

        assertFalse(resolver.isRagReady(genAi));
        verifyNoInteractions(globalSettingsService, turAIAgentRepository);
    }

    @Test
    void isRagReadyTrueWhenHybridModeAndRagReadyOwnAgent() {
        // HYBRID needs the vector setup, so a RAG-ready agent still qualifies.
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        bind(genAi, ragReadyAgent());
        genAi.setKnowledgeBaseMode(
                com.viglet.turing.sn.persistence.model.genai.TurSNKnowledgeBaseMode.HYBRID);

        assertTrue(resolver.isRagReady(genAi));
    }

    private static TurSNSite site(TurSNSiteGenAi genAi) {
        TurSNSite site = new TurSNSite();
        site.setName("site");
        site.setTurSNSiteGenAi(genAi);
        return site;
    }

    @Test
    void isRagReadyForSiteNameFallsBackToTheDefaultAgent() {
        // T1147 — a binding with no agent of its own is embedded through the default.
        when(turSNSiteRepository.findByNameWithGenAi("site")).thenReturn(Optional.of(site(new TurSNSiteGenAi())));
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("def-1");
        when(turAIAgentRepository.findById("def-1")).thenReturn(Optional.of(ragReadyAgent()));

        assertTrue(resolver.isRagReadyForSiteName("site"));
    }

    @Test
    void isRagReadyForSiteNameReadsTheOwnAgent() {
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        TurAIAgent agent = ragReadyAgent();
        agent.setRagEnabled(false);
        bind(genAi, agent);
        when(turSNSiteRepository.findByNameWithGenAi("site")).thenReturn(Optional.of(site(genAi)));

        assertFalse(resolver.isRagReadyForSiteName("site"));
        verifyNoInteractions(globalSettingsService);
    }

    @Test
    void isRagReadyForSiteNameFalseForAMissingSite() {
        when(turSNSiteRepository.findByNameWithGenAi("missing")).thenReturn(Optional.empty());

        assertFalse(resolver.isRagReadyForSiteName("missing"));
        verifyNoInteractions(globalSettingsService, turAIAgentRepository);
    }

    @Test
    void isDefaultRagReadyTrueWhenDefaultAgentRagReady() {
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("def-1");
        when(turAIAgentRepository.findById("def-1")).thenReturn(Optional.of(ragReadyAgent()));

        assertTrue(resolver.isDefaultRagReady());
    }

    @Test
    void isDefaultRagReadyFalseWhenNoDefaultConfigured() {
        when(globalSettingsService.getDefaultAiAgentId()).thenReturn("");

        assertFalse(resolver.isDefaultRagReady());
    }
}
