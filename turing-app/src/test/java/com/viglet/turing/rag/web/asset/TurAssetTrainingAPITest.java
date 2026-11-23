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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.viglet.turing.rag.asset.TurAssetTrainingService;
import com.viglet.turing.rag.asset.TurAssetTrainingState;
import com.viglet.turing.rag.asset.TurAssetTrainingStatus;

/**
 * T1322 — the asset training endpoints, moved out of storage's asset API on the
 * same paths.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurAssetTrainingAPITest {

    private final TurAssetTrainingService trainingService = mock(TurAssetTrainingService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TurAssetTrainingAPI(trainingService))
            .build();

    @Test
    void testStartTrainingRunning() throws Exception {
        when(trainingService.startTraining()).thenReturn(new TurAssetTrainingStatus(
                TurAssetTrainingState.RUNNING, 10, 0, 0, "2026-01-01", "", ""));

        mockMvc.perform(post("/api/asset/train"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("RUNNING"));
    }

    @Test
    void testStartTrainingConflict() throws Exception {
        when(trainingService.startTraining()).thenReturn(new TurAssetTrainingStatus(
                TurAssetTrainingState.FAILED, 0, 0, 0, "", "", "RAG not enabled"));

        mockMvc.perform(post("/api/asset/train"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.state").value("FAILED"));
    }

    @Test
    void testGetTrainingStatus() throws Exception {
        when(trainingService.getStatus()).thenReturn(TurAssetTrainingStatus.idle());

        mockMvc.perform(get("/api/asset/train/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("IDLE"));
    }

    @Test
    void testGetTrainingRecords() throws Exception {
        when(trainingService.getTrainedAtMap(List.of("file1.pdf", "file2.pdf")))
                .thenReturn(Map.of("file1.pdf", "2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/asset/train/records").param("objectNames", "file1.pdf", "file2.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['file1.pdf']").value("2026-01-01T00:00:00Z"));
    }
}
