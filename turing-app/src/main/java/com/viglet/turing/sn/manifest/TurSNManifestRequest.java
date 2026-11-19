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

import java.util.List;

import com.viglet.core.manifest.VigletFieldManifest;
import com.viglet.core.manifest.VigletFieldMigration;
import com.viglet.core.manifest.VigletFieldSpec;

/**
 * What {@code POST /api/sn/manifest} binds (T1084): the neutral
 * {@link VigletFieldManifest}, component for component, plus the Turing-only
 * {@code customFacets}, (T1104) {@code customSorts}, (T1116) {@code genAi} and
 * (T1135) {@code facets}. A manifest without them is the same JSON it always was.
 *
 * <p>T1296 — {@code convergeFields}: {@code true} makes the manifest the whole truth
 * for the fields it declares, so an existing field's {@code facet},
 * {@code mandatory}, {@code description} and facet labels are converged to it
 * instead of kept. Off (absent), an existing field keeps them, as it always has:
 * {@code facet} and {@code mandatory} cannot say "not declared", so converging them
 * by default would switch off a facet someone turned on in the console.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public record TurSNManifestRequest(String name, String description, String seInstanceId,
        String schemaVersion, List<String> locales, List<VigletFieldSpec> fields,
        List<VigletFieldMigration> migrations, List<TurSNManifestCustomFacet> customFacets,
        List<TurSNManifestCustomSort> customSorts, TurSNManifestGenAi genAi, TurSNManifestFacets facets,
        Boolean convergeFields) {

    /** The request as it was before {@link #convergeFields}, kept for existing callers. */
    public TurSNManifestRequest(String name, String description, String seInstanceId,
            String schemaVersion, List<String> locales, List<VigletFieldSpec> fields,
            List<VigletFieldMigration> migrations, List<TurSNManifestCustomFacet> customFacets,
            List<TurSNManifestCustomSort> customSorts, TurSNManifestGenAi genAi, TurSNManifestFacets facets) {
        this(name, description, seInstanceId, schemaVersion, locales, fields, migrations,
                customFacets, customSorts, genAi, facets, null);
    }

    /** T1296 — whether existing fields converge to what the manifest declares. */
    public boolean convergesFields() {
        return Boolean.TRUE.equals(convergeFields);
    }

    /** The request as it was before {@link #facets}, kept for existing callers. */
    public TurSNManifestRequest(String name, String description, String seInstanceId,
            String schemaVersion, List<String> locales, List<VigletFieldSpec> fields,
            List<VigletFieldMigration> migrations, List<TurSNManifestCustomFacet> customFacets,
            List<TurSNManifestCustomSort> customSorts, TurSNManifestGenAi genAi) {
        this(name, description, seInstanceId, schemaVersion, locales, fields, migrations,
                customFacets, customSorts, genAi, null);
    }

    /** The request as it was before {@link #genAi}, kept for existing callers. */
    public TurSNManifestRequest(String name, String description, String seInstanceId,
            String schemaVersion, List<String> locales, List<VigletFieldSpec> fields,
            List<VigletFieldMigration> migrations, List<TurSNManifestCustomFacet> customFacets,
            List<TurSNManifestCustomSort> customSorts) {
        this(name, description, seInstanceId, schemaVersion, locales, fields, migrations,
                customFacets, customSorts, null);
    }

    /** The request with no custom facets, sorts or GenAI, for callers that hold a neutral manifest. */
    public static TurSNManifestRequest of(VigletFieldManifest manifest) {
        return new TurSNManifestRequest(manifest.name(), manifest.description(),
                manifest.seInstanceId(), manifest.schemaVersion(), manifest.locales(),
                manifest.fields(), manifest.migrations(), null, null, null, null, null);
    }

    public List<TurSNManifestCustomSort> customSortsOrEmpty() {
        return customSorts == null ? List.of() : customSorts;
    }

    public VigletFieldManifest toManifest() {
        return new VigletFieldManifest(name, description, seInstanceId, schemaVersion, locales,
                fields, migrations);
    }

    public List<TurSNManifestCustomFacet> customFacetsOrEmpty() {
        return customFacets == null ? List.of() : customFacets;
    }
}
