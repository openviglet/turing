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

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;

/**
 * T1315 — the LLM instances an agent names. The agent holds them as ids (the
 * instances belong to the llm module); this loads the rows in one query, in id
 * order so "the first one" is the same on every read. Ids whose instance is gone
 * are skipped.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurAgentLlmInstances {

    private final TurLLMInstanceRepository llmInstanceRepository;

    public TurAgentLlmInstances(TurLLMInstanceRepository llmInstanceRepository) {
        this.llmInstanceRepository = llmInstanceRepository;
    }

    public Set<TurLLMInstance> of(TurAIAgent agent) {
        if (agent == null || agent.getLlmInstanceIds() == null || agent.getLlmInstanceIds().isEmpty()) {
            return Set.of();
        }
        Set<TurLLMInstance> instances = new LinkedHashSet<>();
        llmInstanceRepository.findAllById(agent.getLlmInstanceIds()).stream()
                .sorted(Comparator.comparing(TurLLMInstance::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(instances::add);
        return instances;
    }
}
