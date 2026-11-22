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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;

/**
 * T1310 — the site configuration an engine reads while it builds a query, declared
 * here and implemented by the module that owns the sites (ADR 0005 section 6,
 * option 1 of T1310). The engines keep building queries; they reach the
 * configuration through this interface instead of importing {@code sn}.
 *
 * <p>Each method takes the site's id and answers in engine-neutral records.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSESiteSearchConfig {

    /**
     * The site's ranking expressions, ordered by name ignoring case, each with its
     * conditions; empty when the site has none.
     */
    List<TurSERankingExpression> rankingExpressions(String siteId);

    /**
     * The levels of the site's custom sort called {@code name}, in position order
     * (levels without a position last); empty when the site has no sort by that
     * name.
     */
    Optional<List<TurSECustomSortItem>> customSort(String siteId, String name);

    /**
     * The type of each of the site's fields by name; where two fields share a name,
     * the first in name order ignoring case wins.
     */
    Map<String, TurSEFieldType> fieldTypes(String siteId);

    /**
     * The site's index fields by name, read while a document is written; where two
     * fields share a name, the last one read wins. Empty for a {@code null} id.
     */
    Map<String, TurSEIndexField> indexFields(String siteId);

    /**
     * The SE instance and core of the site called {@code siteName} (ignoring case)
     * in {@code locale}; empty when there is no such site or it has no such locale.
     */
    Optional<TurSESiteCore> siteCore(String siteName, Locale locale);

    /**
     * The language of the first site locale whose core is {@code core} and names a
     * language; empty when none does.
     */
    Optional<Locale> coreLanguage(String core);

    /** The id of the site called {@code siteName}, ignoring case; empty when there is none. */
    Optional<String> siteId(String siteName);

    /**
     * The site-wide settings of the site called {@code siteName}, ignoring case;
     * empty when there is no such site.
     */
    Optional<TurSESiteSettings> siteSettings(String siteName);

    /** The site's enabled fields; empty when the site is unknown or the id {@code null}. */
    List<TurSESearchField> enabledFields(String siteId);

    /**
     * The names of the site's enabled fields flagged for MoreLikeThis; empty when
     * the site is unknown, has none, or the id is {@code null}.
     */
    List<String> mltFields(String siteId);

    /** The site locales that write to {@code core}; empty when none does. */
    List<TurSECoreUsage> coreUsage(String core);

    /** Whether any site locale writes to {@code core}, so the core must not be deleted. */
    boolean coreInUse(String core);

    /**
     * Applies the first of the site's enabled search rules whose conditions all
     * match {@code params} in {@code locale}, rewriting {@code params} in place.
     */
    void applySearchRules(String siteId, TurSEParameters params, Locale locale);

    /**
     * The facet position of each of the site's enabled facet fields by name, in the
     * order the site sorts its facets; empty when the site is unknown.
     */
    Map<String, Integer> facetPositions(String siteId);

    /** The site's enabled facet fields in facet-position order; empty for a {@code null} id. */
    List<TurSEFacetField> facetFields(String siteId);

    /**
     * The site's custom facets on enabled fields, by position then name; empty when
     * the site is unknown or has none.
     */
    List<TurSECustomFacet> customFacets(String siteId);
}
