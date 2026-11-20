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
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.exchange.TurExchange;
import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.agent.persistence.model.TurAIAgentSlot;
import com.viglet.turing.agent.persistence.model.TurAIAgentSlotType;
import com.viglet.turing.agent.persistence.model.TurChatFlow;
import com.viglet.turing.agent.persistence.model.TurChatFlowGuardrailMethod;
import com.viglet.turing.agent.persistence.model.TurChatFlowTriggerMode;
import com.viglet.turing.agent.persistence.model.customtool.TurCustomTool;
import com.viglet.turing.agent.persistence.model.intent.TurIntent;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.mcp.persistence.model.TurMcpServer;
import com.viglet.turing.mcp.persistence.repository.TurMcpServerRepository;
import com.viglet.turing.rag.persistence.model.store.TurStoreInstance;
import com.viglet.turing.rag.persistence.repository.store.TurStoreInstanceRepository;
import com.viglet.turing.persona.persistence.model.TurPersona;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.agent.persistence.repository.TurAIAgentSlotRepository;
import com.viglet.turing.agent.persistence.repository.TurAgentEvalSetRepository;
import com.viglet.turing.agent.persistence.repository.TurChatFlowRepository;
import com.viglet.turing.agent.persistence.repository.intent.TurIntentRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the export contract: ZIP layout, file naming, secret omission, and
 * envelope structure. No Spring context — wires mocked repositories so the
 * test runs in ~30 ms on every {@code mvn test}.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.8
 */
@ExtendWith(MockitoExtension.class)
class TurAIAgentExportServiceTest {

    @Mock private TurAIAgentRepository agentRepository;
    @Mock private TurChatFlowRepository chatFlowRepository;
    @Mock private TurAIAgentSlotRepository slotRepository;
    @Mock private TurIntentRepository intentRepository;
    @Mock private TurAgentEvalSetRepository evalSetRepository;
    @Mock private TurMcpServerRepository mcpServerRepository;
    @Mock private TurStoreInstanceRepository storeInstanceRepository;
    @Mock private com.viglet.turing.llm.persistence.repository.embedding.TurEmbeddingModelRepository embeddingModelRepository;

    private TurAIAgentExportService service;
    private Path producedZip;

    @BeforeEach
    void setUp() {
        service = new TurAIAgentExportService(agentRepository, chatFlowRepository,
                slotRepository, intentRepository, evalSetRepository, mcpServerRepository,
                storeInstanceRepository, embeddingModelRepository);
    }

    @AfterEach
    void cleanup() {
        if (producedZip != null) {
            service.cleanup(producedZip);
            producedZip = null;
        }
    }

