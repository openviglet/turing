/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.searchengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.searchengine.persistence.model.TurSEVendor;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1315 — an SN site names its SE instance by id; these are the reads that
 * turn the id back into the instance and into the engine plugin it runs.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSEInstancePluginTypesTest {

    private final InMemorySNSiteSEInstances seInstances = new InMemorySNSiteSEInstances();

    private static TurSEInstance instance(String id, String vendorId, String plugin) {
        TurSEVendor vendor = new TurSEVendor();
        vendor.setId(vendorId);
        vendor.setPlugin(plugin);
        TurSEInstance instance = new TurSEInstance();
        instance.setId(id);
        instance.setTurSEVendor(vendor);
        return instance;
    }

    @Test
    void thePluginIsTheVendorsPluginElseItsId() {
        seInstances.attach(new TurSNSite(), instance("se-lucene", "LUCENE", "lucene"));
        seInstances.attach(new TurSNSite(), instance("se-legacy", "solr", " "));

        assertThat(seInstances.pluginTypes().of("se-lucene")).isEqualTo("lucene");
        assertThat(seInstances.pluginTypes().of("se-legacy")).isEqualTo("solr");
    }

    @Test
    void aMissingInstanceOrVendorFallsBackToSolr() {
        TurSEInstance noVendor = new TurSEInstance();
        noVendor.setId("se-bare");
        seInstances.attach(new TurSNSite(), noVendor);

        assertThat(seInstances.pluginTypes().of("se-bare")).isEqualTo("solr");
        assertThat(seInstances.pluginTypes().of("se-deleted")).isEqualTo("solr");
    }

    @Test
    void aSiteResolvesTheEndpointOfTheInstanceItNames() {
        TurSEInstance solr = instance("se-solr", "SOLR", "solr");
        solr.setEndpointUrl("http://solr:8983/solr");
        TurSNSite site = seInstances.attach(new TurSNSite(), solr);
        TurSEInstance bare = new TurSEInstance();
        bare.setId("se-bare");
        TurSNSite bareSite = seInstances.attach(new TurSNSite(), bare);

        assertThat(TurSEInstanceEndpoints.ofSite(seInstances.endpoints(), site)).isEqualTo("http://solr:8983/solr");
        // T1337 — nothing null reaches the cache; a missing endpoint reads as none.
        assertThat(seInstances.endpoints().of("se-deleted")).isEmpty();
        assertThat(TurSEInstanceEndpoints.ofSite(seInstances.endpoints(), bareSite)).isNull();
        assertThat(TurSEInstanceEndpoints.ofSite(seInstances.endpoints(), new TurSNSite())).isNull();
    }

    @Test
    void aSiteResolvesTheInstanceItNames() {
        TurSEInstance lucene = instance("se-lucene", "LUCENE", "lucene");
        TurSNSite site = seInstances.attach(new TurSNSite(), lucene);

        assertThat(seInstances.service().of(site)).isSameAs(lucene);
        assertThat(seInstances.service().of(new TurSNSite())).isNull();
        assertThat(seInstances.service().of(null)).isNull();
    }
}
