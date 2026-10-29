package com.viglet.turing.sn.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.lucene.TurLuceneFoldingAnalyzer;
import com.viglet.turing.lucene.TurLucenePerFieldAnalyzer;

/**
 * T1170 — the copilot's DSL path analyzes text in the site locale's language, as
 * the stemmed index does, so a plural match finds the singular-titled document.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurDslLuceneStemmingTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Map<String, TurSEFieldType> TYPES = Map.of("title", TurSEFieldType.TEXT);

    private final TurDslLuceneExecutor executor = new TurDslLuceneExecutor(null, null, null);

    private Set<String> search(Locale locale, TurDslQuery query) throws Exception {
        try (Directory directory = new ByteBuffersDirectory();
                IndexWriter writer = new IndexWriter(directory,
                        new IndexWriterConfig(new TurLucenePerFieldAnalyzer(locale)))) {
            add(writer, "jacket", "Waterproof jacket");
            add(writer, "boots", "Hiking boots");
            add(writer, "jaqueta", "Jaqueta de chuva");
            writer.commit();
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query lucene = executor.translateQuery(query, TYPES, new TurLuceneFoldingAnalyzer(locale));
                Set<String> ids = new TreeSet<>();
                for (ScoreDoc hit : searcher.search(lucene, 10).scoreDocs) {
                    ids.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                return ids;
            }
        }
    }

    private static void add(IndexWriter writer, String id, String title) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        writer.addDocument(doc);
    }

    @Test
    void aPluralMatchFindsTheSingular() throws Exception {
        assertThat(search(Locale.US, new TurDslQuery.Match("title", "waterproof jackets", "and")))
                .containsExactly("jacket");
        assertThat(search(Locale.US, new TurDslQuery.Match("_text_", "boot", null)))
                .containsExactly("boots");
        assertThat(search(PT_BR, new TurDslQuery.Match("title", "jaquetas", null)))
                .containsExactly("jaqueta");
    }

    @Test
    void aQueryStringIsStemmedToo() throws Exception {
        assertThat(search(Locale.US, new TurDslQuery.QueryString("jackets", "title", null)))
                .containsExactly("jacket");
    }
}
