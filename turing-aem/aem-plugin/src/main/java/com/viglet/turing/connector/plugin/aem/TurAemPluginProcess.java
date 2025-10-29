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

import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR_PRIMARY_TYPE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.REP;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.STATIC_FILE_SUB_TYPE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.api.TurAemPathList;
import com.viglet.turing.connector.plugin.aem.conf.AemPluginHandlerConfiguration;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemAttributeSpecificationRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemConfigVarRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginSystemRepository;
import com.viglet.turing.connector.plugin.aem.service.TurAemContentMappingService;
import com.viglet.turing.connector.plugin.aem.service.TurAemJobService;
import com.viglet.turing.connector.plugin.aem.service.TurAemReactiveUtils;
import com.viglet.turing.connector.plugin.aem.service.TurAemService;
import com.viglet.turing.connector.plugin.aem.service.TurAemSourceService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Alexandre Oliveira
 * @since 2025.2
 */
@Slf4j
@Getter
@Component
public class TurAemPluginProcess {
        private final TurConnectorContext turConnectorContext;
        private final boolean connectorDependencies;
        private final boolean reativeIndexing;
        private final List<String> runningSources = new ArrayList<>();
        private final TurAemReactiveUtils turAemReactiveUtils;
        private final TurAemContentMappingService turAemContentMappingService;
        private final TurAemAttrProcess turAemAttrProcess;
        private final TurAemSourceService turAemSourceService;
        private final TurAemService turAemService;
        private final TurAemJobService turAemJobService;
        private final TurAemContentDefinitionService turAemContentDefinitionService;

        public TurAemPluginProcess(TurAemPluginSystemRepository turAemPluginSystemRepository,
                        TurAemConfigVarRepository turAemConfigVarRepository,
                        TurAemAttributeSpecificationRepository turAemAttributeSpecificationRepository,
                        TurConnectorContext turConnectorContext,
                        @Value("${turing.connector.dependencies.enabled:true}") boolean connectorDependencies,
                        @Value("${turing.connector.reactive.indexing:false}") boolean reativeIndexing,
                        TurAemReactiveUtils turAemReactiveUtils,
                        TurAemContentMappingService turAemContentMappingService,
                        TurAemAttrProcess turAemAttrProcess,
                        TurAemSourceService turAemSourceService,
                        TurAemService turAemService,
                        TurAemJobService turAemJobService,
                        TurAemContentDefinitionService turAemContentDefinitionService) {
                this.turConnectorContext = turConnectorContext;
                this.connectorDependencies = connectorDependencies;
                this.reativeIndexing = reativeIndexing;
                this.turAemReactiveUtils = turAemReactiveUtils;
                this.turAemContentMappingService = turAemContentMappingService;
                this.turAemAttrProcess = turAemAttrProcess;
                this.turAemSourceService = turAemSourceService;
                this.turAemService = turAemService;
                this.turAemJobService = turAemJobService;
                this.turAemContentDefinitionService = turAemContentDefinitionService;
        }

        @Async
        public void indexAllByNameAsync(String sourceName) {
                turAemSourceService.getTurAemSourceByName(sourceName).ifPresent(this::indexAll);
        }

        @Async
        public void indexAllByIdAsync(String id) {
                turAemSourceService.getTurAemSourceById(id).ifPresent(this::indexAll);
        }

        @Async
        public void sentToIndexStandaloneAsync(@NotNull String source,
                        @NotNull TurAemPathList turAemPathList) {
                sentToIndexStandalone(source, turAemPathList);
        }

        public void sentToIndexStandalone(@NotNull String source, @NotNull TurAemPathList turAemPathList) {
                if (CollectionUtils.isEmpty(turAemPathList.getPaths())) {
                        log.warn("Received empty payload for source: {}", source);
                        return;
                }
                log.info("Processing payload for source '{}' with paths: {}", source, turAemPathList.getPaths());
                turAemSourceService.getTurAemSourceByName(source).ifPresentOrElse(turAemSource -> {
                        TurConnectorSession session = turAemSourceService.getTurConnectorSession(turAemSource);
                        // Index each provided path
                        turAemPathList.getPaths().stream().filter(StringUtils::isNotBlank)
                                        .forEach(path -> indexContentId(session, turAemSource, path,
                                                        true, turAemPathList.getRecursive(),
                                                        turAemPathList.getEvent()));
                        if (connectorDependencies) {
                                indexDependencies(source, turAemPathList.getPaths(), turAemSource, session);
                        }
                        finished(session, true);
                }, () -> log.error("Source '{}' not found", source));
        }

