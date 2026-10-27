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

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickTerm;
import com.viglet.turing.spring.security.auth.TurApiKeyPartitionContext;

/**
 * Click-through rates read back from the click log (T1131): per term, how often
 * it was searched, how often a result was opened, and where the opened results
 * sat; plus the terms searched often whose results were never opened. Searches
 * and clicks are joined on the sanitized term, which both logs derive the same
 * way.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Service
public class TurSNClickThroughService {

    /** How wide to scan the popular terms when looking for the never-clicked ones. */
    private static final int SCAN_MULTIPLIER = 5;

    private final TurSNSiteMetricAccessRepository accessRepository;
    private final TurSNSiteMetricClickRepository clickRepository;

    @Value("${turing.search.metrics.enabled:false}")
    private boolean metricsEnabled;

    public TurSNClickThroughService(TurSNSiteMetricAccessRepository accessRepository,
            TurSNSiteMetricClickRepository clickRepository) {
        this.accessRepository = accessRepository;
        this.clickRepository = clickRepository;
    }

    /**
     * One term of the report. {@code ctr} is clicks per search, so it can pass 1
     * when a visitor opens several results; {@code avgPosition} is {@code null}
     * when nothing was clicked.
     */
    public record Term(String term, long searches, long clicks, double ctr, Double avgPosition,
            double avgResults) {
    }

    /**
     * The report: its window and locale ({@code null} for every locale), the
     * site's totals, the most searched terms, the terms that found results
     * nobody opened, and (T1149) whether search metrics are recorded at all.
     */
    public record Report(Instant from, Instant to, String locale, long searches, long clicks, double ctr,
            List<Term> terms, List<Term> neverClicked, boolean metricsEnabled) {
    }

    /**
     * Parses a {@code locale} parameter, accepting {@code pt_BR} and {@code pt-BR}.
     *
     * @throws IllegalArgumentException for a value that is not a locale
     */
    public static Locale locale(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        return LocaleUtils.toLocale(raw.trim().replace('-', '_'));
    }

    @Transactional(readOnly = true)
    public Report report(TurSNSite site, TurSNSearchInsightsService.Window window, Locale locale,
            Integer rows) {
        int limit = TurSNSearchInsightsService.clamp(rows);
        Instant start = window.from() != null ? window.from() : Instant.EPOCH;
        Instant end = window.to();
        // T1133 — a key bound to a partition of a shared site reads its partition's logs.
        String partition = TurApiKeyPartitionContext.current().map(TurSEPartition::key).orElse(null);

        List<TurSNSiteMetricAccessTerm> searched = accessRepository.termsBetweenDatesForLocale(site, start, end,
                locale, partition, PageRequest.of(0, limit * SCAN_MULTIPLIER));
        List<String> names = searched.stream().map(TurSNSiteMetricAccessTerm::getTerm).filter(Objects::nonNull)
                .toList();
        Map<String, TurSNSiteMetricClickTerm> clicks = names.isEmpty() ? Map.of()
                : clickRepository.clicksByTerm(site, start, end, locale, partition, names).stream()
                        .collect(Collectors.toMap(TurSNSiteMetricClickTerm::term, Function.identity(),
                                (a, b) -> a));

        List<Term> all = searched.stream().map(s -> term(s, clicks.get(s.getTerm()))).toList();
        List<Term> neverClicked = all.stream().filter(t -> t.clicks() == 0 && t.avgResults() > 0).limit(limit)
                .toList();

        long totalSearches = accessRepository.countSearchesForLocale(site, start, end, locale, partition);
        long totalClicks = clickRepository.countClicks(site, start, end, locale, partition);
        return new Report(window.from(), end, locale == null ? null : locale.toString(), totalSearches,
                totalClicks, ratio(totalClicks, totalSearches), all.stream().limit(limit).toList(), neverClicked,
                metricsEnabled);
    }

    private static Term term(TurSNSiteMetricAccessTerm searched, TurSNSiteMetricClickTerm clicked) {
        long clicks = clicked == null ? 0 : clicked.clicks();
        Double position = clicked == null || clicked.avgPosition() == null ? null
                : round(clicked.avgPosition());
        return new Term(searched.getTerm(), searched.getTotal(), clicks, ratio(clicks, searched.getTotal()),
                position, round(searched.getNumFound()));
    }

    private static double ratio(long clicks, long searches) {
        return searches == 0 ? 0 : round((double) clicks / searches);
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }
}
