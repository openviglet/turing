/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent.persona;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.persona.persistence.model.TurPersona;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;

/**
 * T1315 — the personas an agent names. The agent holds its catalog and its
 * default as ids (the personas belong to this module); this loads the rows.
 * Ids whose persona is gone are skipped.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurAgentPersonas {

    private final TurPersonaRepository personaRepository;

    public TurAgentPersonas(TurPersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    /** The agent's catalog, in one query; empty when it names none. */
    public Set<TurPersona> of(TurAIAgent agent) {
        if (agent == null || agent.getPersonaIds() == null || agent.getPersonaIds().isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(personaRepository.findAllById(agent.getPersonaIds()));
    }

    /**
     * The persona with {@code personaId} when it is in the agent's catalog and
     * still exists, else {@code null}.
     */
    public TurPersona inCatalog(TurAIAgent agent, String personaId) {
        if (agent == null || personaId == null || personaId.isBlank()
                || agent.getPersonaIds() == null || !agent.getPersonaIds().contains(personaId)) {
            return null;
        }
        return personaRepository.findById(personaId).orElse(null);
    }

    /** The agent's default persona, when set, in its catalog and still there; else {@code null}. */
    public TurPersona defaultOf(TurAIAgent agent) {
        return agent == null ? null : inCatalog(agent, agent.getDefaultPersonaId());
    }
}