        private void indexDependencies(String source, List<String> idList,
                        TurAemSource turAemSource, TurConnectorSession session) {
                turConnectorContext.getObjectIdByDependency(source, turAemService.getProviderName(), idList)
                                .stream().filter(StringUtils::isNotBlank)
                                .forEach(objectId -> indexContentId(session, turAemSource, objectId,
                                                true, false, TurAemEvent.NONE));
        }

        public void indexAll(TurAemSource turAemSource) {
                if (runningSources.contains(turAemSource.getName())) {
                        log.warn("Skipping. There are already source process running. {}",
                                        turAemSource.getName());
                        return;
                }
                runningSources.add(turAemSource.getName());
                TurConnectorSession turConnectorSession = turAemSourceService.getTurConnectorSession(turAemSource);
                try {
                        this.getNodesFromJson(
                                        turAemSourceService.getTurAemSourceContext(turAemSource),
                                        turConnectorSession, turAemSource);
                } catch (Exception e) {
                        log.error(e.getMessage(), e);
                }
                finished(turConnectorSession, false);
        }

        public void finished(TurConnectorSession turConnectorSession, boolean standalone) {
                if (!standalone)
                        runningSources.remove(turConnectorSession.getSource());
                turConnectorContext.finishIndexing(turConnectorSession, standalone);
        }

        private void getNodesFromJson(TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource) {
                if (!TurAemCommonsUtils.usingContentTypeParameter(turAemSourceContext))
                        return;
                byContentTypeList(turAemSourceContext, turConnectorSession, turAemSource);
        }

        public void indexContentId(TurConnectorSession session, TurAemSource turAemSource,
                        String contentId, boolean standalone, boolean indexChildren,
                        TurAemEvent event) {
                TurAemSourceContext turAemSourceContext = turAemSourceService.getTurAemSourceContext(turAemSource);
                TurAemCommonsUtils.getInfinityJson(contentId, turAemSourceContext, false)
                                .ifPresentOrElse(infinityJson -> {
                                        turAemSourceContext.setContentType(
                                                        infinityJson.getString(JCR_PRIMARY_TYPE));
                                        TurAemObject turAemObject = new TurAemObject(contentId,
                                                        infinityJson, event);
                                        getNodeFromJson(turAemObject,
                                                        turAemSourceContext, session, turAemSource,
                                                        standalone, indexChildren);
                                }, () -> turAemJobService.createDeIndexJobAndSendToConnectorQueue(session, contentId,
                                                standalone));
        }

        private void byContentTypeList(TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource) {
                turAemContentDefinitionService
                                .findByModelNameAndAemSource(
                                                turAemSourceContext.getContentType(), turAemSource)
                                .ifPresentOrElse(
                                                turAemModel -> byContentType(turAemSourceContext,
                                                                turConnectorSession, turAemSource),
                                                () -> log.debug("{} type is not configured in CTD Mapping file.",
                                                                turAemSourceContext
                                                                                .getContentType()));
        }

        private void byContentType(TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource) {
                TurAemCommonsUtils
                                .getInfinityJson(turAemSourceContext.getRootPath(),
                                                turAemSourceContext, false)
                                .ifPresent(infinityJson -> {
                                        TurAemObject turAemObject = new TurAemObject(turAemSourceContext.getRootPath(),
                                                        infinityJson, TurAemEvent.NONE);
                                        getNodeFromJson(turAemObject, turAemSourceContext, turConnectorSession,
                                                        turAemSource, false, true);
                                });
        }

        private void getNodeFromJson(TurAemObject turAemObject,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource, boolean standalone, boolean indexChildren) {
                if (TurAemCommonsUtils.isTypeEqualContentType(turAemObject.getJcrNode(), turAemSourceContext)) {
                        turAemContentDefinitionService.findByModelNameAndAemSource(
                                        turAemSourceContext.getContentType(), turAemSource)
                                        .ifPresent(model -> turAemJobService.prepareIndexObject(model,
                                                        turAemObject,
                                                        turAemSourceContext, session, turAemSource,
                                                        standalone));
                }
                if (indexChildren) {
                        getChildrenFromJson(turAemObject, turAemSourceContext, session,
                                        turAemSource, standalone);
                }
        }

