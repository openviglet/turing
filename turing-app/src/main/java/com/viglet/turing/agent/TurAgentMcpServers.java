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

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.mcp.persistence.model.TurMcpServer;
import com.viglet.turing.mcp.persistence.repository.TurMcpServerRepository;

/**
 * T1315 — the MCP servers an agent names. The agent holds them as ids (the
 * servers belong to the mcp module); this loads the rows in one query. Ids whose
 * server is gone are skipped.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurAgentMcpServers {

    private final TurMcpServerRepository mcpServerRepository;

    public TurAgentMcpServers(TurMcpServerRepository mcpServerRepository) {
        this.mcpServerRepository = mcpServerRepository;
    }

    public Set<TurMcpServer> of(TurAIAgent agent) {
        if (agent == null || agent.getMcpServerIds() == null || agent.getMcpServerIds().isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(mcpServerRepository.findAllById(agent.getMcpServerIds()));
    }
}
