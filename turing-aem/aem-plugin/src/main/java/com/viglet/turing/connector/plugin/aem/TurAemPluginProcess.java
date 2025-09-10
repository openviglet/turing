/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.plugin.aem;

import static com.viglet.turing.client.sn.TurSNConstants.ID_ATTR;
import static com.viglet.turing.client.sn.TurSNConstants.SOURCE_APPS_ATTR;
import static com.viglet.turing.client.sn.job.TurSNJobAction.CREATE;
import static com.viglet.turing.client.sn.job.TurSNJobAction.DELETE;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.DEINDEXED;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.AEM;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CONTENT_FRAGMENT;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_PAGE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DAM_ASSET;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DATA_MASTER;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR_PRIMARY_TYPE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.METADATA;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.REP;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.SITE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.STATIC_FILE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.STATIC_FILE_SUB_TYPE;
import static com.viglet.turing.connector.aem.commons.bean.TurAemEnv.AUTHOR;
import static com.viglet.turing.connector.aem.commons.bean.TurAemEnv.PUBLISHING;
import static com.viglet.turing.connector.commons.logging.TurConnectorLoggingUtils.setSuccessStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.config.IAemConfiguration;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentDefinitionProcess;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentMapping;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.plugin.aem.api.TurAemPathList;
import com.viglet.turing.connector.plugin.aem.conf.AemPluginHandlerConfiguration;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemPluginModel;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemPluginSystem;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemTargetAttribute;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemAttributeSpecificationRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemConfigVarRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginModelRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginSystemRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceLocalePathRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemTargetAttributeRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
                this.turAemAttributeSpecificationRepository =
                                turAemAttributeSpecificationRepository;
                this.turAemTargetAttributeRepository = turAemTargetAttributeRepository;
                this.turConnectorContext = turConnectorContext;
                this.turingUrl = turingUrl;
                this.turingApiKey = turingApiKey;
        }

        public static String getProviderName() {
                return AEM;
        }

        @Async
        public void indexAllByNameAsync(String sourceName) {
                turAemSourceRepository.findByName(sourceName).ifPresent(this::indexAll);
        }

        @Async
        public void indexAllByIdAsync(String id) {
                turAemSourceRepository.findById(id).ifPresent(this::indexAll);
        }

        @Async
        public void sentToIndexStandaloneAsync(@NotNull String source,
                        @NotNull TurAemPathList turAemPathList) {
                sentToIndexStandalone(source, turAemPathList.getPaths());
        }

        public void sentToIndexStandalone(@NotNull String source, @NotNull List<String> idList) {
                if (CollectionUtils.isEmpty(idList)) {
                        log.warn("Received empty payload for source: {}", source);
                        return;
                }
                log.info("Processing payload for source '{}' with paths: {}", source, idList);
                turAemSourceRepository.findByName(source).ifPresentOrElse(turAemSource -> {
                        TurConnectorSession session = getTurConnectorSession(turAemSource);
                        // Index each provided path
                        idList.stream().filter(StringUtils::isNotBlank)
                                        .forEach(path -> indexContentId(session, turAemSource, path,
                                                        true, true));
                        // Index dependencies if any
                        turConnectorContext
                                        .getObjectIdByDependency(source, getProviderName(), idList)
                                        .stream().filter(StringUtils::isNotBlank)
                                        .forEach(objectId -> indexContentId(session, turAemSource,
                                                        objectId, true, false));
                        finished(session, true);
                }, () -> log.error("Source '{}' not found", source));
        }

        public void indexAll(TurAemSource turAemSource) {
                if (runningSources.contains(turAemSource.getName())) {
                        log.warn("Skipping. There are already source process running. {}",
                                        turAemSource.getName());
                        return;
                }
                runningSources.add(turAemSource.getName());
                TurConnectorSession turConnectorSession = getTurConnectorSession(turAemSource);
                try {
                        this.getNodesFromJson(
                                        getTurAemSourceContext(new AemPluginHandlerConfiguration(
                                                        turAemSource)),
                                        turConnectorSession, turAemSource,
                                        new TurAemContentDefinitionProcess(
                                                        getTurAemContentMapping(turAemSource)));
                } catch (Exception e) {
                        log.error(e.getMessage(), e);
                }
                finished(turConnectorSession, false);
        }

        public static @NotNull TurConnectorSession getTurConnectorSession(
                        TurAemSource turAemSource) {
                // sites parameter is null, because in next step need check if author our
                // publishing.
                return new TurConnectorSession(turAemSource.getName(), null, getProviderName(),
                                turAemSource.getDefaultLocale());
        }

        public void finished(TurConnectorSession turConnectorSession, boolean standalone) {
                if (!standalone)
                        runningSources.remove(turConnectorSession.getSource());
                turConnectorContext.finishIndexing(turConnectorSession, standalone);
        }

        private @NotNull TurAemContentMapping getTurAemContentMapping(TurAemSource turAemSource) {
                return TurAemContentMapping.builder().deltaClassName(turAemSource.getDeltaClass())
                                .models(getTurAemModels(turAemSource))
                                .targetAttrDefinitions(getTurSNAttributeSpecs(turAemSource))
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

        private @NotNull List<TurSNAttributeSpec> getTurSNAttributeSpecs(
                        TurAemSource turAemSource) {
                List<TurSNAttributeSpec> targetAttrDefinitions = new ArrayList<>();
                turAemAttributeSpecificationRepository.findByTurAemSource(turAemSource)
                                .ifPresent(attributeSpecifications -> attributeSpecifications
                                                .forEach(attributeSpec -> targetAttrDefinitions
                                                                .add(TurSNAttributeSpec.builder()
                                                                                .className(attributeSpec
                                                                                                .getClassName())
                                                                                .name(attributeSpec
                                                                                                .getName())
                                                                                .type(attributeSpec
                                                                                                .getType())
                                                                                .facetName(attributeSpec
                                                                                                .getFacetNames())
                                                                                .description(attributeSpec
                                                                                                .getDescription())
                                                                                .mandatory(attributeSpec
                                                                                                .isMandatory())
                                                                                .multiValued(attributeSpec
                                                                                                .isMultiValued())
                                                                                .facet(attributeSpec
                                                                                                .isFacet())
                                                                                .build())));
                return targetAttrDefinitions;
        }

        private TurAemSourceContext getTurAemSourceContext(IAemConfiguration config) {
                TurAemSourceContext turAemSourceContext = TurAemSourceContext.builder()
                                .id(config.getCmsGroup()).contentType(config.getCmsContentType())
                                .defaultLocale(config.getDefaultLocale())
                                .rootPath(config.getCmsRootPath()).url(config.getCmsHost())
                                .subType(config.getCmsSubType())
                                .oncePattern(config.getOncePatternPath())
                                .providerName(config.getProviderName())
                                .password(config.getCmsPassword())
                                .authorURLPrefix(config.getAuthorURLPrefix())
                                .publishURLPrefix(config.getPublishURLPrefix())
                                .username(config.getCmsUsername()).localePaths(config.getLocales())
                                .build();
                TurAemCommonsUtils.getInfinityJson(config.getCmsRootPath(), turAemSourceContext)
                                .flatMap(infinityJson -> TurAemCommonsUtils
                                                .getSiteName(turAemSourceContext, infinityJson))
                                .ifPresent(turAemSourceContext::setSiteName);
                if (log.isDebugEnabled()) {
                        log.debug("TurAemSourceContext: {}", turAemSourceContext);
                }
                return turAemSourceContext;
        }

        private void getNodesFromJson(TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
                if (!TurAemCommonsUtils.usingContentTypeParameter(turAemSourceContext))
                        return;
                byContentTypeList(turAemSourceContext, turConnectorSession, turAemSource,
                                turAemContentDefinitionProcess);
        }

        public void indexContentId(TurConnectorSession session, TurAemSource turAemSource,
                        String contentId, boolean standalone, boolean indexChildren) {
                TurAemSourceContext turAemSourceContext = getTurAemSourceContext(
                                new AemPluginHandlerConfiguration(turAemSource));
                TurAemCommonsUtils.getInfinityJson(contentId, turAemSourceContext)
                                .ifPresentOrElse(infinityJson -> {
                                        turAemSourceContext.setContentType(
                                                        infinityJson.getString(JCR_PRIMARY_TYPE));
                                        getNodeFromJson(contentId, infinityJson,
                                                        turAemSourceContext, session, turAemSource,
                                                        new TurAemContentDefinitionProcess(
                                                                        getTurAemContentMapping(
                                                                                        turAemSource)),
                                                        standalone, indexChildren);
                                }, () -> createDeIndexJobAndSendToConnectorQueue(session, contentId,
                                                standalone));
        }

        private void createDeIndexJobAndSendToConnectorQueue(TurConnectorSession session,
                        String contentId, boolean standalone) {
                turConnectorContext.getIndexingItem(contentId, session.getSource(),
                                session.getProviderName()).forEach(indexing -> {
                                        log.info("DeIndex because {} infinity Json file not found.",
                                                        TurAemPluginUtils.getObjectDetailForLogs(
                                                                        contentId, indexing,
                                                                        session));
                                        TurJobItemWithSession turJobItemWithSession =
                                                        new TurJobItemWithSession(
                                                                        deIndexJob(session,
                                                                                        indexing),
                                                                        session,
                                                                        Collections.emptySet(),
                                                                        standalone);
                                        turConnectorContext.addJobItem(turJobItemWithSession);
                                });
        }

        private TurSNJobItem deIndexJob(TurConnectorSession session,
                        TurConnectorIndexing turConnectorIndexingDTO) {
                return deIndexJob(session, turConnectorIndexingDTO.getSites(),
                                turConnectorIndexingDTO.getLocale(),
                                turConnectorIndexingDTO.getObjectId(),
                                turConnectorIndexingDTO.getEnvironment());
        }

        private TurSNJobItem deIndexJob(TurConnectorSession session, List<String> sites,
                        Locale locale, String objectId, String environment) {
                TurSNJobItem turSNJobItem = new TurSNJobItem(DELETE, sites, locale, Map.of(ID_ATTR,
                                objectId, SOURCE_APPS_ATTR, session.getProviderName()));
                turSNJobItem.setEnvironment(environment);
                setSuccessStatus(turSNJobItem, session, DEINDEXED);
                return turSNJobItem;
        }

        private void byContentTypeList(TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
                turAemContentDefinitionProcess
                                .findByNameFromModelWithDefinition(
                                                turAemSourceContext.getContentType())
                                .ifPresentOrElse(
                                                turAemModel -> byContentType(turAemSourceContext,
                                                                turConnectorSession, turAemSource,
                                                                turAemContentDefinitionProcess),
                                                () -> log.debug("{} type is not configured in CTD Mapping file.",
                                                                turAemSourceContext
                                                                                .getContentType()));
        }

        private void byContentType(TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
                TurAemCommonsUtils
                                .getInfinityJson(turAemSourceContext.getRootPath(),
                                                turAemSourceContext)
                                .ifPresent(infinityJson -> getNodeFromJson(
                                                turAemSourceContext.getRootPath(), infinityJson,
                                                turAemSourceContext, turConnectorSession,
                                                turAemSource, turAemContentDefinitionProcess, false,
                                                true));
        }

        private void getNodeFromJson(String nodePath, JSONObject jsonObject,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone, boolean indexChildren) {
                if (TurAemCommonsUtils.isTypeEqualContentType(jsonObject, turAemSourceContext)) {
                        turAemContentDefinitionProcess
                                        .findByNameFromModelWithDefinition(
                                                        turAemSourceContext.getContentType())
                                        .ifPresent(model -> prepareIndexObject(model,
                                                        new TurAemObject(nodePath, jsonObject),
                                                        turAemContentDefinitionProcess
                                                                        .getTargetAttrDefinitions(),
                                                        turAemSourceContext, session, turAemSource,
                                                        turAemContentDefinitionProcess,
                                                        standalone));
                }
                if (indexChildren) {
                        getChildrenFromJson(nodePath, jsonObject, turAemSourceContext, session,
                                        turAemSource, turAemContentDefinitionProcess, standalone);
                }
        }

        private void getChildrenFromJson(String nodePath, JSONObject jsonObject,
                        TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone) {
                jsonObject.toMap().forEach((nodeName, nodeValue) -> {
                        if (isIndexedNode(turAemSourceContext, nodeName)) {
                                String nodePathChild = "%s/%s".formatted(nodePath, nodeName);
                                if (!isOnce(turAemSourceContext)
                                                || TurAemCommonsUtils.isNotOnceConfig(nodePathChild,
                                                                new AemPluginHandlerConfiguration(
                                                                                turAemSource))) {
                                        TurAemCommonsUtils
                                                        .getInfinityJson(nodePathChild,
                                                                        turAemSourceContext)
                                                        .ifPresent(infinityJson -> getNodeFromJson(
                                                                        nodePathChild, infinityJson,
                                                                        turAemSourceContext,
                                                                        turConnectorSession,
                                                                        turAemSource,
                                                                        turAemContentDefinitionProcess,
                                                                        standalone, true));
                                }
                        }
                });
        }

        private static boolean isIndexedNode(TurAemSourceContext turAemSourceContext,
                        String nodeName) {
                return !nodeName.startsWith(JCR) && !nodeName.startsWith(REP)
                                && !nodeName.startsWith(CQ)
                                && (turAemSourceContext.getSubType() != null
                                                && turAemSourceContext.getSubType()
                                                                .equals(STATIC_FILE_SUB_TYPE)
                                                || TurAemCommonsUtils
                                                                .checkIfFileHasNotImageExtension(
                                                                                nodeName));
        }

        private boolean isOnce(TurAemSourceContext turAemSourceContext) {
                return turAemSystemRepository
                                .findByConfig(TurAemCommonsUtils.configOnce(turAemSourceContext))
                                .map(TurAemPluginSystem::isBooleanValue).orElse(false);
        }

        private void prepareIndexObject(TurAemModel turAemModel, TurAemObject aemObject,
                        List<TurSNAttributeSpec> targetAttrDefinitions,
                        TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
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
                                turConnectorSession, turAemSource, turAemContentDefinitionProcess,
                                standalone);
        }

        private static boolean isNotValidType(TurAemModel turAemModel, TurAemObject aemObject,
                        String type) {
                return !isPage(type) && !isContentFragment(turAemModel, type, aemObject)
                                && !isStaticFile(turAemModel, type);
        }

        private static boolean isPage(String type) {
                return type.equals(CQ_PAGE);
        }

        private static boolean isStaticFile(TurAemModel turAemModel, String type) {
                return isAsset(turAemModel, type) && turAemModel.getSubType().equals(STATIC_FILE);
        }

        private static boolean isContentFragment(TurAemModel turAemModel, String type,
                        TurAemObject aemObject) {
                return isAsset(turAemModel, type)
                                && turAemModel.getSubType().equals(CONTENT_FRAGMENT)
                                && aemObject.isContentFragment();
        }

        private static boolean isAsset(TurAemModel turAemModel, String type) {
                return type.equals(DAM_ASSET) && StringUtils.isNotEmpty(turAemModel.getSubType());
        }

        private void indexObject(@NotNull TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone) {
                indexingAuthor(aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                                session, turAemSource, turAemContentDefinitionProcess, standalone);
                indexingPublish(aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                                session, turAemSource, turAemContentDefinitionProcess, standalone);
        }

        private void indexingAuthor(TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone) {
                if (isAuthor(turAemSource)) {
                        indexByEnvironment(AUTHOR, turAemSource.getAuthorSNSite(), aemObject,
                                        turAemModel, turSNAttributeSpecList, turAemSourceContext,
                                        session, turAemContentDefinitionProcess, standalone);
                } ;
        }

        private void indexingPublish(TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone) {
                if (isPublish(turAemSource)) {
                        if (aemObject.isDelivered()) {
                                indexByEnvironment(PUBLISHING, turAemSource.getPublishSNSite(),
                                                aemObject, turAemModel, turSNAttributeSpecList,
                                                turAemSourceContext, session,
                                                turAemContentDefinitionProcess, standalone);
                        } else if (standalone) {
                                forcingDeIndex(aemObject, turAemSourceContext, session,
                                                turAemSource);
                        } else {
                                ignoringDeIndex(aemObject, turAemSourceContext, session);
                        }
                }
        }

        private void ignoringDeIndex(TurAemObject aemObject,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session) {
                log.info("Ignoring deIndex because {} is not publishing.", TurAemPluginUtils
                                .getObjectDetailForLogs(aemObject, turAemSourceContext, session));
        }

        private void forcingDeIndex(TurAemObject aemObject, TurAemSourceContext turAemSourceContext,
                        TurConnectorSession session, TurAemSource turAemSource) {
                TurSNJobItem deIndexJobItem =
                                deIndexJob(session, List.of(turAemSource.getPublishSNSite()),
                                                TurAemCommonsUtils.getLocaleFromAemObject(
                                                                turAemSourceContext, aemObject),
                                                aemObject.getPath(), PUBLISHING.toString());
                TurJobItemWithSession turJobItemWithSession = new TurJobItemWithSession(
                                deIndexJobItem, session, aemObject.getDependencies(), true);
                turConnectorContext.addJobItem(turJobItemWithSession);
                log.info("Forcing deIndex because {} is not publishing.", TurAemPluginUtils
                                .getObjectDetailForLogs(aemObject, turAemSourceContext, session));
        }

        private void indexByEnvironment(TurAemEnv turAemEnv, String snSite,
                        @NotNull TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone) {
                turAemSourceContext.setEnvironment(turAemEnv);
                turAemSourceContext.setTurSNSite(snSite);
                session.setSites(Collections.singletonList(snSite));
                createIndexJobAndSendToConnectorQueue(aemObject, turAemModel,
                                turSNAttributeSpecList,
                                TurAemCommonsUtils.getLocaleFromAemObject(turAemSourceContext,
                                                aemObject),
                                turAemSourceContext, session, turAemContentDefinitionProcess,
                                standalone);
        }

        private static boolean isPublish(TurAemSource turAemSource) {
                return turAemSource.isPublish()
                                && StringUtils.isNotEmpty(turAemSource.getPublishSNSite());
        }

        private static boolean isAuthor(TurAemSource turAemSource) {
                return turAemSource.isAuthor()
                                && StringUtils.isNotEmpty(turAemSource.getAuthorSNSite());
        }

        private void createIndexJobAndSendToConnectorQueue(TurAemObject aemObject,
                        TurAemModel turAemModel, List<TurSNAttributeSpec> turSNAttributeSpecList,
                        Locale locale, TurAemSourceContext turAemSourceContext,
                        TurConnectorSession session,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        boolean standalone) {
                TurSNJobItem turSNJobItem = getTurSNJobItem(aemObject, turSNAttributeSpecList,
                                locale, turAemSourceContext, session,
                                turAemContentDefinitionProcess,
                                getJobItemAttributes(turAemSourceContext,
                                                getTargetAttrValueMap(aemObject, turAemModel,
                                                                turSNAttributeSpecList,
                                                                turAemSourceContext,
                                                                turAemContentDefinitionProcess)));
                TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(turSNJobItem,
                                session, aemObject.getDependencies(), standalone);
                turConnectorContext.addJobItem(jobItemWithSession);
        }

        private static @NotNull TurAemTargetAttrValueMap getTargetAttrValueMap(
                        TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
                TurAemTargetAttrValueMap turAemTargetAttrValueMap = new TurAemAttrProcess()
                                .prepareAttributeDefs(aemObject, turAemContentDefinitionProcess,
                                                turSNAttributeSpecList, turAemSourceContext);
                turAemTargetAttrValueMap.merge(TurAemCommonsUtils.runCustomClassFromContentType(
                                turAemModel, aemObject, turAemSourceContext));
                return turAemTargetAttrValueMap;
        }

        private static @NotNull TurSNJobItem getTurSNJobItem(TurAemObject aemObject,
                        List<TurSNAttributeSpec> turSNAttributeSpecList, Locale locale,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                        Map<String, Object> attributes) {
                TurSNJobItem jobItem = new TurSNJobItem(CREATE,
                                session.getSites().stream().toList(), locale, attributes,
                                TurAemCommonsUtils.castSpecToJobSpec(TurAemCommonsUtils
                                                .getDefinitionFromModel(turSNAttributeSpecList,
                                                                attributes)));
                jobItem.setChecksum(
                                String.valueOf(TurAemCommonsUtils
                                                .getDeltaDate(aemObject, turAemSourceContext,
                                                                turAemContentDefinitionProcess)
                                                .getTime()));
                jobItem.setEnvironment(turAemSourceContext.getEnvironment().toString());
                return jobItem;
        }

        private static @NotNull Map<String, Object> getJobItemAttributes(
                        TurAemSourceContext turAemSourceContext,
                        TurAemTargetAttrValueMap targetAttrValueMap) {
                Map<String, Object> attributes = new HashMap<>();
                String siteName = turAemSourceContext.getSiteName();
                if (StringUtils.isNotBlank(siteName)) {
                        attributes.put(SITE, siteName);
                }
                targetAttrValueMap.entrySet().stream()
                                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                                .forEach(e -> getJobItemAttribute(e, attributes));
                return attributes;
        }

        private static void getJobItemAttribute(Map.Entry<String, TurMultiValue> entry,
                        Map<String, Object> attributes) {
                String attributeName = entry.getKey();
                entry.getValue().stream().filter(StringUtils::isNotBlank)
                                .forEach(attributeValue -> {
                                        if (attributes.containsKey(attributeName)) {
                                                TurAemCommonsUtils.addItemInExistingAttribute(
                                                                attributeValue, attributes,
                                                                attributeName);
                                        } else {
                                                TurAemCommonsUtils.addFirstItemToAttribute(
                                                                attributeName, attributeValue,
                                                                attributes);
                                        }
                                });
        }
}
