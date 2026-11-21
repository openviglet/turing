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
package com.viglet.turing.sn.facet;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.viglet.turing.searchengine.facet.TurSECustomFacetSpan;
import com.viglet.turing.searchengine.spi.TurSECustomFacet;
import com.viglet.turing.sn.api.TurSNSiteCustomFacets;
import com.viglet.turing.sn.api.TurSNSiteFieldExts;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacet;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacetItem;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;

/**
 * T1310 — a site's custom facets as the engines read them through the site search
 * SPI: each facet on an enabled field, by position then name, with its labelled
 * items by position then label and the range each stands for, built by
 * {@link TurSNCustomFacetFilterQuery} so one item cannot mean two ranges on two
 * engines. Moved out of the Lucene engine's {@code TurLuceneCustomFacets}, which
 * built the same definitions from sn's entities.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSNCustomFacetCatalog {

    private final TurSNSiteFieldExts turSNSiteFieldExtRepository;
    private final TurSNSiteCustomFacets turSNSiteCustomFacetRepository;

    public TurSNCustomFacetCatalog(TurSNSiteFieldExts turSNSiteFieldExtRepository,
            TurSNSiteCustomFacets turSNSiteCustomFacetRepository) {
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        this.turSNSiteCustomFacetRepository = turSNSiteCustomFacetRepository;
    }

    /** The site's custom facets on enabled fields, by position then name. */
    public List<TurSECustomFacet> customFacets(TurSNSite turSNSite) {
        List<TurSNSiteFieldExt> enabledFields = turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(turSNSite, 1);
        if (enabledFields == null || enabledFields.isEmpty()) {
            return List.of();
        }
        Map<String, TurSNSiteFieldExt> fieldById = enabledFields.stream()
                .filter(field -> field.getId() != null)
                .collect(Collectors.toMap(TurSNSiteFieldExt::getId, Function.identity(), (a, b) -> a));
        Map<String, TurSECustomFacet> byName = turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(enabledFields)
                .stream()
                .filter(facet -> facet.getName() != null && facet.getTurSNSiteFieldExt() != null)
                .filter(facet -> fieldById.containsKey(facet.getTurSNSiteFieldExt().getId()))
                .sorted(Comparator.comparing(TurSNCustomFacetCatalog::position)
                        .thenComparing(TurSNSiteCustomFacet::getName, String::compareToIgnoreCase))
                .collect(Collectors.toMap(TurSNSiteCustomFacet::getName,
                        facet -> toCustomFacet(facet, fieldById.get(facet.getTurSNSiteFieldExt().getId())),
                        (a, b) -> a, java.util.LinkedHashMap::new));
        return List.copyOf(byName.values());
    }

    private static TurSECustomFacet toCustomFacet(TurSNSiteCustomFacet facet, TurSNSiteFieldExt field) {
        List<TurSECustomFacet.Item> items = orderedItems(facet).stream()
                .map(item -> toItem(facet, field, item))
                .toList();
        return new TurSECustomFacet(facet.getName(), facet.getFacetPosition(), items);
    }

    private static TurSECustomFacet.Item toItem(TurSNSiteCustomFacet facet, TurSNSiteFieldExt field,
            TurSNSiteCustomFacetItem item) {
        String fq = TurSNCustomFacetFilterQuery.of(field.getName(), field.getType(), item);
        String endField = facet.getEndField();
        TurSECustomFacetSpan span = endField == null || endField.isBlank() ? null
                : TurSNCustomFacetFilterQuery.span(field.getName(), endField, field.getType(), item);
        return new TurSECustomFacet.Item(item.getLabel(), item.getLabels(), field.getName(), field.getType(),
                fq.substring(fq.indexOf(':') + 1), span);
    }

    private static int position(TurSNSiteCustomFacet facet) {
        Integer position = facet.getFacetPosition();
        return position != null && position > 0 ? position : Integer.MAX_VALUE;
    }

    /** Items with a label, by position then label, as Solr lists them. */
    private static List<TurSNSiteCustomFacetItem> orderedItems(TurSNSiteCustomFacet facet) {
        return facet.getItems() == null ? List.of() : facet.getItems().stream()
                .filter(item -> item.getLabel() != null && !item.getLabel().isBlank())
                .sorted(Comparator.comparing((TurSNSiteCustomFacetItem item) ->
                        item.getPosition() != null && item.getPosition() > 0 ? item.getPosition()
                                : Integer.MAX_VALUE)
                        .thenComparing(TurSNSiteCustomFacetItem::getLabel, String::compareToIgnoreCase))
                .toList();
    }
}
