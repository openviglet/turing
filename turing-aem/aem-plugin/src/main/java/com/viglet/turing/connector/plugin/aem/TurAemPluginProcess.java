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
import com.viglet.turing.client.sn.TurSNConstants;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private final TurAemPluginService turAemPluginService;
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
                               @Value("${turing.apiKey}") String turingApiKey,
                               TurAemPluginService turAemPluginService) {
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
        this.turAemPluginService = turAemPluginService;
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
    public void sentToIndexStandaloneAsync(String name, TurAemPathList turAemPathList) {
        if (turAemPathList == null || turAemPathList.getPaths() == null || turAemPathList.getPaths().isEmpty()) {
            log.error("Payload is empty");
            return;
        }
        log.debug("Receiving Async payload to {} source with paths {}", name, turAemPathList);
        turAemSourceRepository.findByName(name).ifPresentOrElse(turAemSource -> {
                    TurConnectorSession turConnectorSession = TurAemPluginProcess.getTurConnectorSession(turAemSource);
                    turAemPathList.getPaths().forEach(path -> indexContentId(turConnectorSession, turAemSource, path, true));
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
            this.getNodesFromJson(turAemPluginService.getTurAemSourceContext(new AemPluginHandlerConfiguration(turAemSource)),
                    turConnectorSession, turAemSource,
                    new TurAemContentDefinitionProcess(turAemPluginService.getTurAemContentMapping(turAemSource)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        finished(turConnectorSession, false);
    }

    public static @NotNull TurConnectorSession getTurConnectorSession(TurAemSource turAemSource) {
        // sites parameter is null, because in next step need check if author our
        // publishing.
        return new TurConnectorSession(turAemSource.getName(), null, AEM, turAemSource.getDefaultLocale());
    }

    public void finished(TurConnectorSession turConnectorSession, boolean standalone) {
        if (!standalone)
            runningSources.remove(turConnectorSession.getSource());
        turConnectorContext.finishIndexing(turConnectorSession, standalone);
    }

    private void getNodesFromJson(TurAemSourceContext turAemSourceContext,
                                  TurConnectorSession turConnectorSession,
                                  TurAemSource turAemSource,
                                  TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        if (!TurAemCommonsUtils.usingContentTypeParameter(turAemSourceContext))
            return;
        byContentTypeList(turAemSourceContext, turConnectorSession, turAemSource,
                turAemContentDefinitionProcess);
    }

    public void indexContentId(TurConnectorSession session, TurAemSource turAemSource, String contentId,
                               boolean standalone) {
        TurAemSourceContext turAemSourceContext = turAemPluginService.getTurAemSourceContext(
                new AemPluginHandlerConfiguration(turAemSource));
        TurAemCommonsUtils.getInfinityJson(contentId, turAemSourceContext, false)
                .ifPresentOrElse(infinityJson -> {
                    turAemSourceContext.setContentType(infinityJson.getString(JCR_PRIMARY_TYPE));
                    getNodeFromJson(contentId, infinityJson, turAemSourceContext, session, turAemSource,
                            new TurAemContentDefinitionProcess(turAemPluginService
                                    .getTurAemContentMapping(turAemSource)), standalone);
                }, () -> sendToTuringToBeDeIndexed(session, contentId, standalone));
    }

    private void sendToTuringToBeDeIndexed(TurConnectorSession session, String contentId, boolean standalone) {
        turConnectorContext.getIndexingItem(contentId, session.getSource())
                .forEach(
                        indexing -> turConnectorContext
                                .addJobItem(deIndexJob(session, indexing), session, standalone));
    }

    private TurSNJobItem deIndexJob(TurConnectorSession session,
                                    TurConnectorIndexingDTO turConnectorIndexingDTO) {
        return new TurSNJobItem(
                TurSNJobAction.DELETE,
                turConnectorIndexingDTO.getSites(),
                turConnectorIndexingDTO.getLocale(),
                Map.of(
                        TurSNConstants.ID_ATTR, turConnectorIndexingDTO.getObjectId(),
                        TurSNConstants.SOURCE_APPS_ATTR, session.getProviderName()));
    }

    private void byContentTypeList(TurAemSourceContext turAemSourceContext,
                                   TurConnectorSession turConnectorSession,
                                   TurAemSource turAemSource,
                                   TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        turAemContentDefinitionProcess.findByNameFromModelWithDefinition(turAemSourceContext.getContentType())
                .ifPresentOrElse(turAemModel -> byContentType(turAemSourceContext, turConnectorSession, turAemSource,
                                turAemContentDefinitionProcess),
                        () -> log.debug("{} type is not configured in CTD Mapping file.",
                                turAemSourceContext.getContentType()));
    }

    private void byContentType(TurAemSourceContext turAemSourceContext,
                               TurConnectorSession turConnectorSession,
                               TurAemSource turAemSource,
                               TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        TurAemCommonsUtils.getInfinityJson(turAemSourceContext.getRootPath(), turAemSourceContext, false)
                .ifPresent(infinityJson -> getNodeFromJson(turAemSourceContext.getRootPath(), infinityJson,
                        turAemSourceContext,
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
                    .ifPresent(model -> prepareIndexObject(model, new TurAemObject(nodePath, jsonObject),
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
                            .ifPresent(infinityJson -> getNodeFromJson(nodePathChild, infinityJson, turAemSourceContext,
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
        if (TurAemPluginUtils.isNotValidType(turAemModel, aemObject, type)) {
            return;
        }
        if (TurAemPluginUtils.isContentFragment(turAemModel, type, aemObject)) {
            aemObject.setDataPath(DATA_MASTER);
        } else if (TurAemPluginUtils.isStaticFile(turAemModel, type)) {
            aemObject.setDataPath(METADATA);
        }
        indexObject(aemObject, turAemModel, targetAttrDefinitions, turAemSourceContext,
                turConnectorSession, turAemSource, turAemContentDefinitionProcess, standalone);
    }

    private void indexObject(@NotNull TurAemObject aemObject, TurAemModel turAemModel,
                             List<TurSNAttributeSpec> turSNAttributeSpecList,
                             TurAemSourceContext turAemSourceContext,
                             TurConnectorSession turConnectorSession,
                             TurAemSource turAemSource,
                             TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                             boolean standalone) {
        if (TurAemPluginUtils.isAuthor(turAemSource)) {
            indexByEnvironment(TurAemEnv.AUTHOR, turAemSource.getAuthorSNSite(),
                    aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                    turConnectorSession, turAemContentDefinitionProcess, standalone);
        }
        if (TurAemPluginUtils.isPublish(turAemSource)) {
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
                        TurAemPluginUtils.getJobItemAttributes(
                                turAemSourceContext,
                                TurAemPluginUtils.getTargetAttrValueMap(
                                        aemObject,
                                        turAemModel,
                                        turSNAttributeSpecList,
                                        turAemSourceContext,
                                        turAemContentDefinitionProcess))),
                turConnectorSession,
                standalone);
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
                        TurAemCommonsUtils.getDefinitionFromModel(turSNAttributeSpecList, attributes)));
        jobItem.setChecksum(String.valueOf(TurAemCommonsUtils
                .getDeltaDate(aemObject, turAemSourceContext, turAemContentDefinitionProcess).getTime()));
        jobItem.setEnvironment(turAemSourceContext.getEnvironment().toString());
        return jobItem;
    }



}