/*
 * Copyright (C) 2016-2023 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.connector.plugin.aem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.commons.cache.TurCustomClassCache;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.ext.TurAemExtDeltaDate;
import com.viglet.turing.connector.aem.commons.ext.TurAemExtDeltaDateInterface;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentMapping;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.service.TurAemContentMappingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TurAemContentDefinitionService {
        private final TurAemContentMappingService turAemContentMappingService;

        public TurAemContentDefinitionService(TurAemContentMappingService turAemContentMappingService) {
                this.turAemContentMappingService = turAemContentMappingService;
        }

        public List<TurSNAttributeSpec> getTargetAttrDefinitions(
                        TurAemContentMapping turAemContentMapping) {
                return Optional.ofNullable(turAemContentMapping)
                                .map(TurAemContentMapping::getTargetAttrDefinitions).orElse(new ArrayList<>());

        }

        public String getDeltaClassName(TurAemContentMapping turAemContentMapping) {
                return Optional.ofNullable(turAemContentMapping)
                                .map(TurAemContentMapping::getDeltaClassName).orElse(null);
        }

        public Date getDeltaDate(TurAemObject aemObject, TurAemConfiguration turAemSourceContext,
                        TurAemContentMapping turAemContentMapping) {
                Date deltaDate = Optional
                                .ofNullable(getDeltaClassName(turAemContentMapping))
                                .map(className -> TurCustomClassCache.getCustomClassMap(className)
                                                .map(classInstance -> ((TurAemExtDeltaDateInterface) classInstance)
                                                                .consume(aemObject, turAemSourceContext))
                                                .orElseGet(() -> defaultDeltaDate(aemObject, turAemSourceContext)))
                                .orElseGet(() -> defaultDeltaDate(aemObject, turAemSourceContext));
                log.debug("Delta Date {} from {}", deltaDate.toString(), aemObject.getPath());
                return deltaDate;
        }

        private Date defaultDeltaDate(TurAemObject aemObject,
                        TurAemConfiguration turAemSourceContext) {
                return new TurAemExtDeltaDate().consume(aemObject, turAemSourceContext);
        }

        public Optional<TurAemModel> findByModelNameAndAemSource(String modelName,
                        TurAemSource turAemSource) {
                TurAemContentMapping turAemContentMapping = turAemContentMappingService
                                .getTurAemContentMapping(turAemSource);
                return Optional.ofNullable(turAemContentMapping).flatMap(
                                turCmsContentMapping -> findByNameFromModel(turCmsContentMapping.getModels(),
                                                modelName).map(model -> {
                                                        List<TurAemTargetAttr> turCmsTargetAttrs = new ArrayList<>(
                                                                        addTargetAttrFromDefinition(model,
                                                                                        turCmsContentMapping));
                                                        model.getTargetAttrs().forEach(turCmsTargetAttr -> {
                                                                if (turCmsTargetAttrs.stream().noneMatch(
                                                                                o -> o.getName().equals(turCmsTargetAttr
                                                                                                .getName())))
                                                                        turCmsTargetAttrs.add(turCmsTargetAttr);
                                                        });
                                                        model.setTargetAttrs(turCmsTargetAttrs);
                                                        return model;
                                                }));
        }

        public Optional<TurAemModel> findByNameFromModelWithDefinition(String modelName,
                        TurAemContentMapping turAemContentMapping) {
                return Optional.ofNullable(turAemContentMapping).flatMap(
                                turCmsContentMapping -> findByNameFromModel(turCmsContentMapping.getModels(),
                                                modelName).map(model -> {
                                                        List<TurAemTargetAttr> turCmsTargetAttrs = new ArrayList<>(
                                                                        addTargetAttrFromDefinition(model,
                                                                                        turCmsContentMapping));
                                                        model.getTargetAttrs().forEach(turCmsTargetAttr -> {
                                                                if (turCmsTargetAttrs.stream().noneMatch(
                                                                                o -> o.getName().equals(turCmsTargetAttr
                                                                                                .getName())))
                                                                        turCmsTargetAttrs.add(turCmsTargetAttr);
                                                        });
                                                        model.setTargetAttrs(turCmsTargetAttrs);
                                                        return model;
                                                }));
        }

        private Optional<TurAemModel> findByNameFromModel(final List<TurAemModel> turCmsModels,
                        final String name) {
                return turCmsModels != null
                                ? turCmsModels.stream().filter(o -> o != null && o.getType().equals(name))
                                                .findFirst()
                                : Optional.empty();
        }

        private List<TurAemTargetAttr> addTargetAttrFromDefinition(TurAemModel model,
                        TurAemContentMapping turCmsContentMapping) {
                List<TurAemTargetAttr> turCmsTargetAttrs = new ArrayList<>();
                turCmsContentMapping.getTargetAttrDefinitions()
                                .forEach(
                                                targetAttrDefinition -> findByNameFromTargetAttrs(
                                                                model.getTargetAttrs(),
                                                                targetAttrDefinition.getName())
                                                                .ifPresentOrElse(
                                                                                targetAttr -> turCmsTargetAttrs
                                                                                                .add(setTargetAttrFromDefinition(
                                                                                                                targetAttrDefinition,
                                                                                                                targetAttr)),
                                                                                () -> {
                                                                                        if (targetAttrDefinition
                                                                                                        .isMandatory()) {
                                                                                                turCmsTargetAttrs
                                                                                                                .add(setTargetAttrFromDefinition(
                                                                                                                                targetAttrDefinition,
                                                                                                                                new TurAemTargetAttr()));
                                                                                        }
                                                                                }));

                return turCmsTargetAttrs;
        }

        private Optional<TurAemTargetAttr> findByNameFromTargetAttrs(
                        final List<TurAemTargetAttr> turCmsTargetAttrs, final String name) {
                return turCmsTargetAttrs.stream().filter(o -> o.getName().equals(name)).findFirst();
        }

        private TurAemTargetAttr setTargetAttrFromDefinition(
                        TurSNAttributeSpec turSNAttributeSpec, TurAemTargetAttr targetAttr) {
                if (StringUtils.isBlank(targetAttr.getName())) {
                        targetAttr.setName(turSNAttributeSpec.getName());
                }
                if (StringUtils.isNotBlank(turSNAttributeSpec.getClassName())) {
                        if (CollectionUtils.isEmpty(targetAttr.getSourceAttrs())) {
                                List<TurAemSourceAttr> sourceAttrs = Collections.singletonList(
                                                TurAemSourceAttr.builder().className(turSNAttributeSpec.getClassName())
                                                                .uniqueValues(false).convertHtmlToText(false).build());
                                targetAttr.setSourceAttrs(sourceAttrs);
                                targetAttr.setClassName(turSNAttributeSpec.getClassName());
                        } else {
                                targetAttr.getSourceAttrs().stream()
                                                .filter(turCmsSourceAttr -> Objects.nonNull(turCmsSourceAttr)
                                                                && StringUtils.isBlank(turCmsSourceAttr.getClassName()))
                                                .forEach(turCmsSourceAttr -> turCmsSourceAttr
                                                                .setClassName(turSNAttributeSpec.getClassName()));
                        }
                }
                targetAttr.setDescription(turSNAttributeSpec.getDescription());
                targetAttr.setFacet(turSNAttributeSpec.isFacet());
                targetAttr.setFacetName(turSNAttributeSpec.getFacetName());
                targetAttr.setMandatory(turSNAttributeSpec.isMandatory());
                targetAttr.setMultiValued(turSNAttributeSpec.isMultiValued());
                targetAttr.setType(turSNAttributeSpec.getType());
                return targetAttr;

        }
}
