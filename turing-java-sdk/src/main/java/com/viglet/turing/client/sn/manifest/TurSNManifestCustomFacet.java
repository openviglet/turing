/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.client.sn.manifest;

import java.util.List;
import java.util.Map;

/**
 * T1084 — a custom facet a {@link TurSNManifest} provisions: named buckets over
 * one field, such as a posting date's last 24 hours, 7 days and 30 days, counted
 * in the site's structured facets.
 *
 * <p>The server matches it to the live facet by {@code field} and
 * {@code name}: absent, it is created; different, its labels and buckets are
 * replaced; equal, nothing happens. A facet the manifest does not declare is
 * left as the console made it.
 *
 * @param field        the site field the buckets filter (required)
 * @param name         the facet name, unique per field (required)
 * @param defaultLabel the label when no locale label matches
 * @param label        per-locale facet labels
 * @param position     the facet's place among the site's custom facets;
 *                     {@code null} appends a new facet and keeps an existing one
 * @param items        the buckets, in order
 * @param endField     (T1224) the site field where a span ends, beside {@code field}
 *                     where it starts: a bucket then holds the documents whose
 *                     span overlaps it, and a bucket open at its start those that
 *                     ended by its end; {@code null} buckets {@code field} alone
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public record TurSNManifestCustomFacet(String field, String name, String defaultLabel,
        Map<String, String> label, Integer position, List<TurSNManifestCustomFacetItem> items,
        String endField) {

    /** The facet as it was before {@link #endField}, kept for existing callers. */
    public TurSNManifestCustomFacet(String field, String name, String defaultLabel,
            Map<String, String> label, Integer position, List<TurSNManifestCustomFacetItem> items) {
        this(field, name, defaultLabel, label, position, items, null);
    }

    /** This facet bucketing the span from {@code field} to {@code endField} (T1224). */
    public TurSNManifestCustomFacet withEndField(String endField) {
        return new TurSNManifestCustomFacet(field, name, defaultLabel, label, position, items, endField);
    }

    /** A facet with its buckets and no labels beyond the default one. */
    public static TurSNManifestCustomFacet of(String field, String name, String defaultLabel,
            List<TurSNManifestCustomFacetItem> items) {
        return new TurSNManifestCustomFacet(field, name, defaultLabel, null, null,
                items == null ? null : List.copyOf(items));
    }
}
