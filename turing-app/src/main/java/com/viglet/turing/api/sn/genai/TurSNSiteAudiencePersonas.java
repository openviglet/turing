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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.persona.TurPersona;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.sn.TurSNSearchProcess;

/**
 * The audience personas a site's anonymous clients may name (T1143): the personas
 * of the site's <em>effective</em> agent (resolved exactly as SN chat does, falling
 * back to the global Default AI Agent) that are usable as an audience.
 *
 * <p>Both {@link TurSNSitePersonaContentFitAPI} and {@link TurSNSitePersonaAPI} read
 * through here, so the list a client is offered and the ids content-fit accepts
 * are one computation and cannot drift apart.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Component
public class TurSNSiteAudiencePersonas {

    private final TurSNSearchProcess turSNSearchProcess;
    private final TurDefaultAgentResolver turDefaultAgentResolver;

    public TurSNSiteAudiencePersonas(TurSNSearchProcess turSNSearchProcess,
            TurDefaultAgentResolver turDefaultAgentResolver) {
        this.turSNSearchProcess = turSNSearchProcess;
        this.turDefaultAgentResolver = turDefaultAgentResolver;
    }

    /**
     * The site's effective agent, or empty when neither a site agent nor a default
     * agent exists. An unknown site is a 404.
     */
    public Optional<TurAIAgent> agent(String siteName) {
        // Look the site up first: mapping straight to the agent would read a null
        // agent as an absent site.
        TurSNSite site = turSNSearchProcess.getSNSite(siteName)
                .orElseThrow(() -> new TurNotFoundException("Site not found: " + siteName));
        return Optional.ofNullable(turDefaultAgentResolver.resolveEffectiveAgent(site.getTurSNSiteGenAi()));
    }

    /** The agent's personas usable as an audience, ordered by name. */
    public List<TurPersona> of(TurAIAgent agent) {
        if (agent.getPersonas() == null) {
            return List.of();
        }
        return agent.getPersonas().stream()
                .filter(TurPersona::isUsableAsAudience)
                .sorted(Comparator.comparing(TurPersona::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /**
     * Resolves {@code personaId} to one of the agent's audience personas. Anything
     * else (unknown id, or a speaker-only persona) is a 404, so a guest can never
     * point the evaluator at a persona the agent operator did not attach.
     */
    public TurPersona find(TurAIAgent agent, String personaId) {
        return of(agent).stream()
                .filter(p -> personaId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new TurNotFoundException("Persona not found in this site's catalog"));
    }
}
