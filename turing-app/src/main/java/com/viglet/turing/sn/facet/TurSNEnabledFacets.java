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
package com.viglet.turing.sn.facet;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.sn.api.TurSNSiteCustomFacets;
import com.viglet.turing.sn.api.TurSNSiteFieldExts;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacet;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshot;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshotService;

/**
 * A site's enabled fields and facets, read from its search snapshot when one is
 * cached and from the repositories otherwise (T1310, formerly on
 * {@code TurSolrQueryBuilder}). The SN response builder and every engine's query
 * builder ask the same question, so they get the same answer from here.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSNEnabledFacets {

    private final TurSNSiteFieldExts turSNSiteFieldExtRepository;
    private final TurSNSiteCustomFacets turSNSiteCustomFacetRepository;
    private final TurSNSiteSearchSnapshotService snapshotService;

    public TurSNEnabledFacets(TurSNSiteFieldExts turSNSiteFieldExtRepository,
            TurSNSiteCustomFacets turSNSiteCustomFacetRepository,
            TurSNSiteSearchSnapshotService snapshotService) {
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        this.turSNSiteCustomFacetRepository = turSNSiteCustomFacetRepository;
        this.snapshotService = snapshotService;
    }

    /**
     * T898 / §LXXI.1 — the one place the cached and the freshly-loaded halves meet.
     * The snapshot holds FieldExtView records now, so the transient entity every
     * downstream caller still takes — the facet contract, the field DTO, the three
     * custom-facet finders — is rebuilt here rather than served from the cache.
     */
    public List<TurSNSiteFieldExt> enabledFields(TurSNSite turSNSite) {
        return snapshotForSite(turSNSite)
                .map(snap -> {
                    TurSNSite cachedSite = snap.site();
                    return snap.enabledFields().stream()
                            .map(field -> TurSNSiteSearchSnapshotService.toEntity(field, cachedSite))
                            .toList();
                })
                .orElseGet(() -> turSNSiteFieldExtRepository
                        .findByTurSNSiteAndEnabled(turSNSite, 1));
    }

    /** The enabled fields that facet, on their own or through a custom facet. */
    public List<TurSNSiteFieldExt> enabledFacets(TurSNSite turSNSite) {
        Optional<TurSNSiteSearchSnapshot> snapOpt = snapshotForSite(turSNSite);
        List<TurSNSiteFieldExt> enabledFields = enabledFields(turSNSite);
        Set<String> fieldIdsWithCustomFacets = snapOpt
                .map(TurSNSiteSearchSnapshot::fieldIdsWithCustomFacets)
                .orElseGet(() -> turSNSiteCustomFacetRepository
                        .findByFieldExtsWithDetails(enabledFields).stream()
                        .map(cf -> cf.getTurSNSiteFieldExt().getId())
                        .collect(Collectors.toSet()));
        return enabledFields.stream()
                .filter(field -> field.getFacet() == 1
                        || fieldIdsWithCustomFacets.contains(field.getId()))
                .toList();
    }

    /** The facets, custom ones included, that the request already filters on. */
    public List<String> facetFieldsInFilterQuery(TurSNFacetTypeContext context) {
        TurSNSite turSNSite = context.getTurSNSite();
        Optional<TurSNSiteSearchSnapshot> snapOpt = snapshotForSite(turSNSite);
        Set<String> enabledFacetNames = enabledFacets(turSNSite).stream()
                .map(TurSNSiteFieldExt::getName)
                .collect(Collectors.toCollection(HashSet::new));
        if (snapOpt.isPresent()) {
            snapOpt.get().customFacetsByFieldId().values().stream()
                    .flatMap(List::stream)
                    .map(TurSNSiteSearchSnapshot.CustomFacetView::name)
                    .filter(StringUtils::hasText)
                    .forEach(enabledFacetNames::add);
        } else {
            turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(enabledFields(turSNSite)).stream()
                    .map(TurSNSiteCustomFacet::getName)
                    .filter(StringUtils::hasText)
                    .forEach(enabledFacetNames::add);
        }
        return TurSNFacetTypes.fqFields(context.getTurSNFilterParams()).stream()
                .filter(enabledFacetNames::contains).distinct().toList();
    }

    private Optional<TurSNSiteSearchSnapshot> snapshotForSite(TurSNSite turSNSite) {
        if (snapshotService == null || turSNSite == null || turSNSite.getName() == null) {
            return Optional.empty();
        }
        return snapshotService.getSnapshot(turSNSite.getName(), null);
    }
}