    @Test
    void export_producesZipWithAgentJson_chatFlowFile_groovyFile_andStripsApiKey() throws Exception {
        // ─── arrange: persona, LLM (with apiKey), custom tool, chat flow ───
        TurPersona persona = new TurPersona();
        persona.setId(UUID.randomUUID().toString());
        persona.setName("Marina — Consultora EE");
        persona.setEnabled(1);
        // T1315 — the persona holds its few-shot store and brand MCP server by id.
        TurStoreInstance fewShotStore = new TurStoreInstance();
        fewShotStore.setId(UUID.randomUUID().toString());
        fewShotStore.setTitle("Few-shot store");
        persona.setFewShotStoreId(fewShotStore.getId());
        TurMcpServer brandMcp = new TurMcpServer();
        brandMcp.setId(UUID.randomUUID().toString());
        brandMcp.setTitle("Brand MCP");
        persona.setBrandContextMcpServerId(brandMcp.getId());

        TurLLMInstance llm = new TurLLMInstance();
        llm.setId(UUID.randomUUID().toString());
        llm.setTitle("OpenAI prod");
        llm.setUrl("https://api.openai.com/v1");
        llm.setModelName("gpt-4o-mini");
        llm.setApiKey("sk-SECRET-MUST-NOT-LEAK"); // transient + write-only
        llm.setApiKeyEncrypted("ENC-MUST-NOT-LEAK");
        llm.setEnabled(1);

        TurCustomTool tool = new TurCustomTool();
        tool.setId(UUID.randomUUID().toString());
        tool.setTitle("search_ee_programs");
        tool.setGroovyScript("// groovy script body\nreturn 'ok'");
        tool.setLlmDescription("Searches programs");
        tool.setParametersJson("[]");
        tool.setReturnType("markdown");
        tool.setEnabled(1);

        TurAIAgent agent = new TurAIAgent();
        agent.setId(UUID.randomUUID().toString());
        agent.setTitle("Marina Agent");
        agent.setEnabled(1);
        agent.setRichContentEnabled(true); // T632 — must survive the export envelope
        agent.setPersonas(new HashSet<>(List.of(persona)));
        agent.useDefaultPersona(persona);
        agent.setLlmInstances(new HashSet<>(List.of(llm)));
        agent.setCustomTools(new HashSet<>(List.of(tool)));

        TurChatFlow flow = new TurChatFlow();
        flow.setId(UUID.randomUUID().toString());
        flow.setName("Programa-Match — EE");
        flow.setEnabled(1);
        flow.setGuardrailMethod(TurChatFlowGuardrailMethod.LLM_JUDGE);
        flow.setTriggerMode(TurChatFlowTriggerMode.ALWAYS);
        flow.setDefinitionJson("{\"nodes\":[{\"id\":\"node-1\"}],\"edges\":[]}");

        TurAIAgentSlot slot = new TurAIAgentSlot();
        slot.setId(UUID.randomUUID().toString());
        slot.setName("name");
        slot.setType(TurAIAgentSlotType.STRING);

        TurIntent intent = new TurIntent();
        intent.setId(UUID.randomUUID().toString());
        intent.setTitle("Plano de carreira pessoal");
        intent.setEnabled(1);

        when(agentRepository.findById(agent.getId())).thenReturn(java.util.Optional.of(agent));
        when(storeInstanceRepository.findById(fewShotStore.getId()))
                .thenReturn(java.util.Optional.of(fewShotStore));
        when(mcpServerRepository.findById(brandMcp.getId())).thenReturn(java.util.Optional.of(brandMcp));
        when(slotRepository.findByTurAIAgent_IdOrderByNameAsc(agent.getId()))
                .thenReturn(List.of(slot));
        when(intentRepository.findByTurAIAgent_IdOrderByTitleAsc(agent.getId()))
                .thenReturn(List.of(intent));
        when(chatFlowRepository.findByTurAIAgent_IdOrderByNameAsc(agent.getId()))
                .thenReturn(List.of(flow));

        // ─── act ───
        producedZip = service.exportAgentToZip(agent.getId());

        // ─── assert: ZIP entries ───
        Map<String, byte[]> entries = readZip(producedZip);
        assertThat(entries).containsKey("export.json");
        assertThat(entries.keySet())
                .anyMatch(k -> k.startsWith("chat-flows/") && k.endsWith(".chat-flow.json"));
        assertThat(entries.keySet())
                .anyMatch(k -> k.startsWith("tools/") && k.endsWith(".groovy"));

        // ─── assert: chat-flow file carries the raw definitionJson ───
        String flowFileKey = entries.keySet().stream()
                .filter(k -> k.startsWith("chat-flows/"))
                .findFirst().orElseThrow();
        String flowFileContent = new String(entries.get(flowFileKey), StandardCharsets.UTF_8);
        assertThat(flowFileContent).contains("\"nodes\"").contains("node-1");

        // ─── assert: tool file carries the Groovy script verbatim ───
        String toolFileKey = entries.keySet().stream()
                .filter(k -> k.startsWith("tools/"))
                .findFirst().orElseThrow();
        String toolFileContent = new String(entries.get(toolFileKey), StandardCharsets.UTF_8);
        assertThat(toolFileContent).contains("// groovy script body").contains("return 'ok'");

        // ─── assert: agent.json envelope shape + NO secrets ───
        String agentJson = new String(entries.get("export.json"), StandardCharsets.UTF_8);
        assertThat(agentJson)
                .as("LLM secrets must never appear in the export envelope")
                .doesNotContain("sk-SECRET-MUST-NOT-LEAK")
                .doesNotContain("ENC-MUST-NOT-LEAK")
                .as("Chat-flow definitionJson must NOT be inlined — it goes to a sibling file")
                .doesNotContain("\"definitionJson\"")
                .as("Custom-tool groovyScript must NOT be inlined — it goes to a sibling file")
                .doesNotContain("\"groovyScript\"");

        TurExchange parsed = JsonMapper.builder().build().readValue(agentJson, TurExchange.class);
        assertThat(parsed.getAgents()).hasSize(1);
        TurAIAgentExchange agentExchange = parsed.getAgents().get(0);
        assertThat(agentExchange.getTitle()).isEqualTo("Marina Agent");
        assertThat(agentExchange.isRichContentEnabled()).isTrue();
        assertThat(agentExchange.getPersonaIds()).containsExactly(persona.getId());
        assertThat(agentExchange.getDefaultPersonaId()).isEqualTo(persona.getId());
        assertThat(agentExchange.getLlmInstanceIds()).containsExactly(llm.getId());
        assertThat(agentExchange.getCustomToolIds()).containsExactly(tool.getId());
        assertThat(agentExchange.getSlots()).hasSize(1);
        assertThat(agentExchange.getIntents()).hasSize(1);
        assertThat(agentExchange.getChatFlows()).hasSize(1);
        assertThat(agentExchange.getChatFlows().get(0).getDefinitionFile())
                .startsWith("chat-flows/").endsWith(".chat-flow.json");

        assertThat(parsed.getPersonas()).hasSize(1);
        assertThat(parsed.getPersonas().get(0).getFewShotStoreId()).isEqualTo(fewShotStore.getId());
        assertThat(parsed.getStore()).extracting(TurStoreInstance::getId)
                .containsExactly(fewShotStore.getId());
        assertThat(parsed.getMcpServers()).extracting(TurMcpServer::getId)
                .containsExactly(brandMcp.getId());
        assertThat(parsed.getLlm()).hasSize(1);
        assertThat(parsed.getCustomTools()).hasSize(1);
        assertThat(parsed.getCustomTools().get(0).getGroovyScriptFile())
                .startsWith("tools/").endsWith(".groovy");
    }

