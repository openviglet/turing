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

package com.viglet.turing.sn.manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNKnowledgeBaseMode;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.repository.agent.TurAIAgentRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;

/**
 * T1116 — the manifest's {@code genAi} block turns a site's GenAI on, and a
 * re-post of the same block changes nothing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@ExtendWith(MockitoExtension.class)
class TurSNManifestGenAiConvergerTest {

    @Mock
    private TurAIAgentRepository turAIAgentRepository;
    @Mock
    private TurSNSiteGenAiRepository turSNSiteGenAiRepository;
    @InjectMocks
    private TurSNManifestGenAiConverger converger;

    private TurSNSite site;

    @BeforeEach
    void setUp() {
        site = new TurSNSite();
        site.setName("Sample");
    }

    private static TurAIAgent agent(String id) {
        TurAIAgent agent = new TurAIAgent();
        agent.setId(id);
        return agent;
    }

    @Test
    void anAbsentBlockLeavesTheSiteAlone() {
        assertThat(converger.plan(site, null)).isFalse();
        assertThat(converger.converge(site, null)).isFalse();
        verify(turSNSiteGenAiRepository, never()).save(any());
    }

    @Test
    void theDefaultAgentCreatesABindingWithNoAgentOfItsOwn() {
        when(turSNSiteGenAiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TurSNManifestGenAi genAi = new TurSNManifestGenAi("default", null);

        assertThat(converger.plan(site, genAi)).isTrue();
        assertThat(converger.converge(site, genAi)).isTrue();

        assertThat(site.getTurSNSiteGenAi()).isNotNull();
        assertThat(site.getTurSNSiteGenAi().getTurAIAgent()).isNull();
        assertThat(site.getTurSNSiteGenAi().getKnowledgeBaseMode()).isEqualTo(TurSNKnowledgeBaseMode.VECTOR);
    }

    @Test
    void aNamedAgentAndModeAreBound() {
        when(turAIAgentRepository.findById("a1")).thenReturn(Optional.of(agent("a1")));
        when(turSNSiteGenAiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        converger.converge(site, new TurSNManifestGenAi("a1", TurSNKnowledgeBaseMode.HYBRID));

        assertThat(site.getTurSNSiteGenAi().getTurAIAgent().getId()).isEqualTo("a1");
        assertThat(site.getTurSNSiteGenAi().getKnowledgeBaseMode()).isEqualTo(TurSNKnowledgeBaseMode.HYBRID);
    }

    @Test
    void aMatchingBindingIsANoOp() {
        TurSNSiteGenAi live = new TurSNSiteGenAi();
        live.setTurAIAgent(agent("a1"));
        live.setKnowledgeBaseMode(TurSNKnowledgeBaseMode.HYBRID);
        site.setTurSNSiteGenAi(live);
        TurSNManifestGenAi genAi = new TurSNManifestGenAi("a1", null);

        assertThat(converger.plan(site, genAi)).isFalse();
        assertThat(converger.converge(site, genAi)).isFalse();
        verify(turSNSiteGenAiRepository, never()).save(any());
    }

    @Test
    void switchingToTheDefaultAgentDropsTheSitesOwnAgentAndKeepsTheRest() {
        TurSNSiteGenAi live = new TurSNSiteGenAi();
        live.setTurAIAgent(agent("a1"));
        live.setSitePrompt("kept");
        site.setTurSNSiteGenAi(live);
        when(turSNSiteGenAiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(converger.converge(site, new TurSNManifestGenAi("default", null))).isTrue();

        assertThat(site.getTurSNSiteGenAi()).isSameAs(live);
        assertThat(live.getTurAIAgent()).isNull();
        assertThat(live.getSitePrompt()).isEqualTo("kept");
    }

    @Test
    void anOmittedAgentKeepsTheLiveBindingsAgent() {
        TurSNSiteGenAi live = new TurSNSiteGenAi();
        live.setTurAIAgent(agent("a1"));
        site.setTurSNSiteGenAi(live);
        TurSNManifestGenAi genAi = new TurSNManifestGenAi(null, null);

        assertThat(converger.plan(site, genAi)).isFalse();
        assertThat(converger.converge(site, genAi)).isFalse();
        assertThat(live.getTurAIAgent().getId()).isEqualTo("a1");
        verify(turSNSiteGenAiRepository, never()).save(any());
    }

    @Test
    void anOmittedAgentStillChangesADifferingModeAndKeepsTheAgent() {
        TurSNSiteGenAi live = new TurSNSiteGenAi();
        live.setTurAIAgent(agent("a1"));
        live.setKnowledgeBaseMode(TurSNKnowledgeBaseMode.VECTOR);
        site.setTurSNSiteGenAi(live);
        when(turSNSiteGenAiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TurSNManifestGenAi genAi = new TurSNManifestGenAi(" ", TurSNKnowledgeBaseMode.HYBRID);

        assertThat(converger.plan(site, genAi)).isTrue();
        assertThat(converger.converge(site, genAi)).isTrue();
        assertThat(live.getTurAIAgent().getId()).isEqualTo("a1");
        assertThat(live.getKnowledgeBaseMode()).isEqualTo(TurSNKnowledgeBaseMode.HYBRID);
    }

    @Test
    void anOmittedAgentCreatesABindingOnTheDefaultAgentWhenTheSiteHasNone() {
        when(turSNSiteGenAiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TurSNManifestGenAi genAi = new TurSNManifestGenAi(null, null);

        assertThat(converger.plan(site, genAi)).isTrue();
        assertThat(converger.converge(site, genAi)).isTrue();
        assertThat(site.getTurSNSiteGenAi().getTurAIAgent()).isNull();
    }

    @Test
    void anUnknownAgentIsRefused() {
        when(turAIAgentRepository.existsById("ghost")).thenReturn(false);

        assertThatThrownBy(() -> converger.validate(new TurSNManifestGenAi("ghost", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }
}
