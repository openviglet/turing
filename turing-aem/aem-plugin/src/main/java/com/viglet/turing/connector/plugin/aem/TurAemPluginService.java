package com.viglet.turing.connector.plugin.aem;

import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.config.IAemConfiguration;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentMapping;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemAttributeSpecificationRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TurAemPluginService {
    private final TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository;
    private final TurAemPluginModelRepository turAemPluginModelRepository;
    public TurAemPluginService(TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository,
                               TurAemPluginModelRepository turAemPluginModelRepository) {
        this.turAemAttributeSpecificationRepository = turAemAttributeSpecificationRepository;
        this.turAemPluginModelRepository = turAemPluginModelRepository;
    }

    public TurAemSourceContext getTurAemSourceContext(IAemConfiguration config) {
        TurAemSourceContext turAemSourceContext = TurAemSourceContext.builder()
                .id(config.getCmsGroup())
                .contentType(config.getCmsContentType())
                .defaultLocale(config.getDefaultLocale())
                .rootPath(config.getCmsRootPath())
                .url(config.getCmsHost())
                .subType(config.getCmsSubType())
                .oncePattern(config.getOncePatternPath())
                .providerName(config.getProviderName())
                .password(config.getCmsPassword())
                .authorURLPrefix(config.getAuthorURLPrefix())
                .publishURLPrefix(config.getPublishURLPrefix())
                .username(config.getCmsUsername())
                .localePaths(config.getLocales())
                .build();
        TurAemCommonsUtils.getInfinityJson(config.getCmsRootPath(), turAemSourceContext, false)
                .flatMap(infinityJson -> TurAemCommonsUtils
                        .getSiteName(turAemSourceContext, infinityJson))
                .ifPresent(turAemSourceContext::setSiteName);
        if (log.isDebugEnabled()) {
            log.debug("TurAemSourceContext: {}", turAemSourceContext);
        }
        return turAemSourceContext;
    }

    public @NotNull List<TurSNAttributeSpec> getTurSNAttributeSpecs(TurAemSource turAemSource) {
        List<TurSNAttributeSpec> targetAttrDefinitions = new ArrayList<>();
        turAemAttributeSpecificationRepository.findByTurAemSource(turAemSource)
                .ifPresent(attributeSpecifications -> attributeSpecifications
                        .forEach(attributeSpec ->
                                targetAttrDefinitions.add(TurSNAttributeSpec.builder()
                                        .className(attributeSpec.getClassName())
                                        .name(attributeSpec.getName())
                                        .type(attributeSpec.getType())
                                        .facetName(attributeSpec.getFacetNames())
                                        .description(attributeSpec.getDescription())
                                        .mandatory(attributeSpec.isMandatory())
                                        .multiValued(attributeSpec.isMultiValued())
                                        .facet(attributeSpec.isFacet())
                                        .build())));
        return targetAttrDefinitions;
    }

    public @NotNull TurAemContentMapping getTurAemContentMapping(TurAemSource turAemSource) {
        return TurAemContentMapping.builder()
                .deltaClassName(turAemSource.getDeltaClass())
                .models(getTurAemModels(turAemSource))
                .targetAttrDefinitions(getTurSNAttributeSpecs(turAemSource))
                .build();
    }

    private @NotNull List<TurAemModel> getTurAemModels(TurAemSource turAemSource) {
        return turAemPluginModelRepository.findByTurAemSource(turAemSource)
                .stream()
                .map(pluginModel -> TurAemModel.builder()
                        .className(pluginModel.getClassName())
                        .type(pluginModel.getType())
                        .targetAttrs(TurAemPluginUtils.getTurAemTargetAttrs(pluginModel))
                        .build())
                .toList();
    }

}
