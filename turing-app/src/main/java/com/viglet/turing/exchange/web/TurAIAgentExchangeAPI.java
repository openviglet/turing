/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.exchange.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.exchange.agent.TurAIAgentExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * An AI agent's ZIP export. T1322 — split out of agent's {@code TurAIAgentAPI},
 * since the bundle is the exchange's envelope (ADR 0005 section 6: exchange sits
 * above agent); the path under {@code /api/ai-agent} is unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-agent")
@Tag(name = "AI Agent", description = "AI Agent API")
public class TurAIAgentExchangeAPI {

    private final TurAIAgentRepository turAIAgentRepository;
    private final TurAIAgentExportService turAIAgentExportService;

    public TurAIAgentExchangeAPI(TurAIAgentRepository turAIAgentRepository,
            TurAIAgentExportService turAIAgentExportService) {
        this.turAIAgentRepository = turAIAgentRepository;
        this.turAIAgentExportService = turAIAgentExportService;
    }

    @Operation(summary = "Export an AI Agent as a ZIP bundle",
            description = "Produces a ZIP containing agent.json (TurExchange envelope), one " +
                    "chat-flows/{slug}.chat-flow.json per chat flow, and one tools/{slug}.groovy " +
                    "per custom tool. API keys (LLM apiKey/apiKeyEncrypted) are never serialized.")
    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Secured({"ROLE_ADMIN", "AI_AGENT_VIEW"})
    public StreamingResponseBody turAIAgentExport(@PathVariable String id,
            HttpServletResponse response) {
        TurAIAgent agent = turAIAgentRepository.findById(id).orElse(null);
        if (agent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Agent not found: " + id);
        }
        Path zipPath = turAIAgentExportService.exportAgentToZip(id);
        String filename = "agent-" + safeFilenamePart(agent.getTitle(), id) + ".zip";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);
        return output -> {
            try {
                Files.copy(zipPath, output);
                output.flush();
            } catch (IOException e) {
                log.error("Failed to stream agent export for {}: {}", id, e.getMessage(), e);
            } finally {
                turAIAgentExportService.cleanup(zipPath);
            }
        };
    }

    private static String safeFilenamePart(String name, String id) {
        String base = (name == null ? "untitled" : name)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-)|(-$)", "");
        if (base.isEmpty()) base = "untitled";
        return base + "-" + (id == null ? "" : id.substring(0, Math.min(8, id.length())));
    }
}
