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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.TurSNSiteFacetRangeEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldSortEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteField;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExtFacet;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.sn.TurSNFieldType;
import com.viglet.turing.sn.template.TurSNTemplate;
import com.viglet.turing.solr.TurSolrFieldAction;
import com.viglet.turing.solr.TurSolrUtils;
import com.viglet.turing.spring.utils.TurPersistenceUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sn/{snSiteId}/field/ext")
@Tag(name = "Semantic Navigation Field Ext", description = "Semantic Navigation Field Ext API")
public class TurSNSiteFieldExtAPI {
    public static final String TEXT_GENERAL = "text_general";
    public static final String MULTI_VALUED = "multiValued";
    public static final String STORED = "stored";
    public static final String INDEXED = "indexed";
    public static final String TYPE = "type";
    public static final String STRING = "string";
    public static final String ADD_FIELD = "add-field";
    public static final String TEXT = "_text_";
    public static final String SOURCE = "source";
    public static final String ADD_COPY_FIELD = "add-copy-field";
    public static final String DEST = "dest";
    public static final String PDATE = "pdate";
    public static final String NAME = "name";
    public static final String SOLR_SCHEMA_REQUEST = "http://%s:%d/solr/%s/schema";
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    private final TurSNSiteFieldExtFacetRepository turSNSiteFieldExtFacetRepository;
    private final TurSNSiteFieldRepository turSNSiteFieldRepository;
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurSEInstanceRepository turSEInstanceRepository;
    private final TurSNTemplate turSNTemplate;

