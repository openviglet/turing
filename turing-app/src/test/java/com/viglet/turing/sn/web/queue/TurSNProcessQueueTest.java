package com.viglet.turing.sn.web.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;
import com.viglet.turing.sn.persistence.repository.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.searchengine.TurSearchEnginePlugin;
import com.viglet.turing.searchengine.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.field.TurSNFieldProvisioner;
import com.viglet.turing.sn.spotlight.TurSNSpotlightProcess;
import com.viglet.turing.sn.api.TurSNSiteRefs;

@ExtendWith(MockitoExtension.class)
class TurSNProcessQueueTest {

    private final com.viglet.turing.searchengine.InMemorySNSiteSEInstances seInstances =
            new com.viglet.turing.searchengine.InMemorySNSiteSEInstances();

    @org.mockito.Spy
    private com.viglet.turing.sn.api.TurSNSiteSEInstances snSiteSEInstances = seInstances.service();

    @Mock
    private TurSearchEnginePluginFactory pluginFactory;
    @Mock
    private TurSNSiteRepository turSNSiteRepository;
    @Mock
    private TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    @Mock
    private TurSNMergeProvidersProcess turSNMergeProvidersProcess;
    @Mock
    private TurSNSpotlightProcess turSNSpotlightProcess;
    @Mock
    private TurSNFieldProvisioner turSNFieldProvisioner;
    @Mock
    private TurSEInstanceRepository turSEInstanceRepository;

    @Mock
    private com.viglet.turing.tenant.TurJmsTenantPropagation turJmsTenantPropagation;
    @Mock
    private com.viglet.turing.sn.ranking.TurSNHybridRankingService turSNHybridRankingService;
    @Mock
    private com.viglet.turing.sn.contentfit.TurSNContentFitIndexer turSNContentFitIndexer;
    @Mock
    private com.viglet.turing.sn.media.TurSNGeminiMediaIndexer turSNGeminiMediaIndexer;
    @Mock
    private com.viglet.turing.sn.kb.TurSNThesaurusEnrichment turSNMicrothesaurusIndexer;

    @Mock
    private com.viglet.turing.sn.entity.TurSNEntityIndexer turSNEntityIndexer;
    @Mock
    private com.viglet.turing.sn.checksum.TurSNChecksumIndexer turSNChecksumIndexer;
    @Mock
    private com.viglet.turing.sn.search.TurSNSearchCacheEvictor turSNSearchCacheEvictor;
    @Mock
    private TurSNProcessQueue self;
    @Mock
    private com.viglet.turing.sn.api.TurSNSiteEmbeddings turSNGenAi;
    @org.mockito.Spy
    private TurSNIndexingIssues indexingIssues = new TurSNIndexingIssues();
    @InjectMocks
    private TurSNProcessQueue processQueue;

    @Test
    void testReceiveIndexingQueue_Create() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setSpecs(new ArrayList<>());

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");

        TurSNSite site = new TurSNSite();
        site.setName("site1");
        seInstances.attach(site, seInstance);

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US))).thenReturn(attrs);
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap())).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin, times(1)).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap());
    }

    @Test
    void testReceiveIndexingQueue_Delete() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.DELETE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.deIndex(TurSNSiteRefs.toSE(site), Locale.US, "1")).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin, times(1)).deIndex(TurSNSiteRefs.toSE(site), Locale.US, "1");
        // T1158 — the deleted document leaves the RAG collection too.
        verify(turSNGenAi).removeDocument(site, Locale.US, "1");
    }

    @Test
    void testReceiveIndexingQueue_Commit() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.COMMIT);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);

        Map<String, Object> attrs = new HashMap<>();
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.commit(TurSNSiteRefs.toSE(site), Locale.US)).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin, times(1)).commit(TurSNSiteRefs.toSE(site), Locale.US);
    }

    private TurSNJobItem createItem(String id) {
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setSpecs(new ArrayList<>());
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, id);
        item.setAttributes(attrs);
        return item;
    }

    @Test
    void testReceiveIndexingQueue_BatchesConsecutiveCreates() {
        // T803 / §LV.1 — two consecutive CREATEs for the same (site, locale) go
        // through the bulk indexDocuments(...) API once + one commit, never the
        // per-doc indexDocument(...).
        TurSNJobItems jobItems = new TurSNJobItems();
        jobItems.add(createItem("1"));
        jobItems.add(createItem("2"));

        TurSNSite site = new TurSNSite();
        site.setName("site1"); // no SE instance → ensureCoreExists is a no-op

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(any())).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocuments(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyList())).thenReturn(2);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin, times(1)).indexDocuments(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyList());
        verify(plugin, times(1)).commit(TurSNSiteRefs.toSE(site), Locale.US);
        verify(plugin, never()).indexDocument(any(), any(), anyMap());
        // T383 — vectors still side-written per doc (no-op mock here).
        verify(turSNHybridRankingService, times(2)).indexDocument(eq(site), eq(Locale.US), anyMap());
    }

    /** T1343 — a value the engine dropped is named on that document's INDEXED log entry. */
    @Test
    void aValueTheEngineDroppedIsNamedOnTheDocumentsIndexedEntry() {
        TurSNJobItems jobItems = new TurSNJobItems();
        jobItems.add(createItem("1"));
        jobItems.add(createItem("2"));
        TurSNSite site = new TurSNSite();
        site.setName("site1");
        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(any())).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocuments(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyList())).thenAnswer(invocation -> {
            com.viglet.turing.searchengine.TurSEDroppedValues.record("2", "releaseDate", "DATE", "2017T00:00:00Z");
            return 2;
        });

        try (var logging = org.mockito.Mockito.mockStatic(com.viglet.turing.system.logging.TurLoggingUtils.class)) {
            processQueue.processIndexingQueue(jobItems);

            logging.verify(() -> com.viglet.turing.system.logging.TurLoggingUtils.setLoggingStatus(
                    org.mockito.ArgumentMatchers.argThat(item -> "2".equals(item.getId())),
                    eq(com.viglet.turing.commons.indexing.TurIndexingStatus.INDEXED),
                    eq(com.viglet.turing.commons.indexing.TurLoggingStatus.SUCCESS),
                    eq("indexed; dropped releaseDate '2017T00:00:00Z' (not a DATE)")));
            org.assertj.core.api.Assertions.assertThat(indexingIssues.countsFor("site1"))
                    .isEqualTo(new TurSNIndexingIssues.Counts(1, 0));
            logging.verify(() -> com.viglet.turing.system.logging.TurLoggingUtils.setLoggingStatus(
                    org.mockito.ArgumentMatchers.argThat(item -> "1".equals(item.getId())),
                    eq(com.viglet.turing.commons.indexing.TurIndexingStatus.INDEXED),
                    eq(com.viglet.turing.commons.indexing.TurLoggingStatus.SUCCESS),
                    org.mockito.ArgumentMatchers.isNull()));
        }
    }

    @Test
    void testReceiveIndexingQueue_BatchConvergesFieldUnionOnce() {
        // T805 / §LV.3 — the union of the batch's specs is converged once per
        // field, not once per document.
        TurSNJobItem item1 = createItem("1");
        TurSNJobItem item2 = createItem("2");
        TurSNJobAttributeSpec spec = TurSNJobAttributeSpec.builder()
                .name("kind").type(TurSEFieldType.STRING).build();
        item1.setSpecs(List.of(spec));
        item2.setSpecs(List.of(spec)); // same field on both docs
        TurSNJobItems jobItems = new TurSNJobItems();
        jobItems.add(item1);
        jobItems.add(item2);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(any())).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocuments(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyList())).thenReturn(2);

        processQueue.processIndexingQueue(jobItems);

        verify(turSNFieldProvisioner, times(1)).ensureField(site, spec);
    }

    @Test
    void testReceiveIndexingQueue_BatchFallsBackToPerItemOnFailure() {
        // T803 — a failing bulk index degrades gracefully to per-item indexing.
        TurSNJobItems jobItems = new TurSNJobItems();
        jobItems.add(createItem("1"));
        jobItems.add(createItem("2"));

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(any())).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocuments(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyList()))
                .thenThrow(new RuntimeException("boom"));
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap())).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin, times(2)).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap());
    }

    /**
     * T1343 — a document the engine refuses is an ERROR on its own indexing-log
     * entry, and the rest of the batch still indexes.
     */
    @Test
    void aRefusedDocumentIsLoggedAsAnErrorAndTheNextOneStillIndexes() {
        TurSNJobItems jobItems = new TurSNJobItems();
        jobItems.add(createItem("1"));
        jobItems.add(createItem("2"));
        TurSNSite site = new TurSNSite();
        site.setName("site1");
        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(any())).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocuments(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyList())).thenThrow(new RuntimeException("bulk refused"));
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap()))
                .thenThrow(new com.viglet.turing.searchengine.TurSEDocumentRefusedException("1",
                        "Invalid Date String:'2017T00:00:00Z'", null))
                .thenReturn(true);

        try (var logging = org.mockito.Mockito.mockStatic(com.viglet.turing.system.logging.TurLoggingUtils.class)) {
            processQueue.processIndexingQueue(jobItems);

            logging.verify(() -> com.viglet.turing.system.logging.TurLoggingUtils.setErrorStatus(
                    org.mockito.ArgumentMatchers.argThat(item -> "1".equals(item.getId())),
                    eq(com.viglet.turing.commons.indexing.TurIndexingStatus.NOT_PROCESSED),
                    eq("refused by the search engine: Invalid Date String:'2017T00:00:00Z'")));
            org.assertj.core.api.Assertions.assertThat(indexingIssues.countsFor("SITE1"))
                    .isEqualTo(new TurSNIndexingIssues.Counts(0, 1));
            logging.verify(() -> com.viglet.turing.system.logging.TurLoggingUtils.setLoggingStatus(
                    org.mockito.ArgumentMatchers.argThat(item -> "2".equals(item.getId())),
                    eq(com.viglet.turing.commons.indexing.TurIndexingStatus.INDEXED),
                    eq(com.viglet.turing.commons.indexing.TurLoggingStatus.SUCCESS),
                    org.mockito.ArgumentMatchers.isNull()));
        }
        verify(plugin, times(2)).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap());
    }

    @Test
    void testRemoveDuplicateTerms() {
        Map<String, Object> attrs = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("term1");
        list.add("term1");
        list.add("term2");
        attrs.put("field1", list);

        Map<String, Object> result = processQueue.removeDuplicateTerms(attrs);
        List<?> resultList = (List<?>) result.get("field1");

        assertTrue(resultList.contains("term1"));
        assertTrue(resultList.contains("term2"));
        assertEquals(2, resultList.size());
    }

    @Test
    void testReceiveIndexingQueue_CreateSpotlight() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setAttributes(Map.of(TurSNFieldName.ID, "1"));
        item.setSpecs(new ArrayList<>());
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(true);
        when(turSNSpotlightProcess.createUnmanagedSpotlight(item, site)).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(turSNSpotlightProcess).createUnmanagedSpotlight(item, site);
        verify(pluginFactory, never()).getPluginForSeInstance(any());
    }

    @Test
    void testReceiveIndexingQueue_DeleteSpotlight() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.DELETE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setAttributes(Map.of(TurSNFieldName.ID, "1"));
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(true);
        when(turSNSpotlightProcess.deleteUnmanagedSpotlight(item, site)).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(turSNSpotlightProcess).deleteUnmanagedSpotlight(item, site);
        verify(pluginFactory, never()).getPluginForSeInstance(any());
    }

    @Test
    void testReceiveIndexingQueue_DeleteByType() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.DELETE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setAttributes(Map.of(TurSNFieldName.TYPE, "news"));
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.deIndexByType(TurSNSiteRefs.toSE(site), Locale.US, "news")).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin).deIndexByType(TurSNSiteRefs.toSE(site), Locale.US, "news");
        verify(plugin, never()).deIndex(any(), any(), any());
        // T1158 — the type's RAG chunks go with it.
        verify(turSNGenAi).removeDocuments(site, Locale.US, Map.of(TurSNFieldName.TYPE, "news"));
    }

    /** T1112 — a DELETE naming fields deletes the documents holding them all. */
    @Test
    void testReceiveIndexingQueue_DeleteByFields() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.DELETE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setAttributes(Map.of("site_key", "a.example.org"));
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.deIndexByFields(TurSNSiteRefs.toSE(site), Locale.US, Map.of("site_key", "a.example.org"))).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin).deIndexByFields(TurSNSiteRefs.toSE(site), Locale.US, Map.of("site_key", "a.example.org"));
        verify(plugin, never()).deIndex(any(), any(), any());
        verify(plugin, never()).deIndexByType(any(), any(), any());
        // T1158 — and every document holding those fields leaves the RAG collection.
        verify(turSNGenAi).removeDocuments(site, Locale.US, Map.of("site_key", "a.example.org"));
    }

    @Test
    void testReceiveIndexingQueue_CommitPluginReturnsFalse() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.COMMIT);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setAttributes(new HashMap<>());
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.commit(TurSNSiteRefs.toSE(site), Locale.US)).thenReturn(false);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin).commit(TurSNSiteRefs.toSE(site), Locale.US);
    }

    @Test
    void testRemoveDuplicateTermsHandlesNullAndSimpleValues() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("nullField", null);
        attrs.put("single", "value");

        Map<String, Object> result = processQueue.removeDuplicateTerms(attrs);

        assertTrue(result.containsKey("single"));
        assertTrue(!result.containsKey("nullField"));
        assertTrue(processQueue.removeDuplicateTerms(null).isEmpty());
    }

    @Test
    void testRemoveDuplicateTerms_ConservativeGroundingDropsEmptyAndBlankValues() {
        // T381 / §XX.1 — the indexing boundary must NOT materialize an absent
        // field as an empty value: null, "", whitespace-only, and empty
        // collections are dropped so facets and exists-filters stay correct.
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("present", "value");
        attrs.put("nullField", null);
        attrs.put("emptyString", "");
        attrs.put("blankString", "   ");
        attrs.put("emptyList", new ArrayList<>());

        Map<String, Object> result = processQueue.removeDuplicateTerms(attrs);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("present"));
        assertTrue(!result.containsKey("nullField"));
        assertTrue(!result.containsKey("emptyString"));
        assertTrue(!result.containsKey("blankString"));
        assertTrue(!result.containsKey("emptyList"));
    }

    @Test
    void testIsGroundedValue_AbsentVsPresentContract() {
        // T381 — pin the absent-vs-present predicate directly.
        assertTrue(!TurSNProcessQueue.isGroundedValue(null));
        assertTrue(!TurSNProcessQueue.isGroundedValue(""));
        assertTrue(!TurSNProcessQueue.isGroundedValue("   "));
        assertTrue(!TurSNProcessQueue.isGroundedValue(new ArrayList<>()));
        assertTrue(TurSNProcessQueue.isGroundedValue("x"));
        assertTrue(TurSNProcessQueue.isGroundedValue(0));
        assertTrue(TurSNProcessQueue.isGroundedValue(Boolean.FALSE));
        assertTrue(TurSNProcessQueue.isGroundedValue(List.of("a")));
    }

    @Test
    void testReceiveIndexingQueue_CreateDropsEmptyAttributesBeforeIndexing() {
        // T381 — end-to-end: a CREATE job carrying a present id plus an
        // empty-string field indexes only the grounded field; the empty one is
        // never handed to the search-engine plugin as "".
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setSpecs(new ArrayList<>());
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        attrs.put("absentInSource", "");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");
        TurSNSite site = new TurSNSite();
        site.setName("site1");
        seInstances.attach(site, seInstance);

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        // mergeDocuments echoes back whatever the consolidation step produced,
        // so the captured map is exactly what conservative grounding kept.
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap())).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(plugin).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), captor.capture());
        Map<String, Object> indexed = captor.getValue();
        assertTrue(indexed.containsKey(TurSNFieldName.ID));
        assertTrue(!indexed.containsKey("absentInSource"));
    }

    @Test
    void testReceiveIndexingQueue_CreateDelegatesMissingFieldsToProvisioner() {
        // T382 — the queue no longer creates fields inline; it delegates every
        // declared attribute spec to the shared TurSNFieldProvisioner (the same
        // idempotent path the manifest provisioning uses). Deep field-creation
        // behavior is asserted in TurSNFieldProvisionerTest.
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setAttributes(Map.of(TurSNFieldName.ID, "1"));

        TurSNJobAttributeSpec spec = TurSNJobAttributeSpec.builder()
                .name("customField")
                .type(TurSEFieldType.STRING)
                .description("Custom")
                .multiValued(false)
                .facet(true)
                .facetName(Map.of("default", "Custom", "pt_BR", "Personalizado"))
                .build();
        item.setSpecs(List.of(spec));
        jobItems.add(item);

        TurSEInstance siteSeInstance = new TurSEInstance();
        siteSeInstance.setId("se1");
        TurSNSite site = new TurSNSite();
        site.setName("site1");
        seInstances.attach(site, siteSeInstance);

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US)))
                .thenReturn(new HashMap<>(item.getAttributes()));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap())).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(turSNFieldProvisioner).ensureField(site, spec);
        verify(plugin).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap());
    }

    @Test
    void testReceiveIndexingQueue_CreateProvisionsMissingCoreOnDemand() {
        // T335 / §XIV.8.2 — a new tenant's first index whose prefixed core was
        // never provisioned (standalone Solr topology) self-heals: the queue
        // creates the core on demand before indexing the document.
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setSpecs(new ArrayList<>());
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");
        seInstance.setTitle("Solr");

        TurSNSite site = new TurSNSite();
        site.setName("site1");
        seInstances.attach(site, seInstance);

        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setCore("t1234abcd_site1_en");
        locale.setTurSNSite(site);

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(site, Locale.US)).thenReturn(locale);
        when(turSEInstanceRepository.findById("se1")).thenReturn(Optional.of(seInstance));
        when(pluginFactory.getPluginForInstance(seInstance)).thenReturn(plugin);
        when(plugin.indexExists(seInstance, "t1234abcd_site1_en")).thenReturn(false);
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US))).thenReturn(attrs);
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap())).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin).createIndex(eq(seInstance), eq(TurSNSiteRefs.toSE(locale)), eq("t1234abcd_site1_en"), anyMap());
        verify(plugin).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap());
    }

    @Test
    void testReceiveIndexingQueue_CreateSkipsProvisioningWhenCoreExists() {
        // T335 — when the core is already present the queue does NOT re-create
        // it (one indexExists check, no createIndex round-trip).
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setSpecs(new ArrayList<>());
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");
        seInstance.setTitle("Solr");

        TurSNSite site = new TurSNSite();
        site.setName("site1");
        seInstances.attach(site, seInstance);

        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setCore("core_en");
        locale.setTurSNSite(site);

        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);

        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(site, Locale.US)).thenReturn(locale);
        when(turSEInstanceRepository.findById("se1")).thenReturn(Optional.of(seInstance));
        when(pluginFactory.getPluginForInstance(seInstance)).thenReturn(plugin);
        when(plugin.indexExists(seInstance, "core_en")).thenReturn(true);
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US))).thenReturn(attrs);
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap())).thenReturn(true);

        processQueue.processIndexingQueue(jobItems);

        verify(plugin, never()).createIndex(any(), any(), any(), anyMap());
        verify(plugin).indexDocument(eq(TurSNSiteRefs.toSE(site)), eq(Locale.US), anyMap());
    }
}
