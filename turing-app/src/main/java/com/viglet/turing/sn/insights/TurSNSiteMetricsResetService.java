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

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * T1195 — empties a site's search metrics (the searches and the result clicks
 * its insights read), so the probes and tests run before a demo do not lead its
 * top terms and failed searches for good. Every clear is logged with who made it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
@Slf4j
@Service
public class TurSNSiteMetricsResetService {

    private final TurSNSiteMetricAccessRepository accessRepository;
    private final TurSNSiteMetricClickRepository clickRepository;

    public TurSNSiteMetricsResetService(TurSNSiteMetricAccessRepository accessRepository,
            TurSNSiteMetricClickRepository clickRepository) {
        this.accessRepository = accessRepository;
        this.clickRepository = clickRepository;
    }

    /** How many searches and clicks a clear removed. */
    public record Cleared(long searches, long clicks) {
    }

    /**
     * Removes the site's searches and clicks recorded before {@code before}, or
     * all of them when it is {@code null}.
     */
    @Transactional
    public Cleared clear(TurSNSite site, Instant before, String clearedBy) {
        Instant until = before == null ? Instant.now().plusSeconds(1) : before;
        Cleared cleared = new Cleared(accessRepository.deleteByTurSNSiteAndAccessDateBefore(site, until),
                clickRepository.deleteByTurSNSiteAndAccessDateBefore(site, until));
        log.info("[Audit] SN site '{}' ({}) metrics cleared by '{}' before {}: {} searches, {} clicks",
                site.getName(), site.getId(), clearedBy, before == null ? "now" : before,
                cleared.searches(), cleared.clicks());
        return cleared;
    }
}
