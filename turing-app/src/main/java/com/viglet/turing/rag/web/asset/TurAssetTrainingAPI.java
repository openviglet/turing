/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.rag.web.asset;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.rag.asset.TurAssetTrainingService;
import com.viglet.turing.rag.asset.TurAssetTrainingState;
import com.viglet.turing.rag.asset.TurAssetTrainingStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI training from the stored assets. T1322 — split out of storage's
 * {@code TurAssetAPI}, since training embeds into rag's stores (ADR 0005
 * section 6: storage sits below rag); the paths under {@code /api/asset} and
 * the {@code ROLE_ADMIN} gate are unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/asset")
@Secured("ROLE_ADMIN")
@Tag(name = "Assets", description = "Asset Management API (MinIO)")
public class TurAssetTrainingAPI {

    private final TurAssetTrainingService turAssetTrainingService;

    public TurAssetTrainingAPI(TurAssetTrainingService turAssetTrainingService) {
        this.turAssetTrainingService = turAssetTrainingService;
    }

    @Operation(summary = "Start AI training from assets")
    @PostMapping("/train")
    public ResponseEntity<TurAssetTrainingStatus> startTraining() {
        TurAssetTrainingStatus result = turAssetTrainingService.startTraining();
        if (result.state() == TurAssetTrainingState.RUNNING) {
            return ResponseEntity.accepted().body(result);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    @Operation(summary = "Get AI training status")
    @GetMapping("/train/status")
    public TurAssetTrainingStatus getTrainingStatus() {
        return turAssetTrainingService.getStatus();
    }

    @Operation(summary = "Get training status for a list of assets")
    @GetMapping("/train/records")
    public Map<String, String> getTrainingRecords(@RequestParam List<String> objectNames) {
        return turAssetTrainingService.getTrainedAtMap(objectNames);
    }
}
