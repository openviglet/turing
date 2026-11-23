/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.ranking;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.viglet.turing.searchengine.result.TurSEResult;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T383 hybrid ranking as SN's search and indexing paths call it: the per-site
 * vector collection a {@code HYBRID_RRF} site keeps beside its search index,
 * fused with the lexical page by reciprocal rank fusion, plus the
 * similar-documents neighbours it serves.
 *
 * <p>T1322h — declared by sn and implemented by rag's
 * {@code TurRagHybridRankingService}, since the vector stores, embeddings and
 * reranker it runs on are rag's (ADR 0005 section 6: rag sits above sn).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNHybridRankingService {

    /** Whether the site ranks hybrid ({@code HYBRID_RRF}) and has a vector setup. */
    boolean isEnabled(TurSNSite turSNSite);

    /** Writes the document's embedding into the site's vector collection; a no-op when not hybrid. */
    void indexDocument(TurSNSite turSNSite, Locale locale, Map<String, Object> attributes);

    /** Removes one document's vectors. */
    void deIndex(TurSNSite turSNSite, String id);

    /** Removes the vectors of every document of {@code type}. */
    void deIndexByType(TurSNSite turSNSite, String type);

    /** Whether the site also recalls vector hits the lexical page missed. */
    boolean isSemanticRecallEnabled(TurSNSite turSNSite);

    /** Fuses the lexical {@code page} with the vector ranking for {@code query}. */
    List<TurSEResult> fuse(TurSNSite turSNSite, Locale locale, String query, List<TurSEResult> page);

    /**
     * As {@link #fuse(TurSNSite, Locale, String, List)}, with {@code recall}
     * resolving vector-only ids into results ({@code null} disables recall).
     */
    List<TurSEResult> fuse(TurSNSite turSNSite, Locale locale, String query, List<TurSEResult> page,
            Function<List<String>, List<TurSEResult>> recall);

    /** Up to {@code topK} ids nearest the seed document, seed excluded. */
    List<String> findNeighbors(TurSNSite turSNSite, Map<String, Object> seedAttributes, int topK);

    /** Up to {@code topK} scored neighbours above {@code minSimilarity}; empty on any failure. */
    List<TurSNScoredNeighbor> findScoredNeighbors(TurSNSite turSNSite, Map<String, Object> seedAttributes,
            int topK, double minSimilarity);

    /**
     * As above, confined to {@code partition}; empty (not an empty list) when the
     * collection holds no point of the partition at all.
     */
    Optional<List<TurSNScoredNeighbor>> findScoredNeighbors(TurSNSite turSNSite,
            Map<String, Object> seedAttributes, int topK, double minSimilarity,
            com.viglet.turing.commons.se.TurSEPartition partition);

    /** Drops the cached vector infrastructure of a site, after its configuration changed. */
    void evict(String siteId);
}
