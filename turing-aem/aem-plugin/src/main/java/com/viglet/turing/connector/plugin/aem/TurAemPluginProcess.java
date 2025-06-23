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
import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.client.sn.TurSNConstants;
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
import com.viglet.turing.connector.commons.plugin.dto.TurConnectorIndexingDTO;
import com.viglet.turing.connector.plugin.aem.api.TurAemPathList;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.*;

/**
 * @author Alexandre Oliveira
 * @since 2025.2
 */
@Slf4j
@Getter
@Component
public class TurAemPluginProcess {
    private final Set<String> visitedLinks = new HashSet<>();
    private final Queue<String> remainingLinks = new LinkedList<>();
    private final TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository;
    private final TurAemPluginSystemRepository turAemSystemRepository;
    private final TurAemConfigVarRepository turAemConfigVarRepository;
    private final TurAemSourceLocalePathRepository turAemSourceLocalePathRepository;
    private final TurAemPluginModelRepository turAemPluginModelRepository;
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemTargetAttributeRepository turAemTargetAttributeRepository;
    private final TurConnectorContext turConnectorContext;
    private final String turingUrl;
    private final String turingApiKey;
    private final List<String> runningSources = new ArrayList<>();

    @Inject
    public TurAemPluginProcess(TurAemPluginSystemRepository turAemPluginSystemRepository,
                               TurAemConfigVarRepository turAemConfigVarRepository,
                               TurAemSourceLocalePathRepository turAemSourceLocalePathRepository,
                               TurAemPluginModelRepository turAemPluginModelRepository,
                               TurAemSourceRepository turAemSourceRepository,
                               TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository,
                               TurAemTargetAttributeRepository turAemTargetAttributeRepository,
                               TurConnectorContext turConnectorContext,
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

    @Async
    public void indexAllAsync(TurAemSource turAemSource) {
        indexAll(turAemSource);
    }

    @Async
    public void sentToIndexStandaloneAsync(String name, TurAemPathList turAemPathList) {
        if (turAemPathList == null || turAemPathList.getPaths() == null || turAemPathList.getPaths().isEmpty()) {
            log.error("Payload is empty");
            return;
        }
        log.debug("Receiving Async payload to {} source with paths {}", name, turAemPathList);
        turAemSourceRepository.findByName(name).ifPresentOrElse(turAemSource -> {
                    TurConnectorSession turConnectorSession = TurAemPluginProcess.getTurConnectorSession(turAemSource);
                    turAemPathList.getPaths().forEach(path ->
                            indexContentId(turConnectorSession, turAemSource, path, true));
                    finished(turConnectorSession, true);
                },
                () -> log.error("{} Source not found", name));
    }


    public void indexAll(TurAemSource turAemSource) {
        if (runningSources.contains(turAemSource.getName())) {
            log.warn("Skipping. There are already source process running. {}", turAemSource.getName());
            return;
        }
        runningSources.add(turAemSource.getName());
        TurConnectorSession turConnectorSession = getTurConnectorSession(turAemSource);
        try {
            this.getNodesFromJson(getTurAemSourceContext(new AemPluginHandlerConfiguration(turAemSource)),
                    turConnectorSession, turAemSource,
                    new TurAemContentDefinitionProcess(getTurAemContentMapping(turAemSource)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        finished(turConnectorSession, false);
    }

    public static @NotNull TurConnectorSession getTurConnectorSession(TurAemSource turAemSource) {
        // sites parameter is null, because in next step need check if author our publishing.
        return new TurConnectorSession(turAemSource.getName(), null, AEM, turAemSource.getDefaultLocale());
    }

    public void finished(TurConnectorSession turConnectorSession, boolean standalone) {
        if (!standalone) runningSources.remove(turConnectorSession.getSource());
        turConnectorContext.finishIndexing(turConnectorSession, standalone);
    }

    private @NotNull TurAemContentMapping getTurAemContentMapping(TurAemSource turAemSource) {
        return TurAemContentMapping.builder()
                .deltaClassName(turAemSource.getDeltaClass())
                .models(getTurAemModels(turAemSource))
                .targetAttrDefinitions(getTurSNAttributeSpecs(turAemSource))
                .build();
    }

    private @NotNull List<TurAemModel> getTurAemModels(TurAemSource turAemSource) {
        return turAemPluginModelRepository.findByTurAemSource(turAemSource)
                .stream()
                .map(pluginModel ->
                        TurAemModel.builder()
                                .className(pluginModel.getClassName())
                                .type(pluginModel.getType())
                                .targetAttrs(getTurAemTargetAttrs(pluginModel))
                                .build()
                ).toList();
    }

    private static @NotNull List<TurAemTargetAttr> getTurAemTargetAttrs(TurAemPluginModel pluginModel) {
        return pluginModel.getTargetAttrs()
                .stream()
                .map(targetAttr ->
                        TurAemTargetAttr.builder()
                                .name(targetAttr.getName())
                                .sourceAttrs(getTurAemSourceAttrs(targetAttr))
                                .build()
                ).collect(Collectors.toList());
    }

    private static @NotNull List<TurAemSourceAttr> getTurAemSourceAttrs(TurAemTargetAttribute targetAttr) {
        return targetAttr.getSourceAttrs()
                .stream()
                .map(sourceAttr ->
                        TurAemSourceAttr.builder()
                                .className(sourceAttr.getClassName())
                                .name(sourceAttr.getName())
                                .convertHtmlToText(false)
                                .uniqueValues(false)
                                .build()
                ).toList();
    }

    private @NotNull List<TurSNAttributeSpec> getTurSNAttributeSpecs(TurAemSource turAemSource) {
        List<TurSNAttributeSpec> targetAttrDefinitions = new ArrayList<>();
        turAemAttributeSpecificationRepository.findByTurAemSource(turAemSource)
                .ifPresent(attributeSpecifications ->
                        attributeSpecifications
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
                                        .build()
                                )));
        return targetAttrDefinitions;
    }

    private TurAemSourceContext getTurAemSourceContext(IAemConfiguration config) {
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

    private void getNodesFromJson(TurAemSourceContext turAemSourceContext,
                                  TurConnectorSession turConnectorSession,
                                  TurAemSource turAemSource,
                                  TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        if (!TurAemCommonsUtils.usingContentTypeParameter(turAemSourceContext)) return;
        byContentTypeList(turAemSourceContext, turConnectorSession, turAemSource,
                turAemContentDefinitionProcess);
    }

    public void indexContentId(TurConnectorSession session, TurAemSource turAemSource, String contentId,
                               boolean standalone) {
        TurAemSourceContext turAemSourceContext = getTurAemSourceContext(new AemPluginHandlerConfiguration(turAemSource));
        TurAemCommonsUtils.getInfinityJson(contentId, turAemSourceContext, false)
                .ifPresentOrElse(infinityJson -> {
                    turAemSourceContext.setContentType(infinityJson.getString(JCR_PRIMARY_TYPE));
                    getNodeFromJson(contentId, infinityJson, turAemSourceContext, session, turAemSource,
                            new TurAemContentDefinitionProcess(getTurAemContentMapping(turAemSource)), standalone);
                }, () -> sendToTuringToBeDeIndexed(session, contentId, standalone));
    }

    private void sendToTuringToBeDeIndexed(TurConnectorSession session, String contentId, boolean standalone) {
        turConnectorContext.getIndexingItem(contentId, session.getSource())
                .forEach(indexing ->
                        turConnectorContext.addJobItem(deIndexJob(session, indexing), session, standalone));
    }

    private TurSNJobItem deIndexJob(TurConnectorSession session,
                                    TurConnectorIndexingDTO turConnectorIndexingDTO) {
        return new TurSNJobItem(
                TurSNJobAction.DELETE,
                turConnectorIndexingDTO.getSites(),
                turConnectorIndexingDTO.getLocale(),
                Map.of(
                        TurSNConstants.ID_ATTR, turConnectorIndexingDTO.getObjectId(),
                        TurSNConstants.SOURCE_APPS_ATTR, session.getProviderName()
                )
        );
    }

    private void byContentTypeList(TurAemSourceContext turAemSourceContext,
                                   TurConnectorSession turConnectorSession,
                                   TurAemSource turAemSource,
                                   TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        turAemContentDefinitionProcess.findByNameFromModelWithDefinition(turAemSourceContext.getContentType())
                .ifPresentOrElse(turAemModel ->
                                byContentType(turAemSourceContext, turConnectorSession, turAemSource,
                                        turAemContentDefinitionProcess),
                        () -> log.debug("{} type is not configured in CTD Mapping file.",
                                turAemSourceContext.getContentType()));
    }

    private void byContentType(TurAemSourceContext turAemSourceContext,
                               TurConnectorSession turConnectorSession,
                               TurAemSource turAemSource,
                               TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        TurAemCommonsUtils.getInfinityJson(turAemSourceContext.getRootPath(), turAemSourceContext, false)
                .ifPresent(infinityJson ->
                        getNodeFromJson(turAemSourceContext.getRootPath(), infinityJson, turAemSourceContext,
                                turConnectorSession, turAemSource, turAemContentDefinitionProcess, false));
    }

    private void getNodeFromJson(String nodePath, JSONObject jsonObject,
                                 TurAemSourceContext turAemSourceContext,
                                 TurConnectorSession session,
                                 TurAemSource turAemSource,
                                 TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                 boolean standalone) {
        if (TurAemCommonsUtils.isTypeEqualContentType(jsonObject, turAemSourceContext)) {
            turAemContentDefinitionProcess.findByNameFromModelWithDefinition(turAemSourceContext.getContentType())
                    .ifPresent(model ->
                            prepareIndexObject(model, new TurAemObject(nodePath, jsonObject),
                                    turAemContentDefinitionProcess.getTargetAttrDefinitions(),
                                    turAemSourceContext, session, turAemSource,
                                    turAemContentDefinitionProcess, standalone));
        }
        getChildrenFromJson(nodePath, jsonObject, turAemSourceContext, session, turAemSource,
                turAemContentDefinitionProcess, standalone);
    }

    private void getChildrenFromJson(String nodePath, JSONObject jsonObject,
                                     TurAemSourceContext turAemSourceContext,
                                     TurConnectorSession turConnectorSession,
                                     TurAemSource turAemSource,
                                     TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                     boolean standalone) {
        jsonObject.toMap().forEach((nodeName, nodeValue) -> {
            if (isIndexedNode(turAemSourceContext, nodeName)) {
                String nodePathChild = "%s/%s".formatted(nodePath, nodeName);
                if (!isOnce(turAemSourceContext) || !TurAemCommonsUtils.isOnceConfig(nodePathChild,
                        new AemPluginHandlerConfiguration(turAemSource))) {
                    TurAemCommonsUtils.getInfinityJson(nodePathChild, turAemSourceContext, false)
                            .ifPresent(infinityJson ->
                                    getNodeFromJson(nodePathChild, infinityJson, turAemSourceContext,
                                            turConnectorSession, turAemSource, turAemContentDefinitionProcess,
                                            standalone));
                }
            }
        });
    }

    private static boolean isIndexedNode(TurAemSourceContext turAemSourceContext, String nodeName) {
        return !nodeName.startsWith(JCR) && !nodeName.startsWith(REP) && !nodeName.startsWith(CQ)
                && (turAemSourceContext.getSubType() != null &&
                turAemSourceContext.getSubType().equals(STATIC_FILE_SUB_TYPE)
                || TurAemCommonsUtils.checkIfFileHasNotImageExtension(nodeName));
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
                                    TurAemSource turAemSource,
                                    TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                    boolean standalone) {
        String type = Objects.requireNonNull(turAemSourceContext.getContentType());
        if (isNotValidType(turAemModel, aemObject, type)) {
            return;
        }
        if (isContentFragment(turAemModel, type, aemObject)) {
            aemObject.setDataPath(DATA_MASTER);
        } else if (isStaticFile(turAemModel, type)) {
            aemObject.setDataPath(METADATA);
        }
        indexObject(aemObject, turAemModel, targetAttrDefinitions, turAemSourceContext,
                turConnectorSession, turAemSource, turAemContentDefinitionProcess, standalone);
    }

    private static boolean isNotValidType(TurAemModel turAemModel, TurAemObject aemObject, String type) {
        return !isPage(type) &&
                !isContentFragment(turAemModel, type, aemObject) &&
                !isStaticFile(turAemModel, type);
    }

    private static boolean isPage(String type) {
        return type.equals(CQ_PAGE);
    }

    private static boolean isStaticFile(TurAemModel turAemModel, String type) {
        return isAsset(turAemModel, type) && turAemModel.getSubType().equals(STATIC_FILE);
    }

    private static boolean isContentFragment(TurAemModel turAemModel, String type, TurAemObject aemObject) {
        return isAsset(turAemModel, type) &&
                turAemModel.getSubType().equals(CONTENT_FRAGMENT) &&
                aemObject.isContentFragment();
    }

    private static boolean isAsset(TurAemModel turAemModel, String type) {
        return type.equals(DAM_ASSET) && StringUtils.isNotEmpty(turAemModel.getSubType());
    }

    private void indexObject(@NotNull TurAemObject aemObject, TurAemModel turAemModel,
                             List<TurSNAttributeSpec> turSNAttributeSpecList,
                             TurAemSourceContext turAemSourceContext,
                             TurConnectorSession turConnectorSession,
                             TurAemSource turAemSource,
                             TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                             boolean standalone) {
        if (isAuthor(turAemSource)) {
            indexByEnvironment(TurAemEnv.AUTHOR, turAemSource.getAuthorSNSite(),
                    aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                    turConnectorSession, turAemContentDefinitionProcess, standalone);
        }
        if (isPublish(turAemSource)) {
            if (aemObject.isDelivered()) {
                indexByEnvironment(TurAemEnv.PUBLISHING, turAemSource.getPublishSNSite(),
                        aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                        turConnectorSession, turAemContentDefinitionProcess, standalone);
            } else {
                log.debug("Ignoring because {} object ({}) is not publishing. transactionId = {}",
                        aemObject.getPath(), turAemSourceContext.getId(), turConnectorSession.getTransactionId());
            }
        }
    }

    private void indexByEnvironment(TurAemEnv turAemEnv, String snSite,
                                    @NotNull TurAemObject aemObject, TurAemModel turAemModel,
                                    List<TurSNAttributeSpec> turSNAttributeSpecList,
                                    TurAemSourceContext turAemSourceContext,
                                    TurConnectorSession turConnectorSession,
                                    TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                    boolean standalone) {
        turAemSourceContext.setEnvironment(turAemEnv);
        turConnectorSession.setSites(Collections.singletonList(snSite));
        sendToTuringToBeIndexed(aemObject, turAemModel, turSNAttributeSpecList,
                TurAemCommonsUtils.getLocaleFromAemObject(turAemSourceContext, aemObject),
                turAemSourceContext, turConnectorSession, turAemContentDefinitionProcess, standalone);
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
                                         TurConnectorSession turConnectorSession,
                                         TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                         boolean standalone) {
        turConnectorContext.addJobItem(
                getTurSNJobItem(
                        aemObject,
                        turSNAttributeSpecList,
                        locale,
                        turAemSourceContext,
                        turConnectorSession,
                        turAemContentDefinitionProcess,
                        getJobItemAttributes(
                                turAemSourceContext,
                                getTargetAttrValueMap(
                                        aemObject,
                                        turAemModel,
                                        turSNAttributeSpecList,
                                        turAemSourceContext,
                                        turAemContentDefinitionProcess)
                        )
                ),
                turConnectorSession,
                standalone);
    }

    private static @NotNull TurAemTargetAttrValueMap getTargetAttrValueMap(
            TurAemObject aemObject,
            TurAemModel turAemModel,
            List<TurSNAttributeSpec> turSNAttributeSpecList,
            TurAemSourceContext turAemSourceContext,
            TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        TurAemTargetAttrValueMap turAemTargetAttrValueMap = new TurAemAttrProcess()
                .prepareAttributeDefs(aemObject, turAemContentDefinitionProcess, turSNAttributeSpecList,
                        turAemSourceContext);
        turAemTargetAttrValueMap.merge(TurAemCommonsUtils.runCustomClassFromContentType(turAemModel,
                aemObject, turAemSourceContext));
        return turAemTargetAttrValueMap;
    }

    private static @NotNull TurSNJobItem getTurSNJobItem(TurAemObject aemObject,
                                                         List<TurSNAttributeSpec> turSNAttributeSpecList,
                                                         Locale locale,
                                                         TurAemSourceContext turAemSourceContext,
                                                         TurConnectorSession turConnectorSession,
                                                         TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                                         Map<String, Object> attributes) {
        TurSNJobItem jobItem = new TurSNJobItem(
                TurSNJobAction.CREATE,
                turConnectorSession.getSites().stream().toList(),
                locale,
                attributes,
                TurAemCommonsUtils.castSpecToJobSpec(
                        TurAemCommonsUtils.getDefinitionFromModel(turSNAttributeSpecList, attributes)
                )
        );
        jobItem.setChecksum(String.valueOf(TurAemCommonsUtils
                .getDeltaDate(aemObject, turAemSourceContext, turAemContentDefinitionProcess).getTime()));
        jobItem.setEnvironment(turAemSourceContext.getEnvironment().toString());
        return jobItem;
    }

    private static @NotNull Map<String, Object> getJobItemAttributes(TurAemSourceContext turAemSourceContext,
                                                                     TurAemTargetAttrValueMap targetAttrValueMap) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SITE, turAemSourceContext.getSiteName());
        targetAttrValueMap.entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .forEach(entry -> getJobItemAttribute(entry, attributes));
        return attributes;
    }

    private static void getJobItemAttribute(Map.Entry<String, TurMultiValue> entry, Map<String, Object> attributes) {
        String attributeName = entry.getKey();
        entry.getValue().stream()
                .filter(StringUtils::isNotBlank)
                .forEach(attributeValue -> {
                    if (attributes.containsKey(attributeName)) {
                        TurAemCommonsUtils.addItemInExistingAttribute(attributeValue, attributes, attributeName);
                    } else {
                        TurAemCommonsUtils.addFirstItemToAttribute(attributeName, attributeValue, attributes);
                    }
                });
    }
}