/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.mcp.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.llm.mcp.persistence.dto.TurMcpServerDto;
import com.viglet.turing.llm.mcp.persistence.mapper.TurMcpServerMapperImpl;
import com.viglet.turing.llm.mcp.persistence.model.TurMcpServer;
import com.viglet.turing.llm.mcp.persistence.repository.TurMcpServerRepository;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;
import com.viglet.turing.shared.exception.TurConflictException;
import com.viglet.turing.system.security.TurSecretCryptoService;
import com.viglet.turing.tenant.TurInfraTenantScope;

/**
 * T1319 — an MCP server save made from an older version is refused, and a
 * current one answers the version the save produced. The same check guards the
 * LLM instance, embedding model, search engine and store saves.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurMcpServerAPIVersionTest {

    private final TurMcpServerRepository repository = mock(TurMcpServerRepository.class);
    private final TurInfraTenantScope tenantScope = mock(TurInfraTenantScope.class);
    private final TurMcpServerAPI api = new TurMcpServerAPI(repository, new TurMcpServerMapperImpl(),
            mock(TurAIAgentRepository.class), mock(TurPersonaRepository.class), tenantScope,
            mock(TurSecretCryptoService.class));

    private TurMcpServer stored(long version) {
        TurMcpServer server = new TurMcpServer();
        server.setId("mcp-1");
        server.setTitle("Hosted");
        server.setVersion(version);
        return server;
    }

    private static TurMcpServerDto payload(Long version) {
        TurMcpServerDto dto = new TurMcpServerDto();
        dto.setId("mcp-1");
        dto.setTitle("Mine");
        dto.setVersion(version);
        return dto;
    }

    @Test
    void aSaveFromAnOlderVersionIsRefused() {
        when(tenantScope.isVisibleToTenant(any())).thenReturn(true);
        when(repository.findById("mcp-1")).thenReturn(Optional.of(stored(4L)));

        assertThatThrownBy(() -> api.turMcpServerUpdate("mcp-1", payload(3L)))
                .isInstanceOf(TurConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aSaveFromTheCurrentVersionAnswersTheSavedVersionAndKeepsTheStoredOne() {
        when(tenantScope.isVisibleToTenant(any())).thenReturn(true);
        TurMcpServer existing = stored(4L);
        when(repository.findById("mcp-1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> {
            TurMcpServer merged = stored(5L);
            merged.setTitle(((TurMcpServer) inv.getArgument(0)).getTitle());
            return merged;
        });

        TurMcpServerDto answer = api.turMcpServerUpdate("mcp-1", payload(4L));

        assertThat(answer.getVersion()).isEqualTo(5L);
        assertThat(answer.getTitle()).isEqualTo("Mine");
        // The payload's version never overwrites the stored one before the save.
        assertThat(existing.getVersion()).isEqualTo(4L);
    }

    @Test
    void aSaveWithoutAVersionDoesNotNullTheStoredOne() {
        when(tenantScope.isVisibleToTenant(any())).thenReturn(true);
        TurMcpServer existing = stored(4L);
        when(repository.findById("mcp-1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        api.turMcpServerUpdate("mcp-1", payload(null));

        assertThat(existing.getVersion()).isEqualTo(4L);
    }
}
