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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.spi.TurSECustomSortItem;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingCondition;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingExpression;
import com.viglet.turing.sn.persistence.repository.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.sn.persistence.repository.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.sn.persistence.repository.sort.TurSNSiteCustomSortRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.persistence.model.sort.TurSNSiteCustomSortItem;
import com.viglet.turing.spring.utils.TurPersistenceUtils;

/**
 * T1310 — sn's side of the search-engine SPI: a site's configuration as the
 * engines read it, so they build queries without importing sn's entities.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
@Transactional(readOnly = true)
public class TurSNSiteSearchConfigProvider implements TurSESiteSearchConfig {

    private final TurSNRankingExpressionRepository expressionRepository;
    private final TurSNRankingConditionRepository conditionRepository;
    private final TurSNSiteCustomSortRepository customSortRepository;
    private final TurSNSiteFieldExtRepository fieldExtRepository;

    public TurSNSiteSearchConfigProvider(TurSNRankingExpressionRepository expressionRepository,
            TurSNRankingConditionRepository conditionRepository,
            TurSNSiteCustomSortRepository customSortRepository,
            TurSNSiteFieldExtRepository fieldExtRepository) {
        this.fieldExtRepository = fieldExtRepository;
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

    static TurSERankingCondition toCondition(TurSNRankingCondition condition) {
        TurSERankingCondition.Kind kind = switch (condition.getCondition()) {
            case TurSNRankingCondition.IS_NOT -> TurSERankingCondition.Kind.IS_NOT;
            case TurSNRankingCondition.EXISTS -> TurSERankingCondition.Kind.EXISTS;
            default -> TurSERankingCondition.Kind.IS;
        };
        return new TurSERankingCondition(condition.getAttribute(), kind, condition.getValue());
    }
}
