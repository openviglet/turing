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

import java.util.Collection;
import java.util.Set;

import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;

/**
 * T1322h — what sn needs to know about the AI agents its sites name by id,
 * declared by sn and implemented by the agent module (ADR 0005 section 6: agent
 * sits above sn). The site console, the manifest converger and the import paths
 * ask through it instead of importing an agent type.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNSiteAgents {

    /** Whether an agent with this id exists; {@code false} for a {@code null} id. */
    boolean agentExists(String agentId);

    /**
     * The agent the binding itself names (no Default AI Agent fallback), or empty
     * when it names none or the agent is gone.
     *
     * @param turSNSiteGenAi the site's GenAI binding (may be {@code null})
     */
    java.util.Optional<TurSNSiteAgent> siteAgentOf(TurSNSiteGenAi turSNSiteGenAi);

    /** The Global Settings' Default AI Agent, or empty when none is set or it is gone. */
    java.util.Optional<TurSNSiteAgent> defaultAgent();

    /**
     * The ids among {@code agentIds} whose agent exists and is enabled with RAG
     * on, in one read; empty for an empty input.
     */
    Set<String> ragEnabledAgentIds(Collection<String> agentIds);

    /**
     * Whether the site with this GenAI binding is RAG-ready: its effective agent
     * (its own, else the Default AI Agent) is enabled with RAG on, and its
     * knowledge-base mode needs vectors.
     *
     * @param turSNSiteGenAi the site's GenAI binding (may be {@code null})
     */
    boolean isRagReady(TurSNSiteGenAi turSNSiteGenAi);

    /**
     * {@link #isRagReady} for the site with this (case-sensitive) name, or
     * {@code false} when no site has it.
     */
    boolean isRagReadyForSiteName(String siteName);
}
