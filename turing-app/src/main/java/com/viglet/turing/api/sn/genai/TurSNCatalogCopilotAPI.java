/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.api.sn.genai;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.genai.catalog.TurCatalogCopilotResult;
import com.viglet.turing.genai.catalog.TurCatalogCopilotService;
import com.viglet.turing.genai.catalog.TurCatalogCopilotService.ChatTurn;
import com.viglet.turing.genai.catalog.TurCatalogKitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * T392 / §XX.12 — REST surface for the grounded conversational <strong>catalog
 * copilot</strong>: "talk to the catalog" over one Semantic Navigation site. A
 * multi-turn conversation is answered <em>strictly</em> from the site's search
 * index, with citations and objective ranking, by
 * {@link TurCatalogCopilotService}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@RestController
@RequestMapping("/api/sn/{siteName}/copilot")
@Tag(name = "Catalog Copilot", description = "Grounded conversational catalog assistant (T392)")
public class TurSNCatalogCopilotAPI {

    private final TurCatalogCopilotService copilotService;
    private final TurCatalogKitService kitService;

    public TurSNCatalogCopilotAPI(TurCatalogCopilotService copilotService, TurCatalogKitService kitService) {
        this.copilotService = copilotService;
        this.kitService = kitService;
    }

    /** Whether the copilot can answer (a default LLM is configured). */
    @GetMapping(value = "/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> available() {
        return Map.of("available", copilotService.isAvailable());
    }

    /**
     * Answer a (possibly multi-turn) catalog conversation grounded in the site's
     * index. The latest user turn drives retrieval; the full history is used to
     * resolve conversational follow-ups.
     */
    @Operation(summary = "Answer a grounded, cited catalog conversation")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TurCatalogCopilotResult chat(@PathVariable String siteName,
            @RequestBody CopilotRequest request) {
        List<ChatTurn> turns = request.messages() == null ? List.of()
                : request.messages().stream()
                        .map(m -> new ChatTurn(m.role(), m.content()))
                        .toList();
        log.info("[CatalogCopilot] chat on site '{}' with {} turns", siteName, turns.size());
        return copilotService.answer(siteName, request.locale(), turns, request.documentId());
    }

    /**
     * T1159 — turns a described plan into a staged kit: the needs it implies, each
     * with the catalog products retrieval found for it.
     */
    @Operation(summary = "Stage the catalog products a described plan needs, grouped by need")
    @PostMapping(value = "/kit", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TurCatalogKitService.Result kit(@PathVariable String siteName, @RequestBody KitRequest request) {
        log.info("[CatalogKit] kit on site '{}'", siteName);
        return kitService.plan(siteName, request.locale(), request.plan(),
                new TurCatalogKitService.Options(request.maxGaps(), request.perGap(),
                        request.availabilityField(), request.availability(), request.guideTypes(),
                        request.productTypes()));
    }

    /**
     * T1159 — the plan, an optional locale and optional limits (gaps, products per
     * gap). {@code availabilityField} names the product field that says whether it
     * can be bought; {@code availability} is {@code MARK} (default: keep it, with
     * {@code available: false}) or {@code DROP}. T1167 — {@code guideTypes} names the
     * document types that count as a guide, cited beside the stages. T1175 —
     * {@code productTypes} names the document types that may be staged as products.
     */
    public record KitRequest(String plan, String locale, Integer maxGaps, Integer perGap,
            String availabilityField, TurCatalogKitService.Availability availability, List<String> guideTypes,
            List<String> productTypes) {
    }

    /**
     * Request body: the conversation so far plus an optional locale. T1178 —
     * {@code documentId}, optional, is the indexed document the question is about
     * (the product on the page): pinned first in the answer's context, never a filter.
     */
    public record CopilotRequest(List<Message> messages, String locale, String documentId) {

        /** The body before T1178: no pinned document. */
        public CopilotRequest(List<Message> messages, String locale) {
            this(messages, locale, null);
        }
    }

    public record Message(String role, String content) {
    }
}
