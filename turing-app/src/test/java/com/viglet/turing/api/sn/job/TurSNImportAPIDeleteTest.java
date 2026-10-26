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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;

/**
 * T1112 — a DELETE names an id, a type or the fields to delete by, and one that
 * would delete nothing is refused instead of answering 200.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNImportAPIDeleteTest {

    private static TurSNJobItem delete(Map<String, Object> attributes) {
        return new TurSNJobItem(TurSNJobAction.DELETE, List.of("shared"), Locale.US,
                attributes == null ? null : new HashMap<>(attributes), null);
    }

    @Test
    void acceptsAnIdATypeOrFields() {
        assertThatCode(() -> TurSNImportAPI.validateDelete(delete(Map.of("id", "post-1"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> TurSNImportAPI.validateDelete(delete(Map.of("type", "post"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> TurSNImportAPI.validateDelete(delete(Map.of("site_key", "a.example.org"))))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesADeleteOfNothing() {
        assertThatThrownBy(() -> TurSNImportAPI.validateDelete(delete(Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurSNImportAPI.validateDelete(delete(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesTheStarIdThatUsedToDeleteNothing() {
        assertThatThrownBy(() -> TurSNImportAPI.validateDelete(delete(Map.of("id", "*", "type", "*"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fields");
    }

    @Test
    void refusesAnUnusableFieldOrValue() {
        assertThatThrownBy(() -> TurSNImportAPI.validateDelete(delete(Map.of("site key", "a"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurSNImportAPI.validateDelete(delete(Map.of("site_key", List.of("a", "b")))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
