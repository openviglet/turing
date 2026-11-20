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

package com.viglet.turing.sn.field;

/**
 * The rule for a field's search weight, shared by the field console API and the
 * field manifest (T1072), so neither depends on the other (T1308, ADR 0005).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSNSearchWeight {

    private TurSNSearchWeight() {
    }

    /**
     * T1065 — a search weight is a multiplier from 1 (no change, the same as
     * empty) to 100. Below 1 is refused: Solr applies the weight as an extra boost
     * query, which can raise a field but never lower one, so a demotion would
     * behave differently per engine.
     */
    public static Float valid(Float weight) {
        if (weight == null) {
            return null;
        }
        if (weight.isNaN() || weight < 1f || weight > 100f) {
            throw new IllegalArgumentException("searchWeight must be between 1 and 100, or empty");
        }
        return weight;
    }
}
