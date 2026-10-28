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
package com.viglet.turing.sn.synonym;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.lucene.TurLuceneQueryBuilder;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonymType;
import com.viglet.turing.persistence.model.tenant.TurTenant;
import com.viglet.turing.persistence.model.tenant.TurTenantStatus;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.persistence.repository.tenant.TurTenantRepository;
import com.viglet.turing.plugins.se.TurSESynonymRule;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;
import com.viglet.turing.plugins.se.lucene.TurLuceneSearchEnginePlugin;
import com.viglet.turing.plugins.se.lucene.TurLuceneSynonymRegistry;
import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.tenant.TurTenantContext;
import com.viglet.turing.tenant.TurTenantScheduledFanOut;

/**
 * T1168 — synonyms pushed to a Lucene site are re-installed at boot, so a
 * restart (an empty {@link TurLuceneSynonymRegistry}) no longer drops them,
 * while engines that keep pushed synonyms are left alone.
 */
@ExtendWith(MockitoExtension.class)
class TurSNSynonymStartupReapplyTest {

    private static final Locale EN = Locale.ENGLISH;
    private static final String CORE = "demo_en";

    @Mock
    private TurSearchEnginePluginFactory pluginFactory;
    @Mock
    private TurSNSynonymService synonymService;
    @Mock
    private TurSNSiteRepository siteRepository;
    @Mock
    private TurSNSiteLocaleRepository siteLocaleRepository;
    @Mock
    private TurTenantRepository tenantRepository;
    @Mock
    private TurSNSiteFieldExtRepository fieldExtRepository;

    private static TurSNSite site(String id) {
        TurSNSite site = new TurSNSite();
        site.setId(id);
        site.setTurSEInstance(new TurSEInstance());
        return site;
    }

    private static TurSNSiteLocale locale(String core) {
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setLanguage(EN);
        locale.setCore(core);
        return locale;
    }

    private static TurConfigProperties props(boolean tenancy) {
        TurConfigProperties props = new TurConfigProperties();
        props.getTenancy().setEnabled(tenancy);
        return props;
    }

    private TurSNSynonymStartupReapply reapply(boolean tenancy) {
        TurSNSynonymApplyService applyService = new TurSNSynonymApplyService(pluginFactory,
                synonymService, siteRepository, siteLocaleRepository);
        return new TurSNSynonymStartupReapply(siteRepository, applyService,
                new TurTenantScheduledFanOut(new TurTenantContext(props(tenancy)), tenantRepository));
    }

    private static TurLuceneSearchEnginePlugin lucene(TurLuceneSynonymRegistry registry) {
        return new TurLuceneSearchEnginePlugin(null, null, null, null, null, null, registry);
    }

    private void stubSite(TurSNSite site, TurSearchEnginePlugin plugin) {
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        when(pluginFactory.getPluginForSite(site)).thenReturn(plugin);
    }

    @Test
    void luceneSynonymsAnswerAgainAfterARestart() throws Exception {
        TurSNSite site = site("demo");
        // A restart: the registry the plugin writes starts empty.
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        stubSite(site, lucene(registry));
        when(siteRepository.findAll()).thenReturn(List.of(site));
        when(siteLocaleRepository.findByTurSNSite(site)).thenReturn(List.of(locale(CORE)));
        when(synonymService.rulesForApply(site, EN)).thenReturn(List.of(new TurSESynonymRule(
                TurSNSynonymType.REGULAR, null, List.of("rucksack", "backpack", "daypack"))));
        assertThat(registry.get(CORE)).isNull();

        reapply(false).onReady();

        assertThat(registry.get(CORE)).isNotNull();
        assertThat(search(registry, site, "rucksack")).containsExactly("bag");
    }

    @Test
    void anEngineThatKeepsPushedSynonymsIsNotReapplied() {
        TurSNSite site = site("solr-site");
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.supportsSynonyms()).thenReturn(true);
        when(solr.synonymsSurviveRestart()).thenReturn(true);
        stubSite(site, solr);
        when(siteRepository.findAll()).thenReturn(List.of(site));

        reapply(false).onReady();

        verify(solr, never()).applySynonyms(any(), anyString(), any(), anyList());
        verify(synonymService, never()).rulesForApply(any(), any());
    }

    @Test
    void aFailingSiteDoesNotStopTheNext() {
        TurSNSite broken = site("broken");
        TurSNSite ok = site("ok");
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        TurLuceneSearchEnginePlugin plugin = lucene(registry);
        stubSite(broken, plugin);
        stubSite(ok, plugin);
        when(siteRepository.findAll()).thenReturn(List.of(broken, ok));
        when(siteLocaleRepository.findByTurSNSite(broken)).thenThrow(new IllegalStateException("boom"));
        when(siteLocaleRepository.findByTurSNSite(ok)).thenReturn(List.of(locale(CORE)));
        when(synonymService.rulesForApply(ok, EN)).thenReturn(List.of(new TurSESynonymRule(
                TurSNSynonymType.REGULAR, null, List.of("raincoat", "rain jacket"))));

        reapply(false).onReady();

        assertThat(registry.get(CORE)).isNotNull();
    }

    @Test
    void eachTenantsSitesAreReappliedUnderThatTenant() {
        TurTenant tenant = new TurTenant();
        tenant.setId("acme");
        tenant.setStatus(TurTenantStatus.ACTIVE);
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        TurSNSite site = site("acme-site");
        TurLuceneSynonymRegistry registry = new TurLuceneSynonymRegistry();
        stubSite(site, lucene(registry));
        TurTenantContext ctx = new TurTenantContext(props(true));
        List<String> seenUnder = new ArrayList<>();
        when(siteRepository.findAll()).thenAnswer(inv -> {
            seenUnder.add(ctx.getCurrentTenant());
            return List.of(site);
        });
        when(siteLocaleRepository.findByTurSNSite(site)).thenReturn(List.of(locale("acme_en")));
        when(synonymService.rulesForApply(site, EN)).thenReturn(List.of(new TurSESynonymRule(
                TurSNSynonymType.REGULAR, null, List.of("rucksack", "backpack"))));

        reapply(true).onReady();

        assertThat(seenUnder).containsExactly("acme");
        assertThat(registry.get("acme_en")).isNotNull();
    }

    private List<String> search(TurLuceneSynonymRegistry registry, TurSNSite site, String q)
            throws Exception {
        when(fieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                TurSNSiteFieldExt.builder().name("title").type(TurSEFieldType.TEXT).build()));
        TurLuceneQueryBuilder queryBuilder = new TurLuceneQueryBuilder(fieldExtRepository, null,
                siteLocaleRepository, registry, null);
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ(q);
        Query query = queryBuilder.buildQuery(site,
                new TurSEParameters(params, new TurSNSitePostParamsBean()), EN);
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                writer.addDocument(doc("bag", "Hiking backpack"));
                writer.addDocument(doc("coat", "Waterproof jacket"));
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
        doc.add(new TextField("title", title, Field.Store.YES));
        return doc;
    }
}
