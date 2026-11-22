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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.persona.persistence.model.TurPersona;

/**
 * T1315 — the agent holds its persona catalog and default by id; these are the
 * reads that turn them back into rows.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurAgentPersonasTest {

    private final InMemoryAgentPersonas personas = new InMemoryAgentPersonas();

    private static TurPersona persona(String id) {
        TurPersona p = new TurPersona();
        p.setId(id);
        p.setName(id);
        return p;
    }

    @Test
    void catalogSkipsIdsWhosePersonaIsGone() {
        TurAIAgent agent = personas.attach(new TurAIAgent(), persona("p-1"));
        agent.getPersonaIds().add("p-deleted");

        assertThat(personas.service().of(agent)).extracting(TurPersona::getId).containsExactly("p-1");
    }

    @Test
    void defaultIsReadOnlyFromTheCatalog() {
        TurAIAgent agent = personas.attachDefault(new TurAIAgent(), persona("p-default"));
        assertThat(personas.service().defaultOf(agent).getId()).isEqualTo("p-default");

        agent.getPersonaIds().remove("p-default");
        assertThat(personas.service().defaultOf(agent)).isNull();
    }

    @Test
    void personaOutsideTheCatalogIsNotFound() {
        TurAIAgent agent = personas.attach(new TurAIAgent(), persona("p-in"));
        personas.attach(new TurAIAgent(), persona("p-elsewhere"));

        assertThat(personas.service().inCatalog(agent, "p-in")).isNotNull();
        assertThat(personas.service().inCatalog(agent, "p-elsewhere")).isNull();
        assertThat(personas.service().inCatalog(agent, null)).isNull();
    }

    @Test
    void agentWithoutPersonasHasAnEmptyCatalog() {
        assertThat(personas.service().of(new TurAIAgent())).isEmpty();
        assertThat(personas.service().defaultOf(new TurAIAgent())).isNull();
    }
}
