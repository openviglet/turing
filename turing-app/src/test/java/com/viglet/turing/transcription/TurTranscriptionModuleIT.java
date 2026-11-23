/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.transcription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.viglet.turing.shared.infra.TurSearchEngineReadOnlySource;
import com.viglet.turing.system.TurCodeInterpreterDockerProbe;
import com.viglet.turing.system.TurDefaultRagReadiness;
import com.viglet.turing.system.TurGitServerStatus;
import com.viglet.turing.system.TurStorageStatus;
import com.viglet.turing.tenant.TurTenantObjectStore;
import com.viglet.turing.testutil.AbstractTuringModuleIT;
import com.viglet.turing.testutil.TuringModuleTest;

/**
 * T1325 — the transcription module as a slice, booted with the modules it declares
 * (system, llm): on a fresh database no speech-to-text backend is configured, so the
 * service says it is unavailable and a transcription fails soft instead of throwing.
 * Tenant and observability are named in {@code extraIncludes}: llm takes their beans in
 * {@code @Bean} methods, which Spring Modulith does not count as bootstrap dependencies.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@TuringModuleTest(extraIncludes = {"tenant", "observability"})
class TurTranscriptionModuleIT extends AbstractTuringModuleIT {

    // SPIs the modules in the slice declare and modules outside it implement: stood in.
    @MockitoBean
    private TurStorageStatus storageStatus;

    @MockitoBean
    private TurGitServerStatus gitServerStatus;

    @MockitoBean
    private TurSearchEngineReadOnlySource searchEngineReadOnlySource;

    @MockitoBean
    private TurDefaultRagReadiness defaultRagReadiness;

    @MockitoBean
    private TurCodeInterpreterDockerProbe codeInterpreterDockerProbe;

    @MockitoBean
    private TurTenantObjectStore tenantObjectStore;

    @MockitoBean
    private TurTranscriptionKeywordSource transcriptionKeywordSource;

    @Autowired
    private TurTranscriptionService transcription;

    @Test
    void withNoBackendConfiguredATranscriptionFailsSoft() {
        assertThat(transcription.isAvailable()).isFalse();

        TurTranscriptionResult result = transcription.transcribe(new byte[] {1, 2, 3}, "audio/wav", "en");

        assertThat(result.success()).isFalse();
    }
}
