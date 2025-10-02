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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import com.viglet.turing.api.sn.search.TurSNSiteSearchCachedAPI;
import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchBean;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchResultsBean;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.se.TurSEStopWord;
import com.viglet.turing.sn.TurSNUtils;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstance;
import com.viglet.turing.solr.TurSolrInstanceProcess;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurSNAutoComplete {
    private static final String SPACE_CHAR = " ";
    private final TurSolr turSolr;
    private final TurSEStopWord turSEStopword;
    private final TurSolrInstanceProcess turSolrInstanceProcess;
    private final TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI;

    public TurSNAutoComplete(TurSolr turSolr, TurSEStopWord turSEStopword,
            TurSolrInstanceProcess turSolrInstanceProcess,
            TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI) {
        this.turSolr = turSolr;
        this.turSEStopword = turSEStopword;
        this.turSolrInstanceProcess = turSolrInstanceProcess;
        this.turSNSiteSearchCachedAPI = turSNSiteSearchCachedAPI;
    }

    @NotNull
    public List<String> autoCompleteWithRegularSearch(String siteName,
            TurSNSearchParams turSNSearchParams, HttpServletRequest request) {
        TurSNConfig turSNConfig = new TurSNConfig();
        turSNConfig.setHlEnabled(false);
        turSNSearchParams
                .setQ(String.format("%s:%s*", TurSNFieldName.TITLE, turSNSearchParams.getQ()));
        turSNSearchParams.setGroup(null);
        turSNSearchParams.setNfpr(0);
        turSNSearchParams.setP(1);
        return Optional
                .ofNullable(turSNSiteSearchCachedAPI.searchCached(
                        TurSNUtils.getCacheKey(siteName, request),
                        TurSNUtils.getTurSNSiteSearchContext(turSNConfig, siteName,
                                turSNSearchParams, request,
                                LocaleUtils.toLocale(turSNSearchParams.getLocale()))))
                .map(TurSNSiteSearchBean::getResults).map(TurSNSiteSearchResultsBean::getDocument)
                .map(documents -> {
                    List<String> termList = new ArrayList<>();
                    documents.forEach(document -> Optional.ofNullable(document.getFields())
                            .filter(fields -> fields.containsKey(TurSNFieldName.TITLE))
                            .map(fields -> termList
                                    .add(fields.get(TurSNFieldName.TITLE).toString())));
                    return termList;
                }).orElse(Collections.emptyList());
    }

    public List<String> autoComplete(String siteName, String q, Locale locale, long rows) {
        // Only autocomplete if the query has more than one character
        if (q.length() > 1) {
            // Initialize Solr Instance
            return turSolrInstanceProcess.initSolrInstance(siteName, locale).map(instance -> {
                // Execute AutoComplete Solr API
                SpellCheckResponse turSEResults = executeAutoCompleteFromSE(instance, q);
                int numberOfWordsFromQuery = q.split(SPACE_CHAR).length;
                // It would be possible to infer whether there is space at the end of the query
                // if we do a
                // split keeping the delimiters and seeing if the array size is even.
                if (q.endsWith(SPACE_CHAR)) {
                    numberOfWordsFromQuery++;
                }

                List<String> autoCompleteListFormatted =
                        createFormattedList(turSEResults, instance, numberOfWordsFromQuery);
                return autoCompleteListFormatted.stream().limit(rows >= 0 ? rows : 0).sorted()
                        .toList();
            }).orElse(Collections.emptyList());
        } else {
            return Collections.emptyList();
        }
    }

    private SpellCheckResponse executeAutoCompleteFromSE(TurSolrInstance turSolrInstance,
            String q) {
        SpellCheckResponse turSEResults = null;
        try {
            turSEResults = turSolr.autoComplete(turSolrInstance, q);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return turSEResults;
    }

    private List<String> createFormattedList(SpellCheckResponse turSEResults,
            TurSolrInstance turSolrInstance, int numberOfWordsFromQuery) {
        List<String> autoCompleteListFormatted = new ArrayList<>();
        // if there are suggestions in the response.
        if (hasSuggestions(turSEResults)) {
            // autoCompleteList is the list of auto complete suggestions returned by Solr.
            List<String> autoCompleteList =
                    turSEResults.getSuggestions().getFirst().getAlternatives();
            TurSNAutoCompleteListData autoCompleteListData =
                    new TurSNAutoCompleteListData(turSEStopword.getStopWords(turSolrInstance));

            TurSNSuggestionFilter suggestionFilter =
                    new TurSNSuggestionFilter(autoCompleteListData.getStopWords());
            suggestionFilter.automatonStrategyConfig(numberOfWordsFromQuery);
            autoCompleteListFormatted = suggestionFilter.filter(autoCompleteList);
        }
        return autoCompleteListFormatted;
    }

    private boolean hasSuggestions(SpellCheckResponse turSEResults) {
        return turSEResults != null && turSEResults.getSuggestions() != null
                && !turSEResults.getSuggestions().isEmpty();
    }

}
