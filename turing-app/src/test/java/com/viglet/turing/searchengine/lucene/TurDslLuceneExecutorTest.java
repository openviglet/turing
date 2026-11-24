/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.searchengine.lucene;

import com.viglet.turing.searchengine.dsl.TurDslQuery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.field.TurSEFieldType;

/**
 * Unit coverage for the type-aware range and sort translation in
 * {@link TurDslLuceneExecutor} (the fix surfaced by T407). Numeric / currency /
 * date fields are indexed as Lucene {@code *Point} + {@code NumericDocValues}, so
 * a range must become a point range (a lexicographic {@code TermRangeQuery} matches
 * nothing) and a sort must use a typed {@link SortField} (a
 * {@link SortedSetSortField} throws on a numeric-doc-values field). String/text and
 * unknown fields keep the lexicographic / sorted-set behavior.
 *
 * <p>The translation helpers don't touch the instance collaborators, so they're
 * exercised directly with a {@code null}-wired executor and an explicit field-type
 * map — no Lucene index, no Spring context.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
class TurDslLuceneExecutorTest {

    private final TurDslLuceneExecutor executor = new TurDslLuceneExecutor(null, null);

    // ─────────────── Range translation ───────────────

    @Test
    void doubleAndCurrencyRangesBecomePointRangesNotStringRanges() {
        var range = new TurDslQuery.Range("tuition", null, null, 20000, null);

        var asCurrency = executor.translateQuery(range, Map.of("tuition", TurSEFieldType.CURRENCY));
        var asDouble = executor.translateQuery(range, Map.of("tuition", TurSEFieldType.DOUBLE));

        assertThat(asCurrency).isNotInstanceOf(TermRangeQuery.class);
        assertThat(asDouble).isNotInstanceOf(TermRangeQuery.class);
        // PointRangeQuery#toString renders the field and the (inclusive) bounds.
        assertThat(asCurrency.toString()).contains("tuition").contains("20000.0");
    }

    @Test
    void intRangeHonorsExclusiveAndInclusiveBounds() {
        // gte 12, lte 24 (inclusive) vs gt 12, lt 24 (exclusive → 13..23).
        var inclusive = executor.translateQuery(
                new TurDslQuery.Range("durationMonths", 12, null, 24, null),
                Map.of("durationMonths", TurSEFieldType.INT));
        var exclusive = executor.translateQuery(
                new TurDslQuery.Range("durationMonths", null, 12, null, 24),
                Map.of("durationMonths", TurSEFieldType.INT));

        assertThat(inclusive).isNotInstanceOf(TermRangeQuery.class);
        assertThat(inclusive.toString()).contains("durationMonths").contains("12").contains("24");
        assertThat(exclusive.toString()).contains("13").contains("23");
    }

    @Test
    void stringAndUnknownRangesStayLexicographicTermRanges() {
        var range = new TurDslQuery.Range("degree", "a", null, "m", null);

        assertThat(executor.translateQuery(range, Map.of("degree", TurSEFieldType.STRING)))
                .isInstanceOf(TermRangeQuery.class);
        // No declared type → conservative string-range fallback (interface entry point).
        assertThat(executor.translateQuery(range, Map.of()))
                .isInstanceOf(TermRangeQuery.class);
        assertThat(executor.translateQuery(range)).isInstanceOf(TermRangeQuery.class);
    }

    // ─────────────── T1354: date bounds on a DATE field ───────────────

    @Test
    void aDateRangeReadsDayAndIsoBoundsAgainstEpochMillis() throws IOException {
        // A DATE field is indexed as epoch millis; the DSL bound was parsed as a long
        // ("2027-07-01" → NumberFormatException) before T1354.
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                for (String instant : List.of("2027-06-30T12:00:00Z", "2027-07-01T00:00:00Z",
                        "2027-07-31T18:00:00Z", "2027-08-01T00:00:00Z")) {
                    Document doc = new Document();
                    doc.add(new StringField("id", instant, Field.Store.YES));
                    doc.add(new org.apache.lucene.document.LongPoint("publication_date",
                            java.time.Instant.parse(instant).toEpochMilli()));
                    writer.addDocument(doc);
                }
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Map<String, TurSEFieldType> types = Map.of("publication_date", TurSEFieldType.DATE);

                // A day as lte keeps the whole day, as Elasticsearch reads it: July 1 and 31.
                assertThat(searcher.count(executor.translateQuery(new TurDslQuery.Range(
                        "publication_date", "2027-07-01", null, "2027-07-31", null), types))).isEqualTo(2);
                // A day as gt leaves the whole day out; lt stops before the day starts.
                assertThat(searcher.count(executor.translateQuery(new TurDslQuery.Range(
                        "publication_date", null, "2027-06-30", null, "2027-08-01"), types))).isEqualTo(2);
                // An ISO instant is exact, and a number is still read as millis.
                assertThat(searcher.count(executor.translateQuery(new TurDslQuery.Range(
                        "publication_date", "2027-07-31T18:00:00Z", null, null, null), types))).isEqualTo(2);
                long julyFirst = java.time.Instant.parse("2027-07-01T00:00:00Z").toEpochMilli();
                assertThat(searcher.count(executor.translateQuery(new TurDslQuery.Range(
                        "publication_date", null, null, julyFirst, null), types))).isEqualTo(2);
            }
        }
    }

    // ─────────────── Sort translation ───────────────

    @Test
    void numericSortUsesTypedSortFieldNotSortedSet() {
        SortField tuition = sortField(TurSEFieldType.CURRENCY, "tuition");
        assertThat(tuition).isNotInstanceOf(SortedSetSortField.class);
        assertThat(tuition.getType()).isEqualTo(SortField.Type.DOUBLE);

        assertThat(sortField(TurSEFieldType.INT, "durationMonths").getType())
                .isEqualTo(SortField.Type.INT);
        assertThat(sortField(TurSEFieldType.DATE, "publishedAt").getType())
                .isEqualTo(SortField.Type.LONG);
    }

    @Test
    void stringAndUnknownSortUseSortedSetSortField() {
        assertThat(sortField(TurSEFieldType.STRING, "degree")).isInstanceOf(SortedSetSortField.class);
        // No declared type → SortedSet fallback (interface entry point).
        Sort fallback = executor.buildSort(List.of(Map.of("degree", "asc")), Map.of());
        assertThat(fallback.getSort()[0]).isInstanceOf(SortedSetSortField.class);
    }

    @Test
    void sortDirectionFollowsOrder() {
        SortField asc = sortField(TurSEFieldType.CURRENCY, "tuition", "asc");
        SortField desc = sortField(TurSEFieldType.CURRENCY, "tuition", "desc");
        assertThat(asc.getReverse()).isFalse();
        assertThat(desc.getReverse()).isTrue();
    }

    // ─────────────── T802: case-insensitive STRING facet term matching ───────────────

    @Test
    void stringTermIsCaseInsensitiveButIdStaysExact() {
        // STRING facet fields translate to a case-insensitive RegexpQuery; id keeps
        // its exact TermQuery so unique-id lookups are unaffected.
        assertThat(executor.translateQuery(new TurDslQuery.Term("kind", "embedding"),
                Map.of("kind", TurSEFieldType.STRING))).isInstanceOf(RegexpQuery.class);
        assertThat(executor.translateQuery(new TurDslQuery.Term("id", "abc"),
                Map.of("id", TurSEFieldType.STRING))).isInstanceOf(TermQuery.class);
        // No declared type → conservative exact TermQuery (interface entry point).
        assertThat(executor.translateQuery(new TurDslQuery.Term("kind", "embedding")))
                .isInstanceOf(TermQuery.class);
    }

    @Test
    void caseInsensitiveStringTermActuallyMatchesUppercaseIndexedValue() throws IOException {
        // The real bug: the planner emits kind=embedding but the value is indexed as
        // EMBEDDING. Prove the translated query matches against a live index.
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                writer.addDocument(docOf("m1", "EMBEDDING", "openai"));
                writer.addDocument(docOf("m2", "CHAT", "openai"));
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Map<String, TurSEFieldType> types = Map.of(
                        "kind", TurSEFieldType.STRING, "vendor", TurSEFieldType.STRING);

                // lowercase query → matches the uppercase-indexed enum value.
                Query lower = executor.translateQuery(new TurDslQuery.Term("kind", "embedding"), types);
                assertThat(searcher.count(lower)).isEqualTo(1);
                // uppercase query against lowercase-indexed data also matches.
                Query upper = executor.translateQuery(new TurDslQuery.Term("vendor", "OPENAI"), types);
                assertThat(searcher.count(upper)).isEqualTo(2);
                // a terms filter is case-insensitive per value too.
                Query terms = executor.translateQuery(
                        new TurDslQuery.Terms("kind", List.of("embedding", "chat")), types);
                assertThat(searcher.count(terms)).isEqualTo(2);
            }
        }
    }

    // ─────────────── T1169: _text_ resolves to the site's TEXT fields ───────────────

    @Test
    void textCatchAllFindsDocumentsIndexedOnlyUnderTheirTextFields() throws IOException {
        // The Lucene indexer never writes `_text_`: a copilot `match _text_` must reach
        // the TEXT fields the document was actually indexed under.
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                writer.addDocument(product("p1", "Storm shell", "A waterproof jacket for alpine rain"));
                writer.addDocument(product("p2", "Merino base layer", "Warm next-to-skin layer"));
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Map<String, TurSEFieldType> types = Map.of("title", TurSEFieldType.TEXT,
                        "description", TurSEFieldType.TEXT, "type", TurSEFieldType.STRING);

                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.Match("_text_", "waterproof jacket", null), types))).isEqualTo(1);
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.Match("_text_", "storm", null), types))).isEqualTo(1);
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.MultiMatch("warm layer", null, null), types))).isEqualTo(1);
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.QueryString("alpine OR merino", null, null), types))).isEqualTo(2);
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.SimpleQueryString("rain", null, null), types))).isEqualTo(1);
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.MatchPhrase("_text_", "waterproof jacket", null), types))).isEqualTo(1);
                // Each token may sit in a different field; the last is a prefix.
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.MatchBoolPrefix("_text_", "storm waterpr"), types))).isEqualTo(1);
                // Without a resolved site the catch-all stays `_text_`, which finds nothing here.
                assertThat(searcher.count(executor.translateQuery(
                        new TurDslQuery.Match("_text_", "waterproof", null)))).isZero();
            }
        }
    }

    @Test
    void anExplicitFieldIsNotExpanded() {
        Map<String, TurSEFieldType> types = Map.of("title", TurSEFieldType.TEXT,
                "description", TurSEFieldType.TEXT);
        assertThat(TurDslLuceneExecutor.searchFields("title", types)).containsExactly("title");
        assertThat(TurDslLuceneExecutor.searchFields("_text_", types))
                .containsExactly("description", "title");
        assertThat(TurDslLuceneExecutor.searchFields("_text_", Map.of("type", TurSEFieldType.STRING)))
                .containsExactly("_text_");
    }

    private static Document product(String id, String title, String description) {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("description", description, Field.Store.YES));
        doc.add(new StringField("type", "product", Field.Store.YES));
        return doc;
    }

    private static Document docOf(String id, String kind, String vendor) {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StringField("kind", kind, Field.Store.YES));
        doc.add(new StringField("vendor", vendor, Field.Store.YES));
        return doc;
    }

    private SortField sortField(TurSEFieldType type, String field) {
        return sortField(type, field, "asc");
    }

    private SortField sortField(TurSEFieldType type, String field, String order) {
        Sort sort = executor.buildSort(List.of(Map.of(field, order)), Map.of(field, type));
        return sort.getSort()[0];
    }
}
