/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.viglet.turing.genai.provider.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.viglet.turing.genai.TurRagFilters;
import com.viglet.turing.genai.provider.TurProviderOptionsParser;
import com.viglet.turing.persistence.model.store.TurStoreInstance;

/**
 * T1133 — a partitioned key's chat and answer read only the chunks written with
 * its partition, and that rests on each vector store honouring the metadata
 * filter the RAG retrieval sends ({@code site_key == value}). Every store gets
 * the same three chunks with the same vector, one per partition and one with
 * none, so the nearest neighbour is decided by the filter alone: only the asked
 * partition's chunk may come back.
 *
 * <p>The embedded Lucene store is covered by {@code TurLuceneVectorStoreTest};
 * this runs PgVector, Milvus and Chroma in Docker.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
class TurVectorStorePartitionFilterIT {

    private static final int DIMENSIONS = 8;
    private static final TurProviderOptionsParser OPTIONS = new TurProviderOptionsParser();

    /** A model that maps every text to one vector, so the filter is all that tells chunks apart. */
    private static EmbeddingModel embeddingModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        float[] vector = new float[DIMENSIONS];
        java.util.Arrays.fill(vector, 0.5f);
        lenient().when(model.embed(anyString())).thenReturn(vector);
        lenient().when(model.embed(any(Document.class))).thenReturn(vector);
        lenient().when(model.embed(anyList(), any(EmbeddingOptions.class), any()))
                .thenAnswer(inv -> {
                    List<float[]> vectors = new ArrayList<>();
                    for (int i = 0; i < ((List<?>) inv.getArgument(0)).size(); i++) {
                        vectors.add(vector);
                    }
                    return vectors;
                });
        lenient().when(model.dimensions()).thenReturn(DIMENSIONS);
        return model;
    }

    /** Adds one chunk per partition (and one without), then searches as partition {@code blog-a}. */
    private static void onlyThePartitionComesBack(VectorStore store) throws Exception {
        if (store instanceof InitializingBean bean) {
            bean.afterPropertiesSet();
        }
        String a = UUID.randomUUID().toString();
        store.add(List.of(
                new Document(a, "the spring sale ends Friday", Map.of("site_key", "blog-a")),
                new Document(UUID.randomUUID().toString(), "the spring sale ends Friday",
                        Map.of("site_key", "blog-b")),
                new Document(UUID.randomUUID().toString(), "the spring sale ends Friday",
                        Map.of("source_id", "shared"))));

        List<Document> found = store.similaritySearch(SearchRequest.builder()
                .query("when does the sale end")
                .topK(10)
                .similarityThreshold(0.0)
                .filterExpression(TurRagFilters.buildFilterExpression(Map.of("site_key", List.of("blog-a"))))
                .build());

        assertThat(found).extracting(Document::getId).containsExactly(a);
    }

    private static TurStoreInstance instance(String url, String optionsJson) {
        TurStoreInstance instance = new TurStoreInstance();
        instance.setId("partition-" + UUID.randomUUID());
        instance.setTitle("Partition filter");
        instance.setUrl(url);
        instance.setCollectionName("partition_it");
        instance.setProviderOptionsJson(optionsJson);
        return instance;
    }

    @Test
    void pgVectorHonoursThePartitionFilter() throws Exception {
        try (GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
                .withEnv("POSTGRES_PASSWORD", "turing")
                .withEnv("POSTGRES_DB", "turing")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))) {
            postgres.start();
            String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/turing";
            VectorStore store = new TurPgVectorStoreProvider(OPTIONS).createVectorStore(
                    instance(url, "{\"dimensions\":" + DIMENSIONS + "}"), embeddingModel(), "postgres:turing",
                    null);

            onlyThePartitionComesBack(store);
        }
    }

    @Test
    void milvusHonoursThePartitionFilter() throws Exception {
        String embeddedEtcd = """
                listen-client-urls: http://0.0.0.0:2379
                advertise-client-urls: http://0.0.0.0:2379
                quota-backend-bytes: 4294967296
                auto-compaction-mode: revision
                auto-compaction-retention: '1000'
                """;
        try (GenericContainer<?> milvus = new GenericContainer<>(DockerImageName.parse("milvusdb/milvus:v2.5.4"))
                .withCommand("milvus", "run", "standalone")
                .withEnv("ETCD_USE_EMBED", "true")
                .withEnv("ETCD_DATA_DIR", "/var/lib/milvus/etcd")
                .withEnv("ETCD_CONFIG_PATH", "/milvus/configs/embedEtcd.yaml")
                .withEnv("COMMON_STORAGETYPE", "local")
                .withCopyToContainer(Transferable.of(embeddedEtcd), "/milvus/configs/embedEtcd.yaml")
                .withExposedPorts(19530, 9091)
                .waitingFor(Wait.forHttp("/healthz").forPort(9091).withStartupTimeout(Duration.ofMinutes(4)))) {
            milvus.start();
            String url = "http://" + milvus.getHost() + ":" + milvus.getMappedPort(19530);
            VectorStore store = new TurMilvusStoreProvider(OPTIONS).createVectorStore(
                    instance(url, "{\"embeddingDimension\":" + DIMENSIONS + "}"), embeddingModel(), null, null);

            onlyThePartitionComesBack(store);
        }
    }

    @Test
    void chromaHonoursThePartitionFilter() throws Exception {
        try (GenericContainer<?> chroma = new GenericContainer<>(DockerImageName.parse("chromadb/chroma:0.5.23"))
                .withExposedPorts(8000)
                .waitingFor(Wait.forHttp("/api/v2/heartbeat").forStatusCode(200))) {
            chroma.start();
            TurChromaStoreProvider provider = new TurChromaStoreProvider(OPTIONS);
            TurStoreInstance instance = instance("http://" + chroma.getHost() + ":" + chroma.getMappedPort(8000),
                    "{}");
            provider.createCollection(instance, null, instance.getCollectionName());
            VectorStore store = provider.createVectorStore(instance, embeddingModel(), null);

            onlyThePartitionComesBack(store);
        }
    }
}
