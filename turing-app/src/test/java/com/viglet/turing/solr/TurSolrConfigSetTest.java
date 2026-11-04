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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.viglet.turing.lucene.TurLuceneNfcCharFilter;

/**
 * T1218 — the Solr configsets ship in two copies: the directories baked into the
 * Solr image ({@code containers/solr/.../configsets}, used by a standalone core)
 * and the zips Turing uploads to SolrCloud ({@code solr/configsets/*.zip}). They
 * had drifted (the zips lacked T1185's {@code text_phrase}), so this keeps them
 * equal, and keeps the generated {@code mapping-nfc.txt} equal to
 * {@link TurLuceneNfcCharFilter#PAIRS}.
 *
 * <p>Regenerate both after changing a configset or the pairs:
 * {@code mvn test -pl turing-app -Dskip.npm=true -Dtest=TurSolrConfigSetTest -Dturing.regenerate=true}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSolrConfigSetTest {

    private static final Path CONFIGSETS = Path.of("..", "containers", "solr", "config", "solr", "configsets");
    private static final Path ZIPS = Path.of("src", "main", "resources", "solr", "configsets");
    static final String MAPPING_FILE = "mapping-nfc.txt";
    private static final boolean REGENERATE = Boolean.getBoolean("turing.regenerate");

    /** {@link TurLuceneNfcCharFilter#PAIRS} in Solr's {@code MappingCharFilterFactory} syntax. */
    static String nfcMapping() {
        StringBuilder out = new StringBuilder()
                .append("# T1218 - generated from TurLuceneNfcCharFilter.PAIRS by TurSolrConfigSetTest;\n")
                .append("# do not edit by hand. Recomposes decomposed (Unicode NFD) accents before the\n")
                .append("# tokenizer, as Lucene (T1194) and Elasticsearch (T1217) do.\n");
        TurLuceneNfcCharFilter.PAIRS.forEach((decomposed, composed) -> out.append('"')
                .append(escaped(decomposed)).append("\" => \"").append(escaped(composed)).append("\"\n"));
        return out.toString();
    }

    private static String escaped(String text) {
        StringBuilder out = new StringBuilder();
        text.chars().forEach(c -> out.append(String.format("\\u%04X", c)));
        return out.toString();
    }

    @ParameterizedTest
    @ValueSource(strings = { "ca", "en", "es", "pt" })
    @DisplayName("Each configset carries the NFC mapping generated from the Lucene table")
    void mappingIsCurrent(String locale) throws IOException {
        Path mapping = CONFIGSETS.resolve(locale).resolve("conf").resolve(MAPPING_FILE);
        if (REGENERATE) {
            Files.writeString(mapping, nfcMapping(), StandardCharsets.UTF_8);
        }
        assertThat(Files.readString(mapping, StandardCharsets.UTF_8)).isEqualTo(nfcMapping());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ca", "en", "es", "pt" })
    @DisplayName("The SolrCloud zip holds exactly the configset the Solr image ships")
    void zipMatchesTheImageConfigSet(String locale) throws IOException {
        Path conf = CONFIGSETS.resolve(locale).resolve("conf");
        Path zip = ZIPS.resolve(locale + ".zip");
        if (REGENERATE) {
            // The mapping is part of the configset, so it is current before the zip is built.
            Files.writeString(conf.resolve(MAPPING_FILE), nfcMapping(), StandardCharsets.UTF_8);
        }
        Map<String, byte[]> expected = directory(conf);
        if (REGENERATE) {
            writeZip(zip, expected);
        }
        Map<String, byte[]> actual = zip(zip);
        assertThat(actual.keySet()).containsExactlyInAnyOrderElementsOf(expected.keySet());
        expected.forEach((name, bytes) -> assertThat(actual.get(name)).as(locale + ".zip " + name).isEqualTo(bytes));
    }

    private static Map<String, byte[]> directory(Path conf) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(conf)) {
            for (Path file : walk.filter(Files::isRegularFile).sorted().toList()) {
                files.put(conf.relativize(file).toString().replace('\\', '/'), Files.readAllBytes(file));
            }
        }
        return files;
    }

    private static Map<String, byte[]> zip(Path zip) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (InputStream in = Files.newInputStream(zip); ZipInputStream entries = new ZipInputStream(in)) {
            for (ZipEntry entry; (entry = entries.getNextEntry()) != null;) {
                if (!entry.isDirectory()) {
                    files.put(entry.getName().replace('\\', '/'), entries.readAllBytes());
                }
            }
        }
        return files;
    }

    private static void writeZip(Path zip, Map<String, byte[]> files) throws IOException {
        try (var out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            for (String name : List.copyOf(files.keySet())) {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0L);
                out.putNextEntry(entry);
                out.write(files.get(name));
                out.closeEntry();
            }
        }
    }
}
