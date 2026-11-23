/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.rag.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.ApplicationModuleListener;

import com.viglet.core.tenancy.VigletTenantContext;
import com.viglet.turing.storage.asset.TurAssetEvent;
import com.viglet.turing.system.TurGlobalSettingsService;

/**
 * T1324 — the asset listener is a registry-tracked module listener, runs in the tenant
 * the event was published under, and leaves the worker thread's tenant as it was.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurAssetEventListenerTest {

    private final TurGlobalSettingsService settings = mock(TurGlobalSettingsService.class);
    private final TurAssetTrainingService training = mock(TurAssetTrainingService.class);
    private final VigletTenantContext tenantContext = mock(VigletTenantContext.class);
    private final TurAssetEventListener listener = new TurAssetEventListener(settings, training, tenantContext);

    @Test
    void itIsAnApplicationModuleListenerSoTheRegistryTracksIt() throws Exception {
        assertThat(TurAssetEventListener.class.getMethod("handleAssetEvent", TurAssetEvent.class)
                .isAnnotationPresent(ApplicationModuleListener.class)).isTrue();
    }

    @Test
    void anUploadIsIndexedInTheTenantItWasPublishedUnder() {
        when(settings.isRagEnabled()).thenReturn(true);
        AtomicReference<String> tenantAtIndexing = new AtomicReference<>();
        doAnswer(invocation -> {
            verify(tenantContext).setCurrentTenant("acme");
            tenantAtIndexing.set("acme");
            return null;
        }).when(training).indexSingleFile("docs/a.pdf", "application/pdf", 10L);

        listener.handleAssetEvent(TurAssetEvent.uploaded("docs/a.pdf", "application/pdf", 10L, "acme"));

        assertThat(tenantAtIndexing).hasValue("acme");
        verify(tenantContext).clear();
    }

    @Test
    void theWorkerThreadsOwnTenantIsPutBack() {
        when(settings.isRagEnabled()).thenReturn(true);
        when(tenantContext.getCurrentTenant()).thenReturn("previous");

        listener.handleAssetEvent(TurAssetEvent.deleted("docs/a.pdf", "acme"));

        verify(training).deindexSingleFile("docs/a.pdf");
        verify(tenantContext).setCurrentTenant("previous");
        verify(tenantContext, never()).clear();
    }

    @Test
    void aFailureReachesTheRegistrySoThePublicationStaysIncomplete() {
        when(settings.isRagEnabled()).thenReturn(true);
        doThrow(new IllegalStateException("store down")).when(training).deindexSingleFile("docs/a.pdf");

        assertThatThrownBy(() -> listener.handleAssetEvent(TurAssetEvent.deleted("docs/a.pdf", null)))
                .hasMessage("store down");
        verify(tenantContext, never()).setCurrentTenant(org.mockito.ArgumentMatchers.anyString());
        verify(tenantContext).clear();
    }
}
