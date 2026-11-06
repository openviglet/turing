/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.plugins.se.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstance;
import com.viglet.turing.solr.TurSolrInstanceProcess;

/**
 * T1262 — the standalone writes (the RAG BM25 index, the chat-analytics intent
 * index) reach their core on Solr: a document indexed alone or in a batch is found,
 * and deleting it by id or by field leaves nothing. Each write becomes visible
 * within the configured {@code commitWithin}, with no commit of its own.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TurSolrStandaloneDocumentsIT {

    private static final String CORE = "standalone";

    private static final GenericContainer<?> SOLR = new GenericContainer<>(DockerImageName.parse("solr:9"))
            .withExposedPorts(8983)
            .withCommand("solr-precreate", CORE)
            .waitingFor(Wait.forHttp("/solr/" + CORE + "/admin/ping").forStatusCode(200));

    private TurSolrSearchEnginePlugin plugin;
    private HttpJdkSolrClient client;
    private final TurSEInstance seInstance = new TurSEInstance();

    @BeforeAll
    void start() throws Exception {
        SOLR.start();
        String baseUrl = "http://" + SOLR.getHost() + ":" + SOLR.getMappedPort(8983) + "/solr";
        client = new HttpJdkSolrClient.Builder(baseUrl).build();
        // The node's base URL, no core: the way the instance process builds it.
        TurSolrInstance instance = new TurSolrInstance(client, URI.create(baseUrl).toURL(), CORE);
        TurSolrInstanceProcess instanceProcess = mock(TurSolrInstanceProcess.class);
        when(instanceProcess.initSolrInstance(any(TurSEInstance.class), eq(CORE))).thenReturn(Optional.of(instance));
        TurSolr turSolr = mock(TurSolr.class);
        when(turSolr.getCommitWithin()).thenReturn(200);
        plugin = new TurSolrSearchEnginePlugin(turSolr, instanceProcess,
                new com.viglet.turing.properties.TurConfigProperties(),
                mock(org.springframework.core.io.ResourceLoader.class),
                mock(com.viglet.turing.solr.source.TurSolrInstanceSource.class));
    }

    @AfterAll
    void stop() throws Exception {
        client.close();
        SOLR.stop();
    }

    /** How many documents match {@code q}, once {@code commitWithin} has made the writes visible. */
    private long eventually(String q, long expected) throws Exception {
        long found = -1;
        for (int attempt = 0; attempt < 50; attempt++) {
            found = client.query(CORE, new SolrQuery(q)).getResults().getNumFound();
            if (found == expected) {
                return found;
            }
            Thread.sleep(100);
        }
        return found;
    }

    @Test
    void standaloneWritesReachTheCore() throws Exception {
        assertThat(plugin.indexStandaloneDocument(seInstance, CORE,
                Map.of("id", "chunk-1", "source_s", "a.pdf"))).isTrue();
        assertThat(plugin.indexStandaloneDocuments(seInstance, CORE, List.of(
                Map.of("id", "chunk-2", "source_s", "b.pdf"),
                Map.of("id", "chunk-3", "source_s", "b.pdf")))).isEqualTo(2);
        assertThat(eventually("*:*", 3)).isEqualTo(3);

        assertThat(plugin.deIndexStandalone(seInstance, CORE, "chunk-1")).isTrue();
        assertThat(eventually("id:chunk-1", 0)).isZero();

        assertThat(plugin.deIndexStandaloneByField(seInstance, CORE, "source_s", "b.pdf")).isTrue();
        assertThat(eventually("*:*", 0)).isZero();
    }
}
