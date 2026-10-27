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

import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The audience personas a site's client may offer (T1143): exactly the ids the
 * anonymous {@code /persona/{personaId}/content-fit} endpoint accepts, read through
 * the same {@link TurSNSiteAudiencePersonas}. Anonymous and gated like content-fit,
 * but it calls no model, and it carries no prompt or private persona field.
 *
 * <p>A site with no agent (and no Default AI Agent) answers an empty list: there is
 * nothing to offer, which is not an error for a picker.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@RestController
@RequestMapping("/api/sn/{siteName}/personas")
@Tag(name = "SN Audience Personas", description = "The audience personas a site's content-fit accepts")
public class TurSNSitePersonaAPI {

    private final TurSNSiteAudiencePersonas audiencePersonas;

    public TurSNSitePersonaAPI(TurSNSiteAudiencePersonas audiencePersonas) {
        this.audiencePersonas = audiencePersonas;
    }

    /** One persona a client may name: id, display name and its short description. */
    public record TurSNSitePersonaOption(String id, String name, String description) {
    }

    @Operation(summary = "List the audience personas this site's content-fit endpoint accepts")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public List<TurSNSitePersonaOption> list(@PathVariable String siteName) {
        return audiencePersonas.agent(siteName)
                .map(agent -> audiencePersonas.of(agent).stream()
                        .map(p -> new TurSNSitePersonaOption(p.getId(), p.getName(), p.getDescription()))
                        .toList())
                .orElseGet(List::of);
    }
}
