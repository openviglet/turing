/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.searchengine.facet;

import java.util.List;

/**
 * T1224 — a custom facet item's filter over a span that starts at {@code field}
 * and ends at {@code endField}: a document with no end value is the point at its
 * start and matches {@code point} on {@code field}, as without an end field; a
 * document with one matches when every {@code overlap} clause holds. Moved from
 * {@code TurSNCustomFacetFilterQuery} by T1310 so the engines read it without sn.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSECustomFacetSpan(String field, String endField, String point, List<Clause> overlap) {

    /** One condition of a span filter: {@code field} holds {@code value}, a range. */
    public record Clause(String field, String value) {
    }
}
