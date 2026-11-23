package com.viglet.turing.sn.web.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.searchengine.TurSearchEnginePlugin;
import com.viglet.turing.searchengine.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.merge.TurSNSiteMergeProviders;
import com.viglet.turing.sn.persistence.model.merge.TurSNSiteMergeProvidersField;
import com.viglet.turing.sn.persistence.repository.merge.TurSNSiteMergeProvidersRepository;

/**
 * Merge providers reach the site's engine only through the plugin contract (T1310).
 */
@ExtendWith(MockitoExtension.class)
class TurSNMergeProvidersProcessTest {

        @Mock
        private TurSearchEnginePluginFactory pluginFactory;

        @Mock
        private TurSNSiteMergeProvidersRepository turSNSiteMergeProvidersRepository;

        private TurSearchEnginePlugin plugin;

        private TurSNMergeProvidersProcess process;

        @BeforeEach
        void setUp() {
                plugin = mock(TurSearchEnginePlugin.class);
                process = new TurSNMergeProvidersProcess(pluginFactory, turSNSiteMergeProvidersRepository);
        }

        private TurSNSite site() {
                TurSNSite site = new TurSNSite();
                site.setName("site1");
                return site;
        }

        private TurSNSiteMergeProviders mergeProviders(TurSNSite site, Locale locale, String overwritten) {
                TurSNSiteMergeProviders mergeProviders = new TurSNSiteMergeProviders();
                mergeProviders.setTurSNSite(site);
                mergeProviders.setProviderFrom("providerA");
                mergeProviders.setProviderTo("providerB");
                mergeProviders.setRelationFrom("relFrom");
                mergeProviders.setRelationTo("relTo");
                mergeProviders.setLocale(locale);
                TurSNSiteMergeProvidersField field = new TurSNSiteMergeProvidersField();
                field.setName(overwritten);
                mergeProviders.setOverwrittenFields(new HashSet<>(Collections.singletonList(field)));
                when(turSNSiteMergeProvidersRepository.findByTurSNSite(site))
                                .thenReturn(Collections.singletonList(mergeProviders));
                when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
                return mergeProviders;
        }

        @Test
        void testMergeDocuments_EmptyProviders() {
                TurSNSite site = new TurSNSite();
                Map<String, Object> attrs = new HashMap<>();

                when(turSNSiteMergeProvidersRepository.findByTurSNSite(site)).thenReturn(Collections.emptyList());

                Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);
                assertEquals(attrs, result);
        }

        @Test
        void testMergeDocuments_ProviderFrom() {
                TurSNSite site = site();
                mergeProviders(site, Locale.US, "field1");

                Map<String, Object> attrs = new HashMap<>();
                attrs.put(TurSNFieldName.SOURCE_APPS, "providerA");
                attrs.put("relFrom", "value1");

                when(plugin.findByFieldValues(eq(site), any(Locale.class), anyMap())).thenReturn(List.of());

                Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);
                assertEquals(attrs, result);
        }

        @Test
        void testMergeDocuments_ProviderTo() {
                TurSNSite site = site();
                mergeProviders(site, Locale.US, "field1");

                Map<String, Object> attrs = new HashMap<>();
                attrs.put(TurSNFieldName.SOURCE_APPS, "providerB");
                attrs.put("relTo", "value1");
                attrs.put(TurSNFieldName.ID, "1");

                when(plugin.findByFieldValues(eq(site), any(Locale.class), anyMap())).thenReturn(List.of());

                Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);
                assertEquals(attrs, result);
        }

        @Test
        void testMergeDocuments_ProviderFromMergesFromAndToResultAndOverwritesFields() {
                TurSNSite site = site();
                mergeProviders(site, Locale.US, "title");

                Map<String, Object> attrs = new HashMap<>();
                attrs.put(TurSNFieldName.SOURCE_APPS, List.of("providerA", "providerX"));
                attrs.put("relFrom", "value1");
                attrs.put("title", "queue-title");

                Map<String, Object> fromAndTo = Map.of(TurSNFieldName.ID, "doc-1",
                                TurSNFieldName.SOURCE_APPS, List.of("providerB"), "title", "search-title");
                when(plugin.findByFieldValues(eq(site), eq(Locale.US), anyMap()))
                                .thenReturn(List.of(), List.of(), List.of(fromAndTo));

                Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);

                assertEquals("queue-title", result.get("title"));
                @SuppressWarnings("unchecked")
                List<String> providers = (List<String>) result.get(TurSNFieldName.SOURCE_APPS);
                assertTrue(providers.contains("providerA"));
                assertTrue(providers.contains("providerB"));
                verify(plugin, never()).deIndex(any(), any(), any());
        }

        @Test
        void testMergeDocuments_ProviderToMergesFromResultAndDeindexesPreviousDocument() {
                TurSNSite site = site();
                mergeProviders(site, Locale.US, "headline");

                Map<String, Object> attrs = new HashMap<>();
                attrs.put(TurSNFieldName.SOURCE_APPS, "providerB");
                attrs.put("relTo", "group-1");
                attrs.put(TurSNFieldName.ID, "id-200");

                Map<String, Object> fromDoc = Map.of(TurSNFieldName.ID, "old-id", "headline", "merged headline",
                                TurSNFieldName.SOURCE_APPS, "providerA");
                when(plugin.findByFieldValues(eq(site), eq(Locale.US), anyMap()))
                                .thenReturn(List.of(fromDoc), List.of());

                Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);

                assertEquals("merged headline", result.get("headline"));
                @SuppressWarnings("unchecked")
                List<String> providers = (List<String>) result.get(TurSNFieldName.SOURCE_APPS);
                assertTrue(providers.contains("providerA"));
                assertTrue(providers.contains("providerB"));
                verify(plugin).deIndex(site, Locale.US, "old-id");
        }

        @Test
        void testMergeDocuments_UsesMergeLocaleWhenLocaleParameterIsNull() {
                TurSNSite site = site();
                mergeProviders(site, Locale.CANADA_FRENCH, "field1");

                Map<String, Object> attrs = new HashMap<>();
                attrs.put(TurSNFieldName.SOURCE_APPS, "providerA");
                attrs.put("relFrom", "value1");

                when(plugin.findByFieldValues(eq(site), eq(Locale.CANADA_FRENCH), anyMap())).thenReturn(List.of());

                Map<String, Object> result = process.mergeDocuments(site, attrs, null);

                assertEquals(attrs, result);
                verify(plugin, times(3)).findByFieldValues(eq(site), eq(Locale.CANADA_FRENCH), anyMap());
        }
}
