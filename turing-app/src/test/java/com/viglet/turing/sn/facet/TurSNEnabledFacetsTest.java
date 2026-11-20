/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.facet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.sn.api.TurSNSiteCustomFacets;
import com.viglet.turing.sn.api.TurSNSiteFieldExts;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacet;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshotService;

/** T1310 - moved from TurSolrQueryBuilderTest with the logic it covers. */
@ExtendWith(MockitoExtension.class)
class TurSNEnabledFacetsTest {

    @Mock
    private TurSNSiteFieldExts turSNSiteFieldExtRepository;

    @Mock
    private TurSNSiteCustomFacets turSNSiteCustomFacetRepository;

    @Mock
    private TurSNSiteSearchSnapshotService snapshotService;

    private TurSNEnabledFacets enabledFacets() {
        return new TurSNEnabledFacets(turSNSiteFieldExtRepository, turSNSiteCustomFacetRepository,
                snapshotService);
    }

    @Test
    void facetFieldsInFilterQueryFiltersEnabledFacets() {
        TurSNSite site = new TurSNSite();
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(List.of("category:books", "other:1"))
                .build();
        TurSNSiteFieldExt categoryFacet = TurSNSiteFieldExt.builder().name("category").facet(1).enabled(1)
                .build();
        TurSNSiteFieldExt typeFacet = TurSNSiteFieldExt.builder().name("type").facet(1).enabled(1)
                .build();
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                .thenReturn(List.of(categoryFacet, typeFacet));
        TurSNFacetTypeContext context = new TurSNFacetTypeContext(site, params);

        List<String> fields = enabledFacets().facetFieldsInFilterQuery(context);

        assertThat(fields).containsExactly("category");
    }

    @Test
    void facetFieldsInFilterQueryIncludesCustomFacetNames() {
        TurSNSite site = new TurSNSite();
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(List.of("price_range:101 - 500", "other:1"))
                .build();

        TurSNSiteFieldExt idFacet = TurSNSiteFieldExt.builder()
                .id("field-1")
                .name("id")
                .build();
        TurSNSiteCustomFacet customFacet = TurSNSiteCustomFacet.builder()
                .name("price_range").turSNSiteFieldExt(idFacet).build();

        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                .thenReturn(List.of(idFacet));
        when(turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(any()))
                .thenReturn(List.of(customFacet));

        List<String> fields = enabledFacets().facetFieldsInFilterQuery(new TurSNFacetTypeContext(site, params));

        assertThat(fields).containsExactly("price_range");
    }

    @Test
    void facetFieldsInFilterQueryIncludesCustomFacetWhenBaseFieldIsNotFacet() {
        TurSNSite site = new TurSNSite();
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(List.of("price_range:101 - 500"))
                .build();

        TurSNSiteFieldExt nonFacetField = TurSNSiteFieldExt.builder()
                .id("field-1")
                .name("id")
                .facet(0)
                .enabled(1)
                .build();
        TurSNSiteCustomFacet customFacet = TurSNSiteCustomFacet.builder()
                .name("price_range").turSNSiteFieldExt(nonFacetField).build();

        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                .thenReturn(List.of(nonFacetField));
        when(turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(any()))
                .thenReturn(List.of(customFacet));

        List<String> fields = enabledFacets().facetFieldsInFilterQuery(new TurSNFacetTypeContext(site, params));

        assertThat(fields).containsExactly("price_range");
    }
}
