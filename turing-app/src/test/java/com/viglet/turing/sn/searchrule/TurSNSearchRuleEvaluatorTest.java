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
package com.viglet.turing.sn.searchrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.viglet.turing.persistence.repository.sn.searchrule.TurSNSiteSearchRuleRepository;

@ExtendWith(MockitoExtension.class)
class TurSNSearchRuleEvaluatorTest {

    @Mock
    private TurSNSiteSearchRuleRepository searchRuleRepository;

    @InjectMocks
    private TurSNSearchRuleEvaluator evaluator;

    private static TurSNSite site() {
        TurSNSite site = new TurSNSite();
        site.setId("s1");
        site.setName("s1");
        return site;
    }

    private static TurSNSiteSearchRule ruleSettingProfile(String queryMatch, String profileValue) {
        TurSNSiteSearchRuleCondition condition = TurSNSiteSearchRuleCondition.builder()
                .parameter(TurSNSiteSearchRuleParameterEnum.QUERY)
                .operator(TurSNSiteSearchRuleOperatorEnum.EQUALS)
                .value(queryMatch)
                .build();
        TurSNSiteSearchRuleAction action = TurSNSiteSearchRuleAction.builder()
                .actionType(TurSNSiteSearchRuleActionTypeEnum.SET_PROFILE)
                .value(profileValue)
                .build();
        return TurSNSiteSearchRule.builder().id("r1").name("rule").enabled(1).position(1)
                .conditions(Set.of(condition)).actions(Set.of(action)).build();
    }

    private static TurSEParameters params(String query) {
        TurSEParameters params = new TurSEParameters(new TurSNSearchParams());
        params.setQuery(query);
        return params;
    }

    @Test
    void setProfileActionAppliesProfileWhenRuleMatches() {
        TurSNSite site = site();
        when(searchRuleRepository.findEnabledByTurSNSite(site))
                .thenReturn(List.of(ruleSettingProfile("livros", "Cursos EE")));
        TurSEParameters params = params("livros");

        evaluator.applyRules(site, params, null);

        assertThat(params.getProfile()).isEqualTo("Cursos EE");
    }

    @Test
    void setProfileActionIgnoredWhenRuleDoesNotMatch() {
        TurSNSite site = site();
        when(searchRuleRepository.findEnabledByTurSNSite(site))
                .thenReturn(List.of(ruleSettingProfile("livros", "Cursos EE")));
        TurSEParameters params = params("outro");

        evaluator.applyRules(site, params, null);

        assertThat(params.getProfile()).isNull();
    }

    // ── Mixed AND/OR groups: order of the conditions ─────────────────────

    private static final String RULE_SORT = "sort_ee";
    private static final String DEFAULT_SORT = "relevance";

    private static TurSNSiteSearchRuleCondition queryCondition(String id, Integer position,
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

    private static TurSNSiteSearchRule ruleSettingSort(List<TurSNSiteSearchRuleCondition> loadOrder) {
        TurSNSiteSearchRuleAction action = TurSNSiteSearchRuleAction.builder()
                .actionType(TurSNSiteSearchRuleActionTypeEnum.SET_SORT)
                .value(RULE_SORT)
                .build();
        return TurSNSiteSearchRule.builder().id("r1").name("rule").enabled(1).position(1)
                .conditions(new LinkedHashSet<>(loadOrder)).actions(Set.of(action)).build();
    }

    /** Every order in which the conditions' Set could come back when the rule is loaded. */
    private static <T> List<List<T>> permutations(List<T> items) {
        if (items.size() <= 1) {
            return List.of(items);
        }
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<T> rest = new ArrayList<>(items);
            T head = rest.remove(i);
            for (List<T> tail : permutations(rest)) {
                List<T> permutation = new ArrayList<>();
                permutation.add(head);
                permutation.addAll(tail);
                result.add(permutation);
            }
        }
        return result;
    }

