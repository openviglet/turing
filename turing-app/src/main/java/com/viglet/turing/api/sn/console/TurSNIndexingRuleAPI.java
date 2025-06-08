/*
 * Copyright (C) 2016-2025 the original author or authors.
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

import com.google.inject.Inject;
import com.viglet.turing.persistence.model.sn.indexingRule.TurSNIndexingRule;
import com.viglet.turing.persistence.model.sn.indexingRule.TurSNIndexingCondition;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.indexingRule.TurSNIndexingRuleConditionRepository;
import com.viglet.turing.persistence.repository.sn.indexingRule.TurSNIndexingRuleRepository;
import com.viglet.turing.persistence.utils.TurPersistenceUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Alexandre Oliveira
 * @since 2025.2
 */

@RestController
@RequestMapping("/api/sn/{snSiteId}/indexing-rule")
@Tag(name = "Semantic Navigation Indexing Rules", description = "Semantic Navigation Indexing Rules API")
public class TurSNIndexingRuleAPI {
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNIndexingRuleRepository turSNIndexingRuleRepository;
    private final TurSNIndexingRuleConditionRepository turSNIndexingRuleConditionRepository;

    @Inject
    public TurSNIndexingRuleAPI(TurSNSiteRepository turSNSiteRepository,
                                TurSNIndexingRuleRepository turSNIndexingRuleRepository,
                                TurSNIndexingRuleConditionRepository turSNIndexingRuleConditionRepository) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNIndexingRuleRepository = turSNIndexingRuleRepository;
        this.turSNIndexingRuleConditionRepository = turSNIndexingRuleConditionRepository;
    }

    @Operation(summary = "Semantic Navigation Indexing Rule List")
    @GetMapping
    public Set<TurSNIndexingRule> turSNIndexingRuleList(@PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(site ->
                        this.turSNIndexingRuleRepository
                                .findByTurSNSite(TurPersistenceUtils.orderByNameIgnoreCase(), site))
                .orElse(new HashSet<>());
    }

    @Operation(summary = "Show a Semantic Navigation Indexing Rules")
    @GetMapping("/{id}")
    public TurSNIndexingRule turSNIndexingRuleGet(@PathVariable String snSiteId, @PathVariable String id) {
        return turSNSiteRepository.findById(snSiteId).map(site ->
                        turSNIndexingRuleRepository.findById(id).map(rule -> {
                                    rule.setTurSNIndexingRuleConditions(
                                            turSNIndexingRuleConditionRepository.findByTurSNIndexingRule(rule));
                                    return rule;
                                })
                                .orElse(new TurSNIndexingRule()))
                .orElse(new TurSNIndexingRule());

    }

    @Operation(summary = "Update a Semantic Navigation Indexing Rules")
    @PutMapping("/{id}")
    public TurSNIndexingRule turSNIndexingRuleUpdate(@PathVariable String id,
                                                               @RequestBody TurSNIndexingRule turSNIndexingRule,
                                                               @PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(site ->
                        turSNIndexingRuleRepository.findById(id).map(turSNIndexingRuleEdit -> {
                            turSNIndexingRuleEdit.setName(turSNIndexingRule.getName());
                            return getTurSNIndexingRule(turSNIndexingRule, turSNIndexingRuleEdit);
                        }).orElse(new TurSNIndexingRule()))
                .orElse(new TurSNIndexingRule());
    }

    @NotNull

    private TurSNIndexingRule getTurSNIndexingRule(@RequestBody TurSNIndexingRule turSNIndexingRule,
                                                             TurSNIndexingRule turSNIndexingRuleEdit) {
        Set<TurSNIndexingCondition> set = new HashSet<>();
        for (TurSNIndexingCondition condition : turSNIndexingRule.getConditions()) {
            condition.setTurSNIndexingRule(turSNIndexingRule);
            set.add(condition);
        }
        turSNIndexingRuleEdit.setTurSNIndexingRuleConditions(set);
        turSNIndexingRuleRepository.save(turSNIndexingRuleEdit);
        return turSNIndexingRuleEdit;
    }

    @Transactional
    @Operation(summary = "Delete a Semantic Navigation Indexing Rules")
    @DeleteMapping("/{id}")
    public boolean turSNIndexingRuleDelete(@PathVariable String id, @PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(site -> {
                    turSNIndexingRuleRepository.deleteById(id);
                    return true;
                })
                .orElse(false);
    }

    @Operation(summary = "Create a Semantic Navigation Ranking Expression")
    @PostMapping
    public TurSNIndexingRule turSNIndexingRuleAdd(@RequestBody TurSNIndexingRule turSNIndexingRule,
                                                            @PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(site ->
                        getTurSNIndexingRule(turSNIndexingRule, turSNIndexingRule))
                .orElse(new TurSNIndexingRule());
    }

    @Operation(summary = "Semantic Navigation Ranking Expression Structure")
    @GetMapping("structure")
    public TurSNIndexingRule turSNIndexingRuleStructure(@PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(turSNSite -> {
                    TurSNIndexingRule turSNIndexingRule = new TurSNIndexingRule();
                    turSNIndexingRule.setTurSNSite(turSNSite);
                    return turSNIndexingRule;
                })
                .orElse(new TurSNIndexingRule());
    }
}