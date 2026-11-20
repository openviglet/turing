/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;

/**
 * T1315 — the agent holds its LLM instances by id; this is the read that turns
 * them back into rows.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurAgentLlmInstancesTest {

    private final InMemoryAgentLlmInstances llms = new InMemoryAgentLlmInstances();

    private static TurLLMInstance llm(String id) {
        TurLLMInstance instance = new TurLLMInstance();
        instance.setId(id);
        return instance;
    }

    @Test
    void instancesComeBackInIdOrderSoTheFirstIsStable() {
        TurAIAgent agent = llms.attach(new TurAIAgent(), llm("llm-c"), llm("llm-a"), llm("llm-b"));

        assertThat(llms.service().of(agent)).extracting(TurLLMInstance::getId)
                .containsExactly("llm-a", "llm-b", "llm-c");
    }

    @Test
    void idsWhoseInstanceIsGoneAreSkipped() {
        TurAIAgent agent = llms.attach(new TurAIAgent(), llm("llm-1"));
        agent.getLlmInstanceIds().add("llm-deleted");

        assertThat(llms.service().of(agent)).extracting(TurLLMInstance::getId).containsExactly("llm-1");
    }

    @Test
    void agentWithoutLlmsHasNone() {
        assertThat(llms.service().of(new TurAIAgent())).isEmpty();
        assertThat(llms.service().of(null)).isEmpty();
    }
}
