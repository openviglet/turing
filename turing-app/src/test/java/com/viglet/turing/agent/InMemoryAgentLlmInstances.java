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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mockito.quality.Strictness;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;

/**
 * T1315 — a real {@link TurAgentLlmInstances} over an in-memory LLM instance
 * table, for unit tests: the agent holds LLM instance ids, and the instances a
 * test attaches here are the rows those ids resolve to.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class InMemoryAgentLlmInstances {

    private final Map<String, TurLLMInstance> rows = new LinkedHashMap<>();
    private final TurAgentLlmInstances service;

    public InMemoryAgentLlmInstances() {
        TurLLMInstanceRepository repository = mock(TurLLMInstanceRepository.class,
                withSettings().strictness(Strictness.LENIENT));
        when(repository.findAllById(any())).thenAnswer(inv -> {
            List<TurLLMInstance> found = new ArrayList<>();
            for (String id : inv.<Iterable<String>>getArgument(0)) {
                TurLLMInstance instance = rows.get(id);
                if (instance != null) {
                    found.add(instance);
                }
            }
            return found;
        });
        this.service = new TurAgentLlmInstances(repository);
    }

    public TurAgentLlmInstances service() {
        return service;
    }

    /**
     * Registers the instances and adds them to the agent's LLM set. An instance
     * without an id gets one, since the agent can only name it by id.
     */
    public TurAIAgent attach(TurAIAgent agent, TurLLMInstance... instances) {
        for (TurLLMInstance instance : instances) {
            if (instance.getId() == null) {
                instance.setId("llm-" + (rows.size() + 1));
            }
            rows.put(instance.getId(), instance);
            agent.getLlmInstanceIds().add(instance.getId());
        }
        return agent;
    }
}
