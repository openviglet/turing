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

import com.viglet.turing.persistence.model.sn.genai.TurSNKnowledgeBaseMode;

/**
 * The GenAI binding an SN manifest declares for its site (T1116). Declaring it
 * at all is what turns the site's GenAI on: the binding is created if the site
 * has none.
 *
 * @param agentId           the AI agent the site answers through; {@code null}
 *                          or {@code "default"} binds no agent of its own, so the
 *                          global Default AI Agent answers (T622)
 * @param knowledgeBaseMode how the site retrieves; {@code null} keeps the live
 *                          mode, or {@code VECTOR} on a new binding
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public record TurSNManifestGenAi(String agentId, TurSNKnowledgeBaseMode knowledgeBaseMode) {

    /** The {@link #agentId} that means "the global Default AI Agent". */
    public static final String DEFAULT_AGENT = "default";

    /** The agent the site binds as its own, or {@code null} for the Default AI Agent. */
    public String ownAgentId() {
        return agentId == null || agentId.isBlank() || DEFAULT_AGENT.equalsIgnoreCase(agentId.trim())
                ? null
                : agentId.trim();
    }
}
