/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.searchengine.lucene;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.springframework.stereotype.Component;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.facet.TurSEFacetResult;
import com.viglet.turing.searchengine.facet.TurSEFacetResultAttr;
import com.viglet.turing.searchengine.facet.TurSECustomFacetSpan;
import com.viglet.turing.searchengine.spi.TurSECustomFacet;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * T1083 — custom facets (a field's labelled range items) on the Lucene engine,
 * mirroring what {@code TurSolrQueryBuilder} and {@code TurSolrResultProcessor}
 * do on Solr:
 * <ul>
 * <li>{@link #count} answers one {@link TurSEFacetResult} per custom facet,
 * named after it, with one item per label and the number of matching
 * documents in that item's range;</li>
 * <li>{@link #resolveFilter} turns the {@code <customFacet>:<label>} filter
 * query a clicked item sends back into the range it stands for.</li>
 * </ul>
 * The range is built by sn's {@code TurSNCustomFacetFilterQuery}, the same text Solr
 * receives, handed over through {@link TurSESiteSearchConfig#customFacets} (T1310),
 * and read by {@link TurLuceneFilterQueries}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Component
public class TurLuceneCustomFacets {

    private final TurSESiteSearchConfig siteSearchConfig;

    public TurLuceneCustomFacets(TurSESiteSearchConfig siteSearchConfig) {
        this.siteSearchConfig = siteSearchConfig;
    }

    /**
     * A custom facet item resolved to its field and the filter value of its range;
     * {@code span} is set when the facet names an end field (T1224).
     */
    public record ItemFilter(String field, TurSEFieldType type, String value,
            TurSECustomFacetSpan span) {

        public ItemFilter(String field, TurSEFieldType type, String value) {
            this(field, type, value, null);
        }

        /**
         * The Lucene query the item stands for: its range on {@code field}, or with
         * a span, the point range for a document with no end value or every overlap
         * clause for one with an end value.
         */
        public Query toLuceneQuery(Instant now) {
            if (span == null) {
                return TurLuceneFilterQueries.build(field, value, type, now);
            }
            Query point = new BooleanQuery.Builder()
                    .add(TurLuceneFilterQueries.build(field, span.point(), type, now), BooleanClause.Occur.MUST)
                    .add(TurLuceneFilterQueries.build(span.endField(), "[* TO *]", type, now),
                            BooleanClause.Occur.MUST_NOT)
                    .build();
            BooleanQuery.Builder overlap = new BooleanQuery.Builder();
            span.overlap().forEach(clause -> overlap.add(
                    TurLuceneFilterQueries.build(clause.field(), clause.value(), type, now),
                    BooleanClause.Occur.MUST));
            return new BooleanQuery.Builder()
                    .setMinimumNumberShouldMatch(1)
                    .add(point, BooleanClause.Occur.SHOULD)
                    .add(overlap.build(), BooleanClause.Occur.SHOULD)
                    .build();
        }
    }

    /** T1232 — one item of a custom facet, ready for an engine to count. */
    public record Item(String label, String displayLabel, ItemFilter filter) {
    }

    /** T1232 — one custom facet of a site, its items in display order. */
    public record Facet(String name, Integer position, List<Item> items) {
    }

    /**
     * T1232 — the site's custom facets in display order, each item with the
     * filter it stands for and its label for {@code locale}, for an engine that
     * counts them in its own request (Elasticsearch) rather than here.
     */
    public List<Facet> facets(String siteId, Locale locale) {
        List<Facet> facets = new ArrayList<>();
        for (TurSECustomFacet definition : definitions(siteId).values()) {
            List<Item> items = definition.items().stream()
                    .map(item -> new Item(item.label(), displayLabel(item, locale), itemFilter(item)))
                    .toList();
            if (!items.isEmpty()) {
                facets.add(new Facet(definition.name(), definition.position(), items));
            }
        }
        return facets;
    }

    /**
     * The range a {@code <customFacet>:<label>} selection stands for, or empty when
     * {@code facetName} names no custom facet of the site (an ordinary field).
     */
    public Optional<ItemFilter> resolveFilter(String siteId, String facetName, String label) {
        TurSECustomFacet definition = definitions(siteId).get(facetName);
        if (definition == null) {
            return Optional.empty();
        }
        return definition.items().stream()
                .filter(item -> label.equals(item.label()))
                .findFirst()
                .map(TurLuceneCustomFacets::itemFilter);
    }

    /**
     * Counts every custom facet's items over the documents {@code query} matches.
     * With {@code onlyFacet} set, counts that custom facet alone (the facet
     * drill-down), and nothing when it names no custom facet.
     */
    public List<TurSEFacetResult> count(IndexSearcher searcher, Query query, String siteId,
            Locale locale, String onlyFacet) {
        List<TurSEFacetResult> results = new ArrayList<>();
        for (TurSECustomFacet definition : definitions(siteId).values()) {
            if (onlyFacet != null && !onlyFacet.equals(definition.name())) {
                continue;
            }
            countFacet(searcher, query, definition, locale).ifPresent(results::add);
        }
        return results;
    }

    private Optional<TurSEFacetResult> countFacet(IndexSearcher searcher, Query query,
            TurSECustomFacet definition, Locale locale) {
        TurSEFacetResult result = new TurSEFacetResult();
        result.setFacet(definition.name());
        Instant now = Instant.now();
        for (TurSECustomFacet.Item item : definition.items()) {
            try {
                ItemFilter filter = itemFilter(item);
                Query itemQuery = new BooleanQuery.Builder()
                        .add(query, BooleanClause.Occur.MUST)
                        .add(filter.toLuceneQuery(now), BooleanClause.Occur.FILTER)
                        .build();
                result.add(item.label(), new TurSEFacetResultAttr(item.label(),
                        searcher.count(itemQuery), displayLabel(item, locale)));
            } catch (IOException | IllegalArgumentException e) {
                log.warn("[Lucene] custom facet item '{}' of '{}' not counted: {}", item.label(),
                        definition.name(), e.getMessage());
            }
        }
        return result.getTurSEFacetResultAttr().isEmpty() ? Optional.empty() : Optional.of(result);
    }

    private static ItemFilter itemFilter(TurSECustomFacet.Item item) {
        return new ItemFilter(item.field(), item.type(), item.value(), item.span());
    }

    /** The site's custom facets by name, in display order (T1310: through the SPI). */
    private Map<String, TurSECustomFacet> definitions(String siteId) {
        Map<String, TurSECustomFacet> byName = new java.util.LinkedHashMap<>();
        siteSearchConfig.customFacets(siteId).forEach(facet -> byName.putIfAbsent(facet.name(), facet));
        return byName;
    }
    /** The item's label for the request locale, as {@code TurSolrResultProcessor} resolves it. */
    private static String displayLabel(TurSECustomFacet.Item item, Locale locale) {
        Map<String, String> labels = item.labels();
        if (labels == null || labels.isEmpty() || locale == null) {
            return item.label();
        }
        for (String key : List.of(locale.toString(), locale.toLanguageTag(), locale.getLanguage())) {
            String translated = labels.get(key);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }
        return item.label();
    }
}
