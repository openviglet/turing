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
package com.viglet.turing.client.sn.manifest;

/**
 * T1116 — the GenAI binding a manifest declares for its site. Declaring one turns
 * the site's GenAI on: the server creates the binding when the site has none.
 *
 * @param agentId           the AI agent the site answers through;
 *                          {@value #DEFAULT_AGENT} answers through the server's
 *                          global Default AI Agent; {@code null} keeps the agent
 *                          the site is already bound to, or the Default AI Agent
 *                          on a site with no binding (T1146)
 * @param knowledgeBaseMode {@code VECTOR}, {@code VECTORLESS_STRUCTURED} or
 *                          {@code HYBRID}; {@code null} keeps the live mode
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public record TurSNManifestGenAi(String agentId, String knowledgeBaseMode) {

    /** The {@link #agentId} that means "the server's global Default AI Agent". */
    public static final String DEFAULT_AGENT = "default";

    /** GenAI on, answering through the server's Default AI Agent. */
    public static TurSNManifestGenAi defaultAgent() {
        return new TurSNManifestGenAi(DEFAULT_AGENT, null);
    }

    /**
     * GenAI on, keeping whichever agent the site is already bound to (T1146);
     * a site with no binding gets one on the Default AI Agent.
     */
    public static TurSNManifestGenAi keepAgent() {
        return new TurSNManifestGenAi(null, null);
    }

    /** GenAI on, answering through the named agent. */
    public static TurSNManifestGenAi agent(String agentId) {
        return new TurSNManifestGenAi(agentId, null);
    }

    /** The same binding with the knowledge-base mode set. */
    public TurSNManifestGenAi withKnowledgeBaseMode(String mode) {
        return new TurSNManifestGenAi(agentId, mode);
    }
}
