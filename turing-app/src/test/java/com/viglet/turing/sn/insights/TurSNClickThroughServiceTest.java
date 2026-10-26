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

package com.viglet.turing.sn.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickTerm;

/**
 * T1131 — the click-through report joins searches and clicks per term.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNClickThroughServiceTest {

    private static final Instant TO = Instant.parse("2026-10-26T00:00:00Z");

    private final TurSNSiteMetricAccessRepository accessRepository = mock(TurSNSiteMetricAccessRepository.class);
    private final TurSNSiteMetricClickRepository clickRepository = mock(TurSNSiteMetricClickRepository.class);
    private final TurSNClickThroughService service = new TurSNClickThroughService(accessRepository,
            clickRepository);
    private final TurSNSite site = new TurSNSite();

    @Test
    void joinsSearchesAndClicksPerTerm() {
        when(accessRepository.termsBetweenDatesForLocale(eq(site), eq(Instant.EPOCH), eq(TO), isNull(),
                any(Pageable.class))).thenReturn(List.of(
                        new TurSNSiteMetricAccessTerm("workshop", 10, 8.0, TO),
                        new TurSNSiteMetricAccessTerm("php 9", 6, 0.0, TO),
                        new TurSNSiteMetricAccessTerm("pricing", 4, 2.5, TO)));
        when(clickRepository.clicksByTerm(eq(site), eq(Instant.EPOCH), eq(TO), isNull(), anyCollection()))
                .thenReturn(List.of(new TurSNSiteMetricClickTerm("workshop", 3, 1.6666)));
        when(accessRepository.countSearchesForLocale(site, Instant.EPOCH, TO, null)).thenReturn(20L);
        when(clickRepository.countClicks(site, Instant.EPOCH, TO, null)).thenReturn(3L);

        var report = service.report(site, new TurSNSearchInsightsService.Window(null, TO), null, 10);

        assertThat(report.searches()).isEqualTo(20);
        assertThat(report.clicks()).isEqualTo(3);
        assertThat(report.ctr()).isEqualTo(0.15);
        assertThat(report.terms()).extracting(TurSNClickThroughService.Term::term)
                .containsExactly("workshop", "php 9", "pricing");
        var workshop = report.terms().get(0);
        assertThat(workshop.clicks()).isEqualTo(3);
        assertThat(workshop.ctr()).isEqualTo(0.3);
        assertThat(workshop.avgPosition()).isEqualTo(1.6666);
        assertThat(report.terms().get(2).avgPosition()).isNull();
        // "php 9" found nothing, so it is a failed search, not an ignored one.
        assertThat(report.neverClicked()).extracting(TurSNClickThroughService.Term::term)
                .containsExactly("pricing");
    }

    @Test
    void anEmptyWindowAsksForNoClicks() {
        when(accessRepository.termsBetweenDatesForLocale(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        var report = service.report(site, new TurSNSearchInsightsService.Window(null, TO), Locale.US, 5);

        assertThat(report.terms()).isEmpty();
        assertThat(report.ctr()).isZero();
        assertThat(report.locale()).isEqualTo("en_US");
        verify(clickRepository, never()).clicksByTerm(any(), any(), any(), any(), anyCollection());
    }

    @Test
    void parsesBothLocaleSpellings() {
        assertThat(TurSNClickThroughService.locale("pt-BR")).isEqualTo(Locale.of("pt", "BR"));
        assertThat(TurSNClickThroughService.locale("pt_BR")).isEqualTo(Locale.of("pt", "BR"));
        assertThat(TurSNClickThroughService.locale(" ")).isNull();
        assertThatThrownBy(() -> TurSNClickThroughService.locale("not a locale"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
