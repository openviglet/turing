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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.KeyValue;

import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.sn.persistence.dto.field.TurSNSiteFieldExtDto;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFacetFieldEnum;

/**
 * Which AND/OR a facet and its items combine with, resolved from the request's
 * operators, the field's own setting and the site default, in that order (T1310,
 * formerly on {@code TurSolrQueryBuilder}). This is SN semantics, so it lives in sn:
 * the facet renderer reads it, and each engine's query builder reads the same answer.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSNFacetTypes {

    /** The {@link #typeAndItemType} value of an OR facet whose items OR together. */
    public static final String OR_OR = "OR-OR";
    /** The {@link #typeAndItemType} value of an OR facet whose items AND together. */
    public static final String OR_AND = "OR-AND";

    private TurSNFacetTypes() {
    }

    /** The facet type and the item type, joined as {@code AND-OR}. */
    public static String typeAndItemType(TurSNFacetTypeContext context) {
        return facetType(context).toString() + "-" + facetItemType(context).toString();
    }

    public static TurSNSiteFacetFieldEnum facetType(TurSNFacetTypeContext context) {
        return facetType(context, Optional.ofNullable(context.getTurSNSiteFacetFieldExtDto())
                .map(TurSNSiteFieldExtDto::getFacetType)
                .orElse(TurSNSiteFacetFieldEnum.DEFAULT));
    }

    /** The facet type, where {@code configured} is what the field or custom facet sets. */
    public static TurSNSiteFacetFieldEnum facetType(TurSNFacetTypeContext context,
            TurSNSiteFacetFieldEnum configured) {
        TurSNFilterQueryOperator operator = context.getTurSNFilterParams().getOperator();
        if (operatorIsNotEmpty(operator)) {
            return fromOperator(operator);
        }
        if (context.isSpecificField() && !isDefault(configured)) {
            return configured;
        }
        return facetTypeFromSite(context.getTurSNSite());
    }

    public static TurSNSiteFacetFieldEnum facetItemType(TurSNFacetTypeContext context) {
        return facetItemType(context, Optional.ofNullable(context.getTurSNSiteFacetFieldExtDto())
                .map(TurSNSiteFieldExtDto::getFacetItemType)
                .orElse(TurSNSiteFacetFieldEnum.DEFAULT));
    }

    /** The item type, where {@code configured} is what the field or custom facet sets. */
    public static TurSNSiteFacetFieldEnum facetItemType(TurSNFacetTypeContext context,
            TurSNSiteFacetFieldEnum configured) {
        TurSNFilterQueryOperator itemOperator = context.getTurSNFilterParams().getItemOperator();
        if (operatorIsNotEmpty(itemOperator)) {
            return fromOperator(itemOperator);
        }
        if (context.isSpecificField() && !isDefault(configured)) {
            return configured;
        }
        return facetItemTypeFromSite(context.getTurSNSite());
    }

    public static TurSNSiteFacetFieldEnum facetItemTypeFromSite(TurSNSite turSNSite) {
        return switch (turSNSite.getFacetItemType()) {
            case OR -> TurSNSiteFacetFieldEnum.OR;
            case null, default -> TurSNSiteFacetFieldEnum.AND;
        };
    }

    public static boolean isDefault(TurSNSiteFacetFieldEnum turSNSiteFacetFieldEnum) {
        return turSNSiteFacetFieldEnum == null
                || turSNSiteFacetFieldEnum.equals(TurSNSiteFacetFieldEnum.DEFAULT);
    }

    /** Whether the request or the site makes this an OR selection. */
    public static boolean isOr(TurSNFacetTypeContext context) {
        TurSNFilterQueryOperator operator = context.getTurSNFilterParams().getOperator();
        return (context.getTurSNSite().getFacetItemType().equals(TurSNSiteFacetFieldEnum.OR)
                && !operator.equals(TurSNFilterQueryOperator.AND))
                || operator.equals(TurSNFilterQueryOperator.OR);
    }

    /** The field name of every {@code field:value} filter the request carries. */
    public static List<String> fqFields(TurSNFilterParams turSNFilterParams) {
        List<String> fqFields = new ArrayList<>();
        fqFields.addAll(fqFields(turSNFilterParams.getDefaultValues()));
        fqFields.addAll(fqFields(turSNFilterParams.getAnd()));
        fqFields.addAll(fqFields(turSNFilterParams.getOr()));
        return fqFields;
    }

    private static List<String> fqFields(List<String> filterQueries) {
        return Optional.ofNullable(filterQueries)
                .map(fqOpt -> fqOpt.stream()
                        .map(fq -> TurCommonsUtils.getKeyValueFromColon(fq)
                                .map(KeyValue::getKey).orElse(null))
                        .toList())
                .orElseGet(ArrayList::new);
    }

    static TurSNSiteFacetFieldEnum facetTypeFromSite(TurSNSite turSNSite) {
        return switch (turSNSite.getFacetType()) {
            case OR -> TurSNSiteFacetFieldEnum.OR;
            case null, default -> TurSNSiteFacetFieldEnum.AND;
        };
    }

    static TurSNSiteFacetFieldEnum fromOperator(TurSNFilterQueryOperator operator) {
        return operator.equals(TurSNFilterQueryOperator.OR) ? TurSNSiteFacetFieldEnum.OR
                : TurSNSiteFacetFieldEnum.AND;
    }

    static boolean operatorIsNotEmpty(TurSNFilterQueryOperator operator) {
        return operator != null && !operator.equals(TurSNFilterQueryOperator.NONE);
    }
}
