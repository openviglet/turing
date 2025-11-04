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
import com.viglet.turing.connector.aem.commons.TurAemObjectGeneric;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.plugin.aem.api.TurAemPathList;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.service.TurAemJobService;
import com.viglet.turing.connector.plugin.aem.service.TurAemObjectService;
import com.viglet.turing.connector.plugin.aem.service.TurAemReactiveUtils;
import com.viglet.turing.connector.plugin.aem.service.TurAemService;
import com.viglet.turing.connector.plugin.aem.service.TurAemSessionService;
import com.viglet.turing.connector.plugin.aem.service.TurAemSourceService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Alexandre Oliveira
 * @since 2025.2
 */
@Slf4j
@Component
public class TurAemPluginProcess {
        private final TurConnectorContext turConnectorContext;
        private final boolean connectorDependencies;
        private final boolean reativeIndexing;
        private final List<String> runningSources = new ArrayList<>();
        private final TurAemReactiveUtils turAemReactiveUtils;
        private final TurAemSourceService turAemSourceService;
        private final TurAemService turAemService;
        private final TurAemJobService turAemJobService;
        private final TurAemSessionService turAemSessionService;
        private final TurAemObjectService turAemObjectService;

        public TurAemPluginProcess(TurConnectorContext turConnectorContext,
                        @Value("${turing.connector.dependencies.enabled:true}") boolean connectorDependencies,
                        @Value("${turing.connector.reactive.indexing:false}") boolean reativeIndexing,
                        TurAemReactiveUtils turAemReactiveUtils,
                        TurAemSourceService turAemSourceService,
                        TurAemService turAemService,
                        TurAemJobService turAemJobService,
                        TurAemSessionService turAemSessionService,
                        TurAemObjectService turAemObjectService) {
                this.turConnectorContext = turConnectorContext;
                this.connectorDependencies = connectorDependencies;
                this.reativeIndexing = reativeIndexing;
                this.turAemReactiveUtils = turAemReactiveUtils;
                this.turAemSourceService = turAemSourceService;
                this.turAemService = turAemService;
                this.turAemJobService = turAemJobService;
                this.turAemSessionService = turAemSessionService;
                this.turAemObjectService = turAemObjectService;
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

        public void sentToIndexStandalone(@NotNull String source,
                        @NotNull TurAemPathList turAemPathList) {
                if (CollectionUtils.isEmpty(turAemPathList.getPaths())) {
                        log.warn("Received empty payload for source: {}", source);
                        return;
                }

                log.info("Processing payload for source '{}' with paths: {}", source,
                                turAemPathList.getPaths());
                turAemSourceService.getTurAemSourceByName(source).ifPresentOrElse(turAemSource -> {
                        TurAemSession turAemSession =
                                        turAemSessionService.getTurAemSession(turAemSource,
                                                        turAemPathList);
                        indexContentIdList(turAemPathList.getPaths(), turAemSession);
                        if (connectorDependencies) {
                                indexDependencies(turAemSession, turAemPathList.getPaths());
                        }
                        finished(turAemSession);
                }, () -> log.error("Source '{}' not found", source));
        }

        private void indexContentIdList(List<String> contentIdList, TurAemSession turAemSession) {
                contentIdList.stream().filter(StringUtils::isNotBlank)
                                .forEach(path -> indexContentId(turAemSession, path));
        }

        public void indexContentId(TurAemSession turAemSession, String contentId) {
                if (!isValidSession(turAemSession, contentId)) {
                        return;
                }

                if (StringUtils.isBlank(contentId)) {
                        log.debug("Ignoring blank contentId");
                        return;
                }

                TurAemCommonsUtils
                                .getInfinityJson(contentId, turAemSession.getConfiguration(), false)
                                .ifPresentOrElse(
                                                infinityJson -> processIndexingContent(
                                                                turAemSession, contentId,
                                                                infinityJson),
                                                () -> handleMissingContent(turAemSession,
                                                                contentId));
        }

        private boolean isValidSession(TurAemSession turAemSession, String contentId) {
                if (turAemSession == null || turAemSession.getConfiguration() == null) {
                        log.warn("Session or configuration is null for contentId: {}", contentId);
                        return false;
                }
                return true;
        }

        private void processIndexingContent(TurAemSession turAemSession, String contentId,
                        JSONObject infinityJson) {
                TurAemObjectGeneric turAemObjectGeneric =
                                turAemObjectService.getTurAemObjectGeneric(contentId,
                                                infinityJson, turAemSession.getEvent());
                log.debug("Processing content with primaryType: {} for contentId: {}",
                                turAemObjectGeneric.getType(),
                                turAemObjectGeneric.getPath());
                getNodeFromJson(turAemSession, turAemObjectGeneric);


        }

        private void handleMissingContent(TurAemSession turAemSession, String contentId) {
                try {
                        log.debug("Content not found, creating de-index job for contentId: {}",
                                        contentId);
                        turAemJobService.createDeIndexJobAndSendToConnectorQueue(turAemSession,
                                        contentId);
                } catch (Exception e) {
                        log.error("Failed to create de-index job for contentId {}: {}", contentId,
                                        e.getMessage(), e);
                }
        }

        public void indexAll(TurAemSource turAemSource) {
                if (runningSources.contains(turAemSource.getName())) {
                        log.warn("Skipping. There are already source process running. {}",
                                        turAemSource.getName());
                        return;
                }
                runningSources.add(turAemSource.getName());
                TurAemSession turAemSession = turAemSessionService.getTurAemSession(turAemSource);
                try {
                        this.getNodesFromJson(turAemSession);
                } catch (Exception e) {
                        log.error(e.getMessage(), e);
                }
                finished(turAemSession);
        }

        private void indexDependencies(TurAemSession turAemSession, List<String> idList) {
                List<String> contentIdList = turConnectorContext
                                .getObjectIdByDependency(turAemSession.getSource(),
                                                turAemService.getProviderName(), idList);
                indexContentIdList(contentIdList, turAemSession);
        }

        private void finished(TurAemSession turAemSession) {
                if (!turAemSession.isStandalone())
                        runningSources.remove(turAemSession.getSource());
                turConnectorContext.finishIndexing(turAemSession, turAemSession.isStandalone());
        }

        private void getNodesFromJson(TurAemSession turAemSession) {
                if (!TurAemCommonsUtils.usingContentTypeParameter(turAemSession.getConfiguration()))
                        return;
                byContentType(turAemSession);
        }

        private void byContentType(TurAemSession turAemSession) {
                TurAemConfiguration configuration = turAemSession.getConfiguration();
                String rootPath = configuration.getRootPath();

                TurAemCommonsUtils.getInfinityJson(rootPath, configuration, false)
                                .ifPresent(infinityJson -> {
                                        TurAemObjectGeneric turAemObjectGeneric =
                                                        turAemObjectService.getTurAemObjectGeneric(
                                                                        rootPath,
                                                                        infinityJson,
                                                                        turAemSession.getEvent());
                                        getNodeFromJson(turAemSession, turAemObjectGeneric);

                                });
        }


        private void getNodeFromJson(TurAemSession turAemSession,
                        TurAemObjectGeneric turAemObject) {
                processIndexingForContentType(turAemSession, turAemObject);
                if (turAemSession.isIndexChildren()) {
                        getChildrenFromJson(turAemSession, turAemObject);
                }
        }

        /**
         * Reactive version of getNodeFromJson
         */
        private Mono<Void> getNodeFromJsonReactive(TurAemSession turAemSession,
                        TurAemObjectGeneric turAemObject) {
                processIndexingForContentType(turAemSession, turAemObject);
                if (turAemSession.isIndexChildren()) {
                        return getChildrenFromJsonReactive(turAemSession, turAemObject);
                }
                return Mono.empty();
        }

        private void processIndexingForContentType(TurAemSession turAemSession,
                        TurAemObjectGeneric turAemObjectGeneric) {
                if (TurAemCommonsUtils.isTypeEqualContentType(turAemObjectGeneric,
                                turAemSession.getConfiguration())) {
                        turAemJobService.prepareIndexObject(turAemSession, turAemObjectGeneric);
                }
        }

        private void getChildrenFromJson(TurAemSession turAemSession,
                        TurAemObjectGeneric turAemObject) {
                if (reativeIndexing) {
                        try {
                                getChildrenFromJsonReactive(turAemSession, turAemObject).block();
                        } catch (Exception e) {
                                log.warn("Reactive processing failed, falling back to synchronous: {}",
                                                e.getMessage(), e);
                        }
                } else {
                        getChildrenFromJsonSynchronous(turAemSession, turAemObject);
                }

        }

        private void getChildrenFromJsonSynchronous(TurAemSession turAemSession,
                        TurAemObjectGeneric turAemObject) {
                turAemObject.getJcrNode().toMap().forEach((nodeName, nodeValue) -> {
                        if (isIndexedNode(turAemSession.getConfiguration(), nodeName)) {
                                String nodePathChild =
                                                "%s/%s".formatted(turAemObject.getPath(), nodeName);
                                if (isNotOnce(turAemSession, nodePathChild)) {
                                        getChildNode(turAemSession, nodePathChild);
                                }
                        }
                });
        }

        private void getChildNode(TurAemSession turAemSession, String nodePathChild) {
                TurAemCommonsUtils.getInfinityJson(nodePathChild, turAemSession.getConfiguration(),
                                false).ifPresent(infinityJson -> {
                                        getTurAemObjectChild(turAemSession, nodePathChild,
                                                        infinityJson);
                                });
        }

        private void getTurAemObjectChild(TurAemSession turAemSession, String nodePathChild,
                        JSONObject infinityJson) {
                TurAemObjectGeneric turAemObject = turAemObjectService
                                .getTurAemObjectGeneric(nodePathChild, infinityJson);
                getNodeFromJson(turAemSession, turAemObject);
        }

        private boolean isNotOnce(TurAemSession turAemSession, String nodePathChild) {
                TurAemConfiguration config = turAemSession.getConfiguration();
                return !turAemSourceService.isOnce(config)
                                || TurAemCommonsUtils.isNotOnceConfig(nodePathChild, config);
        }

        private static boolean isIndexedNode(TurAemConfiguration turAemSourceContext,
                        String nodeName) {
                return !nodeName.startsWith(JCR)
                                && !nodeName.startsWith(REP)
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
        private Mono<Void> getChildrenFromJsonReactive(TurAemSession turAemSession,
                        TurAemObjectGeneric turAemObject) {
                TurAemConfiguration config = turAemSession.getConfiguration();
                return Flux.fromIterable(turAemObject.getJcrNode().toMap().entrySet())
                                .filter(entry -> isIndexedNode(config, entry.getKey()))
                                .flatMap(entry -> {
                                        String nodeName = entry.getKey();
                                        String nodePathChild = "%s/%s".formatted(
                                                        turAemObject.getPath(), nodeName);
                                        if (!turAemSourceService.isOnce(config)
                                                        || TurAemCommonsUtils.isNotOnceConfig(
                                                                        nodePathChild, config)) {
                                                return turAemReactiveUtils
                                                                .getInfinityJsonReactive(
                                                                                nodePathChild,
                                                                                config)
                                                                .flatMap(infinityJson -> getNodeFromJsonReactive(
                                                                                turAemSession,
                                                                                turAemObject));
                                        }
                                        return Mono.<Void>empty();
                                }, 10).then();
        }

}
