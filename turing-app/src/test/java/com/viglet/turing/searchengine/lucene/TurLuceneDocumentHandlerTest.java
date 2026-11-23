package com.viglet.turing.searchengine.lucene;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.MMapDirectory;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.spi.TurSEIndexField;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TurLuceneDocumentHandler.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@ExtendWith(MockitoExtension.class)
class TurLuceneDocumentHandlerTest {

    @TempDir
    Path tempDir;

    @Mock
    private TurSESiteSearchConfig siteSearchConfig;

    private static final String SITE_ID = "site-1";

    private TurLuceneDocumentHandler handler;
    private TurLuceneInstance instance;
    private MMapDirectory directory;
    private IndexWriter writer;

    @BeforeEach
    void setUp() throws IOException {
        handler = new TurLuceneDocumentHandler(siteSearchConfig, null);
        directory = new MMapDirectory(tempDir);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        writer = new IndexWriter(directory, config);
        instance = mock(TurLuceneInstance.class);
        lenient().when(instance.getWriter()).thenReturn(writer);
    }

    @Test
    void indexingShouldRemoveScoreVersionAndBoostFields() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("title", TurSEIndexField.builder().name("title").type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc1");
        attrs.put("title", "Hello World");
        attrs.put("score", 1.0f);
        attrs.put("_version_", 3L);
        attrs.put("boost", 2.5);

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        assertThat(topDocs.totalHits.value()).isEqualTo(1);

        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        assertThat(doc.get("title")).isEqualTo("Hello World");
        reader.close();
    }

    @Test
    void indexingShouldHandleStringFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("category", TurSEIndexField.builder().name("category")
                .type(TurSEFieldType.STRING).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc2");
        attrs.put("category", "books");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        assertThat(topDocs.totalHits.value()).isEqualTo(1);

        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        assertThat(doc.get("category")).isEqualTo("books");
        reader.close();
    }

