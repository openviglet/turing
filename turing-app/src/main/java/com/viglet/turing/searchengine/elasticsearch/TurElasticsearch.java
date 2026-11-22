/*
 * Copyright (C) 2016-2022 the original author or authors.
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
package com.viglet.turing.searchengine.elasticsearch;

import com.viglet.turing.searchengine.TurSEFlags;
import com.viglet.turing.searchengine.spi.TurSECustomSortItem;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.UnknownNullability;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.searchengine.lucene.TurLuceneCustomFacets;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;
import com.viglet.turing.searchengine.TurSNQueryRelaxer;
import com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum;
import com.viglet.turing.searchengine.facet.TurSEFacetResult;
import com.viglet.turing.searchengine.facet.TurSEFacetResultAttr;
import com.viglet.turing.searchengine.result.TurSEResult;
import com.viglet.turing.searchengine.result.TurSEResults;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch search operations
 *
 * @author Alexandre Oliveira
 * @since 2025.4.4
 */
@Slf4j
@Component
public class TurElasticsearch {

    // --- S1192: extracted duplicated literals ---
    private static final String INDEX_NOT_FOUND_EXCEPTION = "index_not_found_exception";

    private static final String FACET_AGG_PREFIX = "facet_";

    private final TurSESiteSearchConfig siteSearchConfig;
    private final TurLuceneCustomFacets customFacets;
    private final TurElasticsearchRankingExpressions rankingExpressions;

    /** Without ranking expressions: the shape before T1222. */
    public TurElasticsearch(TurSESiteSearchConfig siteSearchConfig,
            TurLuceneCustomFacets customFacets) {
        this(siteSearchConfig, customFacets, null);
    }

    /**
     * T1310 — the site, its fields and its sorts are read through the search SPI,
     * never from {@code sn}'s repositories.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public TurElasticsearch(TurSESiteSearchConfig siteSearchConfig,
            TurLuceneCustomFacets customFacets,
            TurElasticsearchRankingExpressions rankingExpressions) {
        this.siteSearchConfig = siteSearchConfig;
        this.customFacets = customFacets;
        this.rankingExpressions = rankingExpressions;
    }

    public Optional<TurSEResults> retrieveElasticsearchFromSN(
            TurElasticsearchInstance elasticsearchInstance,
            TurSNSiteSearchContext context) {
        return siteSearchConfig.siteSettings(context.getSiteName())
                .flatMap(turSNSite -> {
                    try {
                        TurSEParameters turSEParameters = context.getTurSEParameters();

                        int rows = turSEParameters.getRows() > 0 ? turSEParameters.getRows() : 10;
                        int from = getStartPosition(turSEParameters);
                        Map<String, TurSESearchField> fields = enabledFieldsByName(turSNSite.id());
                        Query query = buildQuery(turSNSite, fields, turSEParameters);

                        // T1250 — the facets the request leaves out are not aggregated.
                        List<TurSESearchField> facetFields = facetFields(turSNSite.id(), fields).stream()
                                .filter(field -> !turSEParameters.isFacetExcluded(field.name()))
                                .toList();
                        log.debug("ES search index={} query={} from={} size={} facetFields={}",
                                elasticsearchInstance.getIndex(), query, from, rows,
                                facetFields.stream().map(TurSESearchField::name).toList());

                        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                                .index(elasticsearchInstance.getIndex())
                                .query(query)
                                .from(from)
                                .size(rows);

                        addFacetAggregations(searchBuilder, facetFields);
                        // T1232 - the site's custom facets, counted in the same request.
                        List<TurLuceneCustomFacets.Facet> siteCustomFacets = customFacetsOf(turSNSite,
                                context.getLocale(), null);
                        addCustomFacetAggregations(searchBuilder, siteCustomFacets, Instant.now());
                        addOptionalSorting(searchBuilder, turSNSite, fields, turSEParameters);

                        SearchRequest searchRequest = searchBuilder.build();
                        SearchResponse<Map<String, Object>> response = elasticsearchInstance.getClient()
                                .search(searchRequest, (Type) Map.class);

                        TurSEResults results = buildTurSEResults(response, turSEParameters, facetFields);
                        List<TurSEFacetResult> custom = customFacetResults(response.aggregations(),
                                siteCustomFacets);
                        if (!custom.isEmpty()) {
                            List<TurSEFacetResult> facets = new ArrayList<>(results.getFacetResults());
                            facets.addAll(custom);
                            results.setFacetResults(facets);
                        }
                        return Optional.of(results);
                    } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
                        handleElasticsearchException(e, elasticsearchInstance.getIndex());
                        return Optional.empty();
                    } catch (IOException e) {
                        log.error("Error executing Elasticsearch search: {}", e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }

    public Optional<TurSEResults> retrieveFacetElasticsearchFromSN(
            TurElasticsearchInstance elasticsearchInstance,
            TurSNSiteSearchContext context,
            String facetName) {
        return siteSearchConfig.siteSettings(context.getSiteName())
                .flatMap(turSNSite -> {
                    try {
                        TurSEParameters turSEParameters = context.getTurSEParameters();
                        Map<String, TurSESearchField> fields = enabledFieldsByName(turSNSite.id());
                        Query query = buildQuery(turSNSite, fields, turSEParameters);
                        // T1232 - a custom facet's drill-down counts its items.
                        List<TurLuceneCustomFacets.Facet> customFacet = customFacetsOf(turSNSite,
                                context.getLocale(), facetName);
                        if (!customFacet.isEmpty()) {
                            return Optional.of(customFacetDrillDown(elasticsearchInstance, query,
                                    turSEParameters, customFacet));
                        }
                        String aggName = FACET_AGG_PREFIX + facetName;

                        // Resolve the correct ES field name (text fields need ".keyword" sub-field)
                        String aggField = Optional.ofNullable(fields.get(facetName))
                                .filter(field -> TurSEFlags.isTrue(field.facet()))
                                .map(TurElasticsearch::getFacetAggField)
                                .orElse(facetName);

                        SearchRequest searchRequest = SearchRequest.of(r -> r
                                .index(elasticsearchInstance.getIndex())
                                .query(query)
                                .size(0)
                                .aggregations(aggName, a -> a
                                        .terms(t -> t.field(aggField).size(100))));

                        SearchResponse<Map<String, Object>> response = elasticsearchInstance.getClient()
                                .search(searchRequest, (Type) Map.class);

                        TurSEFacetResult facetResult = new TurSEFacetResult();
                        facetResult.setFacet(facetName);
                        facetResult.setFacetPosition(1);

                        var agg = response.aggregations().get(aggName);
                        if (agg != null && agg.isSterms()) {
                            List<StringTermsBucket> buckets = agg.sterms().buckets().array();
                            for (StringTermsBucket bucket : buckets) {
                                String key = bucket.key().stringValue();
                                int count = (int) bucket.docCount();
                                facetResult.add(key, new TurSEFacetResultAttr(key, count));
                            }
                        }

                        TurSEResults results = TurSEResults.builder()
                                .numFound(0)
                                .start(0)
                                .limit(0)
                                .pageCount(0)
                                .currentPage(1)
                                .results(Collections.emptyList())
                                .qTime((int) response.took())
                                .elapsedTime(response.took())
                                .queryString(turSEParameters.getQuery())
                                .sort(turSEParameters.getSort())
                                .spellCheck(null)
                                .similarResults(Collections.emptyList())
                                .facetResults(List.of(facetResult))
                                .groups(Collections.emptyList())
                                .build();
                        return Optional.of(results);
                    } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
                        log.error("ES facet query failed for field={}: type={} reason={}",
                                facetName, e.error().type(), e.error().reason());
                        return Optional.empty();
                    } catch (IOException e) {
                        log.error("IO error executing ES facet query for field={}: {}", facetName, e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Indexing
    // -------------------------------------------------------------------------

    public void indexing(TurElasticsearchInstance instance, String siteName,
            Map<String, Object> attributes) {
        log.debug("Elasticsearch indexing site={} index={}", siteName, instance.getIndex());
        String docId = Optional.ofNullable(attributes.get(TurSNFieldName.ID))
                .map(Object::toString)
                .orElse(null);
        if (docId == null) {
            log.warn("Skipping Elasticsearch document with no id for site={}", siteName);
            return;
        }
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(r -> r
                    .index(instance.getIndex())
                    .id(docId)
                    .document(attributes));
            var response = instance.getClient().index(request);
            log.debug("Elasticsearch indexed doc id={} index={} result={}", docId,
                    instance.getIndex(), response.result());
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            log.error("Elasticsearch server error indexing doc id={} index={}: {}",
                    docId, instance.getIndex(), e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error indexing doc id={} index={}: {}",
                    docId, instance.getIndex(), e.getMessage(), e);
        }
    }

    public long getDocumentTotal(TurElasticsearchInstance instance) {
        try {
            return instance.getClient().count(c -> c.index(instance.getIndex())).count();
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (INDEX_NOT_FOUND_EXCEPTION.equals(e.error().type())) {
                log.debug("Index '{}' not found, returning 0 documents", instance.getIndex());
            } else {
                log.error("Error getting document count from Elasticsearch index '{}'", instance.getIndex(), e);
            }
            return 0L;
        } catch (IOException e) {
            log.error("Error getting document count from Elasticsearch index '{}'", instance.getIndex(), e);
            return 0L;
        }
    }

    /**
     * T388 — counts documents that populate {@code fieldName} via an
     * Elasticsearch {@code exists} query. Returns {@code -1} on error so
     * coverage callers can treat it as "unknown".
     */
    public long getDocumentTotalWithField(TurElasticsearchInstance instance, String fieldName) {
        try {
            return instance.getClient().count(c -> c.index(instance.getIndex())
                    .query(q -> q.exists(e -> e.field(fieldName)))).count();
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (INDEX_NOT_FOUND_EXCEPTION.equals(e.error().type())) {
                log.debug("Index '{}' not found, returning -1 for field '{}'",
                        instance.getIndex(), fieldName);
            } else {
                log.error("Error counting field '{}' in Elasticsearch index '{}'",
                        fieldName, instance.getIndex(), e);
            }
            return -1L;
        } catch (IOException e) {
            log.error("Error counting field '{}' in Elasticsearch index '{}'",
                    fieldName, instance.getIndex(), e);
            return -1L;
        }
    }

