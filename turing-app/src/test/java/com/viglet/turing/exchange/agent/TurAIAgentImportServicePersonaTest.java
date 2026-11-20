/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.exchange.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.viglet.turing.exchange.TurExchange;
import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.persona.persistence.model.TurPersona;
import com.viglet.turing.persona.persistence.model.TurPersonaAudience;
import com.viglet.turing.persona.persistence.model.TurPersonaKind;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.agent.persistence.repository.TurAIAgentSlotRepository;
import com.viglet.turing.agent.persistence.repository.TurAgentEvalSetRepository;
import com.viglet.turing.agent.persistence.repository.TurChatFlowRepository;
import com.viglet.turing.agent.persistence.repository.customtool.TurCustomToolRepository;
import com.viglet.turing.llm.persistence.repository.embedding.TurEmbeddingModelRepository;
import com.viglet.turing.agent.persistence.repository.intent.TurIntentRepository;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;
import com.viglet.turing.mcp.persistence.repository.TurMcpServerRepository;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;
import com.viglet.turing.rag.persistence.repository.store.TurStoreInstanceRepository;
import com.viglet.turing.tenant.TurInfraTenantScope;

/**
 * T655 — verifies the agent-import upsert carries the Block AA persona fields
 * ({@code personaKind} + audience facet) and preserves an explicit incoming id,
 * so demo personas shipped inside an import bundle land audience-usable and with
 * the stable ids the public site references.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TurAIAgentImportServicePersonaTest {

    @Mock private TurAIAgentRepository agentRepository;
    @Mock private TurChatFlowRepository chatFlowRepository;
    @Mock private TurAIAgentSlotRepository slotRepository;
    @Mock private TurAgentEvalSetRepository evalSetRepository;
    @Mock private TurIntentRepository intentRepository;
    @Mock private TurPersonaRepository personaRepository;
    @Mock private TurLLMInstanceRepository llmRepository;
    @Mock private TurMcpServerRepository mcpServerRepository;
    @Mock private TurStoreInstanceRepository storeRepository;
    @Mock private TurEmbeddingModelRepository embeddingRepository;
    @Mock private TurCustomToolRepository customToolRepository;
    @Mock private TurInfraTenantScope infraTenantScope;

    private TurAIAgentImportService service;

    @BeforeEach
    void setUp() {
        service = new TurAIAgentImportService(agentRepository, chatFlowRepository, slotRepository,
                evalSetRepository, intentRepository, personaRepository, llmRepository,
                mcpServerRepository, storeRepository, embeddingRepository, customToolRepository,
                infraTenantScope);

        when(personaRepository.findById(any())).thenReturn(Optional.empty());
        when(personaRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(personaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(agentRepository.findById(any())).thenReturn(Optional.empty());
        when(agentRepository.findByTitleIgnoreCase(any())).thenReturn(Optional.empty());
        when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void importCarriesPersonaKindAudienceAndPreservesId() {
        TurPersonaAudience audience = new TurPersonaAudience();
        audience.setReadingLevel(com.viglet.turing.persona.persistence.model.TurPersonaReadingLevel.MIDDLE);
        audience.setPrimaryLanguage("en");

        TurPersona persona = new TurPersona();
        persona.setId("persona-first-time-user");
        persona.setName("First-time User");
        persona.setPersonaKind(TurPersonaKind.AUDIENCE);
        persona.setAudience(audience);
        persona.setEnabled(1);

        TurAIAgentExchange agentExchange = new TurAIAgentExchange();
        agentExchange.setId("agent-turing-demo");
        agentExchange.setTitle("Turing Demo Assistant");
        agentExchange.setEnabled(1);
        agentExchange.setRagEnabled(true);
        agentExchange.setPersonaIds(List.of("persona-first-time-user"));

        TurExchange exchange = new TurExchange();
        exchange.setPersonas(List.of(persona));
        exchange.setAgents(List.of(agentExchange));

        var result = service.importAgents(exchange, null, true);
        assertThat(result.error()).isNull();

        ArgumentCaptor<TurPersona> personaCaptor = ArgumentCaptor.forClass(TurPersona.class);
        verifySaved(personaCaptor);
        TurPersona saved = personaCaptor.getValue();
        assertThat(saved.getId()).isEqualTo("persona-first-time-user"); // id preserved
        assertThat(saved.getPersonaKind()).isEqualTo(TurPersonaKind.AUDIENCE); // kind carried
        assertThat(saved.getAudience()).isNotNull(); // audience facet carried
        assertThat(saved.getAudience().getPrimaryLanguage()).isEqualTo("en");

        // The agent got the persona wired into its catalog.
        ArgumentCaptor<TurAIAgent> agentCaptor = ArgumentCaptor.forClass(TurAIAgent.class);
        org.mockito.Mockito.verify(agentRepository, org.mockito.Mockito.atLeastOnce())
                .save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getPersonas())
                .anyMatch(p -> "persona-first-time-user".equals(p.getId()));
    }

    /**
     * T632 — {@code richContentEnabled} must survive the import so the public
     * demo can ship its agent with rich-content rendering on (```html/```d2)
     * via the seed bundle rather than a manual admin toggle.
     */
    @Test
    void importCarriesRichContentEnabled() {
        TurAIAgentExchange agentExchange = new TurAIAgentExchange();
        agentExchange.setId("agent-turing-demo");
        agentExchange.setTitle("Turing Demo Assistant");
        agentExchange.setEnabled(1);
        agentExchange.setRagEnabled(true);
        agentExchange.setRichContentEnabled(true);

        TurExchange exchange = new TurExchange();
        exchange.setAgents(List.of(agentExchange));

        var result = service.importAgents(exchange, null, true);
        assertThat(result.error()).isNull();

        ArgumentCaptor<TurAIAgent> agentCaptor = ArgumentCaptor.forClass(TurAIAgent.class);
        org.mockito.Mockito.verify(agentRepository, org.mockito.Mockito.atLeastOnce())
                .save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().isRichContentEnabled()).isTrue();
    }

    /**
     * T1331 — a persona's few-shot store and brand-context MCP server are bundled
     * beside it; the imported persona points at the rows the import resolved them to,
     * and an id the bundle does not carry is dropped.
     */
    @Test
    void importPointsThePersonaAtItsBundledStoreAndMcpServer() {
        com.viglet.turing.rag.persistence.model.store.TurStoreInstance localStore =
                new com.viglet.turing.rag.persistence.model.store.TurStoreInstance();
        localStore.setId("store-local");
        when(storeRepository.findById("store-exported")).thenReturn(Optional.empty());
        when(storeRepository.findByTitleIgnoreCase("Examples")).thenReturn(Optional.of(localStore));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        com.viglet.turing.mcp.persistence.model.TurMcpServer localMcp =
                new com.viglet.turing.mcp.persistence.model.TurMcpServer();
        localMcp.setId("mcp-local");
        when(mcpServerRepository.findById("mcp-exported")).thenReturn(Optional.empty());
        when(mcpServerRepository.findByTitleIgnoreCase("Brand")).thenReturn(Optional.of(localMcp));
        when(mcpServerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.viglet.turing.rag.persistence.model.store.TurStoreInstance bundledStore =
                new com.viglet.turing.rag.persistence.model.store.TurStoreInstance();
        bundledStore.setId("store-exported");
        bundledStore.setTitle("Examples");
        com.viglet.turing.mcp.persistence.model.TurMcpServer bundledMcp =
                new com.viglet.turing.mcp.persistence.model.TurMcpServer();
        bundledMcp.setId("mcp-exported");
        bundledMcp.setTitle("Brand");

        TurPersona withRefs = new TurPersona();
        withRefs.setId("persona-voice");
        withRefs.setName("Voice");
        withRefs.setFewShotStoreId("store-exported");
        withRefs.setBrandContextMcpServerId("mcp-exported");
        TurPersona dangling = new TurPersona();
        dangling.setId("persona-dangling");
        dangling.setName("Dangling");
        dangling.setFewShotStoreId("store-not-in-bundle");

        TurExchange exchange = new TurExchange();
        exchange.setStore(List.of(bundledStore));
        exchange.setMcpServers(List.of(bundledMcp));
        exchange.setPersonas(List.of(withRefs, dangling));
        TurAIAgentExchange agentExchange = new TurAIAgentExchange();
        agentExchange.setId("agent-voice");
        agentExchange.setTitle("Voice Agent");
        agentExchange.setEnabled(1);
        agentExchange.setPersonaIds(List.of("persona-voice"));
        exchange.setAgents(List.of(agentExchange));

        assertThat(service.importAgents(exchange, null, true).error()).isNull();

        ArgumentCaptor<TurPersona> saved = ArgumentCaptor.forClass(TurPersona.class);
        org.mockito.Mockito.verify(personaRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        TurPersona voice = saved.getAllValues().get(0);
        assertThat(voice.getFewShotStoreId()).isEqualTo("store-local");
        assertThat(voice.getBrandContextMcpServerId()).isEqualTo("mcp-local");
        assertThat(saved.getAllValues().get(1).getFewShotStoreId()).isNull();
    }

    private void verifySaved(ArgumentCaptor<TurPersona> captor) {
        org.mockito.Mockito.verify(personaRepository).save(captor.capture());
    }
}
