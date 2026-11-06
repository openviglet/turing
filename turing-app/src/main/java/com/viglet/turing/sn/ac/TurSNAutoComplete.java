/*
 * Copyright (C) 2016-2022 the original author or authors.
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
package com.viglet.turing.sn.ac;

import java.net.URI;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.viglet.turing.api.sn.search.TurSNSiteSearchCachedAPI;
import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchBean;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchResultsBean;
import com.viglet.turing.commons.sn.search.TurSNSearchCacheKey;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;
import com.viglet.turing.se.TurSEStopWord;
import com.viglet.turing.sn.TurSNUtils;
import com.viglet.turing.solr.TurSolrInstanceProcess;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurSNAutoComplete {
    private static final String SPACE_CHAR = " ";
    /** T1226 — how many suggester candidates per answered row a scoped request draws. */
    private static final long CANDIDATE_FACTOR = 5;
    private static final long MIN_CANDIDATES = 50;
    /** T1226 — how many in-scope documents the candidates are matched against. */
    private static final int SCOPE_ROWS = 100;
    private final TurSEStopWord turSEStopword;
    private final TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI;
    private final TurSearchEnginePluginFactory pluginFactory;
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSolrInstanceProcess turSolrInstanceProcess;
    private final TurSNAutoCompleteTopTerms topTerms;

    public TurSNAutoComplete(TurSEStopWord turSEStopword,
            TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI,
            TurSearchEnginePluginFactory pluginFactory,
            TurSNSiteRepository turSNSiteRepository,
            TurSolrInstanceProcess turSolrInstanceProcess,
            TurSNAutoCompleteTopTerms topTerms) {
        this.turSEStopword = turSEStopword;
        this.turSNSiteSearchCachedAPI = turSNSiteSearchCachedAPI;
        this.pluginFactory = pluginFactory;
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSolrInstanceProcess = turSolrInstanceProcess;
        this.topTerms = topTerms;
    }

    @NotNull
    public List<String> autoCompleteWithRegularSearch(String siteName,
            TurSNSearchParams turSNSearchParams, HttpServletRequest request) {
        return autoCompleteWithRegularSearch(siteName, turSNSearchParams,
                TurSNUtils.requestToURI(request));
    }

    /**
     * T712 / §XLV — the POST-driven flavour of
     * {@link #autoCompleteWithRegularSearch(String, TurSNSearchParams, HttpServletRequest)}.
     * A JSON body carries no query string, so the search URI has to be supplied
     * by the caller (the GET flavour derives it from the request).
     *
     * <p>
     * T858 / §LXV.3 — the cache key is no longer a parameter: it is derived from
     * the context built below, <em>after</em> the autocomplete rewrite of
     * {@code q}, so the key describes the search that actually runs and no caller
     * can forget to pass one.
     * </p>
     *
     * <p>
     * T1226 — a scoped autocomplete (filter queries, or a key bound to a partition)
     * answers the same shape as an unscoped one: term completions, not document
     * titles. The engine's term suggester reads the whole index, so its candidates
     * are kept only when they occur in the documents the scope admits: one search
     * for the typed prefix inside the scope, highlighting off, whose documents'
     * text the candidates are matched against as whole words.
     * </p>
     *
     * @param uri the canonical GET-equivalent URI for this request
     * @since 2026.3.4
     */
    @NotNull
    public List<String> autoCompleteWithRegularSearch(String siteName,
            TurSNSearchParams turSNSearchParams, URI uri) {
        String q = turSNSearchParams.getQ();
        if (q == null || q.trim().length() <= 1) {
            return Collections.emptyList();
        }
        long rows = turSNSearchParams.getRows() >= 0 ? turSNSearchParams.getRows() : 0;
        Locale locale = turSNSearchParams.getLocale();
        List<String> candidates = autoComplete(siteName, q, locale,
                Math.max(rows * CANDIDATE_FACTOR, MIN_CANDIDATES));
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        String scopeText = scopeText(siteName, turSNSearchParams, uri, q);
        if (scopeText.isEmpty()) {
            return Collections.emptyList();
        }
        return candidates.stream()
                .filter(candidate -> containsWords(scopeText, candidate))
                .limit(rows)
                .toList();
    }

    /** The folded text of the documents in scope that hold the typed prefix. */
    private String scopeText(String siteName, TurSNSearchParams turSNSearchParams, URI uri, String q) {
        TurSNConfig turSNConfig = new TurSNConfig();
        turSNConfig.setHlEnabled(false);
        String trimmed = q.trim();
        turSNSearchParams.setQ(q.endsWith(SPACE_CHAR) ? trimmed : trimmed + "*");
        turSNSearchParams.setGroup(null);
        turSNSearchParams.setNfpr(0);
        turSNSearchParams.setP(1);
        turSNSearchParams.setRows(SCOPE_ROWS);
        TurSNSitePostParamsBean postParams = new TurSNSitePostParamsBean();
        TurSNSiteSearchContext context = new TurSNSiteSearchContext(siteName, turSNConfig,
                new TurSEParameters(turSNSearchParams, postParams), turSNSearchParams.getLocale(),
                uri, postParams);
        StringBuilder text = new StringBuilder();
        Optional.ofNullable(turSNSiteSearchCachedAPI.searchCached(TurSNSearchCacheKey.of(context), context))
                .map(TurSNSiteSearchBean::getResults)
                .map(TurSNSiteSearchResultsBean::getDocument)
                .ifPresent(documents -> documents.forEach(document -> Optional.ofNullable(document.getFields())
                        .ifPresent(fields -> fields.values().forEach(value -> appendText(text, value)))));
        return fold(text.toString());
    }

    private static void appendText(StringBuilder text, Object value) {
        if (value instanceof Collection<?> values) {
            values.forEach(item -> appendText(text, item));
        } else if (value instanceof CharSequence chars) {
            text.append(' ').append(chars);
        }
    }

    /** Whether {@code candidate}'s words occur in {@code foldedText} as a whole-word phrase. */
    static boolean containsWords(String foldedText, String candidate) {
        String words = fold(candidate).trim();
        if (words.isEmpty()) {
            return false;
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(words) + "(?![\\p{L}\\p{N}])")
                .matcher(foldedText).find();
    }

    /** Lower case, accents removed and runs of whitespace collapsed, as a suggester term reads. */
    static String fold(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return decomposed.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public List<String> autoComplete(String siteName, String q, Locale locale, long rows) {
        if (q.length() <= 1) {
            return Collections.emptyList();
        }
        return turSNSiteRepository.findByNameIgnoreCase(siteName).map(turSNSite -> {
            var plugin = pluginFactory.getPluginForSite(turSNSite);
            List<String> rawSuggestions = plugin.autoComplete(siteName, q, locale, rows);

            int numberOfWordsFromQuery = q.split(SPACE_CHAR).length;
            if (q.endsWith(SPACE_CHAR)) {
                numberOfWordsFromQuery++;
            }

            List<String> stopWords = turSolrInstanceProcess.initSolrInstance(siteName, locale)
                    .map(turSEStopword::getStopWords)
                    .orElse(Collections.emptyList());
            TurSNSuggestionFilter suggestionFilter = new TurSNSuggestionFilter(stopWords);
            suggestionFilter.automatonStrategyConfig(numberOfWordsFromQuery);
            List<String> filtered = suggestionFilter.filter(rawSuggestions);

            List<String> fromIndex = filtered.stream().limit(rows >= 0 ? rows : 0).sorted().toList();
            // T1246 — a site that opts in also offers its most searched terms, after the index's.
            return TurSNAutoCompleteTopTerms.merge(fromIndex,
                    topTerms.suggest(turSNSite, q, locale, rows), rows);
        }).orElse(Collections.emptyList());
    }
}
