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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.sn.search.TurSNSiteSearchService;
import com.viglet.turing.genai.deepdive.TurSNDeepDiveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * T1220 — the deep-dive research stream: a question whose answer is spread over
 * many documents comes back as planned sub-questions, the sources each one found,
 * cited sections and what the index does not cover, as named SSE events.
 *
 * <p>An anonymous LLM-cost surface like {@code /answer}, gated by the same
 * API-key and abuse-control filters. A partitioned key is refused on it: its
 * reads would run off the request thread, where the partition is not carried.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@RestController
@RequestMapping("/api/sn/{siteName}/research")
@Tag(name = "Semantic Navigation Research",
        description = "Deep-dive research: a sectioned, cited overview over an SN site")
public class TurSNResearchAPI {

    private final TurSNDeepDiveService deepDiveService;
    private final TurSNSiteSearchService turSNSiteSearchService;

    public TurSNResearchAPI(TurSNDeepDiveService deepDiveService, TurSNSiteSearchService turSNSiteSearchService) {
        this.deepDiveService = deepDiveService;
        this.turSNSiteSearchService = turSNSiteSearchService;
    }

    @Operation(summary = "Stream a sectioned, cited overview of a question over the site's documents")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> research(@PathVariable String siteName,
            @RequestBody ResearchRequest request) {
        log.info("[SNResearch] research on site '{}'", siteName);
        ResearchRequest body = request == null ? new ResearchRequest(null, null, null, null) : request;
        return deepDiveService.research(siteName, body.question(),
                turSNSiteSearchService.resolveIndexedLocale(body.locale(), siteName), body.filters(),
                body.maxSteps());
    }

    /**
     * The question, an optional locale (a locale the site has no index for falls
     * back to its default indexed one), the filters (field to accepted values)
     * every read stays inside, and the number of sub-questions (3 to 6, default 5).
     */
    public record ResearchRequest(String question, String locale, Map<String, List<String>> filters,
            Integer maxSteps) {
    }
}
