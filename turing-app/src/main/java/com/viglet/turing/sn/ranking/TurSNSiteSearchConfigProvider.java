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
package com.viglet.turing.sn.ranking;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.spi.TurSECustomSortItem;
import com.viglet.turing.searchengine.spi.TurSEIndexField;
import com.viglet.turing.searchengine.spi.TurSECoreUsage;
import com.viglet.turing.searchengine.spi.TurSEFacetField;
import com.viglet.turing.searchengine.spi.TurSECustomFacet;
import com.viglet.turing.sn.facet.TurSNCustomFacetCatalog;
import com.viglet.turing.searchengine.spi.TurSESiteCore;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.domain.TurSNSiteLocaleRepositoryPort;
import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.sn.TurSNFieldProcess;
import com.viglet.turing.sn.searchrule.TurSNSearchRuleEvaluator;
import com.viglet.turing.sn.api.TurSNSiteLocales;
import com.viglet.turing.sn.api.TurSNSites;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;

import lombok.extern.slf4j.Slf4j;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingCondition;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingExpression;
import com.viglet.turing.sn.persistence.repository.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.sn.persistence.repository.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.sn.persistence.repository.sort.TurSNSiteCustomSortRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldRepository;
import com.viglet.turing.sn.persistence.model.sort.TurSNSiteCustomSortItem;
import com.viglet.turing.spring.utils.TurPersistenceUtils;