        private void getChildrenFromJson(TurAemObject turAemObject,
                        TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        boolean standalone) {
                if (reativeIndexing) {
                        try {
                                getChildrenFromJsonReactive(turAemObject,
                                                turAemSourceContext, turConnectorSession,
                                                turAemSource, standalone).block();
                        } catch (Exception e) {
                                log.warn("Reactive processing failed, falling back to synchronous: {}",
                                                e.getMessage(), e);
                        }
                } else {
                        getChildrenFromJsonSynchronous(turAemObject, turAemSourceContext,
                                        turConnectorSession, turAemSource, standalone);
                }

        }

        private void getChildrenFromJsonSynchronous(TurAemObject turAemObject,
                        TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        boolean standalone) {
                turAemObject.getJcrNode().toMap().forEach((nodeName, nodeValue) -> {
                        if (isIndexedNode(turAemSourceContext, nodeName)) {
                                String nodePathChild = "%s/%s".formatted(turAemObject.getPath(), nodeName);
                                if (isNotOnce(turAemSourceContext, nodePathChild, turAemSource)) {
                                        getChildNode(nodePathChild, turAemSourceContext,
                                                        turConnectorSession, turAemSource, standalone);
                                }
                        }
                });
        }

        private void getChildNode(String nodePathChild, TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        boolean standalone) {
                TurAemCommonsUtils.getInfinityJson(nodePathChild, turAemSourceContext, false)
                                .ifPresent(infinityJson -> {
                                        getTurAemObjectChild(nodePathChild, turAemSourceContext, turConnectorSession,
                                                        turAemSource, standalone, infinityJson);
                                });
        }

        private void getTurAemObjectChild(String nodePathChild, TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource, boolean standalone,
                        JSONObject infinityJson) {
                TurAemObject turAemObjectChild = new TurAemObject(nodePathChild, infinityJson, TurAemEvent.NONE);
                getNodeFromJson(turAemObjectChild, turAemSourceContext, turConnectorSession, turAemSource, standalone,
                                true);
        }

        private boolean isNotOnce(TurAemSourceContext turAemSourceContext, String nodePathChild,
                        TurAemSource turAemSource) {
                return !turAemSourceService.isOnce(turAemSourceContext)
                                || TurAemCommonsUtils.isNotOnceConfig(nodePathChild,
                                                new AemPluginHandlerConfiguration(
                                                                turAemSource));
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

        /**
         * Reactive version of getChildrenFromJson that processes children using WebFlux
         */
        private Mono<Void> getChildrenFromJsonReactive(TurAemObject turAemObject,
                        TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        boolean standalone) {

                // Create a Flux from the child nodes
                return Flux.fromIterable(turAemObject.getJcrNode().toMap().entrySet())
                                .filter(entry -> isIndexedNode(turAemSourceContext, entry.getKey()))
                                .flatMap(entry -> {
                                        String nodeName = entry.getKey();
                                        String nodePathChild = "%s/%s".formatted(turAemObject.getPath(), nodeName);

                                        if (!turAemSourceService.isOnce(turAemSourceContext) || TurAemCommonsUtils
                                                        .isNotOnceConfig(nodePathChild,
                                                                        new AemPluginHandlerConfiguration(
                                                                                        turAemSource))) {

                                                // Use reactive getInfinityJson
                                                return turAemReactiveUtils.getInfinityJsonReactive(
                                                                nodePathChild, turAemSourceContext)
                                                                .flatMap(infinityJson -> getNodeFromJsonReactive(
                                                                                turAemObject,
                                                                                turAemSourceContext,
                                                                                turConnectorSession,
                                                                                turAemSource,
                                                                                standalone, true));
                                        }
                                        return Mono.<Void>empty();
                                }, 10) // Process up to 10 concurrent requests
                                .then(); // Convert to Mono<Void>
        }

        /**
         * Reactive version of getNodeFromJson
         */
        private Mono<Void> getNodeFromJsonReactive(TurAemObject turAemObject,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource, boolean standalone, boolean indexChildren) {

                // Handle node indexing synchronously (this part doesn't involve HTTP calls)
                if (TurAemCommonsUtils.isTypeEqualContentType(turAemObject.getJcrNode(), turAemSourceContext)) {
                        turAemContentDefinitionService.findByModelNameAndAemSource(
                                        turAemSourceContext.getContentType(), turAemSource)
                                        .ifPresent(model -> turAemJobService.prepareIndexObject(model,
                                                        turAemObject,
                                                        turAemSourceContext, session, turAemSource,
                                                        standalone));
                }

                // Handle children processing reactively if needed
                if (indexChildren) {
                        return getChildrenFromJsonReactive(turAemObject,
                                        turAemSourceContext, session, turAemSource, standalone);
                }
                return Mono.empty();
        }
}
