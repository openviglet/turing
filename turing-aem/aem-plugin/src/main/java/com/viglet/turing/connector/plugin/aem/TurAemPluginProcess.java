/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.plugin.aem;

import com.google.inject.Inject;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.config.IAemConfiguration;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.mappers.*;
import com.viglet.turing.connector.commons.plugin.TurConnectorContext;
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.conf.AemPluginHandlerConfiguration;
import com.viglet.turing.connector.plugin.aem.persistence.model.*;
import com.viglet.turing.connector.plugin.aem.persistence.repository.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR_PRIMARY_TYPE;


/**
 * @author Alexandre Oliveira
 * @since 2025.2
 */
@Slf4j
@Getter
@Component
public class TurAemPluginProcess {
    public static final String CONTENT_FRAGMENT = "content-fragment";
    public static final String STATIC_FILE = "static-file";
    public static final String SITE = "site";
    public static final String DATA_MASTER = "data/master";
    public static final String METADATA = "metadata";
    public static final String JCR = "jcr:";
    public static final String STATIC_FILE_SUB_TYPE = "STATIC_FILE";
    private static final String CQ_PAGE = "cq:Page";
    private static final String DAM_ASSET = "dam:Asset";
    public static final String CQ = "cq:";
    public static final String AEM = "AEM";
    private final String deltaId = UUID.randomUUID().toString();
    private final Set<String> visitedLinks = new HashSet<>();
    private final Queue<String> remainingLinks = new LinkedList<>();
    private final TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository;
    private String siteName;
    public static final String REP = "rep:";
    private final TurAemPluginSystemRepository turAemSystemRepository;
    private final TurAemConfigVarRepository turAemConfigVarRepository;
    private final TurAemSourceLocalePathRepository turAemSourceLocalePathRepository;
    private final TurAemPluginModelRepository turAemPluginModelRepository;
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemTargetAttributeRepository turAemTargetAttributeRepository;
    private final TurConnectorContext turConnectorContext;
    private IAemConfiguration config = null;
    private TurAemContentDefinitionProcess turAemContentDefinitionProcess;
    private final String turingUrl;
    private final String turingApiKey;

    @Inject
    public TurAemPluginProcess(TurAemPluginSystemRepository turAemPluginSystemRepository,
                               TurAemConfigVarRepository turAemConfigVarRepository,
                               TurAemSourceLocalePathRepository turAemSourceLocalePathRepository,
                               TurAemPluginModelRepository turAemPluginModelRepository,
                               TurAemSourceRepository turAemSourceRepository,
                               TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository,
                               TurAemTargetAttributeRepository turAemTargetAttributeRepository, TurConnectorContext turConnectorContext,
                               @Value("${turing.url}") String turingUrl,
                               @Value("${turing.apiKey}") String turingApiKey) {
        this.turAemSystemRepository = turAemPluginSystemRepository;
        this.turAemConfigVarRepository = turAemConfigVarRepository;
        this.turAemSourceLocalePathRepository = turAemSourceLocalePathRepository;
        this.turAemPluginModelRepository = turAemPluginModelRepository;
        this.turAemSourceRepository = turAemSourceRepository;
        this.turAemAttributeSpecificationRepository = turAemAttributeSpecificationRepository;
        this.turAemTargetAttributeRepository = turAemTargetAttributeRepository;
        this.turConnectorContext = turConnectorContext;
        this.turingUrl = turingUrl;
        this.turingApiKey = turingApiKey;
    }

