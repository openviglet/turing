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
package com.viglet.turing.api.sn.console;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.sn.insights.TurSNClickThroughService;
import com.viglet.turing.sn.insights.TurSNSearchInsightsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * T1131 — click-through rates per term, read from the clicks
 * {@code POST /api/sn/{site}/click} records. Same window and API-key auth as
 * T1119's failed-searches and content-gaps metrics.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@RestController
@RequestMapping("/api/sn/{snSiteId}/metrics")
@Tag(name = "Semantic Navigation Metrics", description = "Semantic Navigation Metrics API")
public class TurSNSiteClickMetricsAPI {

    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSearchInsightsService turSNSearchInsightsService;
    private final TurSNClickThroughService turSNClickThroughService;

    public TurSNSiteClickMetricsAPI(TurSNSiteRepository turSNSiteRepository,
            TurSNSearchInsightsService turSNSearchInsightsService,
            TurSNClickThroughService turSNClickThroughService) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSearchInsightsService = turSNSearchInsightsService;
        this.turSNClickThroughService = turSNClickThroughService;
    }

    /**
     * Per term: searches, clicks, clicks per search and the mean clicked
     * position, most searched first; plus the terms that found results nobody
     * opened. {@code locale} ({@code pt_BR} or {@code pt-BR}) narrows both logs to
     * one locale.
     */
    @Operation(summary = "Semantic Navigation Site click-through rates per term")
    @GetMapping("clicks")
    public TurSNClickThroughService.Report turSNSiteMetricsClicks(@PathVariable String snSiteId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Integer rows) {
        TurSNSite site = turSNSiteRepository.findById(snSiteId)
                .orElseThrow(() -> new TurNotFoundException("SN site not found: " + snSiteId));
        TurSNSearchInsightsService.Window window = turSNSearchInsightsService.window(period, from, to);
        return turSNClickThroughService.report(site, window, TurSNClickThroughService.locale(locale), rows);
    }
}
