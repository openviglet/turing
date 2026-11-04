package com.viglet.turing.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;

/**
 * T1194 — a title stored with decomposed accents (Unicode NFD) is found by the
 * accented and the unaccented query a browser sends (NFC), as its NFC spelling is.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurLuceneNfcCharFilterTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    /** "Jaqueta impermeável do dia a dia Borrowdale", the á as a + combining acute. */
    private static final String NFD_TITLE = "Jaqueta impermeável do dia a dia Borrowdale";
    private static final String NFC_TITLE = "Jaqueta impermeável do dia a dia Borrowdale";

    private final TurSNSiteFieldExtRepository fieldRepository = mock(TurSNSiteFieldExtRepository.class);
    private final TurLuceneQueryBuilder queryBuilder =
            new TurLuceneQueryBuilder(fieldRepository, null, null, null, null);
    private final TurSNSite site = new TurSNSite();

    TurLuceneNfcCharFilterTest() {
        TurSNSiteFieldExt text = new TurSNSiteFieldExt();
        text.setName("text");
        text.setType(TurSEFieldType.TEXT);
        when(fieldRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(text));
    }

    private static List<String> terms(Analyzer analyzer, String text) throws Exception {
        List<String> out = new ArrayList<>();
        try (TokenStream tokens = analyzer.tokenStream("text", text)) {
            CharTermAttribute term = tokens.addAttribute(CharTermAttribute.class);
            tokens.reset();
            while (tokens.incrementToken()) {
                out.add(term.toString());
            }
            tokens.end();
        }
        return out;
    }

    @Test
    @DisplayName("NFD and NFC spellings analyse to the same terms, folded and stemmed")
    void decomposedTextAnalysesLikeComposedText() throws Exception {
        try (Analyzer analyzer = new TurLuceneFoldingAnalyzer(PT_BR)) {
            assertThat(terms(analyzer, NFD_TITLE)).isEqualTo(terms(analyzer, NFC_TITLE))
                    .contains("impermeavel", "impermeável");
        }
    }

    @Test
    @DisplayName("Autocomplete n-grams of an NFD word are those of its NFC spelling")
    void edgeNGramsRecompose() throws Exception {
        try (Analyzer analyzer = new TurLuceneEdgeNGramAnalyzer(2, 20)) {
            assertThat(terms(analyzer, "impermeável")).isEqualTo(terms(analyzer, "impermeável"));
        }
    }

    private Set<String> search(String q) throws Exception {
        try (Directory directory = new ByteBuffersDirectory();
                IndexWriter writer = new IndexWriter(directory,
                        new IndexWriterConfig(new TurLucenePerFieldAnalyzer(PT_BR)))) {
            add(writer, "borrowdale", NFD_TITLE);
            add(writer, "other", "Mochila de trilha leve");
            writer.commit();
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Set<String> ids = new TreeSet<>();
                for (ScoreDoc hit : searcher.search(query(q), 10).scoreDocs) {
                    ids.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                return ids;
            }
        }
    }

    private Query query(String q) {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        params.setRows(10);
        params.setP(1);
        return queryBuilder.buildQuery(site, new TurSEParameters(params));
    }

    private static void add(IndexWriter writer, String id, String text) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new SortedSetDocValuesField("id", new BytesRef(id)));
        doc.add(new TextField("text", text, Field.Store.YES));
        writer.addDocument(doc);
    }

    @Test
    @DisplayName("An NFD title is found by the accented, the unaccented and the two-word query")
    void anNfdTitleIsFoundByEverySpelling() throws Exception {
        assertThat(search("jaqueta impermeável")).containsExactly("borrowdale");
        assertThat(search("jaqueta impermeavel")).containsExactly("borrowdale");
        assertThat(search("impermeável")).containsExactly("borrowdale");
    }

    @Test
    @DisplayName("Highlighting still marks the whole stored NFD word")
    void highlightingKeepsItsOffsets() throws Exception {
        try (Directory directory = new ByteBuffersDirectory();
                IndexWriter writer = new IndexWriter(directory,
                        new IndexWriterConfig(new TurLucenePerFieldAnalyzer(PT_BR)))) {
            add(writer, "borrowdale", NFD_TITLE);
            writer.commit();
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query q = query("impermeável");
                TopDocs topDocs = searcher.search(q, 10);
                String[] snippets = new UnifiedHighlighter.Builder(searcher, new TurLuceneFoldingAnalyzer(PT_BR))
                        .build().highlight("text", q, topDocs, 1);
                assertThat(snippets[0]).contains("<b>impermeável</b>");
            }
        }
    }
}