/**
 * T1310 — sn's side of the search-engine SPI: a site's configuration as the
 * engines read it, so they build queries without importing sn's entities.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class TurSNSiteSearchConfigProvider implements TurSESiteSearchConfig {

    private final TurSNRankingExpressionRepository expressionRepository;
    private final TurSNRankingConditionRepository conditionRepository;
    private final TurSNSiteCustomSortRepository customSortRepository;
    private final TurSNSiteFieldExtRepository fieldExtRepository;
    private final TurSNSiteFieldRepository fieldRepository;
    private final TurSNSites siteRepository;
    private final TurSNSiteLocales siteLocaleRepository;
    private final TurSNSiteLocaleRepositoryPort siteLocaleRepositoryPort;
    private final TurSNSearchRuleEvaluator searchRuleEvaluator;
    private final TurSNFieldProcess fieldProcess;
    private final TurSNCustomFacetCatalog customFacetCatalog;

    public TurSNSiteSearchConfigProvider(TurSNRankingExpressionRepository expressionRepository,
            TurSNRankingConditionRepository conditionRepository,
            TurSNSiteCustomSortRepository customSortRepository,
            TurSNSiteFieldExtRepository fieldExtRepository,
            TurSNSiteFieldRepository fieldRepository,
            TurSNSites siteRepository,
            TurSNSiteLocales siteLocaleRepository,
            TurSNSiteLocaleRepositoryPort siteLocaleRepositoryPort,
            TurSNSearchRuleEvaluator searchRuleEvaluator,
            TurSNFieldProcess fieldProcess,
            TurSNCustomFacetCatalog customFacetCatalog) {
        this.customFacetCatalog = customFacetCatalog;
        this.siteLocaleRepositoryPort = siteLocaleRepositoryPort;
        this.searchRuleEvaluator = searchRuleEvaluator;
        this.fieldProcess = fieldProcess;
        this.fieldExtRepository = fieldExtRepository;
        this.fieldRepository = fieldRepository;
        this.siteRepository = siteRepository;
        this.siteLocaleRepository = siteLocaleRepository;
        this.expressionRepository = expressionRepository;
        this.conditionRepository = conditionRepository;
        this.customSortRepository = customSortRepository;
    }

    @Override
    public List<TurSERankingExpression> rankingExpressions(String siteId) {
        if (siteId == null) {
            return List.of();
        }
        return expressionRepository.findByTurSNSiteId(TurPersistenceUtils.orderByNameIgnoreCase(), siteId)
                .stream()
                .map(this::toExpression)
                .toList();
    }

    private TurSERankingExpression toExpression(TurSNRankingExpression expression) {
        List<TurSERankingCondition> conditions = conditionRepository.findByTurSNRankingExpression(expression)
                .stream()
                .map(TurSNSiteSearchConfigProvider::toCondition)
                .toList();
        return new TurSERankingExpression(expression.getName(), expression.getWeight(), conditions);
    }

    @Override
    public Optional<List<TurSECustomSortItem>> customSort(String siteId, String name) {
        if (siteId == null || name == null) {
            return Optional.empty();
        }
        return customSortRepository.findBySiteIdAndName(siteId, name)
                .map(sort -> sort.getItems().stream()
                        .sorted(Comparator.comparing(TurSNSiteCustomSortItem::getPosition,
                                Comparator.nullsLast(Integer::compareTo)))
                        .map(item -> new TurSECustomSortItem(item.getFieldName(), item.getSortOrder(),
                                item.getMissing()))
                        .toList());
    }

    @Override
    public Map<String, TurSEFieldType> fieldTypes(String siteId) {
        Map<String, TurSEFieldType> types = new LinkedHashMap<>();
        if (siteId == null) {
            return types;
        }
        fieldExtRepository.findByTurSNSiteId(TurPersistenceUtils.orderByNameIgnoreCase(), siteId).stream()
                .filter(field -> field.getName() != null && field.getType() != null)
                .forEach(field -> types.putIfAbsent(field.getName(), field.getType()));
        return types;
    }

    @Override
    public Map<String, TurSEIndexField> indexFields(String siteId) {
        Map<String, TurSEIndexField> fields = new HashMap<>();
        if (siteId == null) {
            return fields;
        }
        fieldRepository.findByTurSNSiteId(siteId)
                .forEach(field -> fields.put(field.getName(), new TurSEIndexField(field.getName(),
                        field.getType(), field.getMultiValued() == 1)));
        return fields;
    }

    @Override
    public Optional<TurSESiteCore> siteCore(String siteName, Locale locale) {
        if (siteName == null) {
            return Optional.empty();
        }
        return siteRepository.findByNameIgnoreCase(siteName).flatMap(site -> {
            TurSNSiteLocale siteLocale = siteLocaleRepository.findByTurSNSiteAndLanguage(site, locale);
            if (siteLocale == null) {
                log.warn("{} site with {} locale not found", site.getName(), locale);
                return Optional.empty();
            }
            return Optional.of(new TurSESiteCore(site.getName(), site.getTurSEInstanceId(), siteLocale.getCore()));
        });
    }

    @Override
    public Optional<String> siteId(String siteName) {
        if (siteName == null) {
            return Optional.empty();
        }
        return siteRepository.findByNameIgnoreCase(siteName).map(site -> site.getId());
    }

    @Override
    public Optional<TurSESiteSettings> siteSettings(String siteName) {
        if (siteName == null) {
            return Optional.empty();
        }
        return siteRepository.findByNameIgnoreCase(siteName).map(TurSNSiteSearchConfigProvider::toSettings);
    }

    @Override
    public List<TurSESearchField> enabledFields(String siteId) {
        if (siteId == null) {
            return List.of();
        }
        return siteRepository.findById(siteId)
                .map(site -> fieldExtRepository.findByTurSNSiteAndEnabled(site, 1).stream()
                        .map(TurSNSiteSearchConfigProvider::toSearchField)
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public List<String> mltFields(String siteId) {
        if (siteId == null) {
            return List.of();
        }
        return siteRepository.findById(siteId)
                .map(site -> fieldExtRepository.findByTurSNSiteAndMltAndEnabled(site, 1, 1).stream()
                        .map(TurSNSiteFieldExt::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .toList())
                .orElseGet(List::of);
    }

    /** One of a site's fields as the engines read it. */
    public static TurSESearchField toSearchField(TurSNSiteFieldExt field) {
        return TurSESearchField.builder()
                .name(field.getName())
                .type(field.getType())
                .facet(field.getFacet())
                .hl(field.getHl())
                .facetPosition(field.getFacetPosition())
                .facetItemType(field.getFacetItemType())
                .searchWeight(field.getSearchWeight())
                .build();
    }

    /** A site's settings as the engines read them. */
    public static TurSESiteSettings toSettings(TurSNSite site) {
        return TurSESiteSettings.builder()
                .id(site.getId())
                .name(site.getName())
                .turSEInstanceId(site.getTurSEInstanceId())
                .rowsPerPage(site.getRowsPerPage())
                .wildcardNoResults(site.getWildcardNoResults())
                .wildcardAlways(site.getWildcardAlways())
                .exactMatch(site.getExactMatch())
                .exactMatchField(site.getExactMatchField())
                .facet(site.getFacet())
                .itemsPerFacet(site.getItemsPerFacet())
                .facetItemType(site.getFacetItemType())
                .hl(site.getHl())
                .hlPre(site.getHlPre())
                .hlPost(site.getHlPost())
                .mlt(site.getMlt())
                .spellCheck(site.getSpellCheck())
                .spellCheckFixes(site.getSpellCheckFixes())
                .defaultTitleField(site.getDefaultTitleField())
                .defaultDateField(site.getDefaultDateField())
                .build();
    }

    @Override
    public List<TurSECoreUsage> coreUsage(String core) {
        return siteLocaleRepositoryPort.findCoreUsage(core).stream()
                .map(usage -> new TurSECoreUsage(usage.snSiteId(), usage.snSiteName(), usage.localeId(),
                        usage.language()))
                .toList();
    }

    @Override
    public void applySearchRules(String siteId, TurSEParameters params, Locale locale) {
        searchRuleEvaluator.applyRules(siteId, params, locale);
    }

    @Override
    public Map<String, Integer> facetPositions(String siteId) {
        Map<String, Integer> positions = new LinkedHashMap<>();
        if (siteId == null) {
            return positions;
        }
        fieldProcess.getTurSNSiteFieldOrdering(siteId).ifPresent(fields -> fields.stream()
                .filter(field -> field.getFacetPosition() != null)
                .forEach(field -> positions.putIfAbsent(field.getName(), field.getFacetPosition())));
        return positions;
    }

    @Override
    public List<TurSEFacetField> facetFields(String siteId) {
        if (siteId == null) {
            return List.of();
        }
        return fieldExtRepository.findByTurSNSiteIdAndFacetAndEnabledOrderByFacetPosition(siteId, 1, 1).stream()
                .map(field -> new TurSEFacetField(field.getName(), field.getFacetPosition(),
                        field.getFacetItemType()))
                .toList();
    }

    @Override
    public List<TurSECustomFacet> customFacets(String siteId) {
        if (siteId == null) {
            return List.of();
        }
        return siteRepository.findById(siteId).map(customFacetCatalog::customFacets).orElseGet(List::of);
    }

    @Override
    public boolean coreInUse(String core) {
        return siteLocaleRepositoryPort.existsByCore(core);
    }

    @Override
    public Optional<Locale> coreLanguage(String core) {
        if (core == null) {
            return Optional.empty();
        }
        return siteLocaleRepository.findByCore(core).stream()
                .map(TurSNSiteLocale::getLanguage)
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    static TurSERankingCondition toCondition(TurSNRankingCondition condition) {
        TurSERankingCondition.Kind kind = switch (condition.getCondition()) {
            case TurSNRankingCondition.IS_NOT -> TurSERankingCondition.Kind.IS_NOT;
            case TurSNRankingCondition.EXISTS -> TurSERankingCondition.Kind.EXISTS;
            default -> TurSERankingCondition.Kind.IS;
        };
        return new TurSERankingCondition(condition.getAttribute(), kind, condition.getValue());
    }
}
