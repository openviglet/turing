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
package com.viglet.turing.client.sn.manifest;

/**
 * T1135 — the site-level facet behaviour a manifest declares. Every component is
 * optional: {@code null} leaves the live setting as it is.
 *
 * @param enabled       whether the site returns facets at all
 * @param type          {@code AND} (drill-down across fields) or {@code OR}
 * @param itemType      {@code OR} (multi-select: an applied facet stays
 *                      visible), {@code AND}, or {@code DEFAULT} to follow
 *                      {@code type}
 * @param itemsPerFacet how many values each facet lists
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public record TurSNManifestFacets(Boolean enabled, String type, String itemType, Integer itemsPerFacet) {

    /** Facets on, with multi-select values: a visitor can pick several values of one facet. */
    public static TurSNManifestFacets multiSelect() {
        return new TurSNManifestFacets(true, "AND", "OR", null);
    }

    /** Facets on, each selection narrowing the results further. */
    public static TurSNManifestFacets drillDown() {
        return new TurSNManifestFacets(true, "AND", "AND", null);
    }

    /** The same behaviour listing {@code items} values per facet. */
    public TurSNManifestFacets withItemsPerFacet(int items) {
        return new TurSNManifestFacets(enabled, type, itemType, items);
    }
}
