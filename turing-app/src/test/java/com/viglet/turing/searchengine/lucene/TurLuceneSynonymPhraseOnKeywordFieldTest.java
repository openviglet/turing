package com.viglet.turing.searchengine.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 * T1091 — a multi-word synonym expansion is a phrase, and a phrase on a
 * keyword field (the {@code id}, a STRING facet) has no positions to run on.
 * The search must answer with the expansion applied instead of throwing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@ExtendWith(MockitoExtension.class)
class TurLuceneSynonymPhraseOnKeywordFieldTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    @Mock
    private TurSESiteSearchConfig fieldExtRepository;

    private final TurSNSite site = new TurSNSite();
    private TurLuceneQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        registry.put("jobs_pt", TurLuceneSynonymMapBuilder.build(List.of(
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null, List.of("home office", "remoto")),
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null,
                        List.of("caixa", "operador de caixa"))))
                .map());
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, registry, null);

        when(fieldExtRepository.enabledFields(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> TurLuceneSpiFixtures.fields(
                TurSNSiteFieldExt.builder().name("id").type(TurSEFieldType.STRING).build(),
                TurSNSiteFieldExt.builder().name("title").type(TurSEFieldType.TEXT).build(),
                TurSNSiteFieldExt.builder().name("contract").type(TurSEFieldType.STRING).build(),
                TurSNSiteFieldExt.builder().name("tags").type(TurSEFieldType.ARRAY).build()));
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setLanguage(PT_BR);
        locale.setCore("jobs_pt");
        org.mockito.Mockito.lenient().when(fieldExtRepository.siteCore(any(), org.mockito.ArgumentMatchers.eq(locale.getLanguage())))
                .thenReturn(java.util.Optional.of(new com.viglet.turing.searchengine.spi.TurSESiteCore(null, null, locale.getCore())));
    }

    private static Document job(String id, String title, String contract, String tag) {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new StringField("contract", contract, Field.Store.YES));
        doc.add(new StringField("tags", tag, Field.Store.YES));
        return doc;
    }

    private List<String> search(String q) throws Exception {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        Query query = queryBuilder.buildQuery(TurLuceneSpiFixtures.settings(site),
                new TurSEParameters(params, new TurSNSitePostParamsBean()), PT_BR);
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                writer.addDocument(job("cashier", "Operador de caixa", "clt", "varejo"));
                writer.addDocument(job("remote", "Desenvolvedor remoto", "pj", "ti"));
                writer.addDocument(job("hybrid", "Analista home office", "clt", "ti"));
                writer.addDocument(job("tagged", "Atendente", "pj", "home office"));
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
    void singleWordExpandingToAPhraseDoesNotFailOnKeywordFields() throws Exception {
        assertThat(search("caixa")).contains("cashier");
    }

    @Test
    void multiWordTermExpandsWithKeywordFieldsInTheSite() throws Exception {
        assertThat(search("remoto")).contains("remote", "hybrid");
    }

    @Test
    void keywordFieldStillMatchesTheWholePhraseAsAnExactTerm() throws Exception {
        assertThat(search("remoto")).contains("tagged");
    }

    @Test
    void quotedPhraseDoesNotFailOnKeywordFields() throws Exception {
        assertThat(search("\"operador de caixa\"")).contains("cashier");
    }
}
