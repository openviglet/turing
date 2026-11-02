/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.viglet.core.manifest.VigletManifestDeriver;
import com.viglet.core.manifest.VigletManifestDiff;
import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.sn.manifest.TurSNManifestPlan;
import com.viglet.turing.sn.manifest.TurSNManifestRequest;
import com.viglet.turing.sn.manifest.TurSNSiteManifestService;
import com.viglet.turing.spring.security.auth.TurApiKeyPartitionContext;

/**
 * T1209 — a partitioned key may re-post its manifest, which keeps a connector's
 * idempotent provision working, but never change the schema every partition shares.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteManifestAPIPartitionTest {

    @Mock
    private TurSNSiteManifestService manifestService;
    @Mock
    private VigletManifestDeriver manifestDeriver;
    @InjectMocks
    private TurSNSiteManifestAPI api;

    private final TurSNManifestRequest manifest = new TurSNManifestRequest("Shared", null, null, null,
            null, List.of(), null, null, null, null, null);

    @AfterEach
    void unbind() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aPartitionedKeyMayRepostAnUnchangedManifest() {
        partitioned();
        when(manifestService.planRequest(manifest)).thenReturn(plan(true, List.of()));

        api.provision(manifest);

        verify(manifestService).provisionRequest(manifest);
    }

    @Test
    void aPartitionedKeyCannotChangeTheSharedSchema() {
        partitioned();
        when(manifestService.planRequest(manifest)).thenReturn(plan(true, List.of("price")));

        ResponseEntity<Object> response = api.provision(manifest);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo(Map.of("error", TurSNSiteManifestAPI.SHARED_SCHEMA));
        verify(manifestService, never()).provisionRequest(any());
    }

    @Test
    void aPartitionedKeyCannotCreateTheSite() {
        partitioned();
        when(manifestService.planRequest(manifest)).thenReturn(plan(false, List.of()));

        assertThat(api.provision(manifest).getStatusCode().value()).isEqualTo(403);
        verify(manifestService, never()).provisionRequest(any());
    }

    @Test
    void anUnpartitionedKeyProvisionsWithoutPlanning() {
        api.provision(manifest);

        verify(manifestService, never()).planRequest(any());
        verify(manifestService).provisionRequest(manifest);
    }

    private static TurSNManifestPlan plan(boolean siteExists, List<String> fieldsToAdd) {
        return new TurSNManifestPlan(new VigletManifestDiff("1", siteExists, fieldsToAdd, List.of(),
                List.of("title"), List.of()), List.of(), List.of());
    }

    private static void partitioned() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sn/manifest");
        request.setAttribute(TurApiKeyPartitionContext.class.getName(),
                new TurSEPartition("site_key", "a.example.org"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
