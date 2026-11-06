package com.viglet.turing.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.TurSNQueryRelaxer;

/**
 * T1242 — a query's stop words are dropped in the core's language, so
 * {@code uma barraca para 2 pessoas} finds a tent described without them and
 * highlights only the content words; a query made only of stop words still finds
 * the documents that hold them.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@ExtendWith(MockitoExtension.class)
class TurLuceneStopWordsTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    @Mock
    private TurSNSiteFieldExtRepository fieldExtRepository;

    private final TurSNSite site = new TurSNSite();
    private TurLuceneQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        site.setDefaultTitleField("title");
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, null, null, null, null);
        lenient().when(fieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                TurSNSiteFieldExt.builder().name("title").type(TurSEFieldType.TEXT).build(),
                TurSNSiteFieldExt.builder().name("text").type(TurSEFieldType.TEXT).build()));
    }

    private Query query(String q, Locale locale) {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        return queryBuilder.buildQuery(site, new TurSEParameters(params, new TurSNSitePostParamsBean()), locale);
    }

    /** The ids that match {@code q}, and the highlighted body of each, against an index in {@code locale}. */
    private static List<String> search(Query query, Locale locale, List<String[]> docs, List<String> highlights)
            throws Exception {
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir,
                    new IndexWriterConfig(new TurLucenePerFieldAnalyzer(locale)))) {
                for (String[] d : docs) {
                    Document doc = new Document();
                    doc.add(new StringField("id", d[0], Field.Store.YES));
                    // The content goes in the body: a title phrase is marked as one span (T1177).
                    doc.add(new TextField("title", d[0], Field.Store.YES));
                    doc.add(new TextField("text", d[1], Field.Store.YES));
                    writer.addDocument(doc);
                }
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs top = searcher.search(query, 10);
                List<String> ids = new ArrayList<>();
                for (var hit : top.scoreDocs) {
                    ids.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                if (highlights != null && top.scoreDocs.length > 0) {
                    UnifiedHighlighter highlighter = new UnifiedHighlighter.Builder(searcher,
                            new TurLucenePerFieldAnalyzer(locale)).withMaxNoHighlightPassages(0).build();
                    highlights.addAll(List.of(highlighter.highlight("text", query, top, 1)));
                }
                return ids;
            }
        }
    }

    @Test
    void aPortugueseQueryNeedsAndHighlightsOnlyItsContentWords() throws Exception {
        List<String> highlights = new ArrayList<>();
        List<String> ids = search(query("uma barraca para 2 pessoas", PT_BR), PT_BR,
                List.<String[]>of(new String[] { "iglu", "Barraca iglu 2 pessoas" },
                        new String[] { "cadeira", "Cadeira dobrável para camping" }),
                highlights);

        assertThat(ids).containsExactly("iglu");
        assertThat(highlights.get(0)).contains("<b>Barraca</b>").contains("<b>pessoas</b>");
    }

    @Test
    void aBodyHoldingTheStopWordsIsNotMarkedOnThem() throws Exception {
        List<String> highlights = new ArrayList<>();
        search(query("uma barraca para 2 pessoas", PT_BR), PT_BR,
                List.<String[]>of(new String[] { "tunel", "Uma barraca para 2 pessoas" }), highlights);

        assertThat(highlights.get(0)).doesNotContain("<b>Uma</b>").doesNotContain("<b>para</b>")
                .contains("<b>barraca</b>");
    }

    @Test
    void anEnglishQueryDropsTheStopWords() throws Exception {
        List<String> ids = search(query("a tent for the beach", Locale.US), Locale.US,
                List.<String[]>of(new String[] { "beach", "Beach tent" }), null);

        assertThat(ids).containsExactly("beach");
    }

    @Test
    void aQueryOfOnlyStopWordsStillFindsTheDocumentsHoldingThem() throws Exception {
        List<String> ids = search(query("para", PT_BR), PT_BR,
                List.<String[]>of(new String[] { "cadeira", "Cadeira dobrável para camping" },
                        new String[] { "iglu", "Barraca iglu" }),
                null);

        assertThat(ids).containsExactly("cadeira");
    }

    @Test
    void aQuotedPhraseKeepsItsStopWords() throws Exception {
        List<String> ids = search(query("\"barraca para 2\"", PT_BR), PT_BR,
                List.<String[]>of(new String[] { "para", "Barraca para 2 pessoas" },
                        new String[] { "iglu", "Barraca iglu 2 pessoas" }),
                null);

        assertThat(ids).containsExactly("para");
    }

    @Test
    void onlyStopWordsReadsTheLocalesList() {
        assertThat(TurSNQueryRelaxer.onlyStopWords("para", PT_BR)).isTrue();
        assertThat(TurSNQueryRelaxer.onlyStopWords("uma barraca para", PT_BR)).isFalse();
        assertThat(TurSNQueryRelaxer.onlyStopWords("the", Locale.US)).isTrue();
        assertThat(TurSNQueryRelaxer.onlyStopWords("para", Locale.JAPAN)).isFalse();
    }
}
