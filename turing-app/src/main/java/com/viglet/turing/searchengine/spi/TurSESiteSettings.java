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
import com.viglet.turing.searchengine.TurSNSiteFacetSortEnum;

import lombok.Builder;

/**
 * T1310 — the site-wide settings an engine reads while it runs a search: paging,
 * highlighting, facets, wildcard, exact match, spell check and more-like-this,
 * and the default fields. The flags keep the site's own encoding ({@code 1} is
 * on); {@link com.viglet.turing.searchengine.TurSEFlags#isTrue} reads them.
 *
 * @param id                the site's id
 * @param name              the site's name
 * @param turSEInstanceId   the SE instance the site writes to
 * @param rowsPerPage       results per page when the request names none
 * @param wildcardNoResults whether a query that matched nothing is retried with a wildcard
 * @param wildcardAlways    whether every query gets a trailing wildcard
 * @param exactMatch        whether a quoted query searches the exact-match field
 * @param exactMatchField   the field a quoted query searches
 * @param facet             whether the site returns facets
 * @param itemsPerFacet     how many items each facet lists
 * @param facetItemType     how the selected items of a facet combine by default
 * @param facetType         how the site's facets combine with each other by default
 *                          (T1310: Solr's filter query and facet exclusion read it)
 * @param facetSort         how a facet's items are ordered when the field sets no order
 * @param hl                whether the site highlights matches
 * @param hlPre             the tag before a highlighted match
 * @param hlPost            the tag after a highlighted match
 * @param mlt               whether the site answers more-like-this
 * @param spellCheck        whether the site checks the query's spelling
 * @param spellCheckFixes   whether a misspelled query is corrected
 * @param defaultTitleField the field that holds a document's title
 * @param defaultDateField  the field that holds a document's date
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Builder
public record TurSESiteSettings(String id, String name, String turSEInstanceId, Integer rowsPerPage,
        Integer wildcardNoResults, Integer wildcardAlways, Integer exactMatch, String exactMatchField,
        Integer facet, Integer itemsPerFacet, TurSNSiteFacetFieldEnum facetItemType, Integer hl,
        String hlPre, String hlPost, Integer mlt, Integer spellCheck, Integer spellCheckFixes,
        String defaultTitleField, String defaultDateField, TurSNSiteFacetFieldEnum facetType,
        TurSNSiteFacetSortEnum facetSort) {
}
