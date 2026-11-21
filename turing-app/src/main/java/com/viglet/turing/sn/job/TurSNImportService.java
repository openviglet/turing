/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.job;

import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import com.viglet.turing.agent.TurDefaultAgentResolver;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.system.logging.TurLoggingUtils;
import com.viglet.turing.answer.TurSNGenAi;
import com.viglet.turing.sn.TurSNConstants;
import com.viglet.turing.sn.domain.TurSNSiteRepositoryPort;
import com.viglet.turing.tenant.TurJmsTenantPropagation;

import lombok.extern.slf4j.Slf4j;

/**
 * Sends a batch of job items to the indexing queue, embedding it first when one
 * of its sites is RAG-ready. The import endpoints and every module that indexes
 * documents (MCP write tools, structured feeds, migrations) go through this one
 * path, so none of them depends on a controller (T1308, ADR 0005).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Service
public class TurSNImportService {
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final TurSNSiteRepositoryPort turSNSiteRepositoryPort;
    private final TurSNGenAi turGenAi;
    private final TurJmsTenantPropagation turJmsTenantPropagation;
    private final TurDefaultAgentResolver turDefaultAgentResolver;

    public TurSNImportService(JmsMessagingTemplate jmsMessagingTemplate,
            TurSNSiteRepositoryPort turSNSiteRepositoryPort,
            TurSNGenAi turGenAi,
            TurJmsTenantPropagation turJmsTenantPropagation,
            TurDefaultAgentResolver turDefaultAgentResolver) {
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.turSNSiteRepositoryPort = turSNSiteRepositoryPort;
        this.turGenAi = turGenAi;
        this.turJmsTenantPropagation = turJmsTenantPropagation;
        this.turDefaultAgentResolver = turDefaultAgentResolver;
    }

    public void send(TurSNJobItems turSNJobItems) {
        sendToGenAi(turSNJobItems);
        sentQueueInfo(turSNJobItems);
        if (log.isDebugEnabled()) {
            log.debug("Sent job - {}", TurSNConstants.INDEXING_QUEUE);
            log.debug("turSNJob: {}", turSNJobItems);
        }
        // T272 / §XIV.4.6 — stamp the current tenant so the consumer thread can restore it.
        this.jmsMessagingTemplate.convertAndSend(TurSNConstants.INDEXING_QUEUE, turSNJobItems,
                turJmsTenantPropagation.headers());
    }

    /**
     * Embeds the batch when any of its documents goes to a RAG-enabled site.
     *
     * <p>T1089 — once per batch, not once per document and site (each call embeds
     * the whole batch, so a batch of 50 was embedded 50 times), and never the
     * reason a batch fails: an embedding error is logged and the batch still goes
     * to the indexing queue, so search keeps working while RAG does not.
     */
    private void sendToGenAi(TurSNJobItems turSNJobItems) {
        boolean anyRagSite = false;
        for (TurSNJobItem turJobItem : turSNJobItems) {
            if (!isValidJobItem(turJobItem)) {
                continue;
            }
            Object objectId = turJobItem.getAttributes().get(TurSNFieldName.ID);
            for (String siteName : turJobItem.getSiteNames()) {
                // T1147 — the effective agent, so a site on the Default AI Agent is embedded too.
                if (turDefaultAgentResolver.isRagReadyForSiteName(siteName)) {
                    log.debug("RAG enabled for Object ID '{}' of SN Site '{}': routing to vector store",
                            objectId, siteName);
                    anyRagSite = true;
                } else {
                    log.debug("RAG skipped for Object ID '{}' of SN Site '{}': site missing, vectorless or no RAG-ready agent",
                            objectId, siteName);
                }
            }
        }
        if (!anyRagSite) {
            return;
        }
        try {
            turGenAi.addDocuments(turSNJobItems);
        } catch (RuntimeException e) {
            log.error("RAG embedding of the batch failed; its documents are still indexed for search: {}",
                    e.getMessage(), e);
        }
    }

    private void sentQueueInfo(TurSNJobItems turSNJobItems) {
        turSNJobItems.forEach(turSNJobItem -> {
            if (isValidJobItem(turSNJobItem)) {
                turSNJobItem.getSiteNames()
                        .forEach(siteName -> turSNSiteRepositoryPort.findByNameIgnoreCase(siteName).ifPresentOrElse(turSNSite -> {
                            log.info("Sent to queue to {} the Object ID '{}' of '{}' SN Site ({}).",
                                    actionType(turSNJobItem),
                                    turSNJobItem.getAttributes().get(TurSNFieldName.ID),
                                    turSNSite.name(),
                                    turSNJobItem.getLocale());
                            TurLoggingUtils.setSuccessStatus(turSNJobItem, TurIndexingStatus.SENT_TO_QUEUE);
                        },
                                () -> importUnsuccessful(siteName, turSNJobItems)));
            }
        });
    }

    private void importUnsuccessful(String siteName, TurSNJobItems turSNJobItems) {
        turSNJobItems.forEach(turSNJobItem -> {
            if (turSNJobItem != null) {
                if (turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.CREATE)) {
                    log.error(
                            "Create Object ID '{}' of '{}' SN Site ({}) was not processed. Because '{}' SN Site doesn't exist",
                            turSNJobItem.getAttributes() != null
                                    ? turSNJobItem.getAttributes().get(TurSNFieldName.ID)
                                    : null,
                            siteName, turSNJobItem.getLocale(), siteName);
                    TurLoggingUtils.setErrorStatus(turSNJobItem, TurIndexingStatus.INDEXED, "Site doesn't exist");
                } else if (turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.DELETE)) {
                    log.error(
                            "Delete Object ID '{}' of '{}' SN Site ({}) was not processed. Because '{}' SN Site doesn't exist",
                            turSNJobItem.getAttributes() != null ? turSNJobItem.getAttributes().get(TurSNFieldName.TYPE)
                                    : "empty",
                            siteName,
                            turSNJobItem.getLocale(), siteName);
                    TurLoggingUtils.setErrorStatus(turSNJobItem, TurIndexingStatus.DEINDEXED, "Site doesn't exist");
                }
            } else {
                log.warn("No JobItem' of '{}' SN Site", siteName);
            }
        });
    }

    private static boolean isValidJobItem(TurSNJobItem turJobItem) {
        return turJobItem != null && turJobItem.getAttributes() != null &&
                turJobItem.getAttributes().containsKey(TurSNFieldName.ID);
    }

    private static String actionType(TurSNJobItem turJobItem) {
        return switch (turJobItem.getTurSNJobAction()) {
            case CREATE -> "index";
            case DELETE -> "deIndex";
            case COMMIT -> "commit";
        };
    }
}
