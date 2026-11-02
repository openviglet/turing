/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.api.system;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.spring.security.auth.TurApiKeyPartitionContext;
import com.viglet.turing.domain.sn.TurSNSiteDomain;
import com.viglet.turing.domain.sn.TurSNSiteRepositoryPort;
import com.viglet.turing.system.TurLlmSummaryService;

/**
 * T1150 — {@code POST /api/v2/summary} records its spend against the site it
 * names, and refuses a site it cannot find instead of dropping the spend.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@ExtendWith(MockitoExtension.class)
class TurSummaryAPITest {

    @Mock
    private TurLlmSummaryService llmSummaryService;
    @Mock
    private TurSNSiteRepositoryPort turSNSiteRepositoryPort;
    @InjectMocks
    private TurSummaryAPI api;

    @Test
    void aNamedSiteIsPassedOnByItsCanonicalName() {
        TurSNSiteDomain site = Mockito.mock(TurSNSiteDomain.class);
        when(site.name()).thenReturn("Sample");
        when(turSNSiteRepositoryPort.findByName("Sample")).thenReturn(Optional.of(site));

        api.generate(new TurSummaryAPI.SummaryRequest("k", "data", "prompt", " Sample "), false);

        verify(llmSummaryService).generate("k", "data", "prompt", false, null, "Sample");
    }

    @Test
    void noSiteRecordsTheSpendUnattributed() {
        api.generate(new TurSummaryAPI.SummaryRequest("k", "data", "prompt", null), true);

        verify(llmSummaryService).generate(eq("k"), eq("data"), eq("prompt"), eq(true), isNull(), isNull());
        verifyNoInteractions(turSNSiteRepositoryPort);
    }

    @Test
    void anUnknownSiteIsRefusedBeforeAnyCall() {
        when(turSNSiteRepositoryPort.findByName("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.generate(new TurSummaryAPI.SummaryRequest("k", "d", "p", "Ghost"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ghost");
        verify(llmSummaryService, never()).generate(anyString(), anyString(), anyString(), anyBoolean(), any(),
                any());
    }

    /** T1209 — a partitioned key's summary never shares a cache entry with another key's. */
    @Test
    void aPartitionedKeyCachesItsSummaryApart() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/summary");
        request.setAttribute(TurApiKeyPartitionContext.class.getName(),
                new TurSEPartition("site_key", "a.example.org"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            api.generate(new TurSummaryAPI.SummaryRequest("k", "data", "prompt", null), false);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        verify(llmSummaryService).generate(eq("partition:site_key=a.example.org:k"), eq("data"), eq("prompt"),
                eq(false), isNull(), isNull());
    }
}
