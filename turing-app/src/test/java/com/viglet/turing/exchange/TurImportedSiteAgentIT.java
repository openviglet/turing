/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import com.viglet.turing.api.sn.genai.TurSNSitePersonaAPI;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.repository.agent.TurAIAgentRepository;
import com.viglet.turing.system.TurGlobalSettingsService;
import com.viglet.turing.testutil.AbstractTuringSpringIT;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * T1192 — a bundle whose site names an agent of the same bundle keeps that link:
 * the site offers its own agent's audience personas while the global Default AI
 * Agent is another agent, instead of falling back to it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
class TurImportedSiteAgentIT extends AbstractTuringSpringIT {

    private static final String SITE = "t1192-site";
    private static final String AGENT = "t1192-agent";
    private static final String PERSONA = "t1192-persona";
    private static final String OTHER_DEFAULT = "t1192-other-default";

    @Autowired
    private TurImportExchange importExchange;
    @Autowired
    private TurSNSitePersonaAPI personaAPI;
    @Autowired
    private TurGlobalSettingsService globalSettingsService;
    @Autowired
    private TurAIAgentRepository agentRepository;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void anImportedSiteOffersItsOwnAgentsPersonasNotTheDefaultAgents() throws IOException {
        // The H2 file outlives a run, so the other default agent may already exist.
        agentRepository.findById(OTHER_DEFAULT).orElseGet(() -> {
            TurAIAgent other = new TurAIAgent();
            other.setId(OTHER_DEFAULT);
            other.setTitle("T1192 other default");
            return agentRepository.save(other);
        });
        String previousDefault = globalSettingsService.getDefaultAiAgentId();
        globalSettingsService.updateDefaultAiAgentId(OTHER_DEFAULT);
        Path luceneDir = Files.createTempDirectory("turing-t1192-lucene");
        try {
            var result = importExchange.importFromMultipartFile(new MockMultipartFile("file", "t1192.zip",
                    "application/zip", bundle(luceneDir)), false, false, null, true);

            assertTrue(result.siteImported(), "the bundle's site must be imported: " + result);
            assertEquals(List.of(), result.warnings(), "the site's agent is in the bundle");
            assertEquals(List.of(PERSONA), personaAPI.list(SITE).stream()
                    .map(TurSNSitePersonaAPI.TurSNSitePersonaOption::id).toList(),
                    "the site must offer its own agent's personas, not the default agent's");
        } finally {
            globalSettingsService.updateDefaultAiAgentId(previousDefault);
        }
    }

    /** The SDK contract fixture's site, re-identified, naming an agent and persona of its own bundle. */
    private byte[] bundle(Path luceneDir) throws IOException {
        ObjectNode root = (ObjectNode) json.readTree(readResource("/sdk-it/export.json"));
        ObjectNode se = (ObjectNode) root.path("se").get(0);
        se.put("id", "t1192-lucene-se");
        se.put("endpointUrl", luceneDir.toString().replace('\\', '/'));
        ObjectNode site = (ObjectNode) root.path("snSites").get(0);
        site.put("id", "t1192-site-id");
        site.put("name", SITE);
        site.put("turSEInstance", "t1192-lucene-se");
        site.path("turSNSiteLocales").forEach(locale -> ((ObjectNode) locale).put("core",
                SITE + "_" + locale.path("language").asString()));
        ObjectNode genAi = site.putObject("turSNSiteGenAi");
        genAi.put("id", "t1192-genai");
        genAi.put("turAIAgent", AGENT);

        ObjectNode agent = ((ArrayNode) root.putArray("agents")).addObject();
        agent.put("id", AGENT);
        agent.put("title", "T1192 bundle agent");
        agent.put("enabled", 1);
        agent.putArray("personaIds").add(PERSONA);
        ObjectNode persona = ((ArrayNode) root.putArray("personas")).addObject();
        persona.put("id", PERSONA);
        persona.put("name", "T1192 buyer");
        persona.put("personaKind", "AUDIENCE");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("export.json"));
            zip.write(json.writeValueAsBytes(root));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private byte[] readResource(String classpath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(classpath)) {
            assertNotNull(in, "missing test resource: " + classpath);
            return in.readAllBytes();
        }
    }
}
