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

import com.viglet.turing.searchengine.TurSEFlags;
import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.facet.TurSEFacetResult;
import com.viglet.turing.searchengine.result.TurSEResults;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main Lucene service — orchestrates query building, execution, and result
 * processing. Mirrors {@code TurSolr} for the Lucene search engine plugin.
 *
 * @author Alexandre Oliveira
 * @since 2026.1
 */
@Slf4j
@Component
@Transactional
public class TurLucene {

    private final TurSESiteSearchConfig siteSearchConfig;
    private final TurLuceneQueryBuilder turLuceneQueryBuilder;
    private final TurLuceneResultProcessor turLuceneResultProcessor;
    private final TurLuceneDocumentHandler turLuceneDocumentHandler;
    private final TurLuceneCustomFacets turLuceneCustomFacets;

    public TurLucene(TurSESiteSearchConfig siteSearchConfig,
            TurLuceneQueryBuilder turLuceneQueryBuilder,
            TurLuceneStorageSync turLuceneStorageSync,
            TurLuceneCustomFacets turLuceneCustomFacets) {
        this.siteSearchConfig = siteSearchConfig;
        this.turLuceneQueryBuilder = turLuceneQueryBuilder;
        this.turLuceneCustomFacets = turLuceneCustomFacets;
        this.turLuceneResultProcessor = new TurLuceneResultProcessor();
        this.turLuceneDocumentHandler = new TurLuceneDocumentHandler(siteSearchConfig,
                turLuceneStorageSync);
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    /**
     * Executes a full search for the given site/locale context and returns
     * populated {@link TurSEResults} including facets.
     */
    public Optional<TurSEResults> retrieveLuceneFromSN(TurLuceneInstance instance,
            TurSNSiteSearchContext context) {
        return siteSettings(context).map(site -> {
            long startTime = System.currentTimeMillis();
            TurSEParameters params = context.getTurSEParameters();

            Query query = turLuceneQueryBuilder.buildQuery(site, params, context.getLocale());
            // T1226 — a caller that turns highlighting off (autocomplete) gets none,
            // as on Solr, whatever the site's own setting says.
            List<TurSESearchField> hlFields = context.getTurSNConfig() != null
                    && !context.getTurSNConfig().isHlEnabled() ? List.of()
                            : turLuceneQueryBuilder.getHLFields(site);
            var sort = turLuceneQueryBuilder.buildSort(site, params);

            TurSEResults results;
            try {
                results = turLuceneResultProcessor.getResults(
                        new TurLuceneSearch(instance, site, query, params),
                        turLuceneQueryBuilder.getFacetFields(site, params),
                        hlFields, sort, startTime);
            } catch (org.apache.lucene.search.IndexSearcher.TooManyClauses e) {
                // T1304 — a synonym expansion past the clause limit answers as the words
                // typed would, rather than failing the search.
                log.warn("[Lucene] query '{}' on site '{}' went past the clause limit ({}); "
                        + "searching again without the site's synonyms", params.getQuery(),
                        site.name(), e.getMessage());
                query = turLuceneQueryBuilder.buildQuery(site, params, context.getLocale(), false);
                results = turLuceneResultProcessor.getResults(
                        new TurLuceneSearch(instance, site, query, params),
                        turLuceneQueryBuilder.getFacetFields(site, params),
                        hlFields, sort, startTime);
            }

            // Wildcard fallback when the query matched nothing and the site allows it. Decided by
            // the total hits, not by the page being empty: a page past the end of the pagination
            // is empty too, and must not be retried with a wildcard.
            if (results.getNumFound() == 0
                    && shouldApplyWildcard(site, params.getQuery())) {
                Query wildcardQuery = turLuceneQueryBuilder.buildWildcardQuery(site, params);
                results = turLuceneResultProcessor.getResults(
                        new TurLuceneSearch(instance, site, wildcardQuery, params),
                        turLuceneQueryBuilder.getFacetFields(site, params),
                        hlFields, sort, startTime);
                query = wildcardQuery;
            }
            return withCustomFacets(results, instance, query, site, context, null);
        });
    }

    /**
     * T1083 — adds the site's custom facets (labelled range items) to the facet
     * results, counted over the documents {@code query} matches, as Solr answers
     * them from its facet queries. Only when the site has facets on.
     */
    private TurSEResults withCustomFacets(TurSEResults results, TurLuceneInstance instance, Query query,
            TurSESiteSettings site, TurSNSiteSearchContext context, String onlyFacet) {
        if (turLuceneCustomFacets == null || !TurSEFlags.isTrue(site.facet())) {
            return results;
        }
        try {
            List<TurSEFacetResult> custom = turLuceneCustomFacets.count(instance.getSearcher(), query,
                    site.id(), context.getLocale(), onlyFacet);
            if (!custom.isEmpty()) {
                List<TurSEFacetResult> facets = new ArrayList<>(
                        results.getFacetResults() == null ? List.of() : results.getFacetResults());
                facets.addAll(custom);
                results.setFacetResults(facets);
            }
        } catch (IOException e) {
            log.warn("[Lucene] custom facets for site '{}' not counted: {}", site.name(),
                    e.getMessage());
        }
        return results;
    }

    /**
     * Executes a search scoped to a single facet dimension (used by the facet
     * drill-down API).
     */
    public Optional<TurSEResults> retrieveFacetLuceneFromSN(TurLuceneInstance instance,
            TurSNSiteSearchContext context, String facetName) {
        return siteSettings(context).map(site -> {
            long startTime = System.currentTimeMillis();
            TurSEParameters params = context.getTurSEParameters();
            Query query = turLuceneQueryBuilder.buildQuery(site, params, context.getLocale());
            TurSEResults results = turLuceneResultProcessor.getResults(
                    new TurLuceneSearch(instance, site, query, params),
                    turLuceneQueryBuilder.getFacetFields(site, facetName),
                    List.of(), null, startTime);
            return withCustomFacets(results, instance, query, site, context, facetName);
        });
    }

    // -------------------------------------------------------------------------
    // Indexing
    // -------------------------------------------------------------------------

    public void indexing(TurLuceneInstance instance, String siteId, Map<String, Object> attributes) {
        turLuceneDocumentHandler.indexing(instance, siteId, attributes);
    }

    /**
     * T804 / §LV.2 — bulk indexing: one writer session + one commit across all
     * documents. Delegates to {@link TurLuceneDocumentHandler#indexingBatch}.
     */
    public int indexingBatch(TurLuceneInstance instance, String siteId,
            List<Map<String, Object>> documents) {
        return turLuceneDocumentHandler.indexingBatch(instance, siteId, documents);
    }


    public void deIndexing(TurLuceneInstance instance, String id) {
        turLuceneDocumentHandler.deIndexing(instance, id);
    }

    public void deIndexingByType(TurLuceneInstance instance, String type) {
        turLuceneDocumentHandler.deIndexingByType(instance, type);
    }

    /** T1112 — deletes the documents whose fields hold all of these values. */
    public void deIndexingByFields(TurLuceneInstance instance, java.util.Map<String, String> fields) {
        turLuceneDocumentHandler.deIndexingByFields(instance, fields);
    }

    // -------------------------------------------------------------------------
    // Wildcard helpers
    // -------------------------------------------------------------------------

    private Optional<TurSESiteSettings> siteSettings(TurSNSiteSearchContext context) {
        return siteSearchConfig.siteSettings(context.getSiteName());
    }

    private static boolean shouldApplyWildcard(TurSESiteSettings site, String query) {
        return TurSEFlags.isTrue(site.wildcardNoResults()) && !isQueryExpression(query);
    }

    /** Returns true when the query already ends with a wildcard or expression boundary. */
    public static boolean isQueryExpression(String query) {
        if (query == null || query.isBlank()) return false;
        String trimmed = query.trim();
        return trimmed.endsWith("*") || trimmed.endsWith("\"")
                || trimmed.endsWith("]") || trimmed.endsWith(")");
    }
}
