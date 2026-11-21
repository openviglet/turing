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

import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;

/**
 * T1310 — one of a site's enabled facet fields as an engine reads it to ask for
 * and lay out its buckets.
 *
 * @param name          the field's name
 * @param facetPosition the facet's position; {@code null} when the site set none
 * @param facetItemType how the facet's selected items combine; {@code null} or
 *                      {@code DEFAULT} defers to the site
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSEFacetField(String name, Integer facetPosition, TurSNSiteFacetFieldEnum facetItemType) {
}
