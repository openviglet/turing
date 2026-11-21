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

package com.viglet.turing.shared.event;

/**
 * An AI agent was created, changed or deleted. Published by the agent module at
 * flush, so a module that caches something derived from an agent (the SN search
 * snapshot reads the site's agent) evicts it without the agent entity naming that
 * module (T1316, ADR 0005).
 *
 * @param agentId the agent's id, or {@code null} when JPA had not assigned one
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurAIAgentChangedEvent(String agentId) {
}