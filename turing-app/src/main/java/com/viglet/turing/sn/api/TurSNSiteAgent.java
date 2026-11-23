/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.sn.api;

import java.util.List;
import java.util.Optional;

import com.viglet.turing.llm.persistence.model.TurLLMInstance;

/**
 * T1322h — what sn reads about the agent a site's GenAI binding names: its
 * title, whether it is enabled with RAG on, and its LLM instances in id order
 * (so "the first one" is stable). Answered by {@link TurSNSiteAgents}.
 *
 * @param title        the agent's title
 * @param ragEnabled   whether the agent is enabled and has RAG turned on
 * @param llmInstances the agent's LLM instances, ordered by id; gone ids skipped
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSNSiteAgent(String title, boolean ragEnabled, List<TurLLMInstance> llmInstances) {

    public TurSNSiteAgent {
        llmInstances = llmInstances == null ? List.of() : List.copyOf(llmInstances);
    }

    /** The first enabled LLM instance, else the first one; empty when there is none. */
    public Optional<TurLLMInstance> activeLlmInstance() {
        return llmInstances.stream()
                .filter(instance -> instance.getEnabled() == 1)
                .findFirst()
                .or(() -> llmInstances.stream().findFirst());
    }
}
