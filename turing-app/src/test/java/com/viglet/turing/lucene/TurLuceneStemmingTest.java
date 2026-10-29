package com.viglet.turing.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedSetDocValuesField;
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
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;

/**
 * T1170 — a plural query finds the singular-titled document (and the reverse) on
 * the Lucene engine's SN search, in the site locale's language; an index written before stemming keeps answering the exact word.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurLuceneStemmingTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final TurSNSiteFieldExtRepository fieldRepository = mock(TurSNSiteFieldExtRepository.class);
    private final TurLuceneQueryBuilder queryBuilder =
            new TurLuceneQueryBuilder(fieldRepository, null, null, null, null);
    private final TurSNSite site = new TurSNSite();

    TurLuceneStemmingTest() {
        TurSNSiteFieldExt text = new TurSNSiteFieldExt();
        text.setName("text");
        text.setType(TurSEFieldType.TEXT);
        when(fieldRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(text));
    }

    private static final Map<String, String> CATALOG = Map.of(
            "jacket", "Waterproof jacket for hiking",
            "boots", "Leather boots with ankle support",
            "bag", "Down sleeping bag rated to minus five",
            "jaqueta", "Jaqueta impermeável para trilha",
            "botas", "Botas de couro para caminhada");

    private Set<String> snSearch(Analyzer indexAnalyzer, String q, Locale locale) throws Exception {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        params.setRows(10);
        params.setP(1);
        return search(indexAnalyzer, queryBuilder.buildQuery(site, new TurSEParameters(params), locale));
    }

    static Set<String> search(Analyzer indexAnalyzer, Query query) throws Exception {
        try (Directory directory = new ByteBuffersDirectory();
                IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(indexAnalyzer))) {
            for (Map.Entry<String, String> e : CATALOG.entrySet()) {
                Document doc = new Document();
                doc.add(new StringField("id", e.getKey(), Field.Store.YES));
                doc.add(new SortedSetDocValuesField("id", new BytesRef(e.getKey())));
                doc.add(new TextField("text", e.getValue(), Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.commit();
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Set<String> ids = new TreeSet<>();
                for (ScoreDoc hit : searcher.search(query, 10).scoreDocs) {
                    ids.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                return ids;
            }
        }
    }

    @Test
    void pluralAndSingularFindEachOtherInEnglish() throws Exception {
        TurLucenePerFieldAnalyzer en = new TurLucenePerFieldAnalyzer(Locale.US);
        assertEquals(Set.of("jacket"), snSearch(en, "jackets", Locale.US));
        assertEquals(Set.of("jacket"), snSearch(en, "Waterproof jackets", Locale.US));
        assertEquals(Set.of("boots"), snSearch(en, "boot", Locale.US));
        assertEquals(Set.of("bag"), snSearch(en, "sleeping bags", Locale.US));
        assertEquals(Set.of("jacket"), snSearch(en, "jacket", Locale.US));
    }

    @Test
    void pluralAndSingularFindEachOtherInPortuguese() throws Exception {
        TurLucenePerFieldAnalyzer pt = new TurLucenePerFieldAnalyzer(PT_BR);
        assertEquals(Set.of("jaqueta"), snSearch(pt, "jaquetas", PT_BR));
        assertEquals(Set.of("jaqueta"), snSearch(pt, "jaquetas para trilha", PT_BR));
        assertEquals(Set.of("botas"), snSearch(pt, "bota", PT_BR));
    }

    @Test
    void anIndexWrittenBeforeStemmingStillMatchesExactWordsAndFindsSingularsForPlurals() throws Exception {
        // Not re-indexed yet: only the surface words are in it.
        Analyzer old = new TurLucenePerFieldAnalyzer();
        assertEquals(Set.of("boots"), snSearch(old, "boots", Locale.US));
        assertEquals(Set.of("jacket"), snSearch(old, "jacket", Locale.US));
        assertEquals(Set.of("jacket"), snSearch(old, "jackets", Locale.US));
        assertEquals(Set.of("jacket"), snSearch(new StandardAnalyzer(), "jackets", Locale.US));
    }

    @Test
    void anUnknownLanguageStaysUnstemmed() throws Exception {
        Locale ja = Locale.JAPAN;
        assertEquals(Set.of(), snSearch(new TurLucenePerFieldAnalyzer(ja), "jackets", ja));
        assertEquals(Set.of(), snSearch(new TurLucenePerFieldAnalyzer((Locale) null), "jackets", null));
    }

}
