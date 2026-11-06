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

import java.io.InputStream;
import java.net.ServerSocket;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

/**
 * T1247 — an instance whose endpoint is a ZooKeeper ensemble ({@code zk://host:port})
 * reads the SolrCloud cluster state from it: the admin calls reach a live node, the
 * collection is created from Turing's configset, and indexing and search go through
 * a cloud client.
 *
 * <p>SolrCloud with its embedded ZooKeeper publishes the node's own address in the
 * cluster state, so the node runs on a free host port bound one-to-one and announces
 * itself as {@code localhost}: the address the client on the host then reaches.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
class TurSolrZooKeeperIT {

    private static final String COLLECTION = "t1247_zk";

    private static int freePortPair() throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            if (port + 1000 > 65535) {
                continue;
            }
            try (ServerSocket zk = new ServerSocket(port + 1000)) {
                return port;
            } catch (java.io.IOException taken) {
                // try another pair
            }
        }
        throw new IllegalStateException("No free port pair for Solr and its ZooKeeper");
    }

    private static Ports bindings(int... ports) {
        Ports bindings = new Ports();
        for (int port : ports) {
            bindings.bind(ExposedPort.tcp(port), Ports.Binding.bindPort(port));
        }
        return bindings;
    }

    @Test
    void aZooKeeperEndpointCreatesItsCollectionAndSearchesIt() throws Exception {
        int solrPort = freePortPair();
        int zkPort = solrPort + 1000;
        try (GenericContainer<?> solr = new GenericContainer<>(DockerImageName.parse("solr:9.6.1"))
                .withExposedPorts(solrPort, zkPort)
                .withEnv("SOLR_PORT", String.valueOf(solrPort))
                .withEnv("SOLR_HOST", "localhost")
                .withEnv("SOLR_ZK_EMBEDDED_HOST", "0.0.0.0")
                .withCommand("solr-foreground", "-c")
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        (cmd.getHostConfig() == null ? HostConfig.newHostConfig() : cmd.getHostConfig())
                                .withPortBindings(bindings(solrPort, zkPort))))
                .waitingFor(Wait.forHttp("/solr/admin/info/system").forPort(solrPort).forStatusCode(200))) {
            solr.start();

            String endpoint = "zk://localhost:" + zkPort;
            String adminRoot = TurSolrEndpoints.adminRoot(endpoint);
            assertThat(adminRoot).isEqualTo("http://localhost:" + solrPort);

            try (InputStream configSet = getClass().getResourceAsStream("/solr/configsets/en.zip")) {
                TurSolrUtils.createCollection(adminRoot, COLLECTION, configSet, 1);
            }

            TurSolrEndpoints endpoints = TurSolrEndpoints.parse(endpoint);
            try (SolrClient client = endpoints.createClient()) {
                assertThat(client).isInstanceOf(CloudSolrClient.class);
                assertThat(endpoints.baseUrl(client)).isEqualTo("http://localhost:" + solrPort + "/solr");

                SolrInputDocument document = new SolrInputDocument();
                document.addField("id", "t1247-zk-1");
                document.addField("title", "Barraca iglu");
                client.add(COLLECTION, List.of(document));
                client.commit(COLLECTION);

                assertThat(client.query(COLLECTION, new SolrQuery("id:t1247-zk-1")).getResults().getNumFound())
                        .isEqualTo(1);
            }
        }
    }
}
