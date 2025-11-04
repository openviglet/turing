package com.viglet.turing.connector.plugin.aem.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentMapping;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemAttributeSpecification;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemPluginModel;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemTargetAttribute;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemAttributeSpecificationRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginModelRepository;

@Component
public class TurAemContentMappingService {
        private final TurAemPluginModelRepository turAemPluginModelRepository;
        private final TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository;

        public TurAemContentMappingService(TurAemPluginModelRepository turAemPluginModelRepository,
                        TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository) {
                this.turAemPluginModelRepository = turAemPluginModelRepository;
                this.turAemAttributeSpecificationRepository =
                                turAemAttributeSpecificationRepository;
        }

        public @NotNull TurAemContentMapping getTurAemContentMapping(TurAemSource turAemSource) {
                return TurAemContentMapping.builder()
                                .deltaClassName(turAemSource.getDeltaClass())
                                .models(getTurAemModels(turAemSource))
                                .targetAttrDefinitions(
                                                collectTurSNAttributeSpecifications(turAemSource))
                                .build();
        }

        private @NotNull List<TurAemModel> getTurAemModels(TurAemSource turAemSource) {
                return turAemPluginModelRepository.findByTurAemSource(turAemSource).stream()
                                .map(pluginModel -> TurAemModel.builder()
                                                .className(pluginModel.getClassName())
                                                .type(pluginModel.getType())
                                                .targetAttrs(getTurAemTargetAttrs(pluginModel))
                                                .build())
                                .toList();
        }

        private static @NotNull List<TurAemTargetAttr> getTurAemTargetAttrs(
                        TurAemPluginModel pluginModel) {
                return pluginModel.getTargetAttrs().stream()
                                .map(targetAttr -> TurAemTargetAttr.builder()
                                                .name(targetAttr.getName())
                                                .sourceAttrs(getTurAemSourceAttrs(targetAttr))
                                                .build())
                                .collect(Collectors.toList());
        }

        private static @NotNull List<TurAemSourceAttr> getTurAemSourceAttrs(
                        TurAemTargetAttribute targetAttr) {
                return targetAttr.getSourceAttrs().stream()
                                .map(sourceAttr -> TurAemSourceAttr.builder()
                                                .className(sourceAttr.getClassName())
                                                .name(sourceAttr.getName()).convertHtmlToText(false)
                                                .uniqueValues(false).build())
                                .toList();
        }

        private @NotNull List<TurSNAttributeSpec> collectTurSNAttributeSpecifications(
                        TurAemSource turAemSource) {
                return turAemAttributeSpecificationRepository.findByTurAemSource(turAemSource)
                                .map(attributeSpecifications -> attributeSpecifications.stream()
                                                .map(this::mapToTurSNAttributeSpec)
                                                .collect(Collectors.toList()))
                                .orElse(new ArrayList<>());
        }

        private TurSNAttributeSpec mapToTurSNAttributeSpec(
                        TurAemAttributeSpecification attributeSpec) {
                return TurSNAttributeSpec.builder()
                                .className(attributeSpec.getClassName())
                                .name(attributeSpec.getName())
                                .type(attributeSpec.getType())
                                .facetName(attributeSpec.getFacetNames())
                                .description(attributeSpec.getDescription())
                                .mandatory(attributeSpec.isMandatory())
                                .multiValued(attributeSpec.isMultiValued())
                                .facet(attributeSpec.isFacet())
                                .build();
        }
}
