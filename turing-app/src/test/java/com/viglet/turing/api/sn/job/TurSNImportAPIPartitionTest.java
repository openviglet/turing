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
package com.viglet.turing.api.sn.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.se.TurSEPartition;

/**
 * T1133 — an import from a partitioned key is stamped with its partition, and a
 * document naming another partition is refused.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNImportAPIPartitionTest {

    private static final TurSEPartition A = new TurSEPartition("site_key", "a.example.org");

    private static TurSNJobItems items(TurSNJobAction action, Map<String, Object> attributes) {
        return new TurSNJobItems(new TurSNJobItem(action, List.of("shared"), Locale.US, attributes, null));
    }

    @Test
    void stampsThePartitionOnEachDocument() {
        Map<String, Object> attributes = new HashMap<>(Map.of("id", "post-1"));

        TurSNImportAPI.stamp(A, items(TurSNJobAction.CREATE, attributes));

        assertThat(attributes).containsEntry("site_key", "a.example.org");
    }

    @Test
    void refusesADocumentFromAnotherPartition() {
        Map<String, Object> attributes = new HashMap<>(Map.of("id", "post-1", "site_key", "b.example.org"));

        assertThatThrownBy(() -> TurSNImportAPI.stamp(A, items(TurSNJobAction.CREATE, attributes)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void leavesADeleteUnstamped() {
        Map<String, Object> attributes = new HashMap<>(Map.of("id", "post-1"));

        TurSNImportAPI.stamp(A, items(TurSNJobAction.DELETE, attributes));

        assertThat(attributes).doesNotContainKey("site_key");
    }
}
