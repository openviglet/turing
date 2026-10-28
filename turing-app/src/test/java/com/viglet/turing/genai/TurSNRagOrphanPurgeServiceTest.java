package com.viglet.turing.genai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viglet.turing.genai.provider.store.TurGenAiStoreProvider;
import com.viglet.turing.genai.provider.store.TurStoreChunkPage;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.store.TurStoreInstance;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;

/**
 * T1158 — the orphan purge removes only the chunks of documents the engine no
 * longer holds, and refuses when the engine cannot prove it reads ids.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNRagOrphanPurgeServiceTest {

    private TurGenAiContextFactory contextFactory;
    private TurSearchEnginePluginFactory pluginFactory;
    private TurRagContextBuilder ragContextBuilder;
    private TurSearchEnginePlugin plugin;
    private TurGenAiStoreProvider storeProvider;
    private TurRagContextBuilder.RagInfrastructure infra;
    private TurSNRagOrphanPurgeService service;
    private TurSNSite site;
    private TurSNSiteLocale siteLocale;

    @BeforeEach
    void setUp() {
        contextFactory = mock(TurGenAiContextFactory.class);
        pluginFactory = mock(TurSearchEnginePluginFactory.class);
        ragContextBuilder = mock(TurRagContextBuilder.class);
        plugin = mock(TurSearchEnginePlugin.class);
        storeProvider = mock(TurGenAiStoreProvider.class);
        infra = mock(TurRagContextBuilder.RagInfrastructure.class);
        when(infra.storeProvider()).thenReturn(storeProvider);
        when(infra.storeInstance()).thenReturn(new TurStoreInstance());
        when(infra.collectionName()).thenReturn("sample_en");
        TurGenAiContext context = mock(TurGenAiContext.class);
        when(context.isEnabled()).thenReturn(true);
        when(context.getRagInfrastructure()).thenReturn(infra);
        when(contextFactory.build(any(), eq("sample_en"))).thenReturn(context);
        when(pluginFactory.getPluginForSite(any())).thenReturn(plugin);
        when(ragContextBuilder.deleteByMetadata(eq(infra), anyMap())).thenReturn(true);

        site = new TurSNSite();
        site.setName("Sample");
        site.setTurSNSiteGenAi(new TurSNSiteGenAi());
        siteLocale = new TurSNSiteLocale();
        siteLocale.setCore("sample_en");
        siteLocale.setLanguage(Locale.US);
        service = new TurSNRagOrphanPurgeService(contextFactory, pluginFactory, ragContextBuilder);
    }

    private static TurStoreChunkPage.Chunk chunk(String sourceId, Object sites) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put(TurSNGenAi.SOURCE_ID, sourceId);
        if (sites != null) {
            metadata.put(TurSNGenAi.SITES, sites);
        }
        return new TurStoreChunkPage.Chunk(sourceId + "-0", "text", metadata);
    }

    private void storeHolds(TurStoreChunkPage.Chunk... chunks) {
        when(storeProvider.listChunks(any(), any(), eq("sample_en"), eq(0), anyInt(), anyMap()))
                .thenReturn(new TurStoreChunkPage(List.of(chunks), chunks.length));
    }

    private void engineHolds(String... ids) {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (String id : ids) {
            docs.add(Map.of("id", id));
        }
        when(plugin.retrieveDocumentPage(eq(siteLocale), anyMap(), eq(0), eq(1)))
                .thenReturn(docs.isEmpty() ? List.of() : List.of(docs.getFirst()));
        when(plugin.getDocumentsByIds(eq(siteLocale), anyList())).thenAnswer(invocation -> {
            List<String> asked = invocation.getArgument(1);
            return docs.stream().filter(d -> asked.contains(d.get("id"))).toList();
        });
    }

    @Test
    void removesOnlyTheChunksOfDocumentsTheEngineNoLongerHolds() {
        storeHolds(chunk("live", "Sample"), chunk("gone", "Sample"), chunk("gone", "Sample"));
        engineHolds("live");

        TurSNRagOrphanPurgeService.Result result = service.purge(site, siteLocale);

        assertNull(result.refused());
        assertEquals(2, result.scanned());
        assertEquals(1, result.removed());
        verify(ragContextBuilder).deleteByMetadata(infra, Map.of(TurSNGenAi.SOURCE_ID, "gone"));
        verify(ragContextBuilder, never()).deleteByMetadata(infra, Map.of(TurSNGenAi.SOURCE_ID, "live"));
    }

    @Test
    void leavesAnotherSitesChunksAlone() {
        storeHolds(chunk("other-doc", List.of("Other")), chunk("live", List.of("Sample", "Other")));
        engineHolds("live");

        TurSNRagOrphanPurgeService.Result result = service.purge(site, siteLocale);

        assertEquals(1, result.scanned());
        assertEquals(0, result.removed());
        verify(ragContextBuilder, never()).deleteByMetadata(any(), anyMap());
    }

    @Test
    void refusesWhenTheEngineCannotReadDocumentsById() {
        storeHolds(chunk("live", "Sample"));
        when(plugin.retrieveDocumentPage(eq(siteLocale), anyMap(), eq(0), eq(1)))
                .thenReturn(List.of(Map.of("id", "live")));
        when(plugin.getDocumentsByIds(any(), anyList())).thenReturn(List.of());

        TurSNRagOrphanPurgeService.Result result = service.purge(site, siteLocale);

        assertNotNull(result.refused());
        verify(ragContextBuilder, never()).deleteByMetadata(any(), anyMap());
    }

    @Test
    void refusesWhenTheEngineHoldsNothingToVerifyAgainst() {
        storeHolds(chunk("gone", "Sample"));
        engineHolds();

        assertNotNull(service.purge(site, siteLocale).refused());
        verify(ragContextBuilder, never()).deleteByMetadata(any(), anyMap());
    }

    @Test
    void refusesASiteWithoutGenAi() {
        site.setTurSNSiteGenAi(null);

        assertNotNull(service.purge(site, siteLocale).refused());
    }

    @Test
    void aChunkBelongsToTheSiteItNamesOrToTheCollectionWhenItNamesNone() {
        assertTrue(TurSNRagOrphanPurgeService.belongsTo(null, "Sample"));
        assertTrue(TurSNRagOrphanPurgeService.belongsTo("Sample", "Sample"));
        assertTrue(TurSNRagOrphanPurgeService.belongsTo(List.of("A", "Sample"), "Sample"));
        assertFalse(TurSNRagOrphanPurgeService.belongsTo(List.of("A"), "Sample"));
    }
}
