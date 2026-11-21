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
package com.viglet.turing.agent.flow;

/**
 * T1322f — a check that may refuse publishing an agent's chat flow, declared by
 * agent and implemented by eval (a blocking eval gate whose last run is red), so
 * agent does not import eval. Publishing is refused when any gate refuses it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurAgentPublishGate {

    /** Whether publishing the agent's chat flow must be refused. */
    boolean shouldBlockPublish(String agentId);
}
