/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent.prompt.contributor;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.agent.prompt.TurPromptAssemblyContext;
import com.viglet.turing.agent.prompt.TurPromptContributor;
import com.viglet.turing.agent.prompt.TurPromptSegment;
import com.viglet.turing.agent.prompt.TurPromptStability;
import com.viglet.turing.agent.tool.TurMcpInstructionsProvider;

/**
 * Block AL / §XXXV.1 — emits the attached MCP servers' {@code llmInstructions}
 * block ({@link TurMcpInstructionsProvider}). The provider already bakes in the
 * leading {@code "\n\n"} separator the legacy assembler relied on, so the segment
 * text is byte-identical to the concatenated form. STABLE — the servers'
 * instructions do not change turn-to-turn.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Component
public class TurMcpPromptContributor implements TurPromptContributor {

    private final TurMcpInstructionsProvider mcpInstructionsProvider;

    private final com.viglet.turing.agent.TurAgentMcpServers agentMcpServers;

    public TurMcpPromptContributor(TurMcpInstructionsProvider mcpInstructionsProvider,
            com.viglet.turing.agent.TurAgentMcpServers agentMcpServers) {
        this.agentMcpServers = agentMcpServers;
        this.mcpInstructionsProvider = mcpInstructionsProvider;
    }

    @Override
    public int order() {
        return ORDER_MCP;
    }

    @Override
    public List<TurPromptSegment> contribute(TurPromptAssemblyContext context) {
        String mcp = mcpInstructionsProvider.buildSystemPromptBlock(agentMcpServers.of(context.agent()));
        if (!StringUtils.hasText(mcp)) {
            return List.of();
        }
        return List.of(TurPromptSegment.of(TurPromptSegment.ORIGIN_MCP,
                null, mcp, TurPromptStability.STABLE));
    }
}
