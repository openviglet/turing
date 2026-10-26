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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;

/**
 * T1135 — the manifest sets the site's facet behaviour, and only what it declares.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNManifestFacetsConvergerTest {

    private final TurSNManifestFacetsConverger converger = new TurSNManifestFacetsConverger();

    private static TurSNSite site() {
        TurSNSite site = new TurSNSite();
        site.setFacet(1);
        site.setItemsPerFacet(10);
        return site;
    }

    @Test
    void writesWhatTheManifestDeclares() {
        TurSNSite site = site();
        TurSNManifestFacets multiSelect = new TurSNManifestFacets(true, TurSNSiteFacetFieldEnum.AND,
                TurSNSiteFacetFieldEnum.OR, 20);

        assertThat(converger.plan(site, multiSelect)).isTrue();
        assertThat(converger.converge(site, multiSelect)).isTrue();

        assertThat(site.getFacetItemType()).isEqualTo(TurSNSiteFacetFieldEnum.OR);
        assertThat(site.getItemsPerFacet()).isEqualTo(20);
        assertThat(converger.plan(site, multiSelect)).isFalse();
        assertThat(converger.converge(site, multiSelect)).isFalse();
    }

    @Test
    void leavesUndeclaredSettingsAlone() {
        TurSNSite site = site();

        assertThat(converger.converge(site, new TurSNManifestFacets(false, null, null, null))).isTrue();

        assertThat(site.getFacet()).isZero();
        assertThat(site.getItemsPerFacet()).isEqualTo(10);
        assertThat(site.getFacetItemType()).isEqualTo(TurSNSiteFacetFieldEnum.AND);
        assertThat(converger.converge(site, null)).isFalse();
    }

    @Test
    void planReportsAChangeForASiteTheManifestCreates() {
        assertThat(converger.plan(null, new TurSNManifestFacets(null, null, TurSNSiteFacetFieldEnum.OR, null)))
                .isTrue();
        assertThat(converger.plan(null, null)).isFalse();
    }

    @Test
    void refusesADefaultTypeOrAnEmptyFacet() {
        assertThatThrownBy(() -> converger.validate(
                new TurSNManifestFacets(null, TurSNSiteFacetFieldEnum.DEFAULT, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converger.validate(new TurSNManifestFacets(null, null, null, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
