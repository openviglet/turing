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
package com.viglet.turing.solr;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * T1218 — a Solr core created from the {@code pt} configset Turing ships matches
 * accents as Lucene (T1080, T1194) and Elasticsearch (T1173, T1217) do: an
 * unaccented query finds the accented word, and a title stored with decomposed
 * accents (Unicode NFD) is found by every spelling.
 *
 * <p>In {@code pt} the first held before T1218 too, because the Portuguese light
 * stemmer drops precomposed accents; the combining mark of an NFD title survived it,
 * which is what {@link #decomposedAccentIsFound()} failed on. The {@code en},
 * {@code es} and {@code ca} configsets had no accent folding at all.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TurSolrAccentFoldingIT {

    private static final Path CONFIG_SET = Path.of("..", "containers", "solr", "config", "solr", "configsets",
            "pt").toAbsolutePath().normalize();
    private static final String STAGED_CONFIG_SET = "/tmp/configsets/pt";
    private static final String CORE = "site_pt";

    private final GenericContainer<?> solr = new GenericContainer<>(DockerImageName.parse("solr:9.6.1"))
            .withExposedPorts(8983)
            .withCopyToContainer(MountableFile.forHostPath(CONFIG_SET, 0777), STAGED_CONFIG_SET)
            .waitingFor(Wait.forHttp("/solr/admin/info/system").forStatusCode(200));
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private String host;

    @BeforeAll
    void startAndSeed() throws Exception {
        solr.start();
        host = "http://" + solr.getHost() + ":" + solr.getMappedPort(8983);
        var copy = solr.execInContainer("bash", "-c",
                "mkdir -p /var/solr/data/configsets && cp -r " + STAGED_CONFIG_SET + " /var/solr/data/configsets/pt");
        assertThat(copy.getExitCode()).as(copy.getStderr()).isZero();
        assertThat(TurSolrUtils.createCore(host, CORE, "pt")).as("core created from the pt configset").isTrue();

        String docs = """
                [{"id":"saude","title":"Hospital Boa Saúde"},
                 {"id":"borrowdale","title":"Jaqueta impermea\\u0301vel Borrowdale"},
                 {"id":"other","title":"Mochila de trilha"}]""";
        HttpResponse<String> update = http.send(HttpRequest.newBuilder(
                URI.create(host + "/solr/" + CORE + "/update?commit=true"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(docs)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(update.statusCode()).as(update.body()).isEqualTo(200);
    }

    @AfterAll
    void stop() {
        solr.stop();
    }

    private List<String> search(String q) throws Exception {
        String url = host + "/solr/" + CORE + "/select?defType=edismax&qf=title&q.op=AND&fl=id&q="
                + URLEncoder.encode(q, StandardCharsets.UTF_8);
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        List<String> ids = new ArrayList<>();
        for (JsonNode doc : json.readTree(response.body()).at("/response/docs")) {
            ids.add(doc.get("id").asString());
        }
        return ids.stream().sorted().toList();
    }

    @Test
    @DisplayName("An unaccented query finds the accented word, and the accented one still does")
    void unaccentedFindsAccented() throws Exception {
        assertThat(search("saude")).containsExactly("saude");
        assertThat(search("saúde")).containsExactly("saude");
    }

    @Test
    @DisplayName("A title stored with a decomposed accent is found by every spelling")
    void decomposedAccentIsFound() throws Exception {
        assertThat(search("impermeável")).containsExactly("borrowdale");
        assertThat(search("impermeavel")).containsExactly("borrowdale");
        assertThat(search("jaqueta impermeável")).containsExactly("borrowdale");
    }
}
