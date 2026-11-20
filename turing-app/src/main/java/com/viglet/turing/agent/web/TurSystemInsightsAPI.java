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

package com.viglet.turing.agent.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.agent.tool.TurSystemInfoToolService;
import com.viglet.turing.llm.TurLlmSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The AI insights of the System Information page. They keep their path under
 * {@code /api/system/info}, but live with the system-status tool that gathers
 * their data, so the system module depends on neither agent nor llm (T1311,
 * ADR 0005).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/system/info")
@Tag(name = "System Information", description = "System Information API")
public class TurSystemInsightsAPI {
    private static final String INSIGHTS_CACHE_KEY = "system-info-insights";
    private static final String INSIGHTS_SYSTEM_PROMPT = """
            You are a system administrator expert analyzing a **Viglet Turing ES** (Enterprise Search) application server.

            ## About the Platform
            Viglet Turing ES is a Java-based enterprise search platform that combines:
            - **Apache Solr** (or Elasticsearch/Lucene) as its search engine for indexing and querying content
            - **Spring Boot 4 + Java 21** as the application runtime
            - **Spring AI** for LLM integration (chat, RAG, embeddings)
            - **React + TypeScript** frontend served by the same JVM process
            - **H2** (dev) or **MySQL/PostgreSQL** (production) as the relational database for configuration, users, and metadata
            - **Apache Artemis** message queue for asynchronous document indexing

            ## About Optional Components
            - **MongoDB**: When enabled, used exclusively for **persisting application logs to disk** \
            (server logs, AEM connector logs, indexing operation logs). It is NOT the primary database. \
            If MongoDB is disabled, logs are only available via standard application logging (stdout/files). \
            When reviewing MongoDB, focus on connectivity and whether log persistence is needed for the workload.
            - **Storage (MinIO or Filesystem)**: Used for **asset management and SPA page hosting**. \
            MinIO provides S3-compatible object storage; Filesystem stores files locally under a configured path. \
            When storage type is "none", asset upload/download and SPA page features are disabled entirely. \
            When reviewing storage, consider whether the chosen backend fits the deployment (e.g., filesystem \
            is simpler for single-node, MinIO for distributed/cloud deployments).

            ## Analysis Instructions
            Analyze the runtime data provided and generate a comprehensive summary in Markdown format. \
            Include these sections:

            ## Health Summary
            Brief overall health assessment — is the system healthy? Any services down?

            ## Resource Usage
            Evaluate JVM heap, physical RAM, swap, and disk usage. Flag high utilization or imbalanced allocation \
            (e.g., JVM heap too small relative to available RAM, or swap being used heavily).

            ## Configuration Review
            Review the database setup (H2 vs production DB), storage backend choice, and optional services. \
            Flag anything unusual for a production environment (e.g., H2 in production, storage disabled, etc.).

            ## Warnings
            Any concerning metrics, potential issues, or misconfigurations that need attention.

            ## Optimization Suggestions
            Actionable recommendations to improve performance, reliability, or resource usage. \
            Be specific — suggest concrete values or changes when possible (e.g., "increase -Xmx to 4G", \
            "switch from H2 to PostgreSQL for production", "enable storage for asset management").

            Be concise but insightful. Use bullet points where appropriate. \
            Tailor suggestions to the actual data — avoid generic advice that doesn't match the metrics.""";

    private final TurLlmSummaryService llmSummaryService;
    private final TurSystemInfoToolService systemInfoToolService;

    public TurSystemInsightsAPI(TurLlmSummaryService llmSummaryService,
            TurSystemInfoToolService systemInfoToolService) {
        this.llmSummaryService = llmSummaryService;
        this.systemInfoToolService = systemInfoToolService;
    }

    @Operation(summary = "Check if AI insights are available")
    @GetMapping("/insights/available")
    public ResponseEntity<Boolean> isInsightsAvailable() {
        return ResponseEntity.ok(llmSummaryService.isAvailable());
    }

    @Operation(summary = "Generate AI insights for system information")
    @GetMapping("/insights")
    public ResponseEntity<TurLlmSummaryService.SummaryResult> getInsights(
            @RequestParam(defaultValue = "false") boolean regenerate) {
        String systemData = systemInfoToolService.getSystemStatus();
        String userData = systemData
                + "\nPlease analyze all this data and provide a comprehensive summary with suggestions.";

        TurLlmSummaryService.SummaryResult result = llmSummaryService.generate(
                INSIGHTS_CACHE_KEY, userData, INSIGHTS_SYSTEM_PROMPT, regenerate);

        if (!result.success() && result.content() == null && result.error() != null
                && result.error().startsWith("No default LLM")) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}