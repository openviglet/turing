/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.batch.web;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.batch.embedding.TurBatchReEmbeddingService;
import com.viglet.turing.llm.persistence.model.embedding.TurEmbeddingModel;
import com.viglet.turing.llm.persistence.model.store.TurStoreInstance;
import com.viglet.turing.llm.persistence.repository.store.TurStoreInstanceRepository;
import com.viglet.turing.rag.TurRagContextBuilder;
import com.viglet.turing.tenant.TurInfraTenantScope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * F.7 / §X.8.c batch re-embedding of an embedding store's collection. T1322 —
 * split out of rag's {@code TurStoreInstanceAPI}, since the batch tier sits
 * above rag (ADR 0005 section 6); the path under {@code /api/store} and its
 * {@code STORE_EDIT} gate are unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/store")
@Tag(name = "Embedding Store", description = "Embedding Store API")
public class TurStoreReEmbeddingAPI {

    private final TurStoreInstanceRepository turStoreInstanceRepository;
    private final TurRagContextBuilder ragContextBuilder;
    private final TurInfraTenantScope tenantScope;
    private final TurBatchReEmbeddingService batchReEmbeddingService;

    public TurStoreReEmbeddingAPI(TurStoreInstanceRepository turStoreInstanceRepository,
            TurRagContextBuilder ragContextBuilder, TurInfraTenantScope tenantScope,
            TurBatchReEmbeddingService batchReEmbeddingService) {
        this.turStoreInstanceRepository = turStoreInstanceRepository;
        this.ragContextBuilder = ragContextBuilder;
        this.tenantScope = tenantScope;
        this.batchReEmbeddingService = batchReEmbeddingService;
    }

    /**
     * Schedules a batch re-embedding of a collection (50% off, 24h) after
     * switching its embedding model, instead of a synchronous re-embed. Reads
     * the collection's chunks, submits their text as an OpenAI embeddings Batch
     * keyed by chunk id; the precomputed vectors are written back when the batch
     * ends. Returns the number of chunks scheduled — {@code 0} (HTTP 409) when
     * the Batch embedding tier is off or the model's vendor has no Batch
     * embeddings API, so the caller can fall back to the synchronous reindex.
     */
    @Operation(summary = "Schedule batch re-embedding of a collection")
    @PostMapping("/{id}/collections/{collectionName}/reembed-batch")
    @Secured({"ROLE_ADMIN", "STORE_EDIT"})
    public ResponseEntity<Map<String, Object>> reEmbedCollection(@PathVariable String id,
            @PathVariable String collectionName,
            @RequestParam("embeddingModelId") String embeddingModelId) {
        TurStoreInstance instance = turStoreInstanceRepository.findById(id)
                .filter(tenantScope::isVisibleToTenant).orElse(null);
        // T756 — resolve the embedding entity unified-first (legacy fallback); the
        // adapter carries tenantId so the visibility filter still applies.
        TurEmbeddingModel embeddingModel = Optional
                .ofNullable(ragContextBuilder.resolveEmbeddingEntity(embeddingModelId))
                .filter(tenantScope::isVisibleToTenant).orElse(null);
        if (instance == null || embeddingModel == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!batchReEmbeddingService.isAvailable(embeddingModel)) {
            return ResponseEntity.status(409).body(Map.of(
                    "scheduled", 0,
                    "reason", "Batch embedding tier disabled or vendor has no embeddings Batch API"));
        }
        int scheduled = batchReEmbeddingService.submitForCollection(instance, collectionName, embeddingModel);
        return ResponseEntity.ok(Map.of("scheduled", scheduled));
    }
}
