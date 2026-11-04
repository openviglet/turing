/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai.deepdive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.viglet.testsupport.research.TurResearchStubLlmSupport;
import com.viglet.turing.exchange.TurImportExchange;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNKnowledgeBaseMode;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.repository.llm.TurLLMInstanceRepository;
import com.viglet.turing.persistence.repository.llm.TurLLMVendorRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;
import com.viglet.turing.system.TurGlobalSettingsService;
import com.viglet.turing.testutil.AbstractTuringSpringIT;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * T1220b — the deep-dive research stream end to end, over a real HTTP port, on
 * the {@code mythical-creatures} marketplace site seeded into the embedded
 * Lucene engine (the {@code TurSdkContractIT} fixture), with a stubbed default
 * LLM ({@link TurResearchStubLlmSupport}). Retrieval, the caller's filters and
 * the citation checks are the real ones; only the model's words are fixed.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jmx.enabled=true")
@Import(TurResearchStubLlmSupport.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TurSNResearchStreamIT extends AbstractTuringSpringIT {

    private static final String SITE = "mythical-creatures";
    private static final String LOCALE = "en_US";
    private static final String LLM_ID = "t1220b-research-stub";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TurImportExchange importExchange;
    @Autowired
    private TurSNSiteRepository siteRepository;
    @Autowired
    private TurSNSiteGenAiRepository genAiRepository;
    @Autowired
    private TurLLMInstanceRepository llmInstanceRepository;
    @Autowired
    private TurLLMVendorRepository llmVendorRepository;
    @Autowired
    private TurGlobalSettingsService globalSettingsService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = JsonMapper.builder().build();
    private Path luceneDir;
    private String previousDefaultLlm;

    /** Same reason as {@code TurSdkContractIT}: a second RANDOM_PORT context needs its own MBean names. */
    @DynamicPropertySource
    static void uniqueJmxObjectNames(DynamicPropertyRegistry registry) {
        registry.add("spring.jmx.unique-names", () -> "true");
    }

    @BeforeAll
    void seed() throws IOException {
        luceneDir = Files.createTempDirectory("turing-research-it-lucene");
        MockMultipartFile bundle = new MockMultipartFile("file", SITE + ".zip", "application/zip",
                importZip(luceneDir));
        importExchange.importFromMultipartFile(bundle, true, false, null, true);
        assertThat(siteRepository.findByNameIgnoreCase(SITE)).isPresent();

        // A default LLM the stub factory builds, so the copilot route is available.
        TurLLMInstance llm = llmInstanceRepository.findById(LLM_ID).orElseGet(TurLLMInstance::new);
        llm.setId(LLM_ID);
        llm.setTitle("Research stub");
        llm.setEnabled(1);
        llm.setTurLLMVendor(llmVendorRepository.findAll().getFirst());
        llmInstanceRepository.save(llm);
        previousDefaultLlm = globalSettingsService.getDefaultLlmId();
        globalSettingsService.updateDefaultLlmId(LLM_ID);

        // A vectorless site answers through the catalog copilot: no embeddings needed.
        transactionTemplate.executeWithoutResult(status -> {
            TurSNSite site = siteRepository.findByNameIgnoreCase(SITE).orElseThrow();
            TurSNSiteGenAi genAi = site.getTurSNSiteGenAi() != null ? site.getTurSNSiteGenAi()
                    : new TurSNSiteGenAi();
            genAi.setKnowledgeBaseMode(TurSNKnowledgeBaseMode.VECTORLESS_STRUCTURED);
            site.setTurSNSiteGenAi(genAiRepository.save(genAi));
            siteRepository.save(site);
        });
    }

    @AfterAll
    void cleanup() {
        globalSettingsService.updateDefaultLlmId(previousDefaultLlm == null ? "" : previousDefaultLlm);
        if (luceneDir != null) {
            try (var walk = Files.walk(luceneDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch (IOException ignored) {
                // a lingering write.lock on Windows; the OS reaps the temp dir
            }
        }
    }

    @Test
    void everySectionCitesAListedSourceAndTheEmptyStepIsReported() throws Exception {
        List<Event> events = research("{\"question\": \"What do myths say about dragons and sea monsters?\","
                + " \"locale\": \"" + LOCALE + "\"}");

        assertThat(events).extracting(Event::name).contains("plan", "step", "section", "uncovered", "sources")
                .last().isEqualTo("done");
        int sourceCount = sourceIds(events).size();
        List<Event> sections = named(events, "section");
        assertThat(sections).isNotEmpty();
        for (Event section : sections) {
            List<Integer> cited = new ArrayList<>();
            section.data().path("citations").forEach(n -> cited.add(n.asInt()));
            assertThat(cited).isNotEmpty().allMatch(n -> n >= 1 && n <= sourceCount);
            assertThat(section.data().path("text").asString()).doesNotContain("[99]");
        }
        // The uncited "Opinion" section never reaches the stream.
        assertThat(sections).extracting(s -> s.data().path("title").asString()).doesNotContain("Opinion");

        Event emptyStep = named(events, "step").stream()
                .filter(s -> TurResearchStubLlmSupport.EMPTY_STEP.equals(s.data().path("question").asString()))
                .findFirst().orElseThrow();
        assertThat(emptyStep.data().path("hits").asInt()).isZero();
        List<String> uncovered = new ArrayList<>();
        named(events, "uncovered").getFirst().data().path("items").forEach(i -> uncovered.add(i.asString()));
        assertThat(uncovered).contains(TurResearchStubLlmSupport.EMPTY_STEP, "Opinion");
    }

    @Test
    void noSourceFallsOutsideTheCallersFilters() throws Exception {
        Set<String> dragons = idsWith("creature_type", "Dragon");
        assertThat(dragons).isNotEmpty();

        List<Event> events = research("{\"question\": \"What do myths say about dragons and sea monsters?\","
                + " \"locale\": \"" + LOCALE + "\", \"filters\": {\"creature_type\": [\"Dragon\"]}}");

        Set<String> cited = sourceIds(events);
        assertThat(cited).isNotEmpty().isSubsetOf(dragons);
    }

    // ── helpers ──

    private record Event(String name, JsonNode data) {
    }

    private List<Event> research(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/api/sn/" + SITE + "/research"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        List<Event> events = new ArrayList<>();
        String name = null;
        StringBuilder data = new StringBuilder();
        for (String line : (response.body() + "\n\n").split("\\r?\\n", -1)) {
            if (line.isEmpty()) {
                if (name != null) {
                    events.add(new Event(name, json.readTree(data.toString())));
                }
                name = null;
                data.setLength(0);
            } else if (line.startsWith("event:")) {
                name = line.substring(6).strip();
            } else if (line.startsWith("data:")) {
                data.append(line.substring(5).strip());
            }
        }
        assertThat(events).as("stream: %s", response.body()).isNotEmpty();
        assertThat(events.getLast().name()).as("stream: %s", response.body()).isNotEqualTo("error");
        return events;
    }

    private static List<Event> named(List<Event> events, String name) {
        return events.stream().filter(e -> name.equals(e.name())).toList();
    }

    private static Set<String> sourceIds(List<Event> events) {
        Set<String> ids = new HashSet<>();
        named(events, "sources").getFirst().data().path("groups")
                .forEach(group -> group.forEach(source -> ids.add(source.path("id").asString())));
        return ids;
    }

    /** The fixture's ids whose multi-valued {@code field} holds {@code value}. */
    private Set<String> idsWith(String field, String value) throws IOException {
        Set<String> ids = new HashSet<>();
        for (JsonNode doc : json.readTree(resource("/sdk-it/mythical-creatures_content.json")).path(SITE)
                .path(LOCALE)) {
            for (JsonNode v : doc.path(field)) {
                if (value.equals(v.asString())) {
                    ids.add(doc.path("id").asString());
                }
            }
        }
        return ids;
    }

    private byte[] importZip(Path indexDir) throws IOException {
        ObjectNode export = (ObjectNode) json.readTree(resource("/sdk-it/export.json"));
        ((ObjectNode) export.path("se").get(0)).put("endpointUrl", indexDir.toString().replace('\\', '/'));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("export.json"));
            zip.write(json.writeValueAsBytes(export));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(SITE + "_content.json"));
            zip.write(resource("/sdk-it/mythical-creatures_content.json"));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private byte[] resource(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return in.readAllBytes();
        }
    }
}
