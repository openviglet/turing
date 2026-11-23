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
package com.viglet.turing.searchengine.facet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.KeyValue;

import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;

/**
 * T1310 — how a facet combines with the others (its type) and how its selected
 * items combine (its item type), decided in this order: the request's operator,
 * the field's or custom facet's own setting, then the site's. Lifted from sn's
 * {@code TurSNFacetTypes}, which now delegates here, so the engines decide it
 * over SPI values without sn's entities.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSEFacetTypes {

    /** The {@link #typeAndItemType} value of an OR facet whose items OR together. */
    public static final String OR_OR = "OR-OR";
    /** The {@link #typeAndItemType} value of an OR facet whose items AND together. */
    public static final String OR_AND = "OR-AND";

    private TurSEFacetTypes() {
    }

    /** The facet type and the item type, joined as {@code AND-OR}. */
    public static String typeAndItemType(TurSNSiteFacetFieldEnum facetType,
            TurSNSiteFacetFieldEnum facetItemType) {
        return facetType.toString() + "-" + facetItemType.toString();
    }

    /**
     * The facet type: the request's operator, else what a specific field or custom
     * facet sets ({@code configured}), else the site's.
     */
    public static TurSNSiteFacetFieldEnum facetType(TurSNFilterParams filterParams, boolean specificField,
            TurSNSiteFacetFieldEnum configured, TurSNSiteFacetFieldEnum siteFacetType) {
        TurSNFilterQueryOperator operator = filterParams.getOperator();
        if (operatorIsNotEmpty(operator)) {
            return fromOperator(operator);
        }
        if (specificField && !isDefault(configured)) {
            return configured;
        }
        return fromSite(siteFacetType);
    }

    /**
     * The item type: the request's item operator, else what a specific field or
     * custom facet sets ({@code configured}), else the site's.
     */
    public static TurSNSiteFacetFieldEnum facetItemType(TurSNFilterParams filterParams, boolean specificField,
            TurSNSiteFacetFieldEnum configured, TurSNSiteFacetFieldEnum siteFacetItemType) {
        TurSNFilterQueryOperator itemOperator = filterParams.getItemOperator();
        if (operatorIsNotEmpty(itemOperator)) {
            return fromOperator(itemOperator);
        }
        if (specificField && !isDefault(configured)) {
            return configured;
        }
        return fromSite(siteFacetItemType);
    }

    /** A site's facet or item type: {@code OR} stays, anything else is {@code AND}. */
    public static TurSNSiteFacetFieldEnum fromSite(TurSNSiteFacetFieldEnum siteType) {
        return switch (siteType) {
            case OR -> TurSNSiteFacetFieldEnum.OR;
            case null, default -> TurSNSiteFacetFieldEnum.AND;
        };
    }

    public static boolean isDefault(TurSNSiteFacetFieldEnum turSNSiteFacetFieldEnum) {
        return turSNSiteFacetFieldEnum == null
                || turSNSiteFacetFieldEnum.equals(TurSNSiteFacetFieldEnum.DEFAULT);
    }

    /** Whether the request or the site's item type makes this an OR selection. */
    public static boolean isOr(TurSNFilterParams filterParams, TurSNSiteFacetFieldEnum siteFacetItemType) {
        TurSNFilterQueryOperator operator = filterParams.getOperator();
        return (siteFacetItemType.equals(TurSNSiteFacetFieldEnum.OR)
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

    /** {@code OR} for the OR operator, {@code AND} for any other. */
    public static TurSNSiteFacetFieldEnum fromOperator(TurSNFilterQueryOperator operator) {
        return operator.equals(TurSNFilterQueryOperator.OR) ? TurSNSiteFacetFieldEnum.OR
                : TurSNSiteFacetFieldEnum.AND;
    }

    public static boolean operatorIsNotEmpty(TurSNFilterQueryOperator operator) {
        return operator != null && !operator.equals(TurSNFilterQueryOperator.NONE);
    }
}
