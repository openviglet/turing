/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.LBSolrClient;
import org.junit.jupiter.api.Test;

/**
 * T1247 — a Solr endpoint is one node or a node list, and the client follows.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSolrEndpointsTest {

    @Test
    void oneUrlIsOneNode() {
        TurSolrEndpoints endpoints = TurSolrEndpoints.parse("http://solr:8983/solr/");

        assertThat(endpoints.urls()).containsExactly("http://solr:8983/solr");
        assertThat(endpoints.isNodeList()).isFalse();
        assertThat(endpoints.isHttp()).isTrue();
    }

    @Test
    void commasOrWhitespaceSeparateANodeList() {
        TurSolrEndpoints endpoints = TurSolrEndpoints
                .parse(" http://a:8983/solr, http://b:8983/solr\nhttp://c:8983/solr ,http://a:8983/solr");

        assertThat(endpoints.urls()).containsExactly("http://a:8983/solr", "http://b:8983/solr",
                "http://c:8983/solr");
        assertThat(endpoints.isNodeList()).isTrue();
        assertThat(endpoints.first()).isEqualTo("http://a:8983/solr");
    }

    @Test
    void nothingNamedIsNoEndpoint() {
        assertThat(TurSolrEndpoints.parse(null)).isNull();
        assertThat(TurSolrEndpoints.parse(" , ")).isNull();
        assertThatThrownBy(() -> new TurSolrEndpoints(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNonHttpNodeIsNotHttp() {
        assertThat(TurSolrEndpoints.parse("http://a:8983/solr,file:///tmp/index").isHttp()).isFalse();
    }

    @Test
    void adminCallsGoToTheFirstNodesRoot() {
        assertThat(TurSolrEndpoints.adminRoot("http://a:8983/solr, http://b:8983/solr"))
                .isEqualTo("http://a:8983");
        assertThat(TurSolrEndpoints.adminRoot("https://solr.example.com/solr")).isEqualTo("https://solr.example.com");
    }

    @Test
    void aZkEndpointIsAZooKeeperEnsembleWithItsChroot() {
        TurSolrEndpoints endpoints = TurSolrEndpoints.parse(" zk://zk1:2181, zk2:2181/solr ");

        assertThat(endpoints.isZooKeeper()).isTrue();
        assertThat(endpoints.zkHosts()).containsExactly("zk1:2181", "zk2:2181");
        assertThat(endpoints.zkChroot()).isEqualTo("/solr");
        assertThat(endpoints.urls()).isEmpty();
        assertThat(endpoints.isHttp()).as("SolrJ reaches it").isTrue();
        assertThat(endpoints.key()).isEqualTo("zk://zk1:2181,zk2:2181/solr");
        assertThat(TurSolrEndpoints.isZooKeeper("ZK://zk1:2181")).isTrue();
        assertThat(TurSolrEndpoints.parse("zk://zk1:2181/").zkChroot()).isNull();
        assertThat(TurSolrEndpoints.isZooKeeper("http://a:8983/solr")).isFalse();
        assertThatThrownBy(endpoints::first).isInstanceOf(IllegalStateException.class);
    }

    /** T1247b — a query side and an update side, each in any form. */
    @Test
    void querySideAndUpdateSideSplitSearchesFromIndexing() throws Exception {
        String endpoint = "query=http://r1:8983/solr, http://r2:8983/solr; update=http://w1:8983/solr";
        TurSolrEndpoints endpoints = TurSolrEndpoints.parse(endpoint);

        assertThat(endpoints.urls()).containsExactly("http://r1:8983/solr", "http://r2:8983/solr");
        assertThat(endpoints.hasUpdateSide()).isTrue();
        assertThat(endpoints.update().urls()).containsExactly("http://w1:8983/solr");
        assertThat(endpoints.key())
                .isEqualTo("query=http://r1:8983/solr,http://r2:8983/solr; update=http://w1:8983/solr");
        assertThat(TurSolrEndpoints.adminRoots(endpoint)).containsExactly("http://w1:8983", "http://r1:8983");
        assertThat(TurSolrEndpoints.adminRoot(endpoint)).as("schema edits go to the update side")
                .isEqualTo("http://w1:8983");
        try (SolrClient query = endpoints.createClient(); SolrClient update = endpoints.createUpdateClient()) {
            assertThat(query).isInstanceOf(LBSolrClient.class);
            assertThat(update).isInstanceOf(HttpJdkSolrClient.class);
        }
    }

    @Test
    void aSplitNamingTheSameNodesTwiceIsNoSplit() {
        assertThat(TurSolrEndpoints.parse("query=http://a:8983/solr; update=http://a:8983/solr/").hasUpdateSide())
                .isFalse();
        assertThat(TurSolrEndpoints.parse("update=http://w:8983/solr").urls()).containsExactly("http://w:8983/solr");
        assertThat(TurSolrEndpoints.parse("http://a:8983/solr").createUpdateClient()).isNull();
    }

    @Test
    void oneNodeGetsItsOwnClientAndANodeListALoadBalancingOne() throws Exception {
        try (SolrClient single = TurSolrEndpoints.parse("http://a:8983/solr").createClient();
                SolrClient list = TurSolrEndpoints.parse("http://a:8983/solr,http://b:8983/solr").createClient()) {
            assertThat(single).isInstanceOf(HttpJdkSolrClient.class);
            assertThat(list).isInstanceOf(LBSolrClient.class);
        }
    }
}