    public void run(TurAemSource turAemSource) {
        TurConnectorSession turConnectorSession = getTurConnectorSession(turAemSource);
        config = new AemPluginHandlerConfiguration(turAemSource);

        turAemContentDefinitionProcess = new TurAemContentDefinitionProcess(getTurAemContentMapping(turAemSource));
        TurAemSourceContext turAemSourceContext = getTurAemSourceContext(config);
        try {
            this.getNodesFromJson(turAemSourceContext, turConnectorSession, turAemSource);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        finished(turConnectorSession);
    }

    public static @NotNull TurConnectorSession getTurConnectorSession(TurAemSource turAemSource) {
        // sites parameter is null, because in next step need check if author our publishing.
        return new TurConnectorSession(turAemSource.getName(), null, AEM, turAemSource.getDefaultLocale());
    }

    public void finished(TurConnectorSession turConnectorSession) {
        turConnectorContext.finishIndexing(turConnectorSession);
    }


    private @NotNull TurAemContentMapping getTurAemContentMapping(TurAemSource turAemSource) {
        List<TurAemModel> turAemModels = new ArrayList<>();
        turAemPluginModelRepository.findByTurAemSource(turAemSource).forEach(pluginModel -> {
            List<TurAemTargetAttr> targetAttrs = new ArrayList<>();
            pluginModel.getTargetAttrs().forEach(targetAttr -> {
                List<TurAemSourceAttr> sourceAttrs = new ArrayList<>();
                targetAttr.getSourceAttrs().forEach(sourceAttr ->
                        sourceAttrs.add(TurAemSourceAttr.builder()
                                .className(sourceAttr.getClassName())
                                .name(sourceAttr.getName())
                                .convertHtmlToText(false)
                                .uniqueValues(false)
                                .build()));
                targetAttrs.add(TurAemTargetAttr.builder()
                        .name(targetAttr.getName())
                        .sourceAttrs(sourceAttrs)
                        .build());
            });
            turAemModels.add(TurAemModel.builder()
                    .className(pluginModel.getClassName())
                    .type(pluginModel.getType())
                    .targetAttrs(targetAttrs)
                    .build());
        });
        List<TurSNAttributeSpec> targetAttrDefinitions = new ArrayList<>();
        turAemAttributeSpecificationRepository.findByTurAemSource(turAemSource)
                .ifPresent(attributeSpecifications ->
                        attributeSpecifications.forEach(attributeSpec ->
                                targetAttrDefinitions.add(TurSNAttributeSpec.builder()
                                        .className(attributeSpec.getClassName())
                                        .name(attributeSpec.getName())
                                        .type(attributeSpec.getType())
                                        .facetName(attributeSpec.getFacetNames())
                                        .description(attributeSpec.getDescription())
                                        .mandatory(attributeSpec.isMandatory())
                                        .multiValued(attributeSpec.isMultiValued())
                                        .facet(attributeSpec.isFacet())
                                        .build()
                                )));

        TurAemContentMapping turAemContentMapping = new TurAemContentMapping();
        turAemContentMapping.setDeltaClassName(turAemSource.getDeltaClass());
        turAemContentMapping.setModels(turAemModels);
        turAemContentMapping.setTargetAttrDefinitions(targetAttrDefinitions);
        return turAemContentMapping;
    }

    private TurAemSourceContext getTurAemSourceContext(IAemConfiguration config) {
        TurAemSourceContext turAemSourceContext = TurAemSourceContext.builder()
                .id(config.getCmsGroup())
                .contentType(config.getCmsContentType())
                .defaultLocale(config.getDefaultLocale())
                .rootPath(config.getCmsRootPath())
                .url(config.getCmsHost())
                .siteName(getSiteName())
                .subType(config.getCmsSubType())
                .oncePattern(config.getOncePatternPath())
                .providerName(config.getProviderName())
                .password(config.getCmsPassword())
                .urlPrefix(config.getCDAURLPrefix())
                .username(config.getCmsUsername())
                .localePaths(config.getLocales())
                .build();
        if (log.isDebugEnabled()) {
            log.debug("TurAemSourceContext: {}", turAemSourceContext.toString());
        }
        return turAemSourceContext;
    }

    private void getNodesFromJson(TurAemSourceContext turAemSourceContext, TurConnectorSession turConnectorSession,
                                  TurAemSource turAemSource) {
        if (TurAemCommonsUtils.usingContentTypeParameter(turAemSourceContext)) {
            byContentTypeList(turAemSourceContext, turConnectorSession, turAemSource);
        }
    }

    public void indexContentId(TurConnectorSession turConnectorSession, TurAemSource turAemSource, String guid) {
        config = new AemPluginHandlerConfiguration(turAemSource);
        turAemContentDefinitionProcess = new TurAemContentDefinitionProcess(getTurAemContentMapping(turAemSource));
        TurAemSourceContext turAemSourceContext = getTurAemSourceContext(config);
        getSiteName(turAemSourceContext);
        TurAemCommonsUtils.getInfinityJson(guid, turAemSourceContext, false)
                .ifPresent(infinityJson -> {
                    turAemSourceContext.setContentType(infinityJson.getString(JCR_PRIMARY_TYPE));
                    getNodeFromJson(guid, infinityJson, turAemSourceContext, turConnectorSession, turAemSource);
                });
    }

    private void getSiteName(TurAemSourceContext turAemSourceContext) {
        TurAemCommonsUtils.getInfinityJson(turAemSourceContext.getRootPath(), turAemSourceContext, false)
                .flatMap(infinityJson -> TurAemCommonsUtils
                        .getSiteName(turAemSourceContext, infinityJson)).ifPresent(s -> siteName = s);
    }

    private void byContentTypeList(TurAemSourceContext turAemSourceContext,
                                   TurConnectorSession turConnectorSession,
                                   TurAemSource turAemSource) {
        turAemContentDefinitionProcess.findByNameFromModelWithDefinition(turAemSourceContext.getContentType())
                .ifPresentOrElse(turAemModel ->
                                byContentType(turAemSourceContext, turConnectorSession, turAemSource),
                        () -> log.info("{} type is not configured in CTD Mapping file.",
                                turAemSourceContext.getContentType()));
    }

    private void byContentType(TurAemSourceContext turAemSourceContext,
                               TurConnectorSession turConnectorSession,
                               TurAemSource turAemSource) {
        TurAemCommonsUtils.getInfinityJson(turAemSourceContext.getRootPath(), turAemSourceContext, false)
                .ifPresent(infinityJson -> {
                    TurAemCommonsUtils.getSiteName(turAemSourceContext, infinityJson).ifPresent(s -> siteName = s);
                    getNodeFromJson(turAemSourceContext.getRootPath(), infinityJson, turAemSourceContext,
                            turConnectorSession, turAemSource);
                });
    }

    private void getNodeFromJson(String nodePath, JSONObject jsonObject,
                                 TurAemSourceContext turAemSourceContext,
                                 TurConnectorSession turConnectorSession,
                                 TurAemSource turAemSource) {
        TurAemObject aemObject = new TurAemObject(nodePath, jsonObject);
        Optional.of(aemObject).ifPresentOrElse(o -> {
            if (TurAemCommonsUtils.isTypeEqualContentType(jsonObject, turAemSourceContext)) {
                turAemContentDefinitionProcess.findByNameFromModelWithDefinition(turAemSourceContext.getContentType())
                        .ifPresent(model ->
                                prepareIndexObject(model, aemObject,
                                        turAemContentDefinitionProcess.getTargetAttrDefinitions(),
                                        turAemSourceContext, turConnectorSession, turAemSource));
            }
        }, () -> log.info("AEM object ({}) is null deltaId = {}",
                turAemSourceContext.getId(), deltaId));
        getChildrenFromJson(nodePath, jsonObject, turAemSourceContext, turConnectorSession, turAemSource);
    }

    private void getChildrenFromJson(String nodePath, JSONObject jsonObject,
                                     TurAemSourceContext turAemSourceContext,
                                     TurConnectorSession turConnectorSession,
                                     TurAemSource turAemSource) {
        jsonObject.toMap().forEach((key, value) -> {
            if (!key.startsWith(JCR) && !key.startsWith(REP) && !key.startsWith(CQ)
                    && (turAemSourceContext.getSubType() != null &&
                    turAemSourceContext.getSubType().equals(STATIC_FILE_SUB_TYPE)
                    || TurAemCommonsUtils.checkIfFileHasNotImageExtension(key))) {
                String nodePathChild = "%s/%s".formatted(nodePath, key);
                if (!isOnce(turAemSourceContext) || !TurAemCommonsUtils.isOnceConfig(nodePathChild, config)) {
                    TurAemCommonsUtils.getInfinityJson(nodePathChild, turAemSourceContext, false)
                            .ifPresent(infinityJson ->
                                    getNodeFromJson(nodePathChild, infinityJson, turAemSourceContext,
                                            turConnectorSession, turAemSource));
                }
            }
        });
    }

    private boolean isOnce(TurAemSourceContext turAemSourceContext) {
        return turAemSystemRepository.findByConfig(TurAemCommonsUtils.configOnce(turAemSourceContext))
                .map(TurAemPluginSystem::isBooleanValue)
                .orElse(false);
    }

    private void prepareIndexObject(TurAemModel turAemModel, TurAemObject aemObject,
                                    List<TurSNAttributeSpec> targetAttrDefinitions,
                                    TurAemSourceContext turAemSourceContext,
                                    TurConnectorSession turConnectorSession,
                                    TurAemSource turAemSource) {
        String type = Objects.requireNonNull(turAemSourceContext.getContentType());
        if (!isPage(type) &&
                !isContentFragment(turAemModel, type, aemObject) &&
                !isStaticFile(turAemModel, type)) {
            return;
        }
        if (isContentFragment(turAemModel, type, aemObject)) {
            aemObject.setDataPath(DATA_MASTER);
        } else if (isStaticFile(turAemModel, type)) {
            aemObject.setDataPath(METADATA);
        }
        indexObjectByPublication(aemObject, turAemModel, targetAttrDefinitions, turAemSourceContext,
                turConnectorSession, turAemSource);
    }

    private static boolean isPage(String type) {
        return type.equals(CQ_PAGE);
    }

    private static boolean isStaticFile(TurAemModel turAemModel, String type) {
        return isAsset(turAemModel, type) &&
                turAemModel.getSubType().equals(STATIC_FILE);
    }

    private static boolean isContentFragment(TurAemModel turAemModel, String type, TurAemObject aemObject) {
        return isAsset(turAemModel, type) &&
                turAemModel.getSubType().equals(CONTENT_FRAGMENT) &&
                aemObject.isContentFragment();
    }

    private static boolean isAsset(TurAemModel turAemModel, String type) {
        return type.equals(DAM_ASSET) &&
                StringUtils.isNotEmpty(turAemModel.getSubType());
    }

    private void indexObjectByPublication(@NotNull TurAemObject aemObject, TurAemModel turAemModel,
                                          List<TurSNAttributeSpec> turSNAttributeSpecList,
                                          TurAemSourceContext turAemSourceContext,
                                          TurConnectorSession turConnectorSession,
                                          TurAemSource turAemSource) {
        if (isAuthor(turAemSource)) {
            turAemSourceContext.setEnvironment(TurAemEnv.AUTHOR);
            turConnectorSession.setSites(Collections.singletonList(turAemSource.getAuthorSNSite()));
            sendToTuringToBeIndexed(aemObject, turAemModel, turSNAttributeSpecList,
                    TurAemCommonsUtils.getLocaleFromAemObject(turAemSourceContext, aemObject),
                    turAemSourceContext, turConnectorSession);
        }
        if (isPublish(turAemSource)) {
            if (aemObject.isDelivered()) {
                turAemSourceContext.setEnvironment(TurAemEnv.PUBLISHING);
                turConnectorSession.setSites(Collections.singletonList(turAemSource.getPublishSNSite()));
                sendToTuringToBeIndexed(aemObject, turAemModel, turSNAttributeSpecList,
                        TurAemCommonsUtils.getLocaleFromAemObject(turAemSourceContext, aemObject),
                        turAemSourceContext, turConnectorSession);
            } else {
                log.info("Ignoring: This {} object ({}) is not publishing. deltaId = {}",
                        aemObject.getPath(), turAemSourceContext.getId(), deltaId);
            }
        }
    }

    private static boolean isPublish(TurAemSource turAemSource) {
        return turAemSource.isPublish() &&
                StringUtils.isNotEmpty(turAemSource.getPublishSNSite());
    }

    private static boolean isAuthor(TurAemSource turAemSource) {
        return turAemSource.isAuthor() &&
                StringUtils.isNotEmpty(turAemSource.getAuthorSNSite());
    }

    private void sendToTuringToBeIndexed(TurAemObject aemObject, TurAemModel turAemModel,
                                         List<TurSNAttributeSpec> turSNAttributeSpecList, Locale locale,
                                         TurAemSourceContext turAemSourceContext,
                                         TurConnectorSession turConnectorSession) {
        TurAemAttrProcess turAEMAttrProcess = new TurAemAttrProcess();
        TurAemTargetAttrValueMap turAemTargetAttrValueMap = turAEMAttrProcess
                .prepareAttributeDefs(aemObject, turAemContentDefinitionProcess, turSNAttributeSpecList,
                        turAemSourceContext);
        turAemTargetAttrValueMap.merge(TurAemCommonsUtils.runCustomClassFromContentType(turAemModel,
                aemObject, turAemSourceContext));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SITE, siteName);
        turAemTargetAttrValueMap.entrySet().stream()
                .filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
                .forEach(entry -> {
                    String attributeName = entry.getKey();
                    entry.getValue().forEach(attributeValue -> {
                        if (StringUtils.isNotBlank(attributeValue)) {
                            if (attributes.containsKey(attributeName)) {
                                TurAemCommonsUtils.addItemInExistingAttribute(attributeValue, attributes, attributeName);
                            } else {
                                TurAemCommonsUtils.addFirstItemToAttribute(attributeName, attributeValue, attributes);
                            }
                        }
                    });
                });
        TurSNJobItem jobItem = new TurSNJobItem(TurSNJobAction.CREATE,
                turConnectorSession.getSites().stream().toList(),
                locale,
                attributes,
                TurAemCommonsUtils.castSpecToJobSpec(
                        TurAemCommonsUtils.getDefinitionFromModel(turSNAttributeSpecList, attributes)));
        jobItem.setChecksum(String.valueOf(TurAemCommonsUtils
                .getDeltaDate(aemObject, turAemSourceContext, turAemContentDefinitionProcess).getTime()));
        jobItem.setEnvironment(turAemSourceContext.getEnvironment().toString());
        turConnectorContext.addJobItem(jobItem, turConnectorSession);
    }
}
