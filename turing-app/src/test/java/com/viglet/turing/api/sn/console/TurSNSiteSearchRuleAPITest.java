/*
 * Copyright (C) 2016-2026 the original author or authors.
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
package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.api.sn.console.TurSNSiteSearchRuleAPI.TurSNSiteSearchRuleActionDto;
import com.viglet.turing.api.sn.console.TurSNSiteSearchRuleAPI.TurSNSiteSearchRuleConditionDto;
import com.viglet.turing.api.sn.console.TurSNSiteSearchRuleAPI.TurSNSiteSearchRuleDto;
import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRule;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRuleAction;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRuleActionTypeEnum;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRuleCondition;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRuleLogicOperatorEnum;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRuleOperatorEnum;
import com.viglet.turing.persistence.model.sn.searchrule.TurSNSiteSearchRuleParameterEnum;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.searchrule.TurSNSiteSearchRuleRepository;
import com.viglet.turing.sn.searchrule.TurSNSearchRuleEvaluator;

/**
 * Unit tests for the condition ordering of {@link TurSNSiteSearchRuleAPI}.
 *
 * @author Arthur Nascimento
 * @since 2026.2.9
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteSearchRuleAPITest {

    @Mock
    private TurSNSiteRepository turSNSiteRepository;

    @Mock
    private TurSNSiteSearchRuleRepository turSNSiteSearchRuleRepository;

    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    @InjectMocks
    private TurSNSiteSearchRuleAPI api;

    private TurSNSite site() {
        TurSNSite site = new TurSNSite();
        site.setId("s1");
        when(turSNSiteRepository.findById("s1")).thenReturn(Optional.of(site));
        return site;
    }

    private static final String RULE_SORT = "sort_ee";
    private static final String DEFAULT_SORT = "relevance";

    private static TurSNSiteSearchRuleConditionDto conditionDto(String value,
            TurSNSiteSearchRuleLogicOperatorEnum logicOperator, Integer stalePosition) {
        return conditionDto(null, TurSNSiteSearchRuleOperatorEnum.EQUALS, value, logicOperator, stalePosition);
    }

    private static TurSNSiteSearchRuleConditionDto conditionDto(String id, TurSNSiteSearchRuleOperatorEnum operator,
            String value, TurSNSiteSearchRuleLogicOperatorEnum logicOperator, Integer stalePosition) {
        TurSNSiteSearchRuleConditionDto dto = new TurSNSiteSearchRuleConditionDto();
        dto.setId(id);
        dto.setParameter(TurSNSiteSearchRuleParameterEnum.QUERY);
        dto.setOperator(operator);
        dto.setValue(value);
        dto.setLogicOperator(logicOperator);
        dto.setPosition(stalePosition);
        return dto;
    }

    /** What the console sends back for a condition it loaded, stale position included. */
    private static TurSNSiteSearchRuleConditionDto echo(TurSNSiteSearchRuleCondition condition) {
        return conditionDto(condition.getId(), condition.getOperator(), condition.getValue(),
                condition.getLogicOperator(), condition.getPosition());
    }

    private static TurSNSiteSearchRuleCondition condition(String id, Integer position, String value) {
        return condition(id, position, TurSNSiteSearchRuleOperatorEnum.EQUALS, value,
                TurSNSiteSearchRuleLogicOperatorEnum.AND);
    }

    private static TurSNSiteSearchRuleCondition condition(String id, Integer position,
            TurSNSiteSearchRuleOperatorEnum operator, String value,
            TurSNSiteSearchRuleLogicOperatorEnum logicOperator) {
        return TurSNSiteSearchRuleCondition.builder()
                .id(id)
                .position(position)
                .parameter(TurSNSiteSearchRuleParameterEnum.QUERY)
                .operator(operator)
                .value(value)
                .logicOperator(logicOperator)
                .build();
    }

    private void saveReturnsItsArgument() {
        when(turSNSiteSearchRuleRepository.save(any(TurSNSiteSearchRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static String sortAfterSearch(TurSNSearchRuleEvaluator evaluator, TurSNSite site, String query) {
        TurSEParameters params = new TurSEParameters(new TurSNSearchParams());
        params.setQuery(query);
        params.setSort(DEFAULT_SORT);
        evaluator.applyRules(site, params, null);
        return params.getSort();
    }

    @Test
    void createStoresConditionPositionsInPayloadOrder() {
        site();
        saveReturnsItsArgument();
        TurSNSiteSearchRuleDto payload = new TurSNSiteSearchRuleDto();
        payload.setName("Cursos EE");
        payload.setEnabled(true);
        // Positions echoed from an older load must not win over the array order.
        payload.setConditions(List.of(
                conditionDto("*:*", TurSNSiteSearchRuleLogicOperatorEnum.AND, 3),
                conditionDto("", TurSNSiteSearchRuleLogicOperatorEnum.OR, null),
                conditionDto("*", TurSNSiteSearchRuleLogicOperatorEnum.OR, 0)));

        TurSNSiteSearchRuleDto result = api.create("s1", payload);

        ArgumentCaptor<TurSNSiteSearchRule> saved = ArgumentCaptor.forClass(TurSNSiteSearchRule.class);
        verify(turSNSiteSearchRuleRepository).save(saved.capture());
        assertThat(saved.getValue().getConditions())
                .extracting(TurSNSiteSearchRuleCondition::getValue, TurSNSiteSearchRuleCondition::getPosition)
                .containsExactlyInAnyOrder(tuple("*:*", 0), tuple("", 1), tuple("*", 2));
        assertThat(result.getConditions())
                .extracting(TurSNSiteSearchRuleConditionDto::getValue, TurSNSiteSearchRuleConditionDto::getPosition)
                .containsExactly(tuple("*:*", 0), tuple("", 1), tuple("*", 2));
    }

    @Test
    void getReturnsConditionsInSavedOrderWithUnpositionedLast() {
        TurSNSite site = site();
        TurSNSiteSearchRule rule = TurSNSiteSearchRule.builder().id("r1").name("rule").enabled(1)
                .conditions(new LinkedHashSet<>(List.of(
                        condition("a", null, "legacy"),
                        condition("b", 2, "third"),
                        condition("c", 0, "first"),
                        condition("d", 1, "second"))))
                .build();
        when(turSNSiteSearchRuleRepository.findByTurSNSiteAndId(site, "r1")).thenReturn(Optional.of(rule));

        TurSNSiteSearchRuleDto result = api.get("s1", "r1");

        assertThat(result.getConditions())
                .extracting(TurSNSiteSearchRuleConditionDto::getValue)
                .containsExactly("first", "second", "third", "legacy");
    }

    @Test
    void updateReplacesConditionsAndRenumbersPositionsInNewArrayOrder() {
        TurSNSite site = site();
        TurSNSiteSearchRuleCondition first = condition("c-1", 0, "first");
        TurSNSiteSearchRuleCondition second = condition("c-2", 1, "second");
        TurSNSiteSearchRuleCondition legacy = condition("c-3", null, "legacy");
        TurSNSiteSearchRuleCondition removed = condition("c-4", 2, "removed");
        TurSNSiteSearchRule rule = TurSNSiteSearchRule.builder().id("r1").name("rule").enabled(1)
                .turSNSite(site)
                .conditions(new LinkedHashSet<>(List.of(first, second, legacy, removed)))
                .build();
        when(turSNSiteSearchRuleRepository.findByTurSNSiteAndId(site, "r1")).thenReturn(Optional.of(rule));
        saveReturnsItsArgument();
        // The console sends back what it loaded, reordered and with one condition
        // dropped; the stale positions travel along and must be ignored.
        TurSNSiteSearchRuleDto payload = new TurSNSiteSearchRuleDto();
        payload.setName("rule");
        payload.setEnabled(true);
        payload.setConditions(List.of(echo(legacy), echo(second), echo(first)));

        TurSNSiteSearchRuleDto result = api.update("s1", "r1", payload);

        ArgumentCaptor<TurSNSiteSearchRule> saved = ArgumentCaptor.forClass(TurSNSiteSearchRule.class);
        verify(turSNSiteSearchRuleRepository).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(rule);
        assertThat(rule.getConditions())
                .extracting(TurSNSiteSearchRuleCondition::getId, TurSNSiteSearchRuleCondition::getPosition)
                .containsExactlyInAnyOrder(tuple("c-3", 0), tuple("c-2", 1), tuple("c-1", 2));
        // setConditions swaps the loaded instances for the payload's (JPA merges
        // them onto the existing rows by id); none of the old ones is left behind.
        assertThat(rule.getConditions())
                .doesNotContain(first, second, legacy, removed)
                .allSatisfy(condition -> assertThat(condition.getTurSNSiteSearchRule()).isSameAs(rule));
        assertThat(result.getConditions())
                .extracting(TurSNSiteSearchRuleConditionDto::getId, TurSNSiteSearchRuleConditionDto::getPosition)
                .containsExactly(tuple("c-3", 0), tuple("c-2", 1), tuple("c-1", 2));
    }

    @Test
    void reorderingAMixedAndOrGroupOnUpdateChangesTheEvaluation() {
        TurSNSite site = site();
        TurSNSiteSearchRuleCondition star = condition("c-1", 0,
                TurSNSiteSearchRuleOperatorEnum.EQUALS, "*", TurSNSiteSearchRuleLogicOperatorEnum.OR);
        TurSNSiteSearchRuleCondition empty = condition("c-2", 1,
                TurSNSiteSearchRuleOperatorEnum.IS_EMPTY, null, TurSNSiteSearchRuleLogicOperatorEnum.AND);
        TurSNSiteSearchRuleCondition blank = condition("c-3", 2,
                TurSNSiteSearchRuleOperatorEnum.EQUALS, "", TurSNSiteSearchRuleLogicOperatorEnum.OR);
        TurSNSiteSearchRuleCondition all = condition("c-4", 3,
                TurSNSiteSearchRuleOperatorEnum.EQUALS, "*:*", TurSNSiteSearchRuleLogicOperatorEnum.AND);
        TurSNSiteSearchRuleAction setSort = TurSNSiteSearchRuleAction.builder().id("a-1")
                .actionType(TurSNSiteSearchRuleActionTypeEnum.SET_SORT).value(RULE_SORT).build();
        TurSNSiteSearchRule rule = TurSNSiteSearchRule.builder().id("r1").name("rule").enabled(1)
                .turSNSite(site)
                .conditions(new LinkedHashSet<>(List.of(star, empty, blank, all)))
                .actions(new LinkedHashSet<>(List.of(setSort)))
                .build();
        when(turSNSiteSearchRuleRepository.findByTurSNSiteAndId(site, "r1")).thenReturn(Optional.of(rule));
        when(turSNSiteSearchRuleRepository.findEnabledByTurSNSite(site)).thenReturn(List.of(rule));
        saveReturnsItsArgument();
        TurSNSearchRuleEvaluator evaluator = new TurSNSearchRuleEvaluator(turSNSiteSearchRuleRepository);

        // Saved as [EQUALS "*", AND IS_EMPTY, OR EQUALS "", AND EQUALS "*:*"]:
        // with q=* folds to ((true AND false) OR false) AND false = false.
        String sortBeforeEdit = sortAfterSearch(evaluator, site, "*");

        // The user moves "*:*" to the top and "*" to the bottom:
        // ((false AND false) OR false) OR true = true.
        TurSNSiteSearchRuleActionDto setSortDto = new TurSNSiteSearchRuleActionDto();
        setSortDto.setId("a-1");
        setSortDto.setActionType(TurSNSiteSearchRuleActionTypeEnum.SET_SORT);
        setSortDto.setValue(RULE_SORT);
        TurSNSiteSearchRuleDto payload = new TurSNSiteSearchRuleDto();
        payload.setName("rule");
        payload.setEnabled(true);
        payload.setConditions(List.of(echo(all), echo(empty), echo(blank), echo(star)));
        payload.setActions(List.of(setSortDto));
        api.update("s1", "r1", payload);

        String sortAfterEdit = sortAfterSearch(evaluator, site, "*");

        assertThat(sortBeforeEdit).isEqualTo(DEFAULT_SORT);
        assertThat(sortAfterEdit).isEqualTo(RULE_SORT);
    }
}
