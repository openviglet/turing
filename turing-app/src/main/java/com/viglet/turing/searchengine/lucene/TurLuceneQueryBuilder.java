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

import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;
import com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum;
import com.viglet.turing.searchengine.TurSEFlags;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.spi.TurSESiteCore;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;
import com.viglet.turing.searchengine.facet.TurSNFilterExclusion;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds Lucene {@link Query} objects from {@link TurSNSiteSearchContext} and
 * {@link TurSEParameters}, including filter queries (for facet selections) and
 * returns the list of facet-enabled fields.
 *
 * @author Alexandre Oliveira
 * @since 2026.1
 */
@Slf4j
@Component
public class TurLuceneQueryBuilder {

    private static final Set<TurSEFieldType> TEXT_FIELD_TYPES = EnumSet.of(TurSEFieldType.TEXT, TurSEFieldType.STRING,
            TurSEFieldType.ARRAY);

    private final TurSESiteSearchConfig siteSearchConfig;
    private final TurLuceneSynonymRegistry synonymRegistry;
    private final TurLuceneRankingExpressions rankingExpressions;
    private final TurLuceneCustomFacets customFacets;

    public TurLuceneQueryBuilder(TurSESiteSearchConfig siteSearchConfig,
            TurLuceneSynonymRegistry synonymRegistry,
            TurLuceneRankingExpressions rankingExpressions) {
        this(siteSearchConfig, synonymRegistry, rankingExpressions, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TurLuceneQueryBuilder(TurSESiteSearchConfig siteSearchConfig,
            TurLuceneSynonymRegistry synonymRegistry,
            TurLuceneRankingExpressions rankingExpressions,
            TurLuceneCustomFacets customFacets) {
        this.siteSearchConfig = siteSearchConfig;
        this.synonymRegistry = synonymRegistry;
        this.rankingExpressions = rankingExpressions;
        this.customFacets = customFacets;
    }

    // -------------------------------------------------------------------------
    // Main query
    // -------------------------------------------------------------------------

    /**
     * Builds the primary Lucene {@link Query} from the search context.
     * Includes the user's query string and any active filter queries.
     */
    public Query buildQuery(TurSESiteSettings site, TurSEParameters params) {
        return buildQuery(site, params, null);
    }

    /**
     * T1310 — as {@link #buildQuery(TurSESiteSettings, TurSEParameters)} for the
     * site called {@code siteName}; {@code null} when there is no such site.
     */
    public Query buildQueryForSite(String siteName, TurSEParameters params) {
        return siteSearchConfig.siteSettings(siteName).map(site -> buildQuery(site, params)).orElse(null);
    }

    /**
     * T665 — same as {@link #buildQuery(TurSESiteSettings, TurSEParameters)} but with the
     * search {@code locale}, so the main query is analyzed with the site's
     * query-time synonym map (when one is registered for that locale's core).
     */
    public Query buildQuery(TurSESiteSettings site, TurSEParameters params, Locale locale) {
        return buildQuery(site, params, locale, true);
    }

    /**
     * T1304 — as {@link #buildQuery(TurSESiteSettings, TurSEParameters, Locale)}, with the
     * site's synonyms left out when {@code synonyms} is {@code false}: the retry for
     * a query whose expansion went past the clause limit.
     */
    public Query buildQuery(TurSESiteSettings site, TurSEParameters params, Locale locale, boolean synonyms) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // Main user query
        Query mainQuery = buildMainQuery(params.getQuery(), site, locale, params.isRelaxedMatch(), synonyms);
        builder.add(mainQuery, BooleanClause.Occur.MUST);
        // T1177 — the title holding the query as typed, as a phrase, ranks first.
        titlePhraseBoost(params.getQuery(), site, locale)
                .ifPresent(boost -> builder.add(boost, BooleanClause.Occur.SHOULD));

        // Filter queries (facet selections, fqAnd/fqOr and their operators)
        TurSNFilterParams filterParams = params.getTurSNFilterParams();
        if (filterParams != null && hasFilterQueries(filterParams)) {
            addFilterQueries(builder, site, filterParams);
        }

        // T1103 — semantic recall's id restriction, apart from the facet filters.
        if (params.getRestrictIds() != null && !params.getRestrictIds().isEmpty()) {
            builder.add(new TermInSetQuery(com.viglet.turing.commons.sn.field.TurSNFieldName.ID,
                    params.getRestrictIds().stream().map(BytesRef::new).toList()),
                    BooleanClause.Occur.FILTER);
        }

        // T1133 — the partition the request's API key is bound to, read by the
        // field's type like a facet selection, but never shown as one.
        if (params.getPartition() != null) {
            String field = params.getPartition().field();
            TurSESearchField ext = enabledFieldsByName(site).get(field);
            builder.add(TurLuceneFilterQueries.build(field, params.getPartition().value(),
                    ext != null ? ext.type() : null, Instant.now()), BooleanClause.Occur.FILTER);
        }

        // T1064 — ranking expressions raise the score of the documents they
        // describe (SHOULD beside the MUST main query), as Solr's bq does.
        if (rankingExpressions != null) {
            rankingExpressions.boostQueries(site.id())
                    .forEach(boost -> builder.add(boost, BooleanClause.Occur.SHOULD));
        }

        return builder.build();
    }

    /**
     * Adds the active facet selections as filter clauses, following standard
     * faceted-navigation semantics:
     * <ul>
     *   <li>selections are grouped by field;</li>
     *   <li><b>within a field</b>, multiple values union (<b>OR</b>) when the
     *       field is multi-select ({@code facetItemType == OR}), otherwise they
     *       intersect (<b>AND</b>);</li>
     *   <li><b>across fields</b>, each field group intersects (<b>AND</b>) — a
     *       selection in one field narrows the result set of another.</li>
     * </ul>
     * The previous implementation added every value as its own {@code FILTER}
     * (AND) clause, so selecting two values of the same multi-select facet (e.g.
     * two mutually-exclusive categories) yielded zero hits even though the facet
     * counts promised a union. Cross-field union from a field's or the site's
     * configured {@code facetType == OR} is intentionally not honoured: it
     * broadens instead of narrowing and breaks the expected "pick another field
     * to drill down" behaviour.
     *
     * <p>T1077 — what the <em>request</em> asks for is honoured, as Solr does:
     * {@code fqItemOperator} sets how the values of one field combine,
     * {@code fqOperator} sets how the plain {@code fq} fields combine, and
     * {@code fqAnd}/{@code fqOr} are fields that intersect/union whatever the
     * operator. The intersected groups are then OR'd with the union groups,
     * which is the shape {@code TurSolrQueryBuilder#setFilterQueryString} builds.
     */
    private void addFilterQueries(BooleanQuery.Builder builder, TurSESiteSettings site,
            TurSNFilterParams filterParams) {
        TurSNSiteFacetFieldEnum defaultFacetType = filterParams.getOperator() == TurSNFilterQueryOperator.OR ? TurSNSiteFacetFieldEnum.OR : TurSNSiteFacetFieldEnum.AND;
        Map<String, FilterGroup> groups = new LinkedHashMap<>();
        collectFilterGroups(groups, filterParams.getDefaultValues(), defaultFacetType);
        collectFilterGroups(groups, filterParams.getAnd(), TurSNSiteFacetFieldEnum.AND);
        collectFilterGroups(groups, filterParams.getOr(), TurSNSiteFacetFieldEnum.OR);
        Map<String, TurSESearchField> fieldsByName = enabledFieldsByName(site);
        Instant now = Instant.now();
        // T1233 — an exclusion always narrows, whatever fqOr or fqItemOperator say.
        TurSNFilterExclusion.in(filterParams.getDefaultValues(), filterParams.getAnd(), filterParams.getOr())
                .forEach(exclusion -> {
                    TurSESearchField ext = fieldsByName.get(exclusion.field());
                    builder.add(valueQuery(site, exclusion.field(), ext, exclusion.value(),
                            ext != null ? ext.type() : null, now), BooleanClause.Occur.MUST_NOT);
                });
        if (groups.isEmpty()) {
            return;
        }

        List<Query> intersected = new ArrayList<>();
        List<Query> united = new ArrayList<>();
        groups.values().forEach(group -> {
            TurSESearchField ext = fieldsByName.get(group.field());
            TurSNSiteFacetFieldEnum itemType = requestedItemType(filterParams.getItemOperator())
                    .orElseGet(() -> effectiveFacetItemType(ext, site));
            TurSEFieldType type = ext != null ? ext.type() : null;
            Query query = buildFieldFilterQuery(group.values(),
                    value -> valueQuery(site, group.field(), ext, value, type, now), itemType);
            (group.facetType() == TurSNSiteFacetFieldEnum.OR ? united : intersected).add(query);
        });

        if (united.isEmpty()) {
            intersected.forEach(query -> builder.add(query, BooleanClause.Occur.FILTER));
            return;
        }
        BooleanQuery.Builder disjunction = new BooleanQuery.Builder().setMinimumNumberShouldMatch(1);
        if (!intersected.isEmpty()) {
            BooleanQuery.Builder conjunction = new BooleanQuery.Builder();
            intersected.forEach(query -> conjunction.add(query, BooleanClause.Occur.MUST));
            disjunction.add(conjunction.build(), BooleanClause.Occur.SHOULD);
        }
        united.forEach(query -> disjunction.add(query, BooleanClause.Occur.SHOULD));
        builder.add(disjunction.build(), BooleanClause.Occur.FILTER);
    }

    /**
     * T1083 — a selection on a field the site has no field for may be a custom
     * facet's {@code <facet>:<label>}: it stands for the item's range on the field
     * the custom facet belongs to, as on Solr.
     */
    private Query valueQuery(TurSESiteSettings site, String field, TurSESearchField ext, String value,
            TurSEFieldType type, Instant now) {
        if (ext == null && customFacets != null) {
            Optional<TurLuceneCustomFacets.ItemFilter> item = customFacets.resolveFilter(site.id(), field, value);
            if (item.isPresent()) {
                return item.get().toLuceneQuery(now);
            }
        }
        return TurLuceneFilterQueries.build(field, value, type, now);
    }

    /** One field's selected values and how the field combines with the others. */
    private record FilterGroup(String field, TurSNSiteFacetFieldEnum facetType, List<String> values) {
    }

    /** Groups by field; a field already grouped keeps the facet type it was first seen with. */
    private void collectFilterGroups(Map<String, FilterGroup> groups, List<String> filterQueries,
            TurSNSiteFacetFieldEnum facetType) {
        if (filterQueries == null) {
            return;
        }
        filterQueries.stream().filter(fq -> !TurSNFilterExclusion.isExclusion(fq))
                .forEach(fq -> parseFieldValue(fq).ifPresent(fv -> groups
                .computeIfAbsent(fv.field(), field -> new FilterGroup(field, facetType, new ArrayList<>()))
                .values().add(fv.value())));
    }

    private static boolean hasFilterQueries(TurSNFilterParams filterParams) {
        return !CollectionUtils.isEmpty(filterParams.getDefaultValues())
                || !CollectionUtils.isEmpty(filterParams.getAnd())
                || !CollectionUtils.isEmpty(filterParams.getOr());
    }

    private static Optional<TurSNSiteFacetFieldEnum> requestedItemType(TurSNFilterQueryOperator operator) {
        if (operator == TurSNFilterQueryOperator.OR) {
            return Optional.of(TurSNSiteFacetFieldEnum.OR);
        }
        if (operator == TurSNFilterQueryOperator.AND) {
            return Optional.of(TurSNSiteFacetFieldEnum.AND);
        }
        return Optional.empty();
    }

    /**
     * Combines the selected values of a single field into one query: OR
     * (SHOULD, min-should-match 1) for a multi-select facet, AND (MUST)
     * otherwise. Each value is read by the field's type (T1075), so a range or
     * a numeric/date value matches the points the field was indexed as.
     */
    private static Query buildFieldFilterQuery(List<String> values,
            java.util.function.Function<String, Query> valueQuery, TurSNSiteFacetFieldEnum itemType) {
        if (values.size() == 1) {
            return valueQuery.apply(values.getFirst());
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        BooleanClause.Occur occur = itemType == TurSNSiteFacetFieldEnum.OR
                ? BooleanClause.Occur.SHOULD
                : BooleanClause.Occur.MUST;
        values.forEach(value -> builder.add(valueQuery.apply(value), occur));
        if (occur == BooleanClause.Occur.SHOULD) {
            builder.setMinimumNumberShouldMatch(1);
        }
        return builder.build();
    }

    private List<TurSESearchField> enabledFields(TurSESiteSettings site) {
        return siteSearchConfig.enabledFields(site.id());
    }

    /** Enabled fields keyed by name, for resolving a selection's facet type. */
    private Map<String, TurSESearchField> enabledFieldsByName(TurSESiteSettings site) {
        Map<String, TurSESearchField> map = new LinkedHashMap<>();
        enabledFields(site)
                .forEach(ext -> {
                    if (ext.name() != null) {
                        map.putIfAbsent(ext.name(), ext);
                    }
                });
        return map;
    }

    /** Field-level {@code facetItemType} wins over the site default; DEFAULT/null → AND. */
    private static TurSNSiteFacetFieldEnum effectiveFacetItemType(TurSESearchField ext,
            TurSESiteSettings site) {
        TurSNSiteFacetFieldEnum fieldType = ext != null ? ext.facetItemType() : null;
        if (fieldType != null && fieldType != TurSNSiteFacetFieldEnum.DEFAULT) {
            return fieldType;
        }
        return normalize(site.facetItemType());
    }

    private static TurSNSiteFacetFieldEnum normalize(TurSNSiteFacetFieldEnum value) {
        return (value == null || value == TurSNSiteFacetFieldEnum.DEFAULT)
                ? TurSNSiteFacetFieldEnum.AND
                : value;
    }

    /**
     * Returns the list of enabled facet fields for the given site.
     */
    public List<TurSESearchField> getFacetFields(TurSESiteSettings site) {
        List<TurSESearchField> all = enabledFields(site);
        List<TurSESearchField> facets = all.stream()
                .filter(ext -> TurSEFlags.isTrue(ext.facet()))
                .sorted(java.util.Comparator.comparingInt(
                        ext -> ext.facetPosition() != null ? ext.facetPosition() : Integer.MAX_VALUE))
                .toList();
        log.debug("[Lucene] site='{}' totalEnabledFields={} facetFields={} names={}",
                site.name(), all.size(), facets.size(),
                facets.stream().map(TurSESearchField::name).toList());
        return facets;
    }

    /** T1250 — the site's facet fields without those the request leaves out. */
    public List<TurSESearchField> getFacetFields(TurSESiteSettings site, TurSEParameters params) {
        List<TurSESearchField> facets = getFacetFields(site);
        if (params == null || params.getExcludedFacets() == null || params.getExcludedFacets().isEmpty()) {
            return facets;
        }
        return facets.stream().filter(ext -> !params.isFacetExcluded(ext.name())).toList();
    }

    /**
     * Returns facet fields limited to a single named facet (used by
     * {@code retrieveFacetResults}).
     */
    public List<TurSESearchField> getFacetFields(TurSESiteSettings site, String facetName) {
        return getFacetFields(site).stream()
                .filter(ext -> facetName.equals(ext.name()))
                .toList();
    }

    /**
     * Returns the list of enabled highlight fields for the given site.
     */
    public List<TurSESearchField> getHLFields(TurSESiteSettings site) {
        // T707 — never highlight identifier/URL fields even if a field is
        // (mis)configured with hl=1: a <mark> injected into `id` corrupts the
        // value clients feed back to /search/similar, silently breaking "Related".
        return enabledFields(site).stream()
                .filter(field -> TurSEFlags.isTrue(field.hl()))
                .filter(field -> !com.viglet.turing.commons.sn.field.TurSNFieldName
                        .isNonHighlightable(field.name()))
                .toList();
    }

    /**
     * Builds a Lucene {@link org.apache.lucene.search.Sort} from the search parameters,
     * mirroring the Solr sort logic (relevance, newest, oldest, field:asc/desc, custom sort).
     *
     * @return null for relevance (default score sort), or a Sort object
     */
    public org.apache.lucene.search.Sort buildSort(TurSESiteSettings site, TurSEParameters params) {
        String sortParam = params.getSort();
        if (sortParam == null || sortParam.isBlank() || sortParam.equalsIgnoreCase("relevance")) {
            return null; // default score sort
        }

        if (sortParam.equalsIgnoreCase(TurLuceneConstants.NEWEST)) {
            String dateField = site.defaultDateField();
            if (dateField != null) {
                return new org.apache.lucene.search.Sort(
                        new org.apache.lucene.search.SortField(dateField,
                                org.apache.lucene.search.SortField.Type.LONG, true));
            }
            return null;
        }

        if (sortParam.equalsIgnoreCase(TurLuceneConstants.OLDEST)) {
            String dateField = site.defaultDateField();
            if (dateField != null) {
                return new org.apache.lucene.search.Sort(
                        new org.apache.lucene.search.SortField(dateField,
                                org.apache.lucene.search.SortField.Type.LONG, false));
            }
            return null;
        }

        // field:asc or field:desc
        var kv = TurCommonsUtils.getKeyValueFromColon(sortParam);
        if (kv.isPresent()) {
            boolean reverse = kv.get().getValue().equalsIgnoreCase("desc");
            return new org.apache.lucene.search.Sort(
                    buildSortField(site, kv.get().getKey(), reverse));
        }

        // Custom sort name
        return siteSearchConfig.customSort(site.id(), sortParam)
                .map(sortedItems -> {
                    if (sortedItems.isEmpty()) return null;
                    org.apache.lucene.search.SortField[] fields = sortedItems.stream()
                            .map(item -> buildSortField(site, item.fieldName(), item.descending(), item.missing()))
                            .toArray(org.apache.lucene.search.SortField[]::new);
                    return new org.apache.lucene.search.Sort(fields);
                })
                .orElse(null);
    }

    private org.apache.lucene.search.SortField buildSortField(TurSESiteSettings site, String fieldName,
            boolean reverse) {
        return buildSortField(site, fieldName, reverse, null);
    }

    private org.apache.lucene.search.SortField buildSortField(TurSESiteSettings site, String fieldName,
            boolean reverse, TurSNSiteCustomSortMissingEnum missing) {
        // The SortField type follows the type the site gave the field; an unknown
        // field uses SortedSetSortField (works with SortedSetDocValuesField from addFacetField)
        return sortField(fieldName, siteSearchConfig.fieldTypes(site.id()).get(fieldName), reverse, missing);
    }

    /**
     * The sort field for a field of {@code type}. T1104 — {@code missing} places
     * documents with no value first or last. Lucene compares the missing value
     * before applying {@code reverse}, so "last" on a descending level is the
     * smallest value, not the largest; {@code null} keeps Lucene's own placement.
     */
    static org.apache.lucene.search.SortField sortField(String fieldName, TurSEFieldType type,
            boolean reverse, TurSNSiteCustomSortMissingEnum missing) {
        boolean naturalLast = (missing == TurSNSiteCustomSortMissingEnum.LAST) != reverse;
        org.apache.lucene.search.SortField.Type sortType = type == null ? null : switch (type) {
            case INT -> org.apache.lucene.search.SortField.Type.INT;
            case LONG, DATE -> org.apache.lucene.search.SortField.Type.LONG;
            case FLOAT -> org.apache.lucene.search.SortField.Type.FLOAT;
            case DOUBLE, CURRENCY -> org.apache.lucene.search.SortField.Type.DOUBLE;
            // TEXT, STRING, ARRAY, BOOL use SortedSetDocValues (multi-value safe)
            default -> null;
        };
        if (sortType == null) {
            return missing == null
                    ? new org.apache.lucene.search.SortedSetSortField(fieldName, reverse)
                    : new org.apache.lucene.search.SortedSetSortField(fieldName, reverse,
                            org.apache.lucene.search.SortedSetSelector.Type.MIN,
                            naturalLast ? org.apache.lucene.search.SortField.STRING_LAST
                                    : org.apache.lucene.search.SortField.STRING_FIRST);
        }
        if (missing == null) {
            return new org.apache.lucene.search.SortField(fieldName, sortType, reverse);
        }
        Object missingValue = switch (sortType) {
            case INT -> naturalLast ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            case LONG -> naturalLast ? Long.MAX_VALUE : Long.MIN_VALUE;
            case FLOAT -> naturalLast ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            default -> naturalLast ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        };
        return new org.apache.lucene.search.SortField(fieldName, sortType, reverse, missingValue);
    }

    /**
     * Builds an autocomplete query that searches across edge-ngram indexed
     * {@code *_ac} fields. The query is analyzed with {@link StandardAnalyzer}
     * (lowercase, tokenize) and matched against the ngram tokens in the index.
     */
    public Query buildAutoCompleteQuery(String siteId, String term) {
        // Map to _ac suffixed fields for ngram matching
        String[] acFields = java.util.Arrays.stream(resolveAutoCompleteFields(siteId))
                .map(f -> f + TurLuceneConstants.AC_SUFFIX)
                .toArray(String[]::new);
        try {
            // Use StandardAnalyzer for the query side (lowercase + tokenize, no ngrams)
            MultiFieldQueryParser parser = new MultiFieldQueryParser(acFields, new StandardAnalyzer());
            parser.setDefaultOperator(QueryParser.Operator.OR);
            return parser.parse(QueryParserBase.escape(term));
        } catch (ParseException e) {
            log.warn("Failed to parse autocomplete query '{}': {}", term, e.getMessage());
            return new TermQuery(new Term(
                    TurLuceneConstants.TITLE + TurLuceneConstants.AC_SUFFIX, term.toLowerCase()));
        }
    }

    /**
     * Builds a wildcard-appended version of the query (for no-results fallback).
     */
    public Query buildWildcardQuery(TurSESiteSettings site, TurSEParameters params) {
        String wildcardQuery = params.getQuery().trim() + "*";
        params.setQuery(wildcardQuery);
        return buildQuery(site, params);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private Query buildMainQuery(String queryStr, TurSESiteSettings site, Locale locale, boolean relaxedMatch,
            boolean synonyms) {
        if (!StringUtils.hasText(queryStr) || queryStr.equals("*") || queryStr.equals("*:*")) {
            return new FieldExistsQuery(TurLuceneConstants.ID);
        }

        String[] textFields = resolveTextFields(site);
        // T1242 — a query made only of stop words keeps them, or it would search for
        // nothing; so does a quoted phrase, which asks for its words as typed (and a
        // dropped word would leave a hole the synonym graph cannot carry).
        boolean keepStopWords = queryStr.indexOf('"') >= 0
                || com.viglet.turing.searchengine.TurSNQueryRelaxer.onlyStopWords(queryStr, locale);
        Analyzer analyzer = synonyms ? queryAnalyzer(site, locale, keepStopWords)
                : TurLuceneFoldingAnalyzer.forQuery(locale, keepStopWords);
        try {
            if (textFields.length == 0) {
                // Fallback: search the id field
                return phrasesOnPositionalFieldsOnly(new QueryParser(TurLuceneConstants.ID, analyzer).parse(
                        QueryParserBase.escape(queryStr)), Set.of());
            }
            // T1065 — a field's search weight multiplies its clause, so a title
            // match can outweigh the same word in body text.
            Map<String, Float> weights = searchWeights(site);
            MultiFieldQueryParser parser = weights.isEmpty()
                    ? new MultiFieldQueryParser(textFields, analyzer)
                    : new MultiFieldQueryParser(textFields, analyzer, weights);
            // T1176 — the relaxed retry ORs the words, half of them required.
            parser.setDefaultOperator(relaxedMatch ? QueryParser.Operator.OR : QueryParser.Operator.AND);
            // T1066 — a multi-word expansion is a phrase, not loose words.
            parser.setAutoGenerateMultiTermSynonymsPhraseQuery(true);
            Query termCentric = parser.parse(queryStr);
            if (relaxedMatch) {
                termCentric = withHalfTheWordsRequired(termCentric);
            }
            Query main = termCentric;
            if (analyzer instanceof TurLuceneSynonymAnalyzer) {
                // T1304 — synonyms only add: the words parsed without them stay a way in.
                // A multi-word key that fires turns the words typed into one path of the
                // graph, which Lucene builds as a phrase without the gaps the stop words
                // left, so the page holding the full name would stop matching it.
                MultiFieldQueryParser plainParser = weights.isEmpty()
                        ? new MultiFieldQueryParser(textFields, TurLuceneFoldingAnalyzer.forQuery(locale, keepStopWords))
                        : new MultiFieldQueryParser(textFields, TurLuceneFoldingAnalyzer.forQuery(locale, keepStopWords),
                                weights);
                plainParser.setDefaultOperator(parser.getDefaultOperator());
                Query plain = plainParser.parse(queryStr);
                if (relaxedMatch) {
                    plain = withHalfTheWordsRequired(plain);
                }
                main = new BooleanQuery.Builder()
                        .add(withMultiWordSynonyms(termCentric, queryStr, textFields, analyzer, weights),
                                BooleanClause.Occur.SHOULD)
                        .add(plain, BooleanClause.Occur.SHOULD)
                        .build();
            }
            return phrasesOnPositionalFieldsOnly(main, positionalFields(site));
        } catch (ParseException e) {
            log.warn("Failed to parse Lucene query '{}', falling back to MatchAllDocsQuery: {}", queryStr,
                    e.getMessage());
            return buildTermQueries(queryStr, textFields);
        }
    }

    /**
     * T1177 — how much a title holding the query as a phrase adds, against the
     * main query's own score: enough that a guide titled "How to choose a
     * waterproof jacket" outranks products that only repeat a synonym.
     */
    static final float TITLE_PHRASE_BOOST = 10f;

    /**
     * T1177 — Solr's {@code pf} for Lucene: the query as typed, as a phrase on the
     * site's title field, analysed without synonyms, so an exact title phrase
     * outranks a document that matches only one of its synonyms (the synonym
     * expansion, and since T1170 the stems, score those as high as the words
     * typed). A boost only, never a filter. Not for a single word, a query using
     * the engine's syntax, or a title field that holds no positions.
     */
    private Optional<Query> titlePhraseBoost(String queryStr, TurSESiteSettings site, Locale locale) {
        if (!com.viglet.turing.searchengine.TurSNQueryRelaxer.isPlainPhrase(queryStr)) {
            return Optional.empty();
        }
        String titleField = StringUtils.hasText(site.defaultTitleField())
                ? site.defaultTitleField()
                : TurLuceneConstants.TITLE;
        if (!positionalFields(site).contains(titleField)) {
            return Optional.empty();
        }
        try {
            // T1242 — stop words dropped as in the main query; their positions stay, so
            // the phrase still lines up with a title that holds them.
            Query phrase = new QueryParser(titleField, TurLuceneFoldingAnalyzer.forQuery(locale, false))
                    .parse("\"" + queryStr.trim() + "\"");
            return phrase instanceof PhraseQuery || phrase instanceof MultiPhraseQuery
                    ? Optional.of(new BoostQuery(phrase, TITLE_PHRASE_BOOST))
                    : Optional.empty();
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    /**
     * T1176 — an ORed parse with at least half its word clauses required
     * (rounded down), as Solr's and Elasticsearch's {@code 50%} minimum match
     * reads. Two words or fewer need any one of them.
     */
    static Query withHalfTheWordsRequired(Query query) {
        if (!(query instanceof BooleanQuery bq)) {
            return query;
        }
        long should = bq.clauses().stream().filter(c -> c.occur() == BooleanClause.Occur.SHOULD).count();
        int required = (int) (should / 2);
        if (required <= 1) {
            return query;
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder().setMinimumNumberShouldMatch(required);
        bq.clauses().forEach(builder::add);
        return builder.build();
    }

    /**
     * T665 — the query-time analyzer: the site's synonym-aware analyzer when a
     * {@link SynonymMap} is registered for the search locale's core, otherwise
     * the plain {@link StandardAnalyzer} (identical to pre-T665 behaviour). Zero
     * overhead for sites without synonyms — the registry short-circuits and no
     * locale/core lookup runs.
     */
    private Analyzer queryAnalyzer(TurSESiteSettings site, Locale locale, boolean keepStopWords) {
        // T1080 — both analyzers add the accent-folded form of each token, as the
        // index side does, so "saude" and "saúde" find each other. T1170 — and the
        // stem in the search locale's language, so "jackets" finds "jacket".
        // T1242 — and drop that language's stop words (the index keeps them).
        if (locale == null || synonymRegistry == null || synonymRegistry.isEmpty()) {
            return TurLuceneFoldingAnalyzer.forQuery(locale, keepStopWords);
        }
        String core = siteSearchConfig.siteCore(site.name(), locale)
                .map(TurSESiteCore::core)
                .orElse(null);
        SynonymMap map = core == null ? null : synonymRegistry.get(core);
        return map == null ? TurLuceneFoldingAnalyzer.forQuery(locale, keepStopWords)
                : new TurLuceneSynonymAnalyzer(map, locale, keepStopWords);
    }

    /**
     * T1066 — the classic parser splits the query on whitespace before analysis,
     * so the synonym filter only ever saw {@code home} and {@code office} apart
     * and a multi-word term such as "home office" never expanded.
     *
     * <p>The term-centric parse is kept as it is (each word may match in any
     * field) and ORed with a per-field parse that hands the filter the whole query
     * string, so a multi-word synonym expands within a field and a multi-word
     * expansion is searched as a phrase, not as loose words. Not splitting on the
     * multi-field parser instead would demand the phrase in every field at once.
     * Only queries analysed with a site's synonyms take this path.
     */
    private static Query withMultiWordSynonyms(Query termCentric, String queryStr, String[] fields,
            Analyzer analyzer, Map<String, Float> weights) {
        BooleanQuery.Builder perField = new BooleanQuery.Builder();
        for (String field : fields) {
            QueryParser parser = new QueryParser(field, analyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            parser.setSplitOnWhitespace(false);
            parser.setAutoGenerateMultiTermSynonymsPhraseQuery(true);
            try {
                Query query = parser.parse(queryStr);
                Float weight = weights.get(field);
                perField.add(weight != null ? new BoostQuery(query, weight) : query, BooleanClause.Occur.SHOULD);
            } catch (ParseException e) {
                return termCentric;
            }
        }
        return new BooleanQuery.Builder()
                .add(termCentric, BooleanClause.Occur.SHOULD)
                .add(perField.build(), BooleanClause.Occur.SHOULD)
                .build();
    }

    /**
     * T1091 — the fields indexed with positions, which are the only ones a phrase
     * can run on: TEXT fields are a {@code TextField}, while STRING/ARRAY fields
     * and the {@code id} (always, see {@code TurLuceneDocumentHandler}) are a
     * {@code StringField} with no position data.
     */
    private Set<String> positionalFields(TurSESiteSettings site) {
        Set<String> fields = new HashSet<>();
        enabledFields(site).forEach(ext -> {
            if (ext.type() == TurSEFieldType.TEXT && ext.name() != null
                    && !TurLuceneConstants.ID.equals(ext.name())) {
                fields.add(ext.name());
            }
        });
        return fields;
    }

    /**
     * T1091 — a phrase (a multi-word synonym expansion, or a quoted query) on a
     * field without positions throws at search time and fails the whole query.
     * On such a field the phrase becomes an exact term on the whole phrase, which
     * is how a keyword field is matched; every other clause is kept as parsed.
     */
    static Query phrasesOnPositionalFieldsOnly(Query query, Set<String> positional) {
        return switch (query) {
            case BooleanQuery bq -> {
                BooleanQuery.Builder builder = new BooleanQuery.Builder()
                        .setMinimumNumberShouldMatch(bq.getMinimumNumberShouldMatch());
                for (BooleanClause clause : bq.clauses()) {
                    builder.add(phrasesOnPositionalFieldsOnly(clause.query(), positional), clause.occur());
                }
                yield builder.build();
            }
            case BoostQuery boost -> new BoostQuery(
                    phrasesOnPositionalFieldsOnly(boost.getQuery(), positional), boost.getBoost());
            case DisjunctionMaxQuery dismax -> new DisjunctionMaxQuery(dismax.getDisjuncts().stream()
                    .map(q -> phrasesOnPositionalFieldsOnly(q, positional)).toList(),
                    dismax.getTieBreakerMultiplier());
            case PhraseQuery phrase when !positional.contains(phrase.getField()) -> new TermQuery(
                    new Term(phrase.getField(), Arrays.stream(phrase.getTerms())
                            .map(Term::text).collect(Collectors.joining(" "))));
            case MultiPhraseQuery multi when !positional.contains(field(multi)) -> new MatchNoDocsQuery(
                    "T1091: no positions on " + field(multi));
            default -> query;
        };
    }

    private static String field(MultiPhraseQuery multi) {
        Term[][] terms = multi.getTermArrays();
        return terms.length == 0 || terms[0].length == 0 ? null : terms[0][0].field();
    }

    /** Fallback: build OR of TermQuery across text fields. */
    private Query buildTermQueries(String queryStr, String[] fields) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String field : fields) {
            for (String token : queryStr.toLowerCase().split("\\s+")) {
                builder.add(new TermQuery(new Term(field, token)), BooleanClause.Occur.SHOULD);
            }
        }
        return builder.build();
    }

    /** Enabled fields whose search weight differs from 1, keyed by name. */
    private Map<String, Float> searchWeights(TurSESiteSettings site) {
        Map<String, Float> weights = new LinkedHashMap<>();
        enabledFields(site).forEach(ext -> {
            Float weight = ext.searchWeight();
            if (ext.name() != null && weight != null && weight > 0 && weight != 1f) {
                weights.putIfAbsent(ext.name(), weight);
            }
        });
        return weights;
    }

    /**
     * T1076 — the fields autocomplete matches and draws its suggestions from: the
     * site's enabled free-text ({@link TurSEFieldType#TEXT}) fields, never the
     * keyword ones (STRING/ARRAY hold slugs, codes and URLs nobody types) nor
     * {@code id}/{@code url}. Falls back to {@code title} when the site has none.
     */
    public String[] resolveAutoCompleteFields(String siteId) {
        String[] fields = siteSearchConfig.enabledFields(siteId)
                .stream()
                .filter(ext -> ext.type() == TurSEFieldType.TEXT && ext.name() != null)
                .map(TurSESearchField::name)
                .filter(name -> !TurLuceneConstants.ID.equals(name)
                        && !TurLuceneConstants.URL.equals(name))
                .distinct()
                .toArray(String[]::new);
        return fields.length > 0 ? fields : new String[] { TurLuceneConstants.TITLE };
    }

    private String[] resolveTextFields(TurSESiteSettings site) {
        return enabledFields(site)
                .stream()
                .filter(ext -> ext.type() != null && TEXT_FIELD_TYPES.contains(ext.type()))
                .map(TurSESearchField::name)
                .distinct()
                .toArray(String[]::new);
    }

    /** A parsed {@code field:value} filter-query selection. */
    private record FieldValue(String field, String value) {
    }

    /**
     * Parses a {@code field:value} filter query into its field and (unquoted)
     * value. Values keep their exact case — Lucene {@code StringField}s are not
     * analyzed, so facet term matching is exact.
     */
    private Optional<FieldValue> parseFieldValue(String fq) {
        if (!StringUtils.hasText(fq))
            return Optional.empty();
        int colonIndex = fq.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= fq.length() - 1) {
            log.warn("Invalid filter query format '{}' — expected 'field:value'", fq);
            return Optional.empty();
        }
        String field = fq.substring(0, colonIndex);
        String value = fq.substring(colonIndex + 1);
        // Strip surrounding quotes if present
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return Optional.of(new FieldValue(field, value));
    }
}
