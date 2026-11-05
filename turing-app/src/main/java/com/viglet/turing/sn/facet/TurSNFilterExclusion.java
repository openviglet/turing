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

package com.viglet.turing.sn.facet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * T1233 — a negative filter query, {@code -field:value}: the documents whose
 * {@code field} holds {@code value} are left out. Read the same way by every
 * engine, from {@code fq}, {@code fqAnd} and {@code fqOr} alike, so an exclusion
 * always narrows the search and is never grouped with the field's positive values
 * (which {@code fqOr} or {@code fqItemOperator} would turn into a union) nor shown
 * as a selected facet item.
 *
 * @param field the field whose value is excluded
 * @param value the value, unquoted
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
public record TurSNFilterExclusion(String field, String value) {

    private static final String NEGATION = "-";

    /** The exclusion {@code fq} spells, or empty when it is no {@code -field:value}. */
    public static Optional<TurSNFilterExclusion> of(String fq) {
        if (fq == null || !fq.startsWith(NEGATION)) {
            return Optional.empty();
        }
        String positive = fq.substring(1);
        int colon = positive.indexOf(':');
        if (colon <= 0 || colon >= positive.length() - 1) {
            return Optional.empty();
        }
        String value = positive.substring(colon + 1);
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return Optional.of(new TurSNFilterExclusion(positive.substring(0, colon), value));
    }

    /** Whether {@code fq} is an exclusion. */
    public static boolean isExclusion(String fq) {
        return of(fq).isPresent();
    }

    /** Every exclusion in the lists, in order. */
    @SafeVarargs
    public static List<TurSNFilterExclusion> in(List<String>... lists) {
        List<TurSNFilterExclusion> exclusions = new ArrayList<>();
        for (List<String> list : lists) {
            if (list != null) {
                list.forEach(fq -> of(fq).ifPresent(exclusions::add));
            }
        }
        return exclusions;
    }

    /** The positive {@code field:value} this exclusion negates, as a positive fq spells it. */
    public String positive() {
        return field + ":" + value;
    }
}
