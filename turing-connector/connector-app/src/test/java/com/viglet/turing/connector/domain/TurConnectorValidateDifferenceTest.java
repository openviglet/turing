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
package com.viglet.turing.connector.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurConnectorValidateDifference.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurConnectorValidateDifferenceTest {

    @Test
    void testBuilderWithAllFields() {
        Map<String, List<String>> missing = new HashMap<>();
        missing.put("site1", Arrays.asList("missing1", "missing2"));
        missing.put("site2", Arrays.asList("missing3"));

        Map<String, List<String>> extra = new HashMap<>();
        extra.put("site1", Arrays.asList("extra1"));
        extra.put("site2", Arrays.asList("extra2", "extra3"));

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(missing)
                .extra(extra)
                .build();

        assertThat(difference.getMissing()).isEqualTo(missing);
        assertThat(difference.getExtra()).isEqualTo(extra);
    }

    @Test
    void testBuilderWithEmptyMaps() {
        Map<String, List<String>> emptyMissing = Collections.emptyMap();
        Map<String, List<String>> emptyExtra = Collections.emptyMap();

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(emptyMissing)
                .extra(emptyExtra)
                .build();

        assertThat(difference.getMissing()).isEmpty();
        assertThat(difference.getExtra()).isEmpty();
    }

    @Test
    void testBuilderWithNullValues() {
        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(null)
                .extra(null)
                .build();

        assertThat(difference.getMissing()).isNull();
        assertThat(difference.getExtra()).isNull();
    }

    @Test
    void testBuilderMinimal() {
        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .build();

        assertThat(difference.getMissing()).isNull();
        assertThat(difference.getExtra()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder().build();

        Map<String, List<String>> missing = new HashMap<>();
        missing.put("testSite", Arrays.asList("item1", "item2"));

        Map<String, List<String>> extra = new HashMap<>();
        extra.put("testSite", Arrays.asList("item3", "item4"));

        difference.setMissing(missing);
        difference.setExtra(extra);

        assertThat(difference.getMissing()).isEqualTo(missing);
        assertThat(difference.getExtra()).isEqualTo(extra);
    }

    @Test
    void testBuilderChaining() {
        Map<String, List<String>> missing = Collections.singletonMap("site1", Arrays.asList("missing1"));
        Map<String, List<String>> extra = Collections.singletonMap("site1", Arrays.asList("extra1"));

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(missing)
                .extra(extra)
                .build();

        assertThat(difference.getMissing()).containsEntry("site1", Arrays.asList("missing1"));
        assertThat(difference.getExtra()).containsEntry("site1", Arrays.asList("extra1"));
    }

    @Test
    void testMutableMaps() {
        Map<String, List<String>> mutableMissing = new HashMap<>();
        mutableMissing.put("site1", Arrays.asList("initial"));

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(mutableMissing)
                .build();

        // Modify original map
        mutableMissing.put("site2", Arrays.asList("added"));

        // The difference object should reflect the change if it's the same reference
        assertThat(difference.getMissing()).containsKey("site2");
    }

    @Test
    void testComplexDataStructure() {
        Map<String, List<String>> missing = new HashMap<>();
        missing.put("production", Arrays.asList("doc1", "doc2", "doc3"));
        missing.put("staging", Arrays.asList("doc4"));
        missing.put("development", Collections.emptyList());

        Map<String, List<String>> extra = new HashMap<>();
        extra.put("production", Arrays.asList("outdated1"));
        extra.put("staging", Arrays.asList("outdated2", "outdated3", "outdated4"));

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(missing)
                .extra(extra)
                .build();

        assertThat(difference.getMissing()).hasSize(3);
        assertThat(difference.getMissing().get("production")).hasSize(3);
        assertThat(difference.getMissing().get("staging")).hasSize(1);
        assertThat(difference.getMissing().get("development")).isEmpty();

        assertThat(difference.getExtra()).hasSize(2);
        assertThat(difference.getExtra().get("production")).hasSize(1);
        assertThat(difference.getExtra().get("staging")).hasSize(4);
        assertThat(difference.getExtra()).doesNotContainKey("development");
    }

    @Test
    void testWithNestedCollections() {
        List<String> missingItems = Arrays.asList(
                "very-long-document-id-1",
                "very-long-document-id-2",
                "very-long-document-id-3"
        );
        
        Map<String, List<String>> missing = Collections.singletonMap("large-site", missingItems);

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(missing)
                .extra(Collections.emptyMap())
                .build();

        assertThat(difference.getMissing().get("large-site")).containsExactlyElementsOf(missingItems);
        assertThat(difference.getExtra()).isEmpty();
    }

    @Test
    void testLombokGeneratedMethods() {
        Map<String, List<String>> missing1 = Collections.singletonMap("site", Arrays.asList("item"));
        Map<String, List<String>> extra1 = Collections.singletonMap("site", Arrays.asList("item"));

        TurConnectorValidateDifference difference1 = TurConnectorValidateDifference.builder()
                .missing(missing1)
                .extra(extra1)
                .build();

        TurConnectorValidateDifference difference2 = TurConnectorValidateDifference.builder()
                .missing(missing1)
                .extra(extra1)
                .build();

        // Test equals and hashCode (generated by Lombok)
        assertThat(difference1).isEqualTo(difference2);
        assertThat(difference1.hashCode()).isEqualTo(difference2.hashCode());
    }

    @Test
    void testToString() {
        Map<String, List<String>> missing = Collections.singletonMap("site", Arrays.asList("item"));
        Map<String, List<String>> extra = Collections.singletonMap("site", Arrays.asList("item"));

        TurConnectorValidateDifference difference = TurConnectorValidateDifference.builder()
                .missing(missing)
                .extra(extra)
                .build();

        String toString = difference.toString();
        
        assertThat(toString).contains("TurConnectorValidateDifference");
        assertThat(toString).contains("missing");
        assertThat(toString).contains("extra");
    }
}