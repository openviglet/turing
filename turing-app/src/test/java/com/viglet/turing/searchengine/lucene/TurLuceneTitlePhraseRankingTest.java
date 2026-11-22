package com.viglet.turing.searchengine.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
import org.apache.lucene.search.ScoreDoc;
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
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import com.viglet.turing.searchengine.TurSNSynonymType;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.searchengine.TurSESynonymRule;

/**
 * T1177 — a guide whose title holds the query as a phrase ranks above products
 * that match only one of its synonyms, on a stemmed en_US site with a synonym
 * group, as on Dumont's demo store.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
@ExtendWith(MockitoExtension.class)
class TurLuceneTitlePhraseRankingTest {

    @Mock
    private TurSESiteSearchConfig fieldExtRepository;

    private final TurSNSite site = new TurSNSite();
    private TurLuceneQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        site.setDefaultTitleField("title");
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        registry.put("demo_en", TurLuceneSynonymMapBuilder.build(List.of(
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null,
                        List.of("raincoat", "rain jacket", "waterproof jacket")),
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null,
                        List.of("puffer", "down jacket", "insulated jacket"))))
                .map());
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, registry, null);

        when(fieldExtRepository.enabledFields(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> TurLuceneSpiFixtures.fields(
                TurSNSiteFieldExt.builder().name("title").type(TurSEFieldType.TEXT).build(),
                TurSNSiteFieldExt.builder().name("text").type(TurSEFieldType.TEXT).build()));
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setLanguage(Locale.US);
        locale.setCore("demo_en");
        org.mockito.Mockito.lenient().when(fieldExtRepository.siteCore(any(), org.mockito.ArgumentMatchers.eq(locale.getLanguage())))
                .thenReturn(java.util.Optional.of(new com.viglet.turing.searchengine.spi.TurSESiteCore(null, null, locale.getCore())));
    }

    private static Document doc(String id, String title, String text) {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("text", text, Field.Store.YES));
        return doc;
    }

    private List<String> search(String q) throws Exception {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        Query query = queryBuilder.buildQuery(TurLuceneSpiFixtures.settings(site),
                new TurSEParameters(params, new TurSNSitePostParamsBean()), Locale.US);
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir,
                    new IndexWriterConfig(new TurLuceneFoldingAnalyzer(Locale.US)))) {
                for (String colour : List.of("slate", "moss", "ember", "sky")) {
                    writer.addDocument(doc("catbells-" + colour, "Catbells light rain jacket, " + colour,
                            "A light rain jacket that packs small. This rain jacket keeps showers off."));
                }
                writer.addDocument(doc("guide-waterproof", "How to choose a waterproof jacket",
                        "Waterproof ratings, breathability and fit: what to look for in a shell."));
                writer.addDocument(doc("guide-insulated", "Down or synthetic: choosing an insulated jacket",
                        "Warmth for weight, wet weather and packing."));
                writer.addDocument(doc("guide-wash", "How to wash a down jacket",
                        "Low heat, tennis balls and patience."));
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                List<String> ids = new ArrayList<>();
                for (ScoreDoc hit : searcher.search(query, 10).scoreDocs) {
                    ids.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                return ids;
            }
        }
    }

    @Test
    void aGuideTitledWithTheQueryPhraseRanksFirst() throws Exception {
        assertThat(search("waterproof jacket")).startsWith("guide-waterproof");
        assertThat(search("insulated jacket")).startsWith("guide-insulated");
    }

    @Test
    void theSynonymsStillFindTheProducts() throws Exception {
        assertThat(search("waterproof jacket")).contains("catbells-slate", "catbells-sky");
    }
}
