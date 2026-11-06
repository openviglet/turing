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

import java.nio.file.Path;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * T1247 — an instance whose endpoint lists two nodes keeps indexing and searching
 * when the first node does not answer: the load-balancing client sets the dead
 * node aside and sends every request to the live one.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
class TurSolrNodeListFailoverIT {

    private static final Path CONFIG_SET = Path.of("..", "containers", "solr", "config", "solr", "configsets",
            "en").toAbsolutePath().normalize();
    private static final String STAGED_CONFIG_SET = "/tmp/configsets/en";
    private static final String CORE = "t1247_en";
    /** Nothing listens on port 1: the first node is down. */
    private static final String DEAD_NODE = "http://127.0.0.1:1/solr";

    @Test
    void aNodeListKeepsAnsweringWhenTheFirstNodeIsDown() throws Exception {
        try (GenericContainer<?> solr = new GenericContainer<>(DockerImageName.parse("solr:9.6.1"))
                .withExposedPorts(8983)
                .withCopyToContainer(MountableFile.forHostPath(CONFIG_SET, 0777), STAGED_CONFIG_SET)
                .waitingFor(Wait.forHttp("/solr/admin/info/system").forStatusCode(200))) {
            solr.start();
            String host = "http://" + solr.getHost() + ":" + solr.getMappedPort(8983);
            TurSolrCoreAdminIT.installConfigSet(solr);
            assertThat(TurSolrUtils.createCore(host, CORE, "en")).isTrue();

            TurSolrEndpoints endpoints = TurSolrEndpoints.parse(DEAD_NODE + ", " + host + "/solr");
            try (SolrClient client = endpoints.createClient()) {
                SolrInputDocument document = new SolrInputDocument();
                document.addField("id", "t1247-1");
                document.addField("title", "Barraca iglu");
                client.add(CORE, document);
                client.commit(CORE);

                for (int i = 0; i < 3; i++) {
                    long found = client.query(CORE, new SolrQuery("id:t1247-1")).getResults().getNumFound();
                    assertThat(found).as("search %d through the node list", i).isEqualTo(1);
                }
            }
        }
    }
}
