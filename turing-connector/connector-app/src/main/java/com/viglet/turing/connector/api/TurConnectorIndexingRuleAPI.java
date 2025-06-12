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

package com.viglet.turing.connector.api;

import com.google.inject.Inject;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRule;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRuleRepository;
import com.viglet.turing.spring.utils.TurPersistenceUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * @author Alexandre Oliveira
 * @since 2025.2
 */

@RestController
@RequestMapping("/api/v2/connector/indexing-rule")
@Tag(name = "Connector Indexing Rules", description = "Connector Indexing Rules API")
public class TurConnectorIndexingRuleAPI {
    private final TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository;

    @Inject
    public TurConnectorIndexingRuleAPI(TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository) {
        this.turConnectorIndexingRuleRepository = turConnectorIndexingRuleRepository;
    }

    @Operation(summary = "Connector Indexing Rule List By Source")
    @GetMapping("source/{source}")
    public Set<TurConnectorIndexingRule> turConnectorIndexingRuleBySourceList(@PathVariable String source) {
        return this.turConnectorIndexingRuleRepository
                .findBySource(TurPersistenceUtils.orderByNameIgnoreCase(), source);
    }

    @Operation(summary = "Connector Indexing Rule List")
    @GetMapping()
    public List<TurConnectorIndexingRule> turConnectorIndexingRuleList() {
        return this.turConnectorIndexingRuleRepository.findAll();
    }

    @Operation(summary = "Show a Connector Indexing Rules")
    @GetMapping("{id}")
    public TurConnectorIndexingRule turConnectorIndexingRuleGet(@PathVariable String id) {
        return turConnectorIndexingRuleRepository.findById(id)
                .orElse(new TurConnectorIndexingRule());
    }

    @Operation(summary = "Update a Connector Indexing Rules")
    @PutMapping("/{id}")
    public TurConnectorIndexingRule turConnectorIndexingRuleUpdate(@PathVariable String id,
                                                                   @RequestBody TurConnectorIndexingRule turConnectorIndexingRule) {
        return turConnectorIndexingRuleRepository.findById(id).map(edit -> {
            edit.setName(turConnectorIndexingRule.getName());
            edit.setDescription(turConnectorIndexingRule.getDescription());
            edit.setAttribute(turConnectorIndexingRule.getAttribute());
            edit.setRuleType(turConnectorIndexingRule.getRuleType());
            edit.setSource(turConnectorIndexingRule.getSource());
            edit.setValues(turConnectorIndexingRule.getValues());
            return turConnectorIndexingRuleRepository.save(edit);
        }).orElse(new TurConnectorIndexingRule());
    }


    @Transactional
    @Operation(summary = "Delete a Connector Indexing Rules")
    @DeleteMapping("/{id}")
    public boolean turConnectorIndexingRuleDelete(@PathVariable String id) {
        turConnectorIndexingRuleRepository.deleteById(id);
        return true;
    }

    @Operation(summary = "Create a Connector Ranking Expression")
    @PostMapping
    public TurConnectorIndexingRule turConnectorIndexingRuleAdd(
            @RequestBody TurConnectorIndexingRule turConnectorIndexingRule) {
        return turConnectorIndexingRuleRepository.save(turConnectorIndexingRule);
    }

    @Operation(summary = "Connector Ranking Expression Structure")
    @GetMapping("structure")
    public TurConnectorIndexingRule turConnectorIndexingRuleStructure() {
        return new TurConnectorIndexingRule();

    }
}