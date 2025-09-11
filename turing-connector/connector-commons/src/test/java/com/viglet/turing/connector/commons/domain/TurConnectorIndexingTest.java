/*
 * Copyright (C) 2016-2025 the original author or authors.
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
package com.viglet.turing.connector.commons.domain;

import com.viglet.turing.commons.indexing.TurIndexingStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurConnectorIndexing.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurConnectorIndexingTest {

    @Test
    void testBuilderWithAllFields() {
        int id = 123;
        String objectId = "test-object-id";
        String source = "test-source";
        String environment = "test-environment";
        String transactionId = "test-transaction-id";
        String checksum = "test-checksum";
        Locale locale = Locale.ENGLISH;
        Date created = new Date();
        Date modificationDate = new Date();
        TurIndexingStatus status = TurIndexingStatus.INDEXING;
        List<String> sites = Arrays.asList("site1", "site2");

        TurConnectorIndexing indexing = TurConnectorIndexing.builder()
                .id(id)
                .objectId(objectId)
                .source(source)
                .environment(environment)
                .transactionId(transactionId)
                .checksum(checksum)
                .locale(locale)
                .created(created)
                .modificationDate(modificationDate)
                .status(status)
                .sites(sites)
                .build();

        assertThat(indexing.getId()).isEqualTo(id);
        assertThat(indexing.getObjectId()).isEqualTo(objectId);
        assertThat(indexing.getSource()).isEqualTo(source);
        assertThat(indexing.getEnvironment()).isEqualTo(environment);
        assertThat(indexing.getTransactionId()).isEqualTo(transactionId);
        assertThat(indexing.getChecksum()).isEqualTo(checksum);
        assertThat(indexing.getLocale()).isEqualTo(locale);
        assertThat(indexing.getCreated()).isEqualTo(created);
        assertThat(indexing.getModificationDate()).isEqualTo(modificationDate);
        assertThat(indexing.getStatus()).isEqualTo(status);
        assertThat(indexing.getSites()).isEqualTo(sites);
    }

    @Test
    void testBuilderWithMinimalFields() {
        String objectId = "minimal-object-id";
        String source = "minimal-source";

        TurConnectorIndexing indexing = TurConnectorIndexing.builder()
                .objectId(objectId)
                .source(source)
                .build();

        assertThat(indexing.getObjectId()).isEqualTo(objectId);
        assertThat(indexing.getSource()).isEqualTo(source);
        assertThat(indexing.getId()).isZero();
        assertThat(indexing.getEnvironment()).isNull();
        assertThat(indexing.getTransactionId()).isNull();
        assertThat(indexing.getChecksum()).isNull();
        assertThat(indexing.getLocale()).isNull();
        assertThat(indexing.getCreated()).isNull();
        assertThat(indexing.getModificationDate()).isNull();
        assertThat(indexing.getStatus()).isNull();
        assertThat(indexing.getSites()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        TurConnectorIndexing indexing = TurConnectorIndexing.builder().build();

        int id = 456;
        String objectId = "setter-object-id";
        String source = "setter-source";
        String environment = "setter-environment";
        String transactionId = "setter-transaction-id";
        String checksum = "setter-checksum";
        Locale locale = Locale.FRENCH;
        Date created = new Date();
        Date modificationDate = new Date();
        TurIndexingStatus status = TurIndexingStatus.INDEXED;
        List<String> sites = Arrays.asList("site3", "site4");

        indexing.setId(id);
        indexing.setObjectId(objectId);
        indexing.setSource(source);
        indexing.setEnvironment(environment);
        indexing.setTransactionId(transactionId);
        indexing.setChecksum(checksum);
        indexing.setLocale(locale);
        indexing.setCreated(created);
        indexing.setModificationDate(modificationDate);
        indexing.setStatus(status);
        indexing.setSites(sites);

        assertThat(indexing.getId()).isEqualTo(id);
        assertThat(indexing.getObjectId()).isEqualTo(objectId);
        assertThat(indexing.getSource()).isEqualTo(source);
        assertThat(indexing.getEnvironment()).isEqualTo(environment);
        assertThat(indexing.getTransactionId()).isEqualTo(transactionId);
        assertThat(indexing.getChecksum()).isEqualTo(checksum);
        assertThat(indexing.getLocale()).isEqualTo(locale);
        assertThat(indexing.getCreated()).isEqualTo(created);
        assertThat(indexing.getModificationDate()).isEqualTo(modificationDate);
        assertThat(indexing.getStatus()).isEqualTo(status);
        assertThat(indexing.getSites()).isEqualTo(sites);
    }

    @Test
    void testBuilderWithNullValues() {
        TurConnectorIndexing indexing = TurConnectorIndexing.builder()
                .objectId(null)
                .source(null)
                .environment(null)
                .transactionId(null)
                .checksum(null)
                .locale(null)
                .created(null)
                .modificationDate(null)
                .status(null)
                .sites(null)
                .build();

        assertThat(indexing.getObjectId()).isNull();
        assertThat(indexing.getSource()).isNull();
        assertThat(indexing.getEnvironment()).isNull();
        assertThat(indexing.getTransactionId()).isNull();
        assertThat(indexing.getChecksum()).isNull();
        assertThat(indexing.getLocale()).isNull();
        assertThat(indexing.getCreated()).isNull();
        assertThat(indexing.getModificationDate()).isNull();
        assertThat(indexing.getStatus()).isNull();
        assertThat(indexing.getSites()).isNull();
    }

    @Test
    void testBuilderChaining() {
        TurConnectorIndexing indexing = TurConnectorIndexing.builder()
                .id(789)
                .objectId("chained-object-id")
                .source("chained-source")
                .environment("chained-environment")
                .build();

        assertThat(indexing.getId()).isEqualTo(789);
        assertThat(indexing.getObjectId()).isEqualTo("chained-object-id");
        assertThat(indexing.getSource()).isEqualTo("chained-source");
        assertThat(indexing.getEnvironment()).isEqualTo("chained-environment");
    }
}