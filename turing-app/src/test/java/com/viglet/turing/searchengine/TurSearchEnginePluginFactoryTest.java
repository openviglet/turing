package com.viglet.turing.searchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.searchengine.persistence.model.TurSEVendor;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * Tests for TurSearchEnginePluginFactory.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
class TurSearchEnginePluginFactoryTest {

    private final InMemorySNSiteSEInstances seInstances = new InMemorySNSiteSEInstances();

    // --- getPlugin ---

    @Test
    void getPluginShouldReturnRegisteredPlugin() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        assertSame(solr, factory.getPlugin("solr"));
        assertSame(solr, factory.getPlugin("SOLR"));
    }

    @Test
    void getPluginShouldFallbackToSolrForUnknown() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        assertSame(solr, factory.getPlugin("unknown"));
    }

    @Test
    void getPluginShouldThrowWhenNoSolrFallback() {
        TurSearchEnginePlugin lucene = mock(TurSearchEnginePlugin.class);
        when(lucene.getPluginType()).thenReturn("lucene");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(lucene), seInstances::pluginTypeOf);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> factory.getPlugin("unknown"));
        assertTrue(ex.getMessage().contains("unknown"));
        assertTrue(ex.getMessage().contains("solr"));
    }

    @Test
    void getPluginShouldSelectCorrectPluginFromMultiple() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePlugin lucene = mock(TurSearchEnginePlugin.class);
        when(lucene.getPluginType()).thenReturn("lucene");
        TurSearchEnginePlugin es = mock(TurSearchEnginePlugin.class);
        when(es.getPluginType()).thenReturn("elasticsearch");

        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(
                List.of(solr, lucene, es));

        assertSame(solr, factory.getPlugin("solr"));
        assertSame(lucene, factory.getPlugin("lucene"));
        assertSame(es, factory.getPlugin("elasticsearch"));
    }

    @Test
    void getPluginShouldBeCaseInsensitive() {
        TurSearchEnginePlugin es = mock(TurSearchEnginePlugin.class);
        when(es.getPluginType()).thenReturn("ElasticSearch");
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");

        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(es, solr), seInstances::pluginTypeOf);

        assertSame(es, factory.getPlugin("elasticsearch"));
        assertSame(es, factory.getPlugin("ELASTICSEARCH"));
    }

    // --- getPluginForSeInstance ---

    @Test
    void getPluginForSeInstanceShouldResolveBySEVendorPlugin() {
        TurSearchEnginePlugin lucene = mock(TurSearchEnginePlugin.class);
        when(lucene.getPluginType()).thenReturn("lucene");
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(lucene, solr), seInstances::pluginTypeOf);

        TurSEVendor vendor = mock(TurSEVendor.class);
        when(vendor.getPlugin()).thenReturn("lucene");
        TurSEInstance seInstance = mock(TurSEInstance.class);
        when(seInstance.getTurSEVendor()).thenReturn(vendor);
        when(seInstance.getId()).thenReturn("se-1");
        TurSNSite site = seInstances.attach(new TurSNSite(), seInstance);

        assertSame(lucene, factory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site)));
    }

    @Test
    void getPluginForSeInstanceShouldFallbackToSolrForNullSite() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        assertSame(solr, factory.getPluginForSeInstance(null));
    }

    @Test
    void getPluginForSeInstanceShouldFallbackToSolrWhenNoSEInstance() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        TurSNSite site = new TurSNSite();

        assertSame(solr, factory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site)));
    }

    @Test
    void getPluginForSeInstanceShouldFallbackToSolrWhenNoVendor() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        TurSEInstance seInstance = mock(TurSEInstance.class);
        when(seInstance.getTurSEVendor()).thenReturn(null);
        when(seInstance.getId()).thenReturn("se-1");
        TurSNSite site = seInstances.attach(new TurSNSite(), seInstance);

        assertSame(solr, factory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site)));
    }

    @Test
    void getPluginForSeInstanceShouldUseVendorIdWhenPluginBlank() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        TurSEVendor vendor = mock(TurSEVendor.class);
        when(vendor.getPlugin()).thenReturn("");
        when(vendor.getId()).thenReturn("solr");
        TurSEInstance seInstance = mock(TurSEInstance.class);
        when(seInstance.getTurSEVendor()).thenReturn(vendor);
        when(seInstance.getId()).thenReturn("se-1");
        TurSNSite site = seInstances.attach(new TurSNSite(), seInstance);

        assertSame(solr, factory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site)));
    }

    @Test
    void getPluginForSeInstanceShouldUseVendorIdWhenPluginNull() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        TurSEVendor vendor = mock(TurSEVendor.class);
        when(vendor.getPlugin()).thenReturn(null);
        when(vendor.getId()).thenReturn("solr");
        TurSEInstance seInstance = mock(TurSEInstance.class);
        when(seInstance.getTurSEVendor()).thenReturn(vendor);
        when(seInstance.getId()).thenReturn("se-1");
        TurSNSite site = seInstances.attach(new TurSNSite(), seInstance);

        assertSame(solr, factory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site)));
    }

    // --- getPluginForInstance ---

    @Test
    void getPluginForInstanceShouldResolveByVendor() {
        TurSearchEnginePlugin es = mock(TurSearchEnginePlugin.class);
        when(es.getPluginType()).thenReturn("elasticsearch");
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(es, solr), seInstances::pluginTypeOf);

        TurSEVendor vendor = mock(TurSEVendor.class);
        when(vendor.getPlugin()).thenReturn("elasticsearch");
        TurSEInstance instance = mock(TurSEInstance.class);
        when(instance.getTurSEVendor()).thenReturn(vendor);

        assertSame(es, factory.getPluginForInstance(instance));
    }

    @Test
    void getPluginForInstanceShouldUseVendorIdWhenPluginBlank() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        TurSEVendor vendor = mock(TurSEVendor.class);
        when(vendor.getPlugin()).thenReturn("");
        when(vendor.getId()).thenReturn("solr");
        TurSEInstance instance = mock(TurSEInstance.class);
        when(instance.getTurSEVendor()).thenReturn(vendor);

        assertSame(solr, factory.getPluginForInstance(instance));
    }

    @Test
    void getPluginForInstanceShouldFallbackToSolrForNull() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        assertSame(solr, factory.getPluginForInstance(null));
    }

    @Test
    void getPluginForInstanceShouldFallbackToSolrWhenNoVendor() {
        TurSearchEnginePlugin solr = mock(TurSearchEnginePlugin.class);
        when(solr.getPluginType()).thenReturn("solr");
        TurSearchEnginePluginFactory factory = new TurSearchEnginePluginFactory(List.of(solr), seInstances::pluginTypeOf);

        TurSEInstance instance = mock(TurSEInstance.class);
        when(instance.getTurSEVendor()).thenReturn(null);

        assertSame(solr, factory.getPluginForInstance(instance));
    }
}
