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

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm;

/**
 * What visitors searched for and did not find (T1119): the zero-result queries
 * and the content gaps, read from the search-access log. The one implementation
 * both the REST metrics and the MCP analytics tools call, so the two surfaces
 * cannot disagree.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Service
public class TurSNSearchInsightsService {

    public static final int DEFAULT_ROWS = 10;
    public static final int MAX_ROWS = 50;
    /** A query is a content gap when its average result count is below this. */
    public static final double LOW_COVERAGE_THRESHOLD = 3.0;
    /** How wide to scan the popular queries when filtering for low coverage. */
    private static final int GAP_SCAN_MULTIPLIER = 5;

    private final TurSNSiteMetricAccessRepository metricAccessRepository;
    private Clock clock = Clock.systemUTC();

    void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    public TurSNSearchInsightsService(TurSNSiteMetricAccessRepository metricAccessRepository) {
        this.metricAccessRepository = metricAccessRepository;
    }

    /** One term of a report: how often it was searched, what it found, when last. */
    public record Term(String term, long searches, long avgResults, Instant lastSeen) {

        static Term of(TurSNSiteMetricAccessTerm term) {
            return new Term(term.getTerm(), term.getTotal(), (long) term.getNumFound(), term.getAcessDate());
        }
    }

    /** The window a report covers; {@code from} is {@code null} for all time. */
    public record Window(Instant from, Instant to) {
    }

    /** A report as the REST metrics answer it: the window it covers and its terms. */
    public record Report(Instant from, Instant to, List<Term> terms) {

        public static Report of(Window window, List<Term> terms) {
            return new Report(window.from(), window.to(), terms);
        }
    }

    /**
     * Resolves a report window: explicit instants win, then the {@code period}
     * shorthand ({@code today}, {@code this-week}, {@code this-month},
     * {@code all-time}); nothing at all is all time, up to now.
     *
     * @throws IllegalArgumentException for an unknown period or {@code from} after {@code to}
     */
    public Window window(String period, Instant from, Instant to) {
        Instant now = clock.instant();
        Instant end = to != null ? to : now;
        Instant start = from != null ? from : periodStart(period, now);
        if (start != null && start.isAfter(end)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        return new Window(start, end);
    }

    private static Instant periodStart(String period, Instant now) {
        if (period == null || period.isBlank()) {
            return null;
        }
        return switch (period.toLowerCase()) {
            case "all-time" -> null;
            case "today" -> now.minus(1, ChronoUnit.DAYS);
            case "this-week", "week" -> now.minus(7, ChronoUnit.DAYS);
            case "this-month", "month" -> now.minus(30, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Unknown period '" + period
                    + "': use today, this-week, this-month or all-time");
        };
    }

    /** Queries that returned nothing every time they ran, most searched first. */
    @Transactional(readOnly = true)
    public List<Term> failedSearches(TurSNSite site, Window window, Integer rows) {
        return metricAccessRepository.zeroResultTermsBetweenDates(site, start(window), window.to(),
                PageRequest.of(0, clamp(rows))).stream().map(Term::of).toList();
    }

    /**
     * Queries searched often whose average result count is below
     * {@link #LOW_COVERAGE_THRESHOLD}: what to write next.
     */
    @Transactional(readOnly = true)
    public List<Term> contentGaps(TurSNSite site, Window window, Integer rows) {
        int limit = clamp(rows);
        List<Term> gaps = new ArrayList<>();
        for (TurSNSiteMetricAccessTerm term : metricAccessRepository.termsWithLastSeenBetweenDates(site,
                start(window), window.to(), PageRequest.of(0, limit * GAP_SCAN_MULTIPLIER))) {
            if (term.getNumFound() < LOW_COVERAGE_THRESHOLD) {
                gaps.add(Term.of(term));
                if (gaps.size() >= limit) {
                    break;
                }
            }
        }
        return gaps;
    }

    private static Instant start(Window window) {
        return window.from() != null ? window.from() : Instant.EPOCH;
    }

    public static int clamp(Integer rows) {
        if (rows == null || rows < 1) {
            return DEFAULT_ROWS;
        }
        return Math.min(rows, MAX_ROWS);
    }
}