    public TurSNSiteFieldExtAPI(TurSNSiteRepository turSNSiteRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository,
            TurSNSiteFieldExtFacetRepository turSNSiteFieldExtFacetRepository,
            TurSNSiteFieldRepository turSNSiteFieldRepository,
            TurSNSiteLocaleRepository turSNSiteLocaleRepository,
            TurSEInstanceRepository turSEInstanceRepository,
            TurSNTemplate turSNTemplate) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        this.turSNSiteFieldExtFacetRepository = turSNSiteFieldExtFacetRepository;
        this.turSNSiteFieldRepository = turSNSiteFieldRepository;
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
        this.turSEInstanceRepository = turSEInstanceRepository;
        this.turSNTemplate = turSNTemplate;
    }

    @Operation(summary = "Semantic Navigation Site Field Ext List")
    @Transactional
    @GetMapping
    public List<TurSNSiteFieldExt> turSNSiteFieldExtList(@PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(turSNSite -> {
            updateFieldExtFromSNSite(turSNSite);
            return turSNSiteFieldExtRepository
                    .findByTurSNSite(TurPersistenceUtils.orderByNameIgnoreCase(), turSNSite);

        }).orElse(Collections.emptyList());

    }

    @Operation(summary = "Show a Semantic Navigation Site Field Ext")
    @GetMapping("/{id}")
    public TurSNSiteFieldExt turSNSiteFieldExtGet(@PathVariable String snSiteId, @PathVariable String id) {
        TurSNSiteFieldExt turSNSiteFieldExt = turSNSiteFieldExtRepository.findById(id)
                .orElse(TurSNSiteFieldExt.builder().build());
        turSNSiteFieldExt.setFacetLocales(turSNSiteFieldExtFacetRepository.findByTurSNSiteFieldExt(turSNSiteFieldExt));
        return turSNSiteFieldExt;
    }

    @GetMapping("/create/{localeRequest}")
    public List<TurSNSite> turSNSiteFieldExtCreate(@PathVariable String snSiteId,
            @PathVariable String localeRequest) {
        Locale locale = LocaleUtils.toLocale(localeRequest);
        return turSNSiteRepository.findById(snSiteId).map(turSNSite -> {
            List<TurSNSiteFieldExt> turSNSiteFieldExtList = turSNSiteFieldExtRepository
                    .findByTurSNSiteAndEnabled(turSNSite, 1);
            turSNSiteFieldExtList.forEach(turSNSiteFieldExt -> this.createField(turSNSite, locale, turSNSiteFieldExt));
            return this.turSNSiteRepository.findAll();
        }).orElse(new ArrayList<>());
    }

    @Operation(summary = "Semantic Navigation Site Field Ext structure")
    @GetMapping("structure")
    public TurSNSiteFieldExt turSNSiteFieldExtStructure(@PathVariable String snSiteId) {
        return turSNSiteRepository.findById(snSiteId).map(turSNSite -> {
            TurSNSiteFieldExt turSNSiteFieldExt = new TurSNSiteFieldExt();
            turSNSiteFieldExt.setTurSNSite(turSNSite);
            return turSNSiteFieldExt;
        }).orElse(new TurSNSiteFieldExt());
    }

    @Operation(summary = "Create a Semantic Navigation Site Field Ext")
    @PostMapping
    public TurSNSiteFieldExt turSNSiteFieldExtAdd(@PathVariable String snSiteId,
            @RequestBody TurSNSiteFieldExt turSNSiteFieldExt) {
        return createSEField(snSiteId, turSNSiteFieldExt);
    }

    @Operation(summary = "Update a Semantic Navigation Site Field Ext")
    @PutMapping("/{id}")
    @Transactional
    public TurSNSiteFieldExt turSNSiteFieldExtUpdate(@PathVariable String snSiteId, @PathVariable String id,
            @RequestBody TurSNSiteFieldExt turSNSiteFieldExt) {
        return this.turSNSiteFieldExtRepository.findById(id).map(turSNSiteFieldExtEdit -> {
            updateFieldExtProperties(turSNSiteFieldExtEdit, turSNSiteFieldExt);
            updateFacetPosition(turSNSiteFieldExtEdit, turSNSiteFieldExt);

            this.turSNSiteFieldExtRepository.save(turSNSiteFieldExtEdit);
            turSNSiteRepository.findById(snSiteId)
                    .ifPresent(turSNSite -> this.updateExternalField(turSNSiteFieldExt, turSNSite));

            return turSNSiteFieldExtEdit;
        }).orElse(TurSNSiteFieldExt.builder().build());
    }

    @Transactional
    @Operation(summary = "Delete a Semantic Navigation Site Field Ext")
    @DeleteMapping("/{id}")
    public boolean turSNSiteFieldExtDelete(@PathVariable String snSiteId, @PathVariable String id) {
        return this.turSNSiteFieldExtRepository.findById(id).map(turSNSiteFieldExt -> {
            if (turSNSiteFieldExt.getSnType().equals(TurSNFieldType.SE)) {
                this.turSNSiteFieldRepository.delete(turSNSiteFieldExt.getExternalId());
            }
            this.turSNSiteFieldExtRepository.delete(id);
            turSNSiteRepository.findById(snSiteId)
                    .ifPresent(turSNSite -> turSNSiteFieldRepository.findById(turSNSiteFieldExt.getExternalId())
                            .ifPresent(turSNSiteField -> this.deleteSolrSchema(turSNSite, turSNSiteField)));
            return true;
        }).orElse(false);
    }

    private void updateFieldExtFromSNSite(TurSNSite turSNSite) {
        turSNSiteFieldExtRepository.deleteByTurSNSiteAndSnType(turSNSite, TurSNFieldType.NER);
        Map<String, TurSNSiteField> fieldMap = createFieldMap(turSNSite);
        List<TurSNSiteFieldExt> turSNSiteFieldExtList = this.turSNSiteFieldExtRepository
                .findByTurSNSite(TurPersistenceUtils.orderByNameIgnoreCase(), turSNSite);
        removeDuplicatedFields(fieldMap, turSNSiteFieldExtList);
        for (TurSNSiteField turSNSiteField : fieldMap.values()) {
            TurSNSiteFieldExt turSNSiteFieldExt = saveSNSiteFieldExt(turSNSite, turSNSiteField);
            turSNSiteFieldExtList.add(turSNSiteFieldExt);
        }
    }

    private Map<String, TurSNSiteField> createFieldMap(TurSNSite turSNSite) {
        List<TurSNSiteField> turSNSiteFields = turSNSiteFieldRepository.findByTurSNSite(turSNSite);
        if (turSNSiteFields.isEmpty()) {
            turSNTemplate.createSEFields(turSNSite);
            turSNSiteFields = turSNSiteFieldRepository.findByTurSNSite(turSNSite);
        }
        Map<String, TurSNSiteField> fieldMap = new HashMap<>();
        turSNSiteFields.forEach(turSNSiteField -> fieldMap.put(turSNSiteField.getId(), turSNSiteField));
        return fieldMap;
    }

    private void removeDuplicatedFields(Map<String, TurSNSiteField> fieldMap,
            List<TurSNSiteFieldExt> turSNSiteFieldExtensions) {
        for (TurSNSiteFieldExt turSNSiteFieldExtension : turSNSiteFieldExtensions) {
            if (Objects.requireNonNull(turSNSiteFieldExtension.getSnType()) == TurSNFieldType.SE) {
                fieldMap.remove(turSNSiteFieldExtension.getExternalId());
            }
        }
    }

    private TurSNSiteFieldExt saveSNSiteFieldExt(TurSNSite turSNSite, TurSNSiteField turSNSiteField) {
        return turSNSiteFieldExtRepository.save(TurSNSiteFieldExt.builder()
                .enabled(0)
                .name(turSNSiteField.getName())
                .description(turSNSiteField.getDescription())
                .facet(0)
                .facetName(turSNSiteField.getName())
                .facetRange(TurSNSiteFacetRangeEnum.DISABLED)
                .facetType(TurSNSiteFacetFieldEnum.DEFAULT)
                .facetItemType(TurSNSiteFacetFieldEnum.DEFAULT)
                .facetSort(TurSNSiteFacetFieldSortEnum.COUNT)
                .showAllFacetItems(false)
                .secondaryFacet(false)
                .hl(0)
                .multiValued(turSNSiteField.getMultiValued())
                .facetPosition(0)
                .mlt(0)
                .externalId(turSNSiteField.getId())
                .snType(TurSNFieldType.SE)
                .type(turSNSiteField.getType())
                .turSNSite(turSNSite).build());
    }

    private void updateFieldExtProperties(TurSNSiteFieldExt target, TurSNSiteFieldExt source) {
        target.setFacetName(source.getFacetName());
        target.setMultiValued(source.getMultiValued());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setType(source.getType());
        target.setFacet(source.getFacet());
        target.setFacetLocales(getTurSNSiteFieldExtFacets(source));
        target.setFacetRange(source.getFacetRange());
        target.setFacetSort(source.getFacetSort());
        target.setFacetType(source.getFacetType());
        target.setFacetItemType(source.getFacetItemType());
        target.setSecondaryFacet(source.getSecondaryFacet());
        target.setShowAllFacetItems(source.getShowAllFacetItems());
        target.setHl(source.getHl());
        target.setEnabled(source.getEnabled());
        target.setMlt(source.getMlt());
        target.setExternalId(source.getExternalId());
        target.setRequired(source.getRequired());
        target.setDefaultValue(source.getDefaultValue());
        target.setSnType(source.getSnType());
    }

    private void updateFacetPosition(TurSNSiteFieldExt target, TurSNSiteFieldExt source) {
        if (isFacetEnabled(source)) {
            target.setFacetPosition(hasFacetPosition(source) ? source.getFacetPosition()
                    : getFacetPositionIncrement());
        } else {
            target.setFacetPosition(0);
        }
    }

    @NotNull
    private static Set<TurSNSiteFieldExtFacet> getTurSNSiteFieldExtFacets(TurSNSiteFieldExt turSNSiteFieldExt) {
        Set<TurSNSiteFieldExtFacet> facetLocales = new HashSet<>();
        if (turSNSiteFieldExt.getFacetLocales() == null) {
            return facetLocales;
        }
        for (TurSNSiteFieldExtFacet fieldExtFacet : turSNSiteFieldExt.getFacetLocales()) {
            fieldExtFacet.setTurSNSiteFieldExt(turSNSiteFieldExt);
            facetLocales.add(fieldExtFacet);
        }
        return facetLocales;
    }

    private static boolean hasFacetPosition(TurSNSiteFieldExt turSNSiteFieldExt) {
        return turSNSiteFieldExt.getFacetPosition() != null && turSNSiteFieldExt.getFacetPosition() > 0;
    }

    private static boolean isFacetEnabled(TurSNSiteFieldExt turSNSiteFieldExt) {
        return turSNSiteFieldExt.getFacet() == 1;
    }

    @NotNull
    private Integer getFacetPositionIncrement() {
        return this.turSNSiteFieldExtRepository.findMaxFacetPosition().map(max -> max + 1).orElse(1);
    }

    private TurSNSiteFieldExt createSEField(String snSiteId, TurSNSiteFieldExt turSNSiteFieldExt) {
        return turSNSiteRepository.findById(snSiteId)
                .map(turSNSite -> createSEFieldForSite(turSNSite, turSNSiteFieldExt))
                .orElse(TurSNSiteFieldExt.builder().build());
    }

    private TurSNSiteFieldExt createSEFieldForSite(TurSNSite turSNSite, TurSNSiteFieldExt turSNSiteFieldExt) {
        TurSNSiteField turSNSiteField = buildAndSaveSiteField(turSNSite, turSNSiteFieldExt);
        TurSNSiteFieldExt savedFieldExt = buildAndSaveFieldExt(turSNSite, turSNSiteFieldExt, turSNSiteField);
        updateSolrSchema(turSNSite, turSNSiteField);
        return savedFieldExt;
    }

    private TurSNSiteField buildAndSaveSiteField(TurSNSite turSNSite, TurSNSiteFieldExt turSNSiteFieldExt) {
        TurSNSiteField turSNSiteField = new TurSNSiteField();
        turSNSiteField.setDescription(turSNSiteFieldExt.getDescription());
        turSNSiteField.setMultiValued(turSNSiteFieldExt.getMultiValued());
        turSNSiteField.setName(turSNSiteFieldExt.getName());
        turSNSiteField.setType(turSNSiteFieldExt.getType());
        turSNSiteField.setTurSNSite(turSNSite);
        return turSNSiteFieldRepository.save(turSNSiteField);
    }

    private TurSNSiteFieldExt buildAndSaveFieldExt(TurSNSite turSNSite, TurSNSiteFieldExt turSNSiteFieldExt,
            TurSNSiteField turSNSiteField) {
        turSNSiteFieldExt.setTurSNSite(turSNSite);
        turSNSiteFieldExt.setSnType(TurSNFieldType.SE);
        turSNSiteFieldExt.setExternalId(turSNSiteField.getId());
        turSNSiteFieldExt.setFacetPosition(calculateFacetPosition(turSNSiteFieldExt));
        return turSNSiteFieldExtRepository.save(turSNSiteFieldExt);
    }

    private Integer calculateFacetPosition(TurSNSiteFieldExt turSNSiteFieldExt) {
        return isFacetEnabled(turSNSiteFieldExt) ? getFacetPositionIncrement() : 0;
    }

    private void deleteSolrSchema(TurSNSite turSNSite, TurSNSiteField turSNSiteField) {
        turSEInstanceRepository.findById(turSNSite.getTurSEInstance().getId())
                .ifPresent(turSEInstance -> turSNSite.getTurSNSiteLocales().parallelStream()
                        .forEach(turSNSiteLocale -> TurSolrUtils.deleteField(
                                turSEInstance,
                                turSNSiteLocale.getCore(),
                                turSNSiteField.getName(),
                                turSNSiteField.getType())));
    }

    private void updateSolrSchema(TurSNSite turSNSite, TurSNSiteField turSNSiteField) {
        turSEInstanceRepository.findById(turSNSite.getTurSEInstance().getId())
                .ifPresent(turSEInstance -> turSNSite.getTurSNSiteLocales().parallelStream()
                        .forEach(turSNSiteLocale -> TurSolrUtils.addOrUpdateField(
                                TurSolrFieldAction.ADD,
                                turSEInstance,
                                turSNSiteLocale.getCore(),
                                turSNSiteField.getName(),
                                turSNSiteField.getType(),
                                true,
                                turSNSiteField.getMultiValued() == 1)));
    }

    public void updateExternalField(TurSNSiteFieldExt turSNSiteFieldExt, TurSNSite turSNSite) {
        if (Objects.requireNonNull(turSNSiteFieldExt.getSnType()) == TurSNFieldType.SE) {
            turSNSiteFieldRepository.findById(turSNSiteFieldExt.getExternalId()).ifPresent(turSNSiteField -> {
                turSNSiteField.setDescription(turSNSiteFieldExt.getDescription());
                turSNSiteField.setMultiValued(turSNSiteFieldExt.getMultiValued());
                turSNSiteField.setName(turSNSiteFieldExt.getName());
                turSNSiteField.setType(turSNSiteFieldExt.getType());
                this.turSNSiteFieldRepository.save(turSNSiteField);
                updateSolrSchema(turSNSite, turSNSiteField);
            });
        }
    }

    public void createField(TurSNSite turSNSite, Locale locale, TurSNSiteFieldExt turSNSiteFieldExt) {
        TurSNSiteLocale turSNSiteLocale = turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(turSNSite, locale);
        String fieldName = getSolrFieldName(turSNSiteFieldExt);
        JSONObject json = buildAddFieldPayload(turSNSiteFieldExt, fieldName);
        executeSchemaPost(turSNSiteLocale.getTurSNSite().getTurSEInstance(), turSNSiteLocale.getCore(), json);
        copyField(turSNSiteLocale, fieldName, TEXT);
    }

    public void copyField(TurSNSiteLocale turSNSiteLocale, String field, String dest) {
        JSONObject jsonAddField = new JSONObject();
        jsonAddField.put(SOURCE, field);
        jsonAddField.put(DEST, dest);
        JSONObject json = new JSONObject();
        json.put(ADD_COPY_FIELD, jsonAddField);
        TurSEInstance turSEInstance = turSNSiteLocale.getTurSNSite().getTurSEInstance();
        executeSchemaPost(turSEInstance, turSNSiteLocale.getCore(), json);
    }

    private String getSolrFieldName(TurSNSiteFieldExt turSNSiteFieldExt) {
        if (turSNSiteFieldExt.getSnType() == TurSNFieldType.NER) {
            return String.format("turing_entity_%s", turSNSiteFieldExt.getName());
        }
        return turSNSiteFieldExt.getName();
    }

    private JSONObject buildAddFieldPayload(TurSNSiteFieldExt turSNSiteFieldExt, String fieldName) {
        JSONObject jsonAddField = new JSONObject();
        jsonAddField.put(NAME, fieldName);
        jsonAddField.put(INDEXED, true);
        jsonAddField.put(STORED, true);
        boolean multiValued = turSNSiteFieldExt.getMultiValued() == 1;
        jsonAddField.put(MULTI_VALUED, multiValued);
        jsonAddField.put(TYPE, resolveSolrType(turSNSiteFieldExt, multiValued));

        JSONObject json = new JSONObject();
        json.put(ADD_FIELD, jsonAddField);
        return json;
    }

    private String resolveSolrType(TurSNSiteFieldExt turSNSiteFieldExt, boolean multiValued) {
        if (multiValued) {
            return STRING;
        }
        if (turSNSiteFieldExt.getType().equals(TurSEFieldType.DATE)) {
            return PDATE;
        }
        return TEXT_GENERAL;
    }

    private void executeSchemaPost(TurSEInstance turSEInstance, String core, JSONObject json) {
        HttpPost httpPost = new HttpPost(String.format(SOLR_SCHEMA_REQUEST, turSEInstance.getHost(),
                turSEInstance.getPort(), core));
        executeHttpPost(json, httpPost);
    }

    private void executeHttpPost(JSONObject json, HttpPost httpPost) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            StringEntity entity = new StringEntity(json.toString());
            httpPost.setEntity(entity);
            httpPost.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    log.warn("Solr schema request failed with status: {}", statusCode);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
