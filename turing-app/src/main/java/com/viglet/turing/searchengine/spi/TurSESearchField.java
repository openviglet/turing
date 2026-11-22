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
package com.viglet.turing.searchengine.spi;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;

import lombok.Builder;

/**
 * T1310 — one of a site's enabled fields as an engine reads it while it builds a
 * query and shapes its results: what to search, weigh, facet on and highlight.
 * The flags keep the site's own encoding ({@code 1} is on).
 *
 * @param name          the field's name
 * @param type          the field's type; {@code null} when the site set none
 * @param facet         whether the field is a facet
 * @param hl            whether the field is highlighted
 * @param facetPosition the facet's position; {@code null} when the site set none
 * @param facetItemType how the facet's selected items combine; {@code null} or
 *                      {@code DEFAULT} defers to the site
 * @param searchWeight  how much a match in the field weighs; {@code null} is 1
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Builder
public record TurSESearchField(String name, TurSEFieldType type, Integer facet, Integer hl,
        Integer facetPosition, TurSNSiteFacetFieldEnum facetItemType, Float searchWeight) {
}
