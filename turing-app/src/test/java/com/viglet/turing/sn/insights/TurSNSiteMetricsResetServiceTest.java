/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickRepository;

/**
 * T1195 — clearing a site's metrics removes its searches and clicks, all of
 * them or those before a date.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteMetricsResetServiceTest {

    @Mock
    private TurSNSiteMetricAccessRepository accessRepository;
    @Mock
    private TurSNSiteMetricClickRepository clickRepository;
    @InjectMocks
    private TurSNSiteMetricsResetService service;

    @Test
    void clearsBothLogsBeforeTheGivenDate() {
        TurSNSite site = new TurSNSite();
        Instant before = Instant.parse("2026-10-01T00:00:00Z");
        when(accessRepository.deleteByTurSNSiteAndAccessDateBefore(site, before)).thenReturn(39L);
        when(clickRepository.deleteByTurSNSiteAndAccessDateBefore(site, before)).thenReturn(4L);

        assertEquals(new TurSNSiteMetricsResetService.Cleared(39, 4), service.clear(site, before, "admin"));
    }

    @Test
    void withoutADateEverythingRecordedUntilNowGoes() {
        TurSNSite site = new TurSNSite();
        Instant start = Instant.now();
        when(accessRepository.deleteByTurSNSiteAndAccessDateBefore(eq(site), any())).thenReturn(0L);
        when(clickRepository.deleteByTurSNSiteAndAccessDateBefore(eq(site), any())).thenReturn(0L);

        service.clear(site, null, "admin");

        ArgumentCaptor<Instant> until = ArgumentCaptor.forClass(Instant.class);
        verify(accessRepository).deleteByTurSNSiteAndAccessDateBefore(eq(site), until.capture());
        assertTrue(until.getValue().isAfter(start), "a search logged this second must go too");
    }
}
