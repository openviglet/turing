/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.ac;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricTerms;
import com.viglet.turing.spring.security.auth.TurApiKeyPartitionContext;

import lombok.extern.slf4j.Slf4j;

/**
 * T1246 — the site's most searched terms as autocomplete suggestions, for a site
 * with {@code autocompleteTopTerms} on.
 *
 * <p>Only terms whose searches found something ({@code numFound > 0} on average,
 * so at least once) are offered, from the last {@link #WINDOW}, ranked by how
 * often they were searched and shown as visitors typed them. A key bound to a
 * partition of a shared site reads only its partition's searches (T1133). The
 * ranked list is kept per site, locale and partition for {@link #TTL}: it is a
 * read of a log that changes by the search, so a few minutes of staleness costs
 * nothing and spares the metric table a grouped query per keystroke.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurSNAutoCompleteTopTerms {
    /** How far back the search log is read. */
    static final Duration WINDOW = Duration.ofDays(90);
    /** How long a site's ranked list is reused. */
    static final Duration TTL = Duration.ofMinutes(5);
    /** How many of the most searched terms are ranked per site. */
    static final int RANKED = 500;

    private final TurSNSiteMetricAccessRepository repository;
    private final Clock clock;
    private final Map<String, Ranked> ranked = new ConcurrentHashMap<>();

    @Autowired
    public TurSNAutoCompleteTopTerms(TurSNSiteMetricAccessRepository repository) {
        this(repository, Clock.systemUTC());
    }

    TurSNAutoCompleteTopTerms(TurSNSiteMetricAccessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    private record Ranked(Instant at, List<String> terms) {
    }

    /** Whether {@code site} mixes its most searched terms into autocomplete. */
    public static boolean enabled(TurSNSite site) {
        return site != null && Integer.valueOf(1).equals(site.getAutocompleteTopTerms());
    }

    /**
     * The most searched terms of {@code site} that start with {@code q} (folded:
     * case and accents ignored), most searched first, at most {@code rows}.
     */
    @Transactional(readOnly = true)
    public List<String> suggest(TurSNSite site, String q, Locale locale, long rows) {
        if (!enabled(site) || q == null || rows <= 0) {
            return Collections.emptyList();
        }
        String prefix = TurSNSiteMetricTerms.fold(q);
        if (prefix.isEmpty()) {
            return Collections.emptyList();
        }
        String partition = TurApiKeyPartitionContext.current().map(TurSEPartition::key).orElse(null);
        List<String> suggestions = new ArrayList<>();
        for (String term : ranked(site, locale, partition)) {
            String key = TurSNSiteMetricTerms.fold(term);
            if (key.startsWith(prefix) && !key.equals(prefix)) {
                suggestions.add(term);
                if (suggestions.size() >= rows) {
                    break;
                }
            }
        }
        return suggestions;
    }

    /**
     * {@code index} followed by those of {@code popular} it does not already hold
     * (folded), at most {@code rows}: the index's suggestions keep their place.
     */
    public static List<String> merge(List<String> index, List<String> popular, long rows) {
        if (popular.isEmpty()) {
            return index;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        index.forEach(term -> merged.putIfAbsent(TurSNSiteMetricTerms.fold(term), term));
        popular.forEach(term -> merged.putIfAbsent(TurSNSiteMetricTerms.fold(term), term));
        return merged.values().stream().limit(Math.max(rows, 0)).toList();
    }

    private List<String> ranked(TurSNSite site, Locale locale, String partition) {
        String cacheKey = site.getId() + "|" + locale + "|" + partition;
        Instant now = clock.instant();
        Ranked cached = ranked.get(cacheKey);
        if (cached != null && cached.at().plus(TTL).isAfter(now)) {
            return cached.terms();
        }
        List<String> terms = load(site, locale, partition, now);
        ranked.put(cacheKey, new Ranked(now, terms));
        return terms;
    }

    private List<String> load(TurSNSite site, Locale locale, String partition, Instant now) {
        try {
            List<TurSNSiteMetricAccessTerm> rows = new ArrayList<>(repository.termsBetweenDatesForLocale(site,
                    now.minus(WINDOW), now, locale, partition, PageRequest.of(0, RANKED)));
            rows.removeIf(row -> row.getTerm() == null || row.getTerm().isBlank() || row.getNumFound() <= 0);
            TurSNSiteMetricTerms.asTyped(repository, site, rows);
            return rows.stream().map(TurSNSiteMetricAccessTerm::getTerm).toList();
        } catch (RuntimeException e) {
            // Suggestions from the log are a bonus: the index's still answer.
            log.warn("Most searched terms of site {} not read for autocomplete: {}", site.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
