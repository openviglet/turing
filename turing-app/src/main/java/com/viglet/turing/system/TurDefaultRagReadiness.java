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
package com.viglet.turing.system;

/**
 * T1322k — the features endpoint asks whether the default agent can answer from
 * retrieved content through this interface, which system declares and the agent
 * module implements (ADR 0005 section 6: system imports no feature module).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurDefaultRagReadiness {

    /** Whether the default agent is set up to answer from retrieved content. */
    boolean isDefaultRagReady();
}
