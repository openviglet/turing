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

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.sn.bean.TurSNFieldRepairPayload;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.plugins.se.TurSEFieldSchema;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * REST endpoint that reconciles a single {@code TurSNSiteFieldExt} entry with
 * the corresponding field in the search engine's schema. Each {@link com.viglet.turing.api.sn.bean.TurSNFieldRepairType}
 * targets one specific discrepancy reported by {@code /field/check}, keeping
 * unrelated attributes untouched. {@code REPAIR_ALL} chains every applicable
 * repair for the given core in one round-trip.
 *
 * <p>T1243 — every schema call goes through the site's
 * {@link TurSearchEnginePlugin}, so an env-configured Solr is reached through its
 * effective instance and an engine without a schema answers for itself.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.7
 */
@Slf4j
@RestController
@RequestMapping("/api/sn/{siteId}/field/repair")
@Tag(name = "Semantic Navigation Field Repair", description = "Semantic Navigation Repair Field API")
@Transactional
public class TurSNSiteFieldRepairAPI {
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    private final TurSEInstanceRepository turSEInstanceRepository;
    private final TurSearchEnginePluginFactory pluginFactory;

    public TurSNSiteFieldRepairAPI(TurSNSiteRepository turSNSiteRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository,
            TurSEInstanceRepository turSEInstanceRepository,
            TurSearchEnginePluginFactory pluginFactory) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        this.turSEInstanceRepository = turSEInstanceRepository;
        this.pluginFactory = pluginFactory;
    }

    @Operation(summary = "Semantic Navigation Site Repair Field")
    @PostMapping
    public String turSNSiteFieldRepair(@PathVariable String siteId,
            @RequestBody TurSNFieldRepairPayload payload) {
        turSNSiteRepository.findById(siteId)
                .ifPresent(turSNSite -> turSNSiteFieldExtRepository.findById(payload.getId())
                        .ifPresent(fieldExt -> resolveInstance(turSNSite)
                                .ifPresent(turSEInstance -> applyRepair(payload, fieldExt,
                                        new Engine(pluginFactory.getPluginForSite(turSNSite), turSEInstance,
                                                payload.getCore())))));
        return "ok";
    }

    /** The site's engine, instance and core one repair works on. */
    private record Engine(TurSearchEnginePlugin plugin, TurSEInstance instance, String core) {

        Optional<TurSEFieldSchema> schema(String fieldName) {
            return plugin.fieldSchema(instance, core, fieldName);
        }

        void write(String fieldName, TurSEFieldType type, boolean multiValued, boolean isNew) {
            plugin.addOrUpdateField(instance, core, fieldName, type, true, multiValued, isNew);
        }
    }

    private Optional<TurSEInstance> resolveInstance(TurSNSite turSNSite) {
        return Optional.ofNullable(turSNSite.getTurSEInstance())
                .map(TurSEInstance::getId)
                .flatMap(turSEInstanceRepository::findById);
    }

    private void applyRepair(TurSNFieldRepairPayload payload, TurSNSiteFieldExt fieldExt, Engine engine) {
        switch (payload.getRepairType()) {
            case SE_CREATE_FIELD -> createField(engine, fieldExt);
            case SE_CHANGE_TYPE -> changeType(engine, fieldExt);
            case SE_ENABLE_MULTI_VALUE -> syncMultiValue(engine, fieldExt);
            case SN_CHANGE_TYPE -> changeTuringTypeToString(fieldExt);
            case REPAIR_ALL -> repairAll(engine, fieldExt);
        }
    }

    /**
     * Engine-side: create the field from scratch using Turing's configured type
     * and multiValued setting. Used when the field is missing from the schema.
     */
    private void createField(Engine engine, TurSNSiteFieldExt fieldExt) {
        engine.write(fieldExt.getName(), fieldExt.getType(), fieldExt.getMultiValued() == 1, true);
    }

    /**
     * Engine-side: replace the existing field, updating only the type to match
     * Turing's configuration. The engine's current multiValued setting is
     * preserved so this operation never silently flips that flag.
     */
    private void changeType(Engine engine, TurSNSiteFieldExt fieldExt) {
        boolean preservedMultiValued = engine.schema(fieldExt.getName()).map(TurSEFieldSchema::multiValued)
                .orElse(fieldExt.getMultiValued() == 1);
        engine.write(fieldExt.getName(), fieldExt.getType(), preservedMultiValued, false);
    }

    /**
     * Engine-side: replace the existing field, updating only multiValued to
     * match Turing's configuration. The engine's current type is preserved so
     * the type isn't changed by a multiValued repair.
     */
    private void syncMultiValue(Engine engine, TurSNSiteFieldExt fieldExt) {
        TurSEFieldType preservedType = engine.schema(fieldExt.getName()).map(TurSEFieldSchema::type)
                .orElse(fieldExt.getType());
        engine.write(fieldExt.getName(), preservedType, fieldExt.getMultiValued() == 1, false);
    }

    /**
     * Turing-side: when a facet field is misconfigured (e.g. facet=1 on a
     * TEXT field), normalize the Turing type to STRING so the facet works.
     * The engine's schema is not touched here — pair with {@code SE_CHANGE_TYPE}
     * (or {@code REPAIR_ALL}) to re-align the engine field afterwards.
     */
    private void changeTuringTypeToString(TurSNSiteFieldExt fieldExt) {
        fieldExt.setType(TurSEFieldType.STRING);
        turSNSiteFieldExtRepository.save(fieldExt);
    }

    /**
     * Apply every fix needed to bring the (field, core) tuple to a fully
     * correct state, in the right order: normalize Turing's type first
     * (so subsequent engine writes see the corrected type), then create or
     * replace the engine field with Turing's full state.
     */
    private void repairAll(Engine engine, TurSNSiteFieldExt fieldExt) {
        if (!isFacetConfigValid(fieldExt)) {
            changeTuringTypeToString(fieldExt);
        }
        Optional<TurSEFieldSchema> current = engine.schema(fieldExt.getName());
        if (current.isEmpty()) {
            // Absent from the schema, or an engine without one (whose write is a no-op).
            createField(engine, fieldExt);
            return;
        }
        boolean typeMatches = current.get().hasType(fieldExt.getType());
        boolean multiValuedMatches = current.get().multiValued() == (fieldExt.getMultiValued() == 1);
        if (typeMatches && multiValuedMatches) {
            return;
        }
        engine.write(fieldExt.getName(), fieldExt.getType(), fieldExt.getMultiValued() == 1, false);
    }

    private boolean isFacetConfigValid(TurSNSiteFieldExt fieldExt) {
        return fieldExt.getFacet() != 1 || !TurSEFieldType.TEXT.equals(fieldExt.getType());
    }
}