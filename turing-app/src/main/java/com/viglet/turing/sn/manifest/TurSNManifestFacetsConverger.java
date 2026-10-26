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

package com.viglet.turing.sn.manifest;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;

/**
 * Converges the site-level facet behaviour an SN manifest declares (T1135), so a
 * connector that provisions its site as code chooses drill-down or multi-select
 * facets without the console. Only the components the manifest declares are
 * compared and written; a manifest without a {@code facets} block leaves the
 * site's facets alone.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Component
public class TurSNManifestFacetsConverger {

    /** Refuses a facet type of {@code DEFAULT}, which only a field's item type may be, and an empty facet. */
    public void validate(TurSNManifestFacets facets) {
        if (facets == null) {
            return;
        }
        if (facets.type() == TurSNSiteFacetFieldEnum.DEFAULT) {
            throw new IllegalArgumentException("Manifest facets.type must be AND or OR");
        }
        if (facets.itemsPerFacet() != null && facets.itemsPerFacet() < 1) {
            throw new IllegalArgumentException("Manifest facets.itemsPerFacet must be at least 1");
        }
    }

    /**
     * Whether {@link #converge} would change the site. Read-only.
     *
     * @param site the live site, or {@code null} when the manifest creates it
     */
    public boolean plan(TurSNSite site, TurSNManifestFacets facets) {
        return facets != null && (site == null || !matches(site, facets));
    }

    /**
     * Writes the declared components onto the site when they differ.
     *
     * @return {@code true} when the site's facet behaviour changed
     */
    public boolean converge(TurSNSite site, TurSNManifestFacets facets) {
        if (facets == null || matches(site, facets)) {
            return false;
        }
        if (facets.enabled() != null) {
            site.setFacet(Boolean.TRUE.equals(facets.enabled()) ? 1 : 0);
        }
        if (facets.type() != null) {
            site.setFacetType(facets.type());
        }
        if (facets.itemType() != null) {
            site.setFacetItemType(facets.itemType());
        }
        if (facets.itemsPerFacet() != null) {
            site.setItemsPerFacet(facets.itemsPerFacet());
        }
        return true;
    }

    private static boolean matches(TurSNSite site, TurSNManifestFacets facets) {
        return (facets.enabled() == null || facets.enabled() == (site.getFacet() != null && site.getFacet() == 1))
                && (facets.type() == null || facets.type() == site.getFacetType())
                && (facets.itemType() == null || facets.itemType() == site.getFacetItemType())
                && (facets.itemsPerFacet() == null
                        || Objects.equals(facets.itemsPerFacet(), site.getItemsPerFacet()));
    }
}