    /**
     * Runs the same search once per load order of the rule's conditions and
     * returns the sort each run ended up with.
     */
    private List<String> sortsAcrossLoadOrders(String query, List<TurSNSiteSearchRuleCondition> conditions) {
        TurSNSite site = site();
        List<List<TurSNSiteSearchRuleCondition>> loadOrders = permutations(conditions);
        Iterator<List<TurSNSiteSearchRuleCondition>> loads = loadOrders.iterator();
        when(searchRuleRepository.findEnabledByTurSNSite(site))
                .thenAnswer(invocation -> List.of(ruleSettingSort(loads.next())));

        List<String> sorts = new ArrayList<>();
        for (int run = 0; run < loadOrders.size(); run++) {
            TurSEParameters params = params(query);
            params.setSort(DEFAULT_SORT);
            evaluator.applyRules(site, params, null);
            sorts.add(params.getSort());
        }
        return sorts;
    }

    @Test
    void mixedAndOrGroupFollowsSavedPositionOnEveryLoad() {
        // q = [EQUALS "*:*", AND IS_EMPTY, OR EQUALS "", OR EQUALS "*"] → with q=* folds to
        // ((false AND false) OR false) OR true = true. Ids run against the positions, so
        // ordering by id instead would fold to false.
        List<TurSNSiteSearchRuleCondition> conditions = List.of(
                queryCondition("d", 0, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*:*",
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("c", 1, TurSNSiteSearchRuleOperatorEnum.IS_EMPTY, null,
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("b", 2, TurSNSiteSearchRuleOperatorEnum.EQUALS, "",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR),
                queryCondition("a", 3, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR));

        List<String> sorts = sortsAcrossLoadOrders("*", conditions);

        assertThat(sorts).hasSize(24).containsOnly(RULE_SORT);
    }

    @Test
    void mixedAndOrGroupSavedInAnotherOrderConsistentlyDoesNotMatch() {
        // Same conditions saved as [EQUALS "*", AND IS_EMPTY, OR EQUALS "", OR EQUALS "*:*"]:
        // ((true AND false) OR false) OR false = false.
        List<TurSNSiteSearchRuleCondition> conditions = List.of(
                queryCondition("a", 0, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR),
                queryCondition("c", 1, TurSNSiteSearchRuleOperatorEnum.IS_EMPTY, null,
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("b", 2, TurSNSiteSearchRuleOperatorEnum.EQUALS, "",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR),
                queryCondition("d", 3, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*:*",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR));

        List<String> sorts = sortsAcrossLoadOrders("*", conditions);

        assertThat(sorts).hasSize(24).containsOnly(DEFAULT_SORT);
    }

    @Test
    void conditionsWithoutPositionAreFoldedInIdOrder() {
        // Rules saved before the position column: no position, so the id decides —
        // still the same answer on every load.
        List<TurSNSiteSearchRuleCondition> conditions = List.of(
                queryCondition("a", null, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*:*",
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("b", null, TurSNSiteSearchRuleOperatorEnum.IS_EMPTY, null,
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("c", null, TurSNSiteSearchRuleOperatorEnum.EQUALS, "",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR),
                queryCondition("d", null, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR));

        List<String> sorts = sortsAcrossLoadOrders("*", conditions);

        assertThat(sorts).hasSize(24).containsOnly(RULE_SORT);
    }

    @Test
    void conditionsWithoutPositionAreFoldedAfterPositionedOnes() {
        // [EQUALS "*:*", AND IS_EMPTY] then the unpositioned OR EQUALS "*":
        // (false AND false) OR true = true. Folding the null first would give false.
        List<TurSNSiteSearchRuleCondition> conditions = List.of(
                queryCondition("z", 0, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*:*",
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("y", 1, TurSNSiteSearchRuleOperatorEnum.IS_EMPTY, null,
                        TurSNSiteSearchRuleLogicOperatorEnum.AND),
                queryCondition("a", null, TurSNSiteSearchRuleOperatorEnum.EQUALS, "*",
                        TurSNSiteSearchRuleLogicOperatorEnum.OR));

        List<String> sorts = sortsAcrossLoadOrders("*", conditions);

        assertThat(sorts).hasSize(6).containsOnly(RULE_SORT);
    }
}
