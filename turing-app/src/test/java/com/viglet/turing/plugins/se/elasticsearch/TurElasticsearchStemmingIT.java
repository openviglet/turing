/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.plugins.se.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.elasticsearch.TurElasticsearchUtils;

/**
 * T1173 — an Elasticsearch core created for a site locale stems its TEXT fields
 * in that language, as Lucene does since T1170, while STRING facet values keep
 * matching exactly. The cores are built through Turing's own
 * {@link TurElasticsearchUtils#createIndex}, because the mapping is under test.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TurElasticsearchStemmingIT {

    private static final GenericContainer<?> ES = new GenericContainer<>(DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch:9.1.0"))
                    .withExposedPorts(9200)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .waitingFor(Wait.forHttp("/_cluster/health")
                            .forStatusCode(200)
                            .withStartupTimeout(java.time.Duration.ofMinutes(4)));

    private static final Map<String, TurSEFieldType> FIELD_TYPES = Map.of(
            "title", TurSEFieldType.TEXT,
            "category", TurSEFieldType.STRING);

    private ElasticsearchClient client;

    @BeforeAll
    void startAndSeed() throws Exception {
        ES.start();
        String url = "http://" + ES.getHost() + ":" + ES.getMappedPort(9200);
        client = new ElasticsearchClient(new RestClientTransport(
                RestClient.builder(HttpHost.create(url)).build(), new JacksonJsonpMapper()));

        TurElasticsearchUtils.createIndex(url, "stem_en", FIELD_TYPES, Locale.US);
        TurElasticsearchUtils.createIndex(url, "stem_pt", FIELD_TYPES, Locale.of("pt", "BR"));
        TurElasticsearchUtils.createIndex(url, "stem_none", FIELD_TYPES);

        for (String index : List.of("stem_en", "stem_none")) {
            add(index, "jacket", "Waterproof jacket", "Waterproof jackets");
            add(index, "boots", "Leather hiking boots", "Footwear");
        }
        add("stem_pt", "jaqueta", "Jaqueta impermeável", "Jaquetas");
        for (String index : List.of("stem_en", "stem_pt", "stem_none")) {
            client.indices().refresh(r -> r.index(index));
        }
    }

    @AfterAll
    void stopContainer() {
        ES.stop();
    }

    private void add(String index, String id, String title, String category) throws Exception {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("title", title);
        doc.put("category", category);
        client.index(i -> i.index(index).id(id).document(doc));
    }

    private List<String> match(String index, String field, String text) throws Exception {
        return client.search(s -> s.index(index).query(q -> q.match(m -> m.field(field).query(text))),
                Map.class).hits().hits().stream().map(h -> h.id()).sorted().toList();
    }

    @Test
    @DisplayName("An en_US core finds the singular title for a plural query, and the reverse")
    void englishStems() throws Exception {
        assertThat(match("stem_en", "title", "jackets")).containsExactly("jacket");
        assertThat(match("stem_en", "title", "boot")).containsExactly("boots");
    }

    @Test
    @DisplayName("A pt_BR core stems in Portuguese and folds accents")
    void portugueseStemsAndFolds() throws Exception {
        assertThat(match("stem_pt", "title", "jaquetas")).containsExactly("jaqueta");
        assertThat(match("stem_pt", "title", "impermeavel")).containsExactly("jaqueta");
    }

    @Test
    @DisplayName("A STRING facet value still matches exactly, never by its stem")
    void facetValuesStayExact() throws Exception {
        assertThat(match("stem_en", "category", "Waterproof jackets")).containsExactly("jacket");
        assertThat(match("stem_en", "category", "Waterproof jacket")).isEmpty();
    }

    @Test
    @DisplayName("A core created without a locale keeps Elasticsearch's standard analyzer")
    void noLocaleNoStemming() throws Exception {
        assertThat(match("stem_none", "title", "jackets")).isEmpty();
        assertThat(match("stem_none", "title", "jacket")).containsExactly("jacket");
    }
}
