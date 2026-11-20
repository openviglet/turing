/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.persona;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mockito.quality.Strictness;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.persona.persistence.model.TurPersona;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;

/**
 * T1315 — a real {@link TurAgentPersonas} over an in-memory persona table, for
 * unit tests: the agent holds persona ids, and the personas a test registers
 * here are the rows those ids resolve to.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class InMemoryAgentPersonas {

    private final Map<String, TurPersona> rows = new LinkedHashMap<>();
    private final TurAgentPersonas service;

    public InMemoryAgentPersonas() {
        TurPersonaRepository repository = mock(TurPersonaRepository.class,
                withSettings().strictness(Strictness.LENIENT));
        when(repository.findById(any())).thenAnswer(inv -> Optional.ofNullable(rows.get(inv.<String>getArgument(0))));
        when(repository.findAllById(any())).thenAnswer(inv -> {
            List<TurPersona> found = new ArrayList<>();
            for (String id : inv.<Iterable<String>>getArgument(0)) {
                TurPersona persona = rows.get(id);
                if (persona != null) {
                    found.add(persona);
                }
            }
            return found;
        });
        this.service = new TurAgentPersonas(repository);
    }

    public TurAgentPersonas service() {
        return service;
    }

    /** Registers the personas and adds them to the agent's catalog. */
    public TurAIAgent attach(TurAIAgent agent, TurPersona... personas) {
        for (TurPersona persona : personas) {
            rows.put(persona.getId(), persona);
            agent.getPersonaIds().add(persona.getId());
        }
        return agent;
    }

    /** Registers the persona, adds it to the catalog and makes it the default. */
    public TurAIAgent attachDefault(TurAIAgent agent, TurPersona persona) {
        rows.put(persona.getId(), persona);
        agent.useDefaultPersona(persona.getId());
        return agent;
    }
}
