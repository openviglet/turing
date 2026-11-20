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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.viglet.turing.sn.api.TurSNSiteFieldExts;
import com.viglet.turing.sn.api.TurSNSiteLocales;
import com.viglet.turing.searchengine.TurSESynonymRule;
import com.viglet.turing.searchengine.lucene.TurLuceneSynonymMapBuilder;
import com.viglet.turing.searchengine.lucene.TurLuceneSynonymRegistry;

/**
 * T1297 — an acronym grouped with its six-word name ("fespsp", "fundação escola de
 * sociologia e política") on a pt-BR site with many text fields: the stem and folded
 * variants of every word of the expansion used to multiply into hundreds of phrases
 * per field, past {@code IndexSearcher}'s clause limit, and a search for the acronym
 * failed. It now finds what it found without the group, plus the full name's pages.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@ExtendWith(MockitoExtension.class)
class TurLuceneSynonymClauseLimitTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final int TEXT_FIELDS = 16;

    @Mock
    private TurSNSiteFieldExts fieldExtRepository;
    @Mock
    private TurSNSiteLocales localeRepository;

    private final TurSNSite site = new TurSNSite();
    private TurLuceneQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        registry.put("fespsp_pt", TurLuceneSynonymMapBuilder.build(List.of(
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null,
                        List.of("fespsp", "fundação escola de sociologia e política")))).map());
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, null, localeRepository, registry, null);

        List<TurSNSiteFieldExt> fields = new ArrayList<>();
        for (int i = 0; i < TEXT_FIELDS; i++) {
            fields.add(TurSNSiteFieldExt.builder().name(field(i)).type(TurSEFieldType.TEXT).build());
        }
        when(fieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(fields);
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setLanguage(PT_BR);
        locale.setCore("fespsp_pt");
        org.mockito.Mockito.lenient().when(localeRepository.findByTurSNSite(any())).thenReturn(List.of(locale));
    }

    private static String field(int i) {
        return i == 0 ? "title" : i == 1 ? "text" : "field" + i;
    }

    @Test
    void theAcronymFindsItsOwnPagesAndTheFullNames() throws Exception {
        assertThat(search("fespsp")).containsExactlyInAnyOrder("acronym", "fullname");
    }

    @Test
    void theAcronymBesideAnotherWordStillSearches() throws Exception {
        assertThat(search("fespsp extensão")).containsExactly("acronym");
    }

    @Test
    void theFullNameFindsItsPages() throws Exception {
        assertThat(search("fundação escola de sociologia e política")).contains("fullname");
    }

    /**
     * T1304 — a long key without stop words fires and keeps within the clause limit:
     * the words typed inside its span get no variants either, past the first.
     */
    @Test
    void aLongKeyThatFiresStaysWithinTheClauseLimit() throws Exception {
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        registry.put("fespsp_pt", TurLuceneSynonymMapBuilder.build(List.of(
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null,
                        List.of("fespsp", "fundação escola sociologia política paulista")))).map());
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, null, localeRepository, registry, null);

        assertThat(search("fundação escola sociologia política paulista")).contains("acronym");
    }

    /**
     * T1304 — built for the core's language, the full name expands to the acronym too,
     * and still finds its own page: synonyms only add.
     */
    @Test
    void theFullNameAndTheAcronymEachFindBothPages() throws Exception {
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        registry.put("fespsp_pt", TurLuceneSynonymMapBuilder.build(List.of(
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null,
                        List.of("fespsp", "fundação escola de sociologia e política"))), PT_BR).map());
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, null, localeRepository, registry, null);

        assertThat(search("fundação escola de sociologia e política")).contains("acronym", "fullname");
        assertThat(search("fespsp")).contains("acronym", "fullname");
    }

    /** T1304 — the synonym-free query a clause-limit retry runs leaves the group out. */
    @Test
    void aQueryBuiltWithoutSynonymsDoesNotExpand() throws Exception {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ("fespsp");
        Query plain = queryBuilder.buildQuery(site, new TurSEParameters(params, new TurSNSitePostParamsBean()),
                PT_BR, false);
        assertThat(plain.toString()).doesNotContain("sociologia");
    }

    @Test
    void aSingleWordSynonymIsStillStemmedAndFolded() throws Exception {
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        registry.put("fespsp_pt", TurLuceneSynonymMapBuilder.build(List.of(
                new TurSESynonymRule(TurSNSynonymType.REGULAR, null, List.of("pós", "especialização")))).map());
        queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, null, localeRepository, registry, null);

        assertThat(search("pós")).contains("plural");
    }

    private List<String> search(String q) throws Exception {
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        Query query = queryBuilder.buildQuery(site, new TurSEParameters(params, new TurSNSitePostParamsBean()), PT_BR);
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new TurLuceneFoldingAnalyzer(PT_BR)))) {
                writer.addDocument(doc("acronym", "Feira de Extensão FESPSP"));
                writer.addDocument(doc("fullname", "Fundação Escola de Sociologia e Política de São Paulo"));
                writer.addDocument(doc("plural", "Cursos de especializações"));
                writer.addDocument(doc("other", "Biblioteca aberta ao público"));
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

    private static Document doc(String id, String title) {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        for (int i = 0; i < TEXT_FIELDS; i++) {
            doc.add(new TextField(field(i), i == 0 ? title : "", Field.Store.NO));
        }
        return doc;
    }
}