    @Test
    void export_emptyAgent_producesZipWithJustAgentJson() throws Exception {
        TurAIAgent agent = new TurAIAgent();
        agent.setId(UUID.randomUUID().toString());
        agent.setTitle("Empty Agent");
        agent.setEnabled(1);

        when(agentRepository.findById(agent.getId())).thenReturn(java.util.Optional.of(agent));
        when(slotRepository.findByTurAIAgent_IdOrderByNameAsc(agent.getId())).thenReturn(List.of());
        when(intentRepository.findByTurAIAgent_IdOrderByTitleAsc(agent.getId())).thenReturn(List.of());
        when(chatFlowRepository.findByTurAIAgent_IdOrderByNameAsc(agent.getId())).thenReturn(List.of());

        producedZip = service.exportAgentToZip(agent.getId());

        Map<String, byte[]> entries = readZip(producedZip);
        assertThat(entries).containsOnlyKeys("export.json");

        TurExchange parsed = JsonMapper.builder().build()
                .readValue(entries.get("export.json"), TurExchange.class);
        assertThat(parsed.getAgents()).hasSize(1);
        assertThat(parsed.getPersonas()).as("Empty lists omitted via JsonInclude.NON_NULL").isNull();
        assertThat(parsed.getLlm()).isNull();
        assertThat(parsed.getCustomTools()).isNull();
    }

    private static Map<String, byte[]> readZip(Path zip) throws Exception {
        java.util.Map<String, byte[]> out = new java.util.LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(zip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    out.put(entry.getName().replace('\\', '/'), zis.readAllBytes());
                }
            }
        }
        return out;
    }
}
