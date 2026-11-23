/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.viglet.turing.sn.web.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.merge.TurSNSiteMergeProviders;
import com.viglet.turing.sn.persistence.model.merge.TurSNSiteMergeProvidersField;
import com.viglet.turing.sn.persistence.repository.merge.TurSNSiteMergeProvidersRepository;
import com.viglet.turing.searchengine.TurSearchEnginePlugin;
import com.viglet.turing.searchengine.TurSearchEnginePluginFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Alexandre Oliveira
 * @since 0.3.5
 */
@Slf4j
@Component
public class TurSNMergeProvidersProcess {
    private final TurSearchEnginePluginFactory pluginFactory;
    private final TurSNSiteMergeProvidersRepository turSNSiteMergeProvidersRepository;

    public TurSNMergeProvidersProcess(TurSearchEnginePluginFactory pluginFactory,
            TurSNSiteMergeProvidersRepository turSNSiteMergeProvidersRepository) {
        this.pluginFactory = pluginFactory;
        this.turSNSiteMergeProvidersRepository = turSNSiteMergeProvidersRepository;
    }

    public Map<String, Object> mergeDocuments(TurSNSite turSNSite,
            Map<String, Object> queueDocumentAttrs, Locale locale) {
        List<TurSNSiteMergeProviders> turSNSiteMergeProvidersList = turSNSiteMergeProvidersRepository
                .findByTurSNSite(turSNSite);

        if (!turSNSiteMergeProvidersList.isEmpty()) {
            TurSNSiteMergeProviders turSNSiteMergeProviders = turSNSiteMergeProvidersList.getFirst();

            if (queueDocumentAttrs.containsKey(TurSNFieldName.SOURCE_APPS)) {
                Object providerAttribute = queueDocumentAttrs.get(TurSNFieldName.SOURCE_APPS);
                List<String> queueDocumentProviders = new ArrayList<>();

                if (providerAttribute instanceof String stringValue) {
                    queueDocumentProviders.add(stringValue);
                } else if (providerAttribute instanceof List<?> listValue) {
                    listValue.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .forEach(queueDocumentProviders::add);
                }

                if (queueDocumentProviders.contains(turSNSiteMergeProviders.getProviderFrom())) {
                    return mergeFrom(queueDocumentAttrs, turSNSiteMergeProviders, locale,
                            pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(turSNSite)));
                } else if (queueDocumentProviders.contains(turSNSiteMergeProviders.getProviderTo())) {
                    return mergeTo(queueDocumentAttrs, turSNSiteMergeProviders, locale,
                            pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(turSNSite)));
                }
            }
        }
        return queueDocumentAttrs;
    }

    private Map<String, Object> mergeFrom(Map<String, Object> queueDocumentAttrs,
            TurSNSiteMergeProviders turSNSiteMergeProviders, Locale locale, TurSearchEnginePlugin plugin) {
        String relationValue = (String) queueDocumentAttrs.get(turSNSiteMergeProviders.getRelationFrom());
        List<Map<String, Object>> resultsFrom = documentsFrom(plugin, turSNSiteMergeProviders, relationValue,
                locale);
        List<Map<String, Object>> resultsTo = documentsTo(plugin, turSNSiteMergeProviders, relationValue, locale);
        List<Map<String, Object>> resultsFromAndTo = documentsFromAndTo(plugin, turSNSiteMergeProviders,
                turSNSiteMergeProviders.getRelationTo(), relationValue, locale);
        if (hasDocuments(resultsFromAndTo)) {
            return doMergeContent(queueDocumentAttrs,
                    new HashMap<>(resultsFromAndTo.getFirst()), turSNSiteMergeProviders);
        } else {
            if (hasDocuments(resultsFrom) && hasDocuments(resultsTo)) {
                deIndexDocuments(plugin, turSNSiteMergeProviders, resultsFrom);
            }
            if (hasDocuments(resultsTo)) {
                return doMergeContent(queueDocumentAttrs, new HashMap<>(resultsTo.getFirst()),
                        turSNSiteMergeProviders);
            }
        }
        return queueDocumentAttrs;
    }

    private Map<String, Object> mergeTo(Map<String, Object> queueDocumentAttrs,
            TurSNSiteMergeProviders turSNSiteMergeProviders, Locale locale, TurSearchEnginePlugin plugin) {
        String relationValue = (String) queueDocumentAttrs.get(turSNSiteMergeProviders.getRelationTo());
        List<Map<String, Object>> resultsFrom = documentsFrom(plugin, turSNSiteMergeProviders, relationValue,
                locale);
        String idValue = (String) queueDocumentAttrs.get(TurSNFieldName.ID);
        List<Map<String, Object>> resultsFromAndTo = documentsFromAndTo(plugin, turSNSiteMergeProviders,
                TurSNFieldName.ID, idValue, locale);
        if (hasDocuments(resultsFromAndTo)) {
            return doMergeContent(new HashMap<>(resultsFromAndTo.getFirst()),
                    queueDocumentAttrs, turSNSiteMergeProviders);
        } else if (hasDocuments(resultsFrom)) {
            Map<String, Object> mergedDocumentAttributes = doMergeContent(new HashMap<>(resultsFrom.getFirst()),
                    queueDocumentAttrs, turSNSiteMergeProviders);
            deIndexDocuments(plugin, turSNSiteMergeProviders, resultsFrom);
            return mergedDocumentAttributes;
        }
        return queueDocumentAttrs;
    }

    private Map<String, Object> doMergeContent(Map<String, Object> attributesFrom, Map<String, Object> attributesTo,
            TurSNSiteMergeProviders turSNSiteMergeProviders) {
        addProviderToSEDocument(attributesTo, turSNSiteMergeProviders.getProviderFrom());
        addOverwrittenAttributesToSolrDocument(attributesFrom, attributesTo,
                turSNSiteMergeProviders.getOverwrittenFields());
        return attributesTo;
    }

    private boolean hasDocuments(List<Map<String, Object>> results) {
        return results != null && !results.isEmpty();
    }

    private List<Map<String, Object>> documentsTo(TurSearchEnginePlugin plugin,
            TurSNSiteMergeProviders turSNSiteMergeProviders, String relationValue, Locale locale) {
        Map<String, Object> queryMapTo = new HashMap<>();
        queryMapTo.put(turSNSiteMergeProviders.getRelationTo(), relationValue);
        queryMapTo.put(TurSNFieldName.SOURCE_APPS, turSNSiteMergeProviders.getProviderTo());
        return resultAnd(plugin, turSNSiteMergeProviders, queryMapTo, locale);
    }

    private List<Map<String, Object>> documentsFrom(TurSearchEnginePlugin plugin,
            TurSNSiteMergeProviders turSNSiteMergeProviders, String relationValue, Locale locale) {
        Map<String, Object> queryMapFrom = new HashMap<>();
        queryMapFrom.put(turSNSiteMergeProviders.getRelationFrom(), relationValue);
        queryMapFrom.put(TurSNFieldName.SOURCE_APPS, turSNSiteMergeProviders.getProviderFrom());
        return resultAnd(plugin, turSNSiteMergeProviders, queryMapFrom, locale);
    }

    private List<Map<String, Object>> documentsFromAndTo(TurSearchEnginePlugin plugin,
            TurSNSiteMergeProviders turSNSiteMergeProviders, String relationAttrib, String relationValue,
            Locale locale) {
        Map<String, Object> queryMapFrom = new HashMap<>();
        queryMapFrom.put(relationAttrib, relationValue);
        queryMapFrom.put(TurSNFieldName.SOURCE_APPS, String.format("(\"%s\" AND \"%s\")",
                turSNSiteMergeProviders.getProviderFrom(), turSNSiteMergeProviders.getProviderFrom()));
        return resultAnd(plugin, turSNSiteMergeProviders, queryMapFrom, locale);
    }

    private void deIndexDocuments(TurSearchEnginePlugin plugin, TurSNSiteMergeProviders turSNSiteMergeProviders,
            List<Map<String, Object>> results) {
        results.forEach(result -> plugin.deIndex(turSNSiteMergeProviders.getTurSNSite(),
                turSNSiteMergeProviders.getLocale(), result.get(TurSNFieldName.ID).toString()));
    }

    private List<Map<String, Object>> resultAnd(TurSearchEnginePlugin plugin,
            TurSNSiteMergeProviders turSNSiteMergeProviders, Map<String, Object> attributes, Locale locale) {
        return plugin.findByFieldValues(turSNSiteMergeProviders.getTurSNSite(),
                Optional.ofNullable(locale).orElse(turSNSiteMergeProviders.getLocale()), attributes);
    }

    private void addOverwrittenAttributesToSolrDocument(Map<String, Object> queueDocumentAttrs,
            Map<String, Object> attributesTo,
            Set<TurSNSiteMergeProvidersField> overwrittenFields) {
        queueDocumentAttrs.forEach((key, value) -> {
            if ((overwrittenFields != null) && overwrittenFields.stream()
                    .anyMatch(o -> o.getName().equals(key))) {
                attributesTo.put(key, value);

            }
        });
    }

    private void addProviderToSEDocument(Map<String, Object> documentAttributes, String providerName) {
        Object providerAttribute = documentAttributes.get(TurSNFieldName.SOURCE_APPS);
        List<String> providers = new ArrayList<>();

        if (providerAttribute instanceof List<?> listValue) {
            listValue.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(providers::add);
        } else if (providerAttribute instanceof String stringValue) {
            providers.add(stringValue);
        }

        if (!providers.isEmpty()) {
            List<String> list = TurCommonsUtils.cloneListOfTermsAsString(providers);

            if (!list.contains(providerName)) {
                list.add(providerName);
            }
            documentAttributes.put(TurSNFieldName.SOURCE_APPS, list);
        } else {
            log.debug("The providers attribute of Merge Providers is empty");
        }
    }
}
