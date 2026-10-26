/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.manifest;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.repository.agent.TurAIAgentRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;

import lombok.RequiredArgsConstructor;

/**
 * Converges the GenAI binding an SN manifest declares onto the site (T1116), so
 * a connector that provisions its site as code can turn the site's GenAI on
 * without the console.
 *
 * <p>Only what the manifest declares is compared: the site's own agent and,
 * when declared, the knowledge-base mode. Every other GenAI setting (ranking,
 * semantic recall, RAG flags) is the console's, and a converge never touches
 * it. A manifest without a {@code genAi} block leaves the binding alone.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Component
@RequiredArgsConstructor
public class TurSNManifestGenAiConverger {

    private final TurAIAgentRepository turAIAgentRepository;
    private final TurSNSiteGenAiRepository turSNSiteGenAiRepository;

    /** Refuses an agent id that names no agent. */
    public void validate(TurSNManifestGenAi genAi) {
        if (genAi == null || genAi.ownAgentId() == null) {
            return;
        }
        if (!turAIAgentRepository.existsById(genAi.ownAgentId())) {
            throw new IllegalArgumentException("Manifest genAi names agent '" + genAi.ownAgentId()
                    + "', which does not exist; name an agent id or \""
                    + TurSNManifestGenAi.DEFAULT_AGENT + "\"");
        }
    }

    /**
     * Whether {@link #converge} would create or change the binding. Read-only.
     *
     * @param site the live site, or {@code null} when the manifest creates it
     */
    public boolean plan(TurSNSite site, TurSNManifestGenAi genAi) {
        return genAi != null && !matches(site == null ? null : site.getTurSNSiteGenAi(), genAi);
    }

    /**
     * Creates the site's binding, or rewrites the declared parts of the live
     * one, when they differ.
     *
     * @return {@code true} when the binding was created or changed
     */
    public boolean converge(TurSNSite site, TurSNManifestGenAi genAi) {
        if (genAi == null) {
            return false;
        }
        TurSNSiteGenAi live = site.getTurSNSiteGenAi();
        if (matches(live, genAi)) {
            return false;
        }
        TurSNSiteGenAi target = live != null ? live : new TurSNSiteGenAi();
        String agentId = genAi.ownAgentId();
        TurAIAgent agent = agentId == null ? null
                : turAIAgentRepository.findById(agentId).orElseThrow(() -> new IllegalArgumentException(
                        "Manifest genAi names agent '" + agentId + "', which does not exist"));
        target.setTurAIAgent(agent);
        if (genAi.knowledgeBaseMode() != null) {
            target.setKnowledgeBaseMode(genAi.knowledgeBaseMode());
        }
        site.setTurSNSiteGenAi(turSNSiteGenAiRepository.save(target));
        return true;
    }

    private static boolean matches(TurSNSiteGenAi live, TurSNManifestGenAi genAi) {
        if (live == null) {
            return false;
        }
        String liveAgentId = live.getTurAIAgent() == null ? null : live.getTurAIAgent().getId();
        return Objects.equals(liveAgentId, genAi.ownAgentId())
                && (genAi.knowledgeBaseMode() == null
                        || genAi.knowledgeBaseMode() == live.getKnowledgeBaseMode());
    }
}
