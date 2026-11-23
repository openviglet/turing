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

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.viglet.turing.answer.exchange.TurSNSiteContentExchangeService;
import com.viglet.turing.exchange.sn.TurSNSiteExport;
import com.viglet.turing.sn.api.TurSNSiteAgents;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A Semantic Navigation site's export and its vector-store reindex, with their
 * progress. T1322 — split out of sn's {@code TurSNSiteAPI}, since both run the
 * exchange (ADR 0005 section 6: exchange sits above sn); the REST paths under
 * {@code /api/sn} are unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@RestController
@RequestMapping("/api/sn")
@Tag(name = "Semantic Navigation Site", description = "Semantic Navigation Site API")
@RequiredArgsConstructor
public class TurSNSiteExchangeAPI {

    private static final String ERROR = "error";
    private static final String TASK_ID = "taskId";

    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteExport turSNSiteExport;
    private final TurSNSiteContentExchangeService contentExchangeService;
    private final TurSNSiteAgents siteAgents;

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public StreamingResponseBody turSNSiteExportAll(HttpServletResponse response) {

        try {
            return turSNSiteExport.exportAll(response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public StreamingResponseBody turSNSiteExport(@PathVariable String id,
            @RequestParam(defaultValue = "false") boolean includeContent,
            @RequestParam(defaultValue = "false") boolean includeTemplate,
            @RequestParam(required = false) String taskId,
            HttpServletResponse response) {
        try {
            return turSNSiteExport.exportBySiteId(id, includeContent, includeTemplate, taskId, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @PostMapping(value = "/{id}/export/async")
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public Map<String, String> turSNSiteExportAsync(@PathVariable String id,
            @RequestParam(defaultValue = "false") boolean includeTemplate,
            @RequestParam(required = false) String taskId) {
        String effectiveTaskId = taskId != null ? taskId : "export-" + id + "-" + System.currentTimeMillis();
        Thread.ofVirtual().start(() -> {
            try {
                turSNSiteExport.exportBySiteIdAsync(id, true, includeTemplate, effectiveTaskId);
            } catch (Exception e) {
                log.error("Async export failed for site {}: {}", id, e.getMessage(), e);
            }
        });
        return Map.of(TASK_ID, effectiveTaskId);
    }

    @GetMapping(value = "/export/download/{taskId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public StreamingResponseBody downloadExport(@PathVariable String taskId, HttpServletResponse response) {
        return turSNSiteExport.downloadAsyncExport(taskId, response);
    }

    /**
     * Returns the in-flight reindex task id (and progress snapshot) for this
     * site so the admin UI can resume the progress display after navigating
     * away and back. {@code 204 No Content} when no task is running.
     *
     * @since 2026.2.4
     */
    @GetMapping(value = "/{id}/genai/reindex/status")
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public ResponseEntity<Map<String, Object>> turSNSiteGenAiReindexStatus(@PathVariable String id) {
        return contentExchangeService.findActiveReindexTask(id)
                .flatMap(taskId -> contentExchangeService.getProgress(taskId)
                        .map(p -> ResponseEntity.ok(Map.of(
                                TASK_ID, (Object) taskId,
                                "totalDocuments", p.totalDocuments(),
                                "processedDocuments", p.processedDocuments(),
                                "percentage", p.percentage(),
                                "currentLocale", p.currentLocale(),
                                "phase", p.phase(),
                                "estimatedRemainingMillis", p.estimatedRemainingMillis(),
                                "parallelism", contentExchangeService.getParallelism(taskId)))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/{id}/genai/reindex")
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public ResponseEntity<Map<String, String>> turSNSiteGenAiReindex(@PathVariable String id,
            @RequestParam(required = false) String taskId) {
        return turSNSiteRepository.findById(id)
                .map(site -> {
                    // T1151 — the effective agent, as the reindex itself resolves it, so a
                    // site on the Default AI Agent can be reindexed into its store.
                    if (!siteAgents.isRagReady(site.getTurSNSiteGenAi())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .<Map<String, String>>body(Map.of(ERROR, "RAG is not enabled on this site"));
                    }
                    String effectiveTaskId = taskId != null
                            ? taskId
                            : "rag-reindex-" + id + "-" + System.currentTimeMillis();
                    String siteId = site.getId();
                    Thread.ofVirtual().start(() -> {
                        try {
                            contentExchangeService.reindexVectorStore(siteId, effectiveTaskId);
                        } catch (Exception e) {
                            log.error("Async RAG reindex failed for site {}: {}", siteId, e.getMessage(), e);
                        }
                    });
                    return ResponseEntity.ok(Map.of(TASK_ID, effectiveTaskId));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/export/progress/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public SseEmitter exportProgress(@PathVariable String taskId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        Thread.ofVirtual().start(() -> streamExportProgress(taskId, emitter));
        return emitter;
    }

    private void streamExportProgress(String taskId, SseEmitter emitter) {
        try {
            while (true) {
                var progress = contentExchangeService.getProgress(taskId);
                if (progress.isPresent()) {
                    var p = progress.get();
                    emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(Map.of(
                                    "totalDocuments", p.totalDocuments(),
                                    "processedDocuments", p.processedDocuments(),
                                    "percentage", p.percentage(),
                                    "currentLocale", p.currentLocale(),
                                    "phase", p.phase(),
                                    "estimatedRemainingMillis", p.estimatedRemainingMillis(),
                                    "parallelism", contentExchangeService.getParallelism(taskId)
                            )));
                    if ("completed".equals(p.phase())) {
                        contentExchangeService.removeProgress(taskId);
                        break;
                    }
                }
                Thread.sleep(500);
            }
            emitter.complete();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emitter.completeWithError(e);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