    @Test
    void indexingShouldHandleIntFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("count", TurSEIndexField.builder().name("count")
                .type(TurSEFieldType.INT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc3");
        attrs.put("count", "42");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleInvalidIntGracefully() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("count", TurSEIndexField.builder().name("count")
                .type(TurSEFieldType.INT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc4");
        attrs.put("count", "not-a-number");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleLongFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("timestamp", TurSEIndexField.builder().name("timestamp")
                .type(TurSEFieldType.LONG).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc5");
        attrs.put("timestamp", "9999999999");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleFloatFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("weight", TurSEIndexField.builder().name("weight")
                .type(TurSEFieldType.FLOAT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc6");
        attrs.put("weight", "3.14");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleDoubleAndCurrencyFieldTypes() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("amount", TurSEIndexField.builder().name("amount")
                .type(TurSEFieldType.DOUBLE).multiValued(false).build());
        fieldMap.put("price", TurSEIndexField.builder().name("price")
                .type(TurSEFieldType.CURRENCY).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc7");
        attrs.put("amount", "99.99");
        attrs.put("price", "150.00,BRL");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleDateFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("created", TurSEIndexField.builder().name("created")
                .type(TurSEFieldType.DATE).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc8");
        attrs.put("created", "2026-01-15T10:30:00Z");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleInvalidDateGracefully() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("created", TurSEIndexField.builder().name("created")
                .type(TurSEFieldType.DATE).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc9");
        attrs.put("created", "not-a-date");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    /**
     * T1338 — one unparsable DATE among valid ones used to be written as a string
     * under the date field's name, which Lucene refuses as an inconsistent schema
     * (and the plugin answered by recreating the whole index). Now the bad value is
     * dropped from that one document and the batch indexes in full.
     */
    @Test
    void anUnparsableDateAmongValidOnesIsDroppedAndTheBatchIndexes() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("releaseDate", TurSEIndexField.builder().name("releaseDate")
                .type(TurSEFieldType.DATE).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        List<Map<String, Object>> batch = List.of(
                Map.of("id", "good-1", "title", "A", "releaseDate", "2016-05-01T00:00:00Z"),
                Map.of("id", "bad", "title", "B", "releaseDate", "2017T00:00:00Z"),
                Map.of("id", "good-2", "title", "C", "releaseDate", "2018-03-10T00:00:00Z"));

        assertThat(handler.indexingBatch(instance, SITE_ID, batch)).isEqualTo(3);

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            assertThat(reader.numDocs()).isEqualTo(3);
            // The two valid dates stay range-searchable as points.
            assertThat(searcher.count(LongPoint.newRangeQuery("releaseDate",
                    Instant.parse("2015-01-01T00:00:00Z").toEpochMilli(),
                    Instant.parse("2019-01-01T00:00:00Z").toEpochMilli()))).isEqualTo(2);
            TopDocs bad = searcher.search(new TermQuery(new Term("id", "bad")), 1);
            Document badDoc = searcher.storedFields().document(bad.scoreDocs[0].doc);
            assertThat(badDoc.get("releaseDate")).isNull();
            assertThat(badDoc.get("title")).isEqualTo("B");
        }
    }

    /** T1338 — the same for the numeric types, which shared the string fallback. */
    @Test
    void anUnparsableNumberAmongValidOnesIsDroppedAndTheBatchIndexes() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("price", TurSEIndexField.builder().name("price")
                .type(TurSEFieldType.DOUBLE).multiValued(false).build());
        fieldMap.put("count", TurSEIndexField.builder().name("count")
                .type(TurSEFieldType.INT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        List<Map<String, Object>> batch = List.of(
                Map.of("id", "good", "price", "9.99", "count", "3"),
                Map.of("id", "bad", "price", "nine", "count", "three"));

        assertThat(handler.indexingBatch(instance, SITE_ID, batch)).isEqualTo(2);

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs bad = searcher.search(new TermQuery(new Term("id", "bad")), 1);
            Document badDoc = searcher.storedFields().document(bad.scoreDocs[0].doc);
            assertThat(badDoc.get("price")).isNull();
            assertThat(badDoc.get("count")).isNull();
            assertThat(searcher.count(IntPoint.newExactQuery("count", 3))).isEqualTo(1);
        }
    }

    @Test
    void indexingShouldHandleJsonArrayMultiValued() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("tags", TurSEIndexField.builder().name("tags")
                .type(TurSEFieldType.STRING).multiValued(true).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc10");
        attrs.put("tags", new JSONArray(List.of("java", "spring")));

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        String[] tagValues = doc.getValues("tags");
        assertThat(tagValues).containsExactly("java", "spring");
        reader.close();
    }

    @Test
    void indexingShouldHandleJsonArraySingleValued() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("name", TurSEIndexField.builder().name("name")
                .type(TurSEFieldType.STRING).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc11");
        attrs.put("name", new JSONArray(List.of("first", "second")));

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        // Single-valued: only first element
        assertThat(doc.get("name")).isEqualTo("first");
        reader.close();
    }

    @Test
    void indexingShouldHandleArrayListMultiValued() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("authors", TurSEIndexField.builder().name("authors")
                .type(TurSEFieldType.STRING).multiValued(true).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc12");
        attrs.put("authors", new ArrayList<>(List.of("Alice", "Bob")));

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        String[] authorValues = doc.getValues("authors");
        assertThat(authorValues).containsExactly("Alice", "Bob");
        reader.close();
    }

    @Test
    void indexingShouldSkipNullValues() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc14");
        attrs.put("nullfield", null);

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldSkipEmptyStringValues() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("desc", TurSEIndexField.builder().name("desc")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc15");
        attrs.put("desc", "  ");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleEmptyJsonArray() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("tags", TurSEIndexField.builder().name("tags")
                .type(TurSEFieldType.STRING).multiValued(true).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc16");
        attrs.put("tags", new JSONArray());

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldHandleEmptyArrayList() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("tags", TurSEIndexField.builder().name("tags")
                .type(TurSEFieldType.STRING).multiValued(true).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc17");
        attrs.put("tags", new ArrayList<>());

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldGenerateIdWhenMissing() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("title", TurSEIndexField.builder().name("title")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("title", "No ID Document");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldUpdateExistingDocumentWithSameId() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("title", TurSEIndexField.builder().name("title")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs1 = new LinkedHashMap<>();
        attrs1.put("id", "sameId");
        attrs1.put("title", "Original");
        handler.indexing(instance, SITE_ID, attrs1);

        Map<String, Object> attrs2 = new LinkedHashMap<>();
        attrs2.put("id", "sameId");
        attrs2.put("title", "Updated");
        handler.indexing(instance, SITE_ID, attrs2);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        assertThat(doc.get("title")).isEqualTo("Updated");
        reader.close();
    }

    @Test
    void deIndexingShouldRemoveDocumentById() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("title", TurSEIndexField.builder().name("title")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "toDelete");
        attrs.put("title", "Delete Me");
        handler.indexing(instance, SITE_ID, attrs);

        handler.deIndexing(instance, "toDelete");

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isZero();
        reader.close();
    }

    @Test
    void deIndexingByTypeShouldRemoveDocumentsByType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("type", TurSEIndexField.builder().name("type")
                .type(TurSEFieldType.STRING).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "typed1");
        attrs.put("type", "article");
        handler.indexing(instance, SITE_ID, attrs);

        handler.deIndexingByType(instance, "article");

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isZero();
        reader.close();
    }

    @Test
    void indexingShouldHandleBoolFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("active", TurSEIndexField.builder().name("active")
                .type(TurSEFieldType.BOOL).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc18");
        attrs.put("active", "true");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        assertThat(doc.get("active")).isEqualTo("true");
        reader.close();
    }

    @Test
    void indexingShouldHandleArrayFieldType() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("items", TurSEIndexField.builder().name("items")
                .type(TurSEFieldType.ARRAY).multiValued(true).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc19");
        attrs.put("items", new ArrayList<>(List.of("a", "b", "c")));

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        String[] values = doc.getValues("items");
        assertThat(values).containsExactly("a", "b", "c");
        reader.close();
    }

    @Test
    void indexingShouldDefaultToStringTypeForUnknownField() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        // No entry for "unknown" field
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc20");
        attrs.put("unknown", "somevalue");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new FieldExistsQuery("id"), 10);
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        assertThat(doc.get("unknown")).isEqualTo("somevalue");
        reader.close();
    }

    @Test
    void indexingShouldTruncateLongFacetValues() throws IOException {
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("desc", TurSEIndexField.builder().name("desc")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        String longValue = "x".repeat(1000);
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc21");
        attrs.put("desc", longValue);

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        assertThat(reader.numDocs()).isEqualTo(1);
        reader.close();
    }

    @Test
    void indexingShouldPropagateIoFailureInsteadOfSwallowing() throws IOException {
        // Regression guard: a swallowed IOException used to falsely report the document
        // as "Indexed" while it was actually lost. It must now surface to the caller so
        // the resilience layer / circuit breaker can react.
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("title", TurSEIndexField.builder().name("title")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        IndexWriter failingWriter = mock(IndexWriter.class);
        when(failingWriter.updateDocument(any(), any())).thenThrow(new IOException("disk full"));
        TurLuceneInstance failingInstance = mock(TurLuceneInstance.class);
        when(failingInstance.getWriter()).thenReturn(failingWriter);

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "doc-io-fail");
        attrs.put("title", "Hello");

        assertThrows(java.io.UncheckedIOException.class,
                () -> handler.indexing(failingInstance, SITE_ID, attrs));
    }

    @Test
    void deIndexingShouldPropagateIoFailureInsteadOfSwallowing() throws IOException {
        IndexWriter failingWriter = mock(IndexWriter.class);
        when(failingWriter.deleteDocuments(any(org.apache.lucene.index.Term.class)))
                .thenThrow(new IOException("disk full"));
        TurLuceneInstance failingInstance = mock(TurLuceneInstance.class);
        when(failingInstance.getWriter()).thenReturn(failingWriter);

        assertThrows(java.io.UncheckedIOException.class,
                () -> handler.deIndexing(failingInstance, "doc-io-fail"));
    }

    @Test
    void indexingShouldForceIdToKeywordEvenWhenConfiguredAsText() throws IOException {
        // Regression (Block AR "Related"/similar): an AEM-style path id configured as TEXT
        // used to be tokenized, so the exact-term seed lookup behind similar (a TermQuery on
        // `id`) never matched a path like /content/wknd/.../ski-touring-mont-blanc and the
        // feature returned empty. The primary key `id` must always be a non-analyzed keyword
        // so the whole path stays one exact term — regardless of the configured field type.
        Map<String, TurSEIndexField> fieldMap = new HashMap<>();
        fieldMap.put("id", TurSEIndexField.builder().name("id")
                .type(TurSEFieldType.TEXT).multiValued(false).build());
        when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

        String pathId = "/content/wknd/language-masters/en/adventures/ski-touring-mont-blanc";
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", pathId);
        attrs.put("title", "Ski Touring Mont Blanc");

        handler.indexing(instance, SITE_ID, attrs);
        writer.commit();

        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        // Exact-term lookup on the full path — the very query the similar feature runs.
        TopDocs exact = searcher.search(new TermQuery(new Term("id", pathId)), 10);
        assertThat(exact.totalHits.value()).isEqualTo(1);
        // And it must NOT be tokenized: a single path segment is not a standalone term.
        TopDocs tokenized = searcher.search(new TermQuery(new Term("id", "blanc")), 10);
        assertThat(tokenized.totalHits.value()).isZero();
        reader.close();
    }

    @Test
    void indexingShouldThrowNpeForNullAttributes() {
        // The cleaned map is a new LinkedHashMap(null) which throws NPE.
        // Use a separate handler/instance to avoid unnecessary stubbing from setUp.
        TurSESiteSearchConfig localSiteSearchConfig = mock(TurSESiteSearchConfig.class);
        TurLuceneDocumentHandler localHandler = new TurLuceneDocumentHandler(localSiteSearchConfig, null);
        TurLuceneInstance localInstance = mock(TurLuceneInstance.class);

        assertThrows(NullPointerException.class,
                () -> localHandler.indexing(localInstance, SITE_ID, null));
    }
}
