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
package com.viglet.turing.connector.aem.commons.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class TurAemContentDefinitionProcess {
    public static List<TurSNAttributeSpec> getTargetAttrDefinitions(
            TurAemContentMapping turAemContentMapping) {
        return Optional.ofNullable(turAemContentMapping)
                .map(TurAemContentMapping::getTargetAttrDefinitions).orElse(new ArrayList<>());

    }

    public static String getDeltaClassName(TurAemContentMapping turAemContentMapping) {
        return Optional.ofNullable(turAemContentMapping)
                .map(TurAemContentMapping::getDeltaClassName).orElse(null);
    }

    public static Optional<TurAemModel> findByNameFromModelWithDefinition(String modelName,
            TurAemContentMapping turAemContentMapping) {
        return Optional.ofNullable(turAemContentMapping).flatMap(
                turCmsContentMapping -> findByNameFromModel(turCmsContentMapping.getModels(),
                        modelName).map(model -> {
                            List<TurAemTargetAttr> turCmsTargetAttrs = new ArrayList<>(
                                    addTargetAttrFromDefinition(model, turCmsContentMapping));
                            model.getTargetAttrs().forEach(turCmsTargetAttr -> {
                                if (turCmsTargetAttrs.stream().noneMatch(
                                        o -> o.getName().equals(turCmsTargetAttr.getName())))
                                    turCmsTargetAttrs.add(turCmsTargetAttr);
                            });
                            model.setTargetAttrs(turCmsTargetAttrs);
                            return model;
                        }));
    }

    private static Optional<TurAemModel> findByNameFromModel(final List<TurAemModel> turCmsModels,
            final String name) {
        return turCmsModels != null
                ? turCmsModels.stream().filter(o -> o != null && o.getType().equals(name))
                        .findFirst()
                : Optional.empty();
    }

    private static List<TurAemTargetAttr> addTargetAttrFromDefinition(TurAemModel model,
            TurAemContentMapping turCmsContentMapping) {
        List<TurAemTargetAttr> turCmsTargetAttrs = new ArrayList<>();
        turCmsContentMapping.getTargetAttrDefinitions()
                .forEach(
                        targetAttrDefinition -> findByNameFromTargetAttrs(model.getTargetAttrs(),
                                targetAttrDefinition.getName())
                                        .ifPresentOrElse(
                                                targetAttr -> turCmsTargetAttrs
                                                        .add(setTargetAttrFromDefinition(
                                                                targetAttrDefinition, targetAttr)),
                                                () -> {
                                                    if (targetAttrDefinition.isMandatory()) {
                                                        turCmsTargetAttrs
                                                                .add(setTargetAttrFromDefinition(
                                                                        targetAttrDefinition,
                                                                        new TurAemTargetAttr()));
                                                    }
                                                }));

        return turCmsTargetAttrs;
    }

    private static Optional<TurAemTargetAttr> findByNameFromTargetAttrs(
            final List<TurAemTargetAttr> turCmsTargetAttrs, final String name) {
        return turCmsTargetAttrs.stream().filter(o -> o.getName().equals(name)).findFirst();
    }

    private static TurAemTargetAttr setTargetAttrFromDefinition(
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