    /**
     * T472 — counts documents whose numeric {@code fieldName} value is strictly
     * below {@code threshold} via an Elasticsearch {@code range lt} query.
     * Returns {@code -1} on error so coverage callers can treat it as "unknown".
     */
    public long getDocumentTotalWithFieldBelow(TurElasticsearchInstance instance, String fieldName,
            int threshold) {
        try {
            return instance.getClient().count(c -> c.index(instance.getIndex())
                    .query(q -> q.range(r -> r.untyped(u -> u.field(fieldName)
                            .lt(co.elastic.clients.json.JsonData.of(threshold)))))).count();
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (INDEX_NOT_FOUND_EXCEPTION.equals(e.error().type())) {
                log.debug("Index '{}' not found, returning -1 for field '{}' range",
                        instance.getIndex(), fieldName);
            } else {
                log.error("Error range-counting field '{}' in Elasticsearch index '{}'",
                        fieldName, instance.getIndex(), e);
            }
            return -1L;
        } catch (IOException e) {
            log.error("Error range-counting field '{}' in Elasticsearch index '{}'",
                    fieldName, instance.getIndex(), e);
            return -1L;
        }
    }

    public void deIndexing(TurElasticsearchInstance instance, String id) {
        log.debug("Elasticsearch deIndexing id={}", id);
        try {
            instance.getClient().delete(d -> d.index(instance.getIndex()).id(id));
        } catch (IOException e) {
            log.error("Error deIndexing id={} from Elasticsearch", id, e);
        }
    }

    public void deIndexingByType(TurElasticsearchInstance instance, String type) {
        log.debug("Elasticsearch deIndexing type={}", type);
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(r -> r
                    .index(instance.getIndex())
                    .query(q -> q.term(t -> t.field(TurSNFieldName.TYPE).value(type))));
            instance.getClient().deleteByQuery(request);
        } catch (IOException e) {
            log.error("Error deIndexing type={} from Elasticsearch", type, e);
        }
    }

    /** T1112 — deletes every document whose fields hold all of {@code fields}' values. */
    public void deIndexingByFields(TurElasticsearchInstance instance, Map<String, String> fields) {
        log.debug("Elasticsearch deIndexing fields={}", fields);
        List<Query> terms = fields.entrySet().stream()
                .map(field -> Query.of(q -> q.term(t -> t.field(field.getKey()).value(field.getValue()))))
                .toList();
        try {
            instance.getClient().deleteByQuery(DeleteByQueryRequest.of(r -> r
                    .index(instance.getIndex())
                    .query(q -> q.bool(b -> b.filter(terms)))));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Error deIndexing " + fields + " from Elasticsearch", e);
        }
    }

    /**
     * The request's text query, narrowed by (T1106) its facet filters and (T1103)
     * semantic recall's ids when it carries them. With neither, the text query
     * goes out as it always did.
     */
    private Query buildQuery(TurSESiteSettings turSNSite, Map<String, TurSESearchField> fields,
            TurSEParameters turSEParameters) {
        Query query = withRankingExpressions(turSNSite.id(),
                withTitlePhraseBoost(turSNSite, fields, turSEParameters.getQuery(),
                        buildTextQuery(turSEParameters)));
        List<Query> filters = filters(turSNSite, fields, turSEParameters);
        if (filters.isEmpty()) {
            return query;
        }
        return Query.of(q -> q.bool(b -> b.must(query).filter(filters)));
    }

    /**
     * The filters a search applies beside its text query: (T1106) its facet
     * selections, (T1103) semantic recall's ids and (T1133) the key's partition.
     * T1132: public so a query that is not a search, such as similar documents,
     * filters the same way.
     */
    public List<Query> filters(String siteName, TurSEParameters turSEParameters) {
        return siteSearchConfig.siteSettings(siteName)
                .map(site -> filters(site, enabledFieldsByName(site.id()), turSEParameters))
                .orElseGet(List::of);
    }

    private List<Query> filters(TurSESiteSettings turSNSite, Map<String, TurSESearchField> fields,
            TurSEParameters turSEParameters) {
        List<Query> filters = new ArrayList<>();
        TurElasticsearchFilterQueries.build(turSNSite.facetItemType(), turSEParameters.getTurSNFilterParams(),
                fields,
                (facet, label) -> customFacets.resolveFilter(turSNSite.id(), facet, label),
                Instant.now())
                .ifPresent(filters::add);
        List<String> restrictIds = turSEParameters.getRestrictIds();
        if (restrictIds != null && !restrictIds.isEmpty()) {
            List<co.elastic.clients.elasticsearch._types.FieldValue> ids = restrictIds.stream()
                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList();
            filters.add(Query.of(f -> f.terms(t -> t.field(TurSNFieldName.ID).terms(tt -> tt.value(ids)))));
        }
        // T1133 — the partition the request's API key is bound to, read by the
        // field's type like a facet selection, but never shown as one.
        if (turSEParameters.getPartition() != null) {
            TurElasticsearchFilterQueries.build(turSNSite.facetItemType(),
                    com.viglet.turing.commons.sn.bean.TurSNFilterParams.builder()
                            .defaultValues(List.of(turSEParameters.getPartition().filterQuery())).build(),
                    fields, (facet, label) -> Optional.empty(), Instant.now())
                    .ifPresent(filters::add);
        }
        return filters;
    }

    private Map<String, TurSESearchField> enabledFieldsByName(String siteId) {
        Map<String, TurSESearchField> byName = new java.util.LinkedHashMap<>();
        siteSearchConfig.enabledFields(siteId).forEach(field -> {
            if (field.name() != null) {
                byName.putIfAbsent(field.name(), field);
            }
        });
        return byName;
    }

    /**
     * The site's enabled facet fields in facet-position order, each with its type
     * from the enabled fields (a terms aggregation on a text field needs its
     * {@code .keyword} sub-field).
     */
    private List<TurSESearchField> facetFields(String siteId, Map<String, TurSESearchField> fields) {
        return siteSearchConfig.facetFields(siteId).stream()
                .map(facet -> fields.getOrDefault(facet.name(), new TurSESearchField(facet.name(), null, 1, null,
                        facet.facetPosition(), facet.facetItemType(), null)))
                .toList();
    }

    /** T1185 — what a title holding the query as a phrase adds, as on Lucene (T1177). */
    static final float TITLE_PHRASE_BOOST = 10f;

    /**
     * T1185 — Lucene's title-phrase boost (T1177) on Elasticsearch: a
     * {@code match_phrase} of the query as typed on the site's title field,
     * boosted beside the text query, so a guide titled with the exact phrase
     * outranks documents matching only a synonym or its words apart. A boost only,
     * for a plain phrase ({@link TurSNQueryRelaxer#isPlainPhrase}) on a TEXT title.
     */
    Query withTitlePhraseBoost(TurSESiteSettings turSNSite, Map<String, TurSESearchField> fields,
            String queryString, Query text) {
        if (!TurSNQueryRelaxer.isPlainPhrase(queryString)) {
            return text;
        }
        String titleField = StringUtils.hasText(turSNSite.defaultTitleField())
                ? turSNSite.defaultTitleField()
                : TurSNFieldName.TITLE;
        TurSESearchField ext = fields.get(titleField);
        if (ext == null || ext.type() != TurSEFieldType.TEXT) {
            return text;
        }
        String phrase = queryString.trim();
        return Query.of(q -> q.bool(b -> b.must(text)
                .should(s -> s.matchPhrase(m -> m.field(titleField).query(phrase).boost(TITLE_PHRASE_BOOST)))));
    }

    /**
     * T1222 — the site's ranking expressions as {@code should} boosts beside the
     * text query, as Lucene adds them (T1064) and Solr sends its {@code bq}: they
     * raise the score of matching documents and never change which match.
     */
    Query withRankingExpressions(String siteId, Query text) {
        List<Query> boosts = rankingExpressions == null ? List.of() : rankingExpressions.boostQueries(siteId);
        if (boosts.isEmpty()) {
            return text;
        }
        return Query.of(q -> q.bool(b -> b.must(text).should(boosts)));
    }

    private Query buildTextQuery(TurSEParameters turSEParameters) {
        String queryString = turSEParameters.getQuery();
        if (!StringUtils.hasText(queryString) || "*".equals(queryString.trim())) {
            // Match all query
            return Query.of(q -> q.matchAll(m -> m));
        }

        // simple_query_string with lenient=true skips fields whose type cannot
        // parse the query text (e.g. float, double, date fields)
        if (turSEParameters.isRelaxedMatch()) {
            // T1176 — the relaxed retry ORs the words, half of them required.
            return Query.of(q -> q.simpleQueryString(qs -> qs
                    .query(queryString)
                    .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                    .minimumShouldMatch("50%")
                    .lenient(true)));
        }
        return Query.of(q -> q.simpleQueryString(qs -> qs
                .query(queryString)
                .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                .lenient(true)));
    }

    private int getStartPosition(TurSEParameters turSEParameters) {
        int currentPage = turSEParameters.getCurrentPage() > 0 ? turSEParameters.getCurrentPage() : 1;
        int rows = turSEParameters.getRows() > 0 ? turSEParameters.getRows() : 10;
        return (currentPage - 1) * rows;
    }

    /** T1232 - the name a custom facet's {@code filters} aggregation is requested under. */
    static final String CUSTOM_FACET_AGG_PREFIX = "custom_facet_";

    /** The site's custom facets ({@code onlyFacet} alone when set), when the site has facets on. */
    private List<TurLuceneCustomFacets.Facet> customFacetsOf(TurSESiteSettings turSNSite, java.util.Locale locale,
            String onlyFacet) {
        if (customFacets == null || !TurSEFlags.isTrue(turSNSite.facet())) {
            return List.of();
        }
        return customFacets.facets(turSNSite.id(), locale).stream()
                .filter(facet -> onlyFacet == null || onlyFacet.equals(facet.name()))
                .toList();
    }

    /**
     * T1232 - one {@code filters} aggregation per custom facet, a bucket per
     * item, each built by the query its selection filters with, so a count and a
     * click always agree. An item whose bounds cannot be read is left out.
     */
    static void addCustomFacetAggregations(SearchRequest.Builder searchBuilder,
            List<TurLuceneCustomFacets.Facet> facets, Instant now) {
        for (TurLuceneCustomFacets.Facet facet : facets) {
            Map<String, Query> keyed = new java.util.LinkedHashMap<>();
            for (TurLuceneCustomFacets.Item item : facet.items()) {
                try {
                    keyed.put(item.label(), TurElasticsearchFilterQueries.itemQuery(item.filter(), now));
                } catch (IllegalArgumentException e) {
                    log.warn("[Elasticsearch] custom facet item '{}' of '{}' not counted: {}", item.label(),
                            facet.name(), e.getMessage());
                }
            }
            if (!keyed.isEmpty()) {
                searchBuilder.aggregations(CUSTOM_FACET_AGG_PREFIX + facet.name(),
                        a -> a.filters(f -> f.filters(b -> b.keyed(keyed))));
            }
        }
    }

    /** T1232 - each custom facet's buckets, in item order, with the items' display labels. */
    static List<TurSEFacetResult> customFacetResults(
            Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations,
            List<TurLuceneCustomFacets.Facet> facets) {
        List<TurSEFacetResult> results = new ArrayList<>();
        if (aggregations == null) {
            return results;
        }
        for (TurLuceneCustomFacets.Facet facet : facets) {
            var agg = aggregations.get(CUSTOM_FACET_AGG_PREFIX + facet.name());
            if (agg == null || !agg.isFilters() || !agg.filters().buckets().isKeyed()) {
                continue;
            }
            var buckets = agg.filters().buckets().keyed();
            TurSEFacetResult result = new TurSEFacetResult();
            result.setFacet(facet.name());
            result.setFacetPosition(facet.position() != null ? facet.position() : 1);
            for (TurLuceneCustomFacets.Item item : facet.items()) {
                var bucket = buckets.get(item.label());
                if (bucket != null) {
                    result.add(item.label(), new TurSEFacetResultAttr(item.label(), (int) bucket.docCount(),
                            item.displayLabel()));
                }
            }
            if (!result.getTurSEFacetResultAttr().isEmpty()) {
                results.add(result);
            }
        }
        return results;
    }

    private TurSEResults customFacetDrillDown(TurElasticsearchInstance elasticsearchInstance, Query query,
            TurSEParameters turSEParameters, List<TurLuceneCustomFacets.Facet> facet) throws IOException {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(elasticsearchInstance.getIndex())
                .query(query)
                .size(0);
        addCustomFacetAggregations(searchBuilder, facet, Instant.now());
        SearchResponse<Map<String, Object>> response = elasticsearchInstance.getClient()
                .search(searchBuilder.build(), (Type) Map.class);
        return TurSEResults.builder()
                .numFound(0)
                .start(0)
                .limit(0)
                .pageCount(0)
                .currentPage(1)
                .results(Collections.emptyList())
                .qTime((int) response.took())
                .elapsedTime(response.took())
                .queryString(turSEParameters.getQuery())
                .sort(turSEParameters.getSort())
                .spellCheck(null)
                .similarResults(Collections.emptyList())
                .facetResults(customFacetResults(response.aggregations(), facet))
                .groups(Collections.emptyList())
                .build();
    }

    private void addFacetAggregations(SearchRequest.Builder searchBuilder,
            List<TurSESearchField> facetFields) {
        for (TurSESearchField facetField : facetFields) {
            String fieldName = facetField.name();
            String aggField = getFacetAggField(facetField);
            searchBuilder.aggregations(FACET_AGG_PREFIX + fieldName,
                    a -> a.terms(t -> t.field(aggField).size(100)));
        }
    }

    /**
     * T1106 — the request's sort as the other engines read it: {@code newest} /
     * {@code oldest} on the site's default date field, {@code field:dir} (or the
     * older {@code field dir}), or the name of one of the site's custom sorts,
     * applied level by level with each level's missing placement.
     */
    private void addOptionalSorting(SearchRequest.Builder searchBuilder, TurSESiteSettings turSNSite,
            Map<String, TurSESearchField> fields, TurSEParameters turSEParameters) {
        String sort = turSEParameters.getSort();
        if (!StringUtils.hasText(sort) || "relevance".equalsIgnoreCase(sort)) {
            return;
        }
        if ("newest".equalsIgnoreCase(sort) || "oldest".equalsIgnoreCase(sort)) {
            String dateField = turSNSite.defaultDateField();
            if (StringUtils.hasText(dateField)) {
                searchBuilder.sort(sortOption(dateField, fieldType(fields, dateField),
                        "newest".equalsIgnoreCase(sort) ? SortOrder.Desc : SortOrder.Asc, null));
            }
            return;
        }
        Optional<org.apache.commons.collections4.KeyValue<String, String>> kv =
                com.viglet.turing.commons.utils.TurCommonsUtils.getKeyValueFromColon(sort);
        if (kv.isPresent()) {
            String field = kv.get().getKey();
            searchBuilder.sort(sortOption(field, fieldType(fields, field),
                    "desc".equalsIgnoreCase(kv.get().getValue()) ? SortOrder.Desc : SortOrder.Asc, null));
            return;
        }
        Optional<java.util.List<TurSECustomSortItem>> customSort = siteSearchConfig.customSort(turSNSite.id(),
                sort);
        if (customSort.isPresent()) {
            customSort.get().forEach(item -> searchBuilder.sort(sortOption(item.fieldName(),
                    fieldType(fields, item.fieldName()),
                    item.descending() ? SortOrder.Desc : SortOrder.Asc,
                    item.missing())));
            return;
        }
        addSorting(searchBuilder, sort);
    }

    private static TurSEFieldType fieldType(Map<String, TurSESearchField> fields, String name) {
        TurSESearchField ext = fields.get(name);
        return ext == null ? null : ext.type();
    }

    /**
     * One sort clause. A string-like field sorts on its {@code .keyword}
     * sub-field (a text field cannot be sorted); {@code missing} becomes
     * {@code _first}/{@code _last}, and {@code null} keeps Elasticsearch's own
     * placement.
     */
    static co.elastic.clients.elasticsearch._types.SortOptions sortOption(String field, TurSEFieldType type,
            SortOrder order, TurSNSiteCustomSortMissingEnum missing) {
        String target = TurElasticsearchFilterQueries.isKeyword(type) && type != null ? field + ".keyword" : field;
        return co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s.field(f -> {
            f.field(target).order(order);
            if (missing != null) {
                f.missing(missing == TurSNSiteCustomSortMissingEnum.LAST ? "_last" : "_first");
            }
            return f;
        }));
    }

    private void addSorting(SearchRequest.Builder searchBuilder, String sortString) {
        String[] sortParts = sortString.split("\\s+");
        if (sortParts.length > 0) {
            String field = sortParts[0];
            SortOrder order = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                    ? SortOrder.Desc
                    : SortOrder.Asc;

            searchBuilder.sort(s -> s.field(f -> f.field(field).order(order)));
        }
    }

    private void handleElasticsearchException(co.elastic.clients.elasticsearch._types.ElasticsearchException e,
            String index) {
        if (INDEX_NOT_FOUND_EXCEPTION.equals(e.error().type())) {
            log.debug("Index '{}' not found, returning empty results", index);
        } else {
            var err = e.error();
            log.error("ES search failed: type={} reason={} causedBy={}",
                    err.type(), err.reason(),
                    Optional.ofNullable(err.causedBy()).map(c -> c.reason()).orElse("none"));
            if (err.rootCause() != null && !err.rootCause().isEmpty()) {
                err.rootCause().forEach(
                        rc -> log.error("  root cause: type={} reason={}", rc.type(), rc.reason()));
            }
        }
    }

    private TurSEResults buildTurSEResults(@UnknownNullability SearchResponse<Map<String, Object>> response,
            TurSEParameters turSEParameters, List<TurSESearchField> facetFields) {
        var total = response.hits().total();
        long numFound = total != null ? total.value() : 0L;
        int rows = turSEParameters.getRows() > 0 ? turSEParameters.getRows() : 10;
        int pageCount = (int) Math.ceil((double) numFound / rows);

        List<TurSEResult> results = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(source -> TurSEResult.builder().fields(source).build())
                .toList();

        List<TurSEFacetResult> facetResults = buildFacetResults(response, facetFields);
        log.debug("ES search returned numFound={} facetResults={}", numFound, facetResults.size());

        return TurSEResults.builder()
                .numFound(numFound)
                .start(getStartPosition(turSEParameters))
                .limit(rows)
                .pageCount(pageCount)
                .currentPage(turSEParameters.getCurrentPage())
                .results(results)
                .qTime((int) response.took())
                .elapsedTime(response.took())
                .queryString(turSEParameters.getQuery())
                .sort(turSEParameters.getSort())
                .spellCheck(null)
                .similarResults(Collections.emptyList())
                .facetResults(facetResults)
                .groups(Collections.emptyList())
                .build();
    }

    /**
     * Returns the ES field name to use for a terms aggregation.
     * String-like types (TEXT, STRING, ARRAY, BOOL) are dynamically mapped by ES as
     * "text"
     * with a ".keyword" sub-field. Terms aggregations must use the ".keyword"
     * sub-field.
     * Numeric types (INT, LONG, FLOAT, DOUBLE, CURRENCY) and DATE are mapped
     * natively
     * and can be aggregated directly.
     */
    private static String getFacetAggField(TurSESearchField facetField) {
        // TEXT, STRING and ARRAY are all stored as "text" in ES (either via dynamic
        // mapping
        // on existing indexes, or explicit mapping with "keyword" analyzer for
        // STRING/ARRAY).
        // Both have a ".keyword" sub-field that must be used for terms aggregations.
        TurSEFieldType type = facetField.type();
        if (type == TurSEFieldType.TEXT || type == TurSEFieldType.STRING
                || type == TurSEFieldType.ARRAY || type == TurSEFieldType.CURRENCY) {
            return facetField.name() + ".keyword";
        }
        return facetField.name();
    }

    private List<TurSEFacetResult> buildFacetResults(SearchResponse<Map<String, Object>> response,
            List<TurSESearchField> facetFields) {
        List<TurSEFacetResult> facetResults = new ArrayList<>();
        Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = response
                .aggregations();
        if (aggregations == null || facetFields.isEmpty()) {
            return facetResults;
        }
        for (TurSESearchField facetField : facetFields) {
            String fieldName = facetField.name();
            String aggName = FACET_AGG_PREFIX + fieldName;
            var agg = aggregations.get(aggName);
            if (agg == null) {
                log.debug("ES facet agg '{}' not found in response", aggName);
                continue;
            }
            TurSEFacetResult facetResult = createFacetResult(facetField, fieldName, agg);
            facetResults.add(facetResult);
        }
        return facetResults;
    }

    private TurSEFacetResult createFacetResult(TurSESearchField facetField, String fieldName,
            co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg) {
        TurSEFacetResult facetResult = new TurSEFacetResult();
        facetResult.setFacet(fieldName);
        facetResult.setFacetPosition(facetField.facetPosition() != null
                ? facetField.facetPosition()
                : 1);

        if (agg.isSterms()) {
            populateStringTermsBuckets(facetResult, fieldName, agg);
        } else if (agg.isDterms()) {
            populateDoubleTermsBuckets(facetResult, fieldName, agg);
        } else if (agg.isLterms()) {
            populateLongTermsBuckets(facetResult, fieldName, agg);
        } else {
            log.debug("ES facet field={} unhandled agg kind={}", fieldName, agg._kind());
        }
        return facetResult;
    }

    private void populateStringTermsBuckets(TurSEFacetResult facetResult, String fieldName,
            co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg) {
        List<StringTermsBucket> buckets = agg.sterms().buckets().array();
        log.debug("ES facet field={} buckets={}", fieldName, buckets.size());
        for (StringTermsBucket bucket : buckets) {
            String key = bucket.key().stringValue();
            facetResult.add(key, new TurSEFacetResultAttr(key, (int) bucket.docCount()));
        }
    }

    private void populateDoubleTermsBuckets(TurSEFacetResult facetResult, String fieldName,
            co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg) {
        List<DoubleTermsBucket> buckets = agg.dterms().buckets().array();
        log.debug("ES facet field={} dterms buckets={}", fieldName, buckets.size());
        for (DoubleTermsBucket bucket : buckets) {
            String key = bucket.keyAsString() != null
                    ? bucket.keyAsString()
                    : String.valueOf(bucket.key());
            facetResult.add(key, new TurSEFacetResultAttr(key, (int) bucket.docCount()));
        }
    }

    private void populateLongTermsBuckets(TurSEFacetResult facetResult, String fieldName,
            co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg) {
        List<LongTermsBucket> buckets = agg.lterms().buckets().array();
        log.debug("ES facet field={} lterms buckets={}", fieldName, buckets.size());
        for (LongTermsBucket bucket : buckets) {
            String key = bucket.keyAsString() != null
                    ? bucket.keyAsString()
                    : String.valueOf(bucket.key());
            facetResult.add(key, new TurSEFacetResultAttr(key, (int) bucket.docCount()));
        }
    }
}
