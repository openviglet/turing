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
package com.viglet.turing.searchengine.spi;

import java.util.List;
import java.util.Map;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.facet.TurSECustomFacetSpan;

/**
 * T1310 — one of a site's custom facets (a field's labelled range items) as an
 * engine counts and filters it, its items already in display order.
 *
 * @param name     the facet's name, which a {@code <name>:<label>} filter names
 * @param position the facet's position; {@code null} when the site set none
 * @param items    the items with a label, by position then label
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSECustomFacet(String name, Integer position, List<Item> items) {

    /**
     * One item: its label, its translated labels by locale key, and the range it
     * stands for on {@code field}; {@code span} is set when the facet names an end
     * field.
     *
     * @param label  the label a filter sends back
     * @param labels translated labels keyed by locale ({@code pt_BR}, {@code pt-BR} or {@code pt})
     * @param field  the field the range is on
     * @param type   the field's type
     * @param value  the range, in Solr range syntax
     * @param span   the span filter, or {@code null}
     */
    public record Item(String label, Map<String, String> labels, String field, TurSEFieldType type,
            String value, TurSECustomFacetSpan span) {
    }
}
