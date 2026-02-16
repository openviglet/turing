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

package com.viglet.turing.api.sn.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sn/{snSiteId}/field/ext")
@Tag(name = "Semantic Navigation Field Ext", description = "Semantic Navigation Field Ext API")
public class TurSNSiteFieldExtAPI {

    private final TurSNSiteFieldExtService fieldExtService;
    private final TurSolrSchemaService solrSchemaService;
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    public TurSNSiteFieldExtAPI(
            TurSNSiteFieldExtService fieldExtService,
            TurSolrSchemaService solrSchemaService,
            TurSNSiteRepository turSNSiteRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.fieldExtService = fieldExtService;
        this.solrSchemaService = solrSchemaService;
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    @Operation(summary = "Semantic Navigation Site Field Ext List")
    @Transactional
    @GetMapping
    public List<TurSNSiteFieldExt> turSNSiteFieldExtList(@PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId)
                .map(site -> fieldExtService.listFieldExtensionsForSite(site))
                .orElse(Collections.emptyList());
    }

    @Operation(summary = "Show a Semantic Navigation Site Field Ext")
    @GetMapping("/{id}")
    public TurSNSiteFieldExt turSNSiteFieldExtGet(@PathVariable String snSiteId, @PathVariable String id) {
        return fieldExtService.getFieldExtWithFacets(id);
    }

    @Operation(summary = "Update a Semantic Navigation Site Field Ext")
    @PutMapping("/{id}")
    @Transactional
    public TurSNSiteFieldExt turSNSiteFieldExtUpdate(@PathVariable String snSiteId, @PathVariable String id,
            @RequestBody TurSNSiteFieldExt turSNSiteFieldExt) {
        return turSNSiteRepository.findById(snSiteId)
                .map(site -> fieldExtService.updateFieldExt(id, turSNSiteFieldExt, site))
                .orElse(TurSNSiteFieldExt.builder().build());
    }

    @Transactional
    @Operation(summary = "Delete a Semantic Navigation Site Field Ext")
    @DeleteMapping("/{id}")
    public boolean turSNSiteFieldExtDelete(@PathVariable String snSiteId, @PathVariable String id) {
        return fieldExtService.deleteFieldExt(id);
    }

    @Operation(summary = "Create a Semantic Navigation Site Field Ext")
    @PostMapping
    public TurSNSiteFieldExt turSNSiteFieldExtAdd(@PathVariable String snSiteId,
            @RequestBody TurSNSiteFieldExt turSNSiteFieldExt) {
        return turSNSiteRepository.findById(snSiteId)
                .map(site -> fieldExtService.createFieldExt(site, turSNSiteFieldExt))
                .orElse(TurSNSiteFieldExt.builder().build());
    }

    @Operation(summary = "Semantic Navigation Site Field Ext structure")
    @GetMapping("/structure")
    public TurSNSiteFieldExt turSNSiteFieldExtStructure(@PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId)
                .map(site -> TurSNSiteFieldExt.builder().turSNSite(site).build())
                .orElse(new TurSNSiteFieldExt());
    }

    @GetMapping("/create/{localeRequest}")
    @Transactional
    public List<TurSNSite> turSNSiteFieldExtCreate(@PathVariable String snSiteId,
            @PathVariable String localeRequest) {
        Locale locale = LocaleUtils.toLocale(localeRequest);
        return turSNSiteRepository.findById(snSiteId)
                .map(site -> fieldExtService.createFieldsForLocale(site, locale))
                .orElse(new ArrayList<>());
    }
}
