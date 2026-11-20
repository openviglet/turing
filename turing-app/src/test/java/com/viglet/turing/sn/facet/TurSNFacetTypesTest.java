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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;

/** T1310 - moved from TurSolrQueryBuilderTest with the logic it covers. */
class TurSNFacetTypesTest {

    @Test
    void fqFieldsExtractsKeys() {
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(List.of("category:books", "type:article"))
                .and(List.of("author:john"))
                .or(List.of("format:pdf"))
                .build();

        List<String> keys = TurSNFacetTypes.fqFields(params);

        assertThat(keys).containsExactly("category", "type", "author", "format");
    }

    @Test
    void typeAndItemTypeUsesSiteDefaults() {
        TurSNSite site = new TurSNSite();
        site.setFacetType(TurSNSiteFacetFieldEnum.AND);
        site.setFacetItemType(TurSNSiteFacetFieldEnum.OR);
        TurSNFilterParams params = TurSNFilterParams.builder()
                .operator(TurSNFilterQueryOperator.NONE)
                .itemOperator(TurSNFilterQueryOperator.NONE)
                .build();

        TurSNFacetTypeContext context = new TurSNFacetTypeContext(site, params);

        assertThat(TurSNFacetTypes.typeAndItemType(context))
                .isEqualTo("AND-OR");
    }

    @Test
    void fqFieldsReturnsEmptyForNullParams() {
        TurSNFilterParams params = TurSNFilterParams.builder().build();
        List<String> keys = TurSNFacetTypes.fqFields(params);
        assertThat(keys).isEmpty();
    }

    @Test
    void fqFieldsExtractsKeysFromAllSources() {
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(List.of("category:books"))
                .and(List.of("author:john"))
                .or(List.of("format:pdf"))
                .build();

        List<String> keys = TurSNFacetTypes.fqFields(params);

        assertThat(keys).containsExactly("category", "author", "format");
    }

    @Test
    void fqFieldsHandlesEmptyLists() {
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(Collections.emptyList())
                .and(Collections.emptyList())
                .or(Collections.emptyList())
                .build();

        List<String> keys = TurSNFacetTypes.fqFields(params);
        assertThat(keys).isEmpty();
    }

    @Test
    void typeAndItemTypeWithParamOverrides() {
        TurSNSite site = new TurSNSite();
        site.setFacetType(TurSNSiteFacetFieldEnum.AND);
        site.setFacetItemType(TurSNSiteFacetFieldEnum.AND);
        TurSNFilterParams params = TurSNFilterParams.builder()
                .operator(TurSNFilterQueryOperator.AND)
                .itemOperator(TurSNFilterQueryOperator.OR)
                .build();

        TurSNFacetTypeContext context = new TurSNFacetTypeContext(site, params);

        assertThat(TurSNFacetTypes.typeAndItemType(context))
                .isEqualTo("AND-OR");
    }

    @Test
    void typeAndItemTypeOrOr() {
        TurSNSite site = new TurSNSite();
        site.setFacetType(TurSNSiteFacetFieldEnum.OR);
        site.setFacetItemType(TurSNSiteFacetFieldEnum.OR);
        TurSNFilterParams params = TurSNFilterParams.builder()
                .operator(TurSNFilterQueryOperator.NONE)
                .itemOperator(TurSNFilterQueryOperator.NONE)
                .build();

        TurSNFacetTypeContext context = new TurSNFacetTypeContext(site, params);

        assertThat(TurSNFacetTypes.typeAndItemType(context))
                .isEqualTo("OR-OR");
    }

    @Test
    void fqFieldsHandlesValuesWithoutColons() {
        TurSNFilterParams params = TurSNFilterParams.builder()
                .defaultValues(List.of("nocolon"))
                .build();

        List<String> keys = TurSNFacetTypes.fqFields(params);
        // No colon means getKeyValueFromColon returns empty Optional -> null key
        assertThat(keys).hasSize(1);
    }

    @Test
    void isDefault() {
        assertThat(TurSNFacetTypes.isDefault(TurSNSiteFacetFieldEnum.DEFAULT)).isTrue();
        assertThat(TurSNFacetTypes.isDefault(null)).isTrue();
        assertThat(TurSNFacetTypes.isDefault(TurSNSiteFacetFieldEnum.AND)).isFalse();
        assertThat(TurSNFacetTypes.isDefault(TurSNSiteFacetFieldEnum.OR)).isFalse();
    }

    @Test
    void operatorIsNotEmpty() {
        assertThat(TurSNFacetTypes.operatorIsNotEmpty(TurSNFilterQueryOperator.AND)).isTrue();
        assertThat(TurSNFacetTypes.operatorIsNotEmpty(TurSNFilterQueryOperator.OR)).isTrue();
        assertThat(TurSNFacetTypes.operatorIsNotEmpty(TurSNFilterQueryOperator.NONE)).isFalse();
        assertThat(TurSNFacetTypes.operatorIsNotEmpty(null)).isFalse();
    }

    @Test
    void fromOperator() {
        assertThat(TurSNFacetTypes.fromOperator(TurSNFilterQueryOperator.OR)).isEqualTo(TurSNSiteFacetFieldEnum.OR);
        assertThat(TurSNFacetTypes.fromOperator(TurSNFilterQueryOperator.AND)).isEqualTo(TurSNSiteFacetFieldEnum.AND);
    }

    @Test
    void facetTypeFromSite() {
        assertThat(TurSNFacetTypes.facetTypeFromSite(siteWithFacetType(TurSNSiteFacetFieldEnum.OR)))
                .isEqualTo(TurSNSiteFacetFieldEnum.OR);
        assertThat(TurSNFacetTypes.facetTypeFromSite(siteWithFacetType(TurSNSiteFacetFieldEnum.AND)))
                .isEqualTo(TurSNSiteFacetFieldEnum.AND);
        assertThat(TurSNFacetTypes.facetTypeFromSite(siteWithFacetType(TurSNSiteFacetFieldEnum.DEFAULT)))
                .isEqualTo(TurSNSiteFacetFieldEnum.AND);
    }

    @Test
    void facetItemTypeFromSite() {
        assertThat(TurSNFacetTypes.facetItemTypeFromSite(siteWithItemType(TurSNSiteFacetFieldEnum.OR)))
                .isEqualTo(TurSNSiteFacetFieldEnum.OR);
        assertThat(TurSNFacetTypes.facetItemTypeFromSite(siteWithItemType(TurSNSiteFacetFieldEnum.AND)))
                .isEqualTo(TurSNSiteFacetFieldEnum.AND);
        assertThat(TurSNFacetTypes.facetItemTypeFromSite(siteWithItemType(TurSNSiteFacetFieldEnum.DEFAULT)))
                .isEqualTo(TurSNSiteFacetFieldEnum.AND);
    }

    private static TurSNSite siteWithFacetType(TurSNSiteFacetFieldEnum type) {
        TurSNSite site = new TurSNSite();
        site.setFacetType(type);
        return site;
    }

    private static TurSNSite siteWithItemType(TurSNSiteFacetFieldEnum type) {
        TurSNSite site = new TurSNSite();
        site.setFacetItemType(type);
        return site;
    }
}
