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
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRuleModel;
import com.viglet.turing.connector.service.TurConnectorIndexingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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
    private final TurConnectorIndexingRuleService indexingRuleService;
    @Inject
    public TurConnectorIndexingRuleAPI(TurConnectorIndexingRuleService turConnectorIndexingRuleService) {
        this.indexingRuleService = turConnectorIndexingRuleService;
    }

    @Operation(summary = "Connector Indexing Rule List By Source")
    @GetMapping("source/{source}")
    public Set<TurConnectorIndexingRuleModel> turConnectorIndexingRuleBySourceList(@PathVariable String source) {
        return indexingRuleService.getBySource(source);
    }



    @Operation(summary = "Connector Indexing Rule List")
    @GetMapping()
    public List<TurConnectorIndexingRuleModel> turConnectorIndexingRuleList() {
        return indexingRuleService.getAll();
    }


    @Operation(summary = "Show a Connector Indexing Rules")
    @GetMapping("{id}")
    public TurConnectorIndexingRuleModel turConnectorIndexingRuleGet(@PathVariable String id) {
        return indexingRuleService.getById(id)
                .orElse(new TurConnectorIndexingRuleModel());
    }



    @Operation(summary = "Update a Connector Indexing Rules")
    @PutMapping("/{id}")
    public TurConnectorIndexingRuleModel turConnectorIndexingRuleUpdate(@PathVariable String id,
                                                                        @RequestBody TurConnectorIndexingRuleModel indexingRule) {
        return indexingRuleService.update(id, indexingRule);
    }




    @Transactional
    @Operation(summary = "Delete a Connector Indexing Rules")
    @DeleteMapping("/{id}")
    public boolean turConnectorIndexingRuleDelete(@PathVariable String id) {
        indexingRuleService.deleteById(id);
        return true;
    }


    @Operation(summary = "Create a Connector Ranking Expression")
    @PostMapping
    public TurConnectorIndexingRuleModel turConnectorIndexingRuleAdd(
            @RequestBody TurConnectorIndexingRuleModel turConnectorIndexingRule) {
        turConnectorIndexingRule.setLastModifiedDate(new Date());
        return indexingRuleService.save(turConnectorIndexingRule);
    }

    @Operation(summary = "Connector Ranking Expression Structure")
    @GetMapping("structure")
    public TurConnectorIndexingRuleModel turConnectorIndexingRuleStructure() {
        return new TurConnectorIndexingRuleModel();

    }
}