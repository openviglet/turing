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
package com.viglet.turing.searchengine.solr;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.viglet.turing.searchengine.spi.TurSECustomFacet;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;
import com.viglet.turing.sn.facet.TurSNCustomFacetCatalog;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacet;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.ranking.TurSNSiteSearchConfigProvider;

/**
 * T1310 — the Solr tests' site and fields as the site search SPI answers them,
 * mapped from sn's entities by sn's own provider and catalog, so a test states
 * its site the way it always did and the engine reads what production would hand
 * it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSolrSpiFixtures {

    private TurSolrSpiFixtures() {
    }

    /** The site's settings as the SPI answers them. */
    public static TurSESiteSettings settings(TurSNSite site) {
        return site == null ? null : TurSNSiteSearchConfigProvider.toSettings(site);
    }

    /** One field as the SPI answers it. */
    public static TurSESearchField field(TurSNSiteFieldExt field) {
        return TurSNSiteSearchConfigProvider.toSearchField(field);
    }

    /** The fields as the SPI answers them. */
    public static List<TurSESearchField> fields(TurSNSiteFieldExt... fields) {
        return fields(Arrays.asList(fields));
    }

    /** The fields as the SPI answers them. */
    public static List<TurSESearchField> fields(List<TurSNSiteFieldExt> fields) {
        return fields.stream().map(TurSNSiteSearchConfigProvider::toSearchField).toList();
    }

    /** The fields by name, as the SPI's field lists answer them. */
    public static Map<String, TurSESearchField> fieldMap(Map<String, TurSNSiteFieldExt> fields) {
        return fields.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> field(entry.getValue())));
    }

    /** The names of the fields, as the SPI's name lists answer them. */
    public static List<String> names(TurSNSiteFieldExt... fields) {
        return names(Arrays.asList(fields));
    }

    /** The names of the fields, as the SPI's name lists answer them. */
    public static List<String> names(List<TurSNSiteFieldExt> fields) {
        return fields.stream().map(TurSNSiteFieldExt::getName).toList();
    }

    /** The required fields' default values by name, the last of a name winning, as the SPI answers them. */
    public static Map<String, String> required(List<TurSNSiteFieldExt> fields) {
        return fields.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(TurSNSiteFieldExt::getName, TurSNSiteFieldExt::getDefaultValue,
                        (a, b) -> b));
    }

    /** The custom facets on the enabled fields, as the SPI answers them. */
    public static List<TurSECustomFacet> customFacets(List<TurSNSiteCustomFacet> facets,
            List<TurSNSiteFieldExt> enabledFields) {
        return TurSNCustomFacetCatalog.toFieldCustomFacets(facets, enabledFields);
    }
}
