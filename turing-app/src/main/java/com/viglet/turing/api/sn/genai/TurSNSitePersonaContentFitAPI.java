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

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.api.exception.TurPayloadTooLargeException;
import com.viglet.turing.api.exception.TurValidationException;
import com.viglet.turing.genai.persona.fit.TurContentFitMisfit;
import com.viglet.turing.genai.persona.fit.TurContentFitResult;
import com.viglet.turing.genai.persona.fit.TurPersonaContentFitEvaluator;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.persona.TurPersona;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * T634 / §XXVII.4 — the <em>anonymous</em> persona content-fit endpoint that
 * powers the public demo's "validate this content as persona X" mode. It fronts
 * {@link TurPersonaContentFitEvaluator} but, unlike the admin-secured
 * {@link com.viglet.turing.api.persona.TurPersonaContentFitAPI}, is reachable on
 * the read-only path — so it is deliberately narrow:
 *
 * <ul>
 *   <li>the persona must belong to the site's <em>effective</em> agent catalog
 *       (resolved exactly as the SN chat does) AND be usable as an audience —
 *       an unknown/speaker-only id is a 404, never an arbitrary persona. Both
 *       checks live in {@link TurSNSiteAudiencePersonas}, which also answers
 *       {@code GET /api/sn/{site}/personas} (T1143);</li>
 *   <li>the submitted text is hard-capped ({@link #MAX_CONTENT_CHARS}) — this is
 *       an LLM-cost/abuse surface, so oversized input is rejected (413) rather
 *       than silently clipped, and the endpoint is nginx-rate-limited on the
 *       demo host (mirrors the chat guard in {@code docker/DEMO.md});</li>
 *   <li>{@code regenerate} is forced off — a guest can never bust the cache.</li>
 * </ul>
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@RestController
@RequestMapping("/api/sn/{siteName}/persona/{personaId}/content-fit")
@Tag(name = "SN Anonymous Persona Content Fit",
        description = "Read-only content-fit validation as a site-catalog persona")
public class TurSNSitePersonaContentFitAPI {

    /** Hard cap on submitted text — matches the evaluator's own clip threshold. */
    static final int MAX_CONTENT_CHARS = 12_000;

    private final TurSNSiteAudiencePersonas audiencePersonas;
    private final TurPersonaContentFitEvaluator evaluator;

    public TurSNSitePersonaContentFitAPI(TurSNSiteAudiencePersonas audiencePersonas,
            TurPersonaContentFitEvaluator evaluator) {
        this.audiencePersonas = audiencePersonas;
        this.evaluator = evaluator;
    }

    @Operation(summary = "Validate content as a persona from the site agent's catalog")
    @PostMapping
    @Transactional(readOnly = true)
    public TurSNPersonaContentFitResponse evaluate(@PathVariable String siteName,
            @PathVariable String personaId, @RequestBody ContentFitRequest request) {
        if (request == null || StringUtils.isBlank(request.content())) {
            throw new TurValidationException("content is required");
        }
        String content = request.content();
        if (content.length() > MAX_CONTENT_CHARS) {
            throw new TurPayloadTooLargeException(
                    "content exceeds the " + MAX_CONTENT_CHARS + "-character limit");
        }

        // A missing genAi binding is not an error on its own: the resolver falls
        // back to the global Default AI Agent, exactly as SN chat does. Only when
        // neither exists is this a 404.
        TurAIAgent agent = audiencePersonas.agent(siteName)
                .orElseThrow(() -> new TurNotFoundException("No AI agent configured for this site"));

        TurPersona persona = audiencePersonas.find(agent, personaId);

        TurContentFitResult result = evaluator.evaluateText(persona, content, null,
                request.sourceName(), /* regenerate = */ false);
        return new TurSNPersonaContentFitResponse(persona.getId(), persona.getName(),
                result.fitScore(), result.summary(), result.fits(), result.misfits(),
                result.llmUsed());
    }

    /** Guest request body: the text to validate + an optional display name. */
    public record ContentFitRequest(String content, String sourceName) {
    }

    /**
     * The public content-fit verdict: overall fit % ({@code fitScore}), a short
     * {@code summary}, what does fit ({@code fits} / <em>condiz</em>), the flagged
     * spans that don't ({@code misfits} / <em>não condiz</em>, each with a reason
     * and a suggested rewrite), and whether the LLM contributed ({@code llmUsed};
     * false = deterministic readability-only fallback).
     */
    public record TurSNPersonaContentFitResponse(String personaId, String personaName,
            double fitScore, String summary, List<String> fits,
            List<TurContentFitMisfit> misfits, boolean llmUsed) {
    }
}
