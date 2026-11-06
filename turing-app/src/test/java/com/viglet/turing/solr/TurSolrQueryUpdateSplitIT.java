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

import com.viglet.turing.persistence.model.se.TurSEInstance;

/**
 * T1247b — an instance whose endpoint names a query side and an update side
 * indexes through the update node and searches through the query node. The two
 * nodes here do not replicate, so where a document lands shows which side took it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
class TurSolrQueryUpdateSplitIT {

    private static final Path CONFIG_SET = Path.of("..", "containers", "solr", "config", "solr", "configsets",
            "en").toAbsolutePath().normalize();
    private static final String CORE = "t1247b_en";

    private static GenericContainer<?> solr() {
        return new GenericContainer<>(DockerImageName.parse("solr:9.6.1"))
                .withExposedPorts(8983)
                .withCopyToContainer(MountableFile.forHostPath(CONFIG_SET, 0777), "/tmp/configsets/en")
                .waitingFor(Wait.forHttp("/solr/admin/info/system").forStatusCode(200));
    }

    private static String root(GenericContainer<?> solr) {
        return "http://" + solr.getHost() + ":" + solr.getMappedPort(8983);
    }

    private static long count(String node, String q) throws Exception {
        try (SolrClient client = TurSolrEndpoints.parse(node + "/solr").createClient()) {
            return client.query(CORE, new SolrQuery(q)).getResults().getNumFound();
        }
    }

    @Test
    void indexingGoesToTheUpdateSideAndSearchesToTheQuerySide() throws Exception {
        try (GenericContainer<?> query = solr(); GenericContainer<?> update = solr()) {
            query.start();
            update.start();
            TurSolrCoreAdminIT.installConfigSet(query);
            TurSolrCoreAdminIT.installConfigSet(update);
            String endpoint = "query=" + root(query) + "/solr; update=" + root(update) + "/solr";
            for (String node : TurSolrEndpoints.adminRoots(endpoint)) {
                assertThat(TurSolrUtils.createCore(node, CORE, "en")).as("core on %s", node).isTrue();
            }

            TurSEInstance seInstance = new TurSEInstance();
            seInstance.setEndpointUrl(endpoint);
            TurSolrInstanceProcess process = new TurSolrInstanceProcess(null, null, null);
            try {
                TurSolrInstance instance = process.initSolrInstance(seInstance, CORE).orElseThrow();

                SolrInputDocument document = new SolrInputDocument();
                document.addField("id", "t1247b-1");
                instance.getUpdateClient().add(CORE, document);
                instance.getUpdateClient().commit(CORE);

                assertThat(count(root(update), "id:t1247b-1")).as("on the update node").isEqualTo(1);
                assertThat(count(root(query), "id:t1247b-1")).as("not on the query node").isZero();
                assertThat(instance.getSolrClient().query(CORE, new SolrQuery("id:t1247b-1")).getResults()
                        .getNumFound()).as("searches read the query node").isZero();
            } finally {
                process.shutdown();
            }
        }
    }
}
