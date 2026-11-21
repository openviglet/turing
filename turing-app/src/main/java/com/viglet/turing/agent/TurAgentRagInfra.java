/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.agent;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.llm.persistence.model.embedding.TurEmbeddingModel;
import com.viglet.turing.llm.persistence.repository.embedding.TurEmbeddingModelRepository;
import com.viglet.turing.llm.persistence.model.store.TurStoreInstance;
import com.viglet.turing.llm.persistence.repository.store.TurStoreInstanceRepository;

/**
 * Resolves an agent's RAG infrastructure (T1315). The agent holds its embedding
 * model and its vector store by id, because they belong to the llm and rag modules;
 * this reads each through the module that owns it, so callers that need the entity
 * ask here rather than walking a cross-module JPA association.
 *
 * <p>The two lookups replace the join-fetch {@code TurAIAgentRepository.findById}
 * used to do on every chat turn: two primary-key reads inside the turn's
 * transaction instead of two joins.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurAgentRagInfra {

    private final TurEmbeddingModelRepository embeddingModelRepository;
    private final TurStoreInstanceRepository storeInstanceRepository;

    public TurAgentRagInfra(TurEmbeddingModelRepository embeddingModelRepository,
            TurStoreInstanceRepository storeInstanceRepository) {
        this.embeddingModelRepository = embeddingModelRepository;
        this.storeInstanceRepository = storeInstanceRepository;
    }

    /** The agent's embedding model, or empty when it has none or the row is gone. */
    public Optional<TurEmbeddingModel> embeddingModel(TurAIAgent agent) {
        if (agent == null || agent.getEmbeddingModelInstanceId() == null) {
            return Optional.empty();
        }
        return embeddingModelRepository.findById(agent.getEmbeddingModelInstanceId());
    }

    /** The agent's vector store, or empty when it has none or the row is gone. */
    public Optional<TurStoreInstance> store(TurAIAgent agent) {
        if (agent == null || agent.getStoreInstanceId() == null) {
            return Optional.empty();
        }
        return storeInstanceRepository.findById(agent.getStoreInstanceId());
    }
}
