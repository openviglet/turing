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

package com.viglet.turing.connector.impl;

import com.google.common.collect.Iterators;
import com.viglet.turing.client.sn.TurSNConstants;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.connector.commons.plugin.domain.TurConnectorIndexing;
import com.viglet.turing.connector.service.TurConnectorIndexingRuleService;
import com.viglet.turing.connector.service.TurConnectorIndexingService;
import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.commons.plugin.TurConnectorContext;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRuleModel;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.service.TurConnectorSolrService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;
import static com.viglet.turing.connector.constant.TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
import static com.viglet.turing.connector.logging.TurConnectorLoggingUtils.setSuccessStatus;

@Slf4j
@Component
public class TurConnectorContextImpl implements TurConnectorContext {

    private final TurConnectorIndexingService indexingService;
    private final TurConnectorIndexingRuleService indexingRuleService;
    private final TurSNJobItems turSNJobItems = new TurSNJobItems();
    private final Queue<TurSNJobItem> queueLinks = new LinkedList<>();
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final TurConnectorSolrService turConnectorSolr;
    private final int jobSize;
    private final TurConnectorPlugin turConnectorPlugin;

    @Autowired
    public TurConnectorContextImpl(@Value("${turing.connector.job.size:50}") int jobSize,
                                   TurConnectorIndexingService turConnectorIndexingService,
                                   TurConnectorIndexingRuleService indexingRuleService,
                                   JmsMessagingTemplate jmsMessagingTemplate,
                                   TurConnectorSolrService turConnectorSolr,
                                   TurConnectorPlugin turConnectorPlugin) {
        this.indexingService = turConnectorIndexingService;
        this.indexingRuleService = indexingRuleService;
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.jobSize = jobSize;
        this.turConnectorSolr = turConnectorSolr;
        this.turConnectorPlugin = turConnectorPlugin;

    }

    @Override
    public void addJobItem(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone) {
        if (turSNJobItem != null) {
            log.debug("Adding {} object to payload.", turSNJobItem.getId());
            queueLinks.offer(turSNJobItem);
            processRemainingJobs(session, standalone);
        }
    }

    @Override
    public void finishIndexing(TurConnectorSession session, boolean standalone) {
        if (turSNJobItems.size() > 0) {
            log.debug("Sending job to connector queue.");
            sendToMessageQueue(session);
            getInfoQueue();
        } else {
            log.debug("No job to send to connector queue.");
        }
        if (!standalone) {
            validateAndReprocessObjects(session);
        }
        queueLinks.clear();
    }

    @Override
    public List<TurConnectorIndexing> getIndexingItem(String objectId, String source) {
        return indexingService.getIndexingItem(objectId, source);
    }

    private void processRemainingJobs(TurConnectorSession session, boolean standalone) {
        while (!queueLinks.isEmpty()) {
            TurSNJobItem turSNJobItem = queueLinks.poll();
            if (isJobItemToDeIndex(turSNJobItem)) {
                indexingService.delete(session, turSNJobItem);
                addJobToMessageQueue(turSNJobItem, session);
                continue;
            }
            if (indexingRuleIgnore(session, turSNJobItem)) {
                ignoreIndexingRulesStatus(turSNJobItem, session, standalone);
                createJobDeleteFromCreate(session, turSNJobItem)
                        .ifPresent(deIndexJobItem -> addJobToMessageQueue(deIndexJobItem, session));
                continue;
            }
            if (objectNeedBeIndexed(turSNJobItem, session)) {
                createStatus(turSNJobItem, session, standalone);
                addJobToMessageQueue(turSNJobItem, session);
            } else {
                if (objectNeedBeReIndexed(turSNJobItem, session)) {
                    reindexLog(turSNJobItem, session);
                    addJobToMessageQueue(turSNJobItem, session);
                    modifyStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_REINDEX, standalone);
                    setSuccessStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_REINDEX);
                } else {
                    unchangedLog(turSNJobItem, session);
                    modifyStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_UNCHANGED, standalone);
                    setSuccessStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_UNCHANGED);
                }
            }
        }
    }


    private Optional<TurSNJobItem> createJobDeleteFromCreate(TurConnectorSession session, TurSNJobItem jobItemCreate) {
        if (!jobItemCreate.getTurSNJobAction().equals(TurSNJobAction.CREATE))
            return Optional.empty();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(TurSNConstants.ID_ATTR, jobItemCreate.getId());
        attributes.put(TurSNConstants.SOURCE_APPS_ATTR, session.getProviderName());
        return Optional.of(new TurSNJobItem(TurSNJobAction.DELETE,
                jobItemCreate.getSiteNames(), jobItemCreate.getLocale(), attributes));
    }

    private boolean isJobItemToDeIndex(TurSNJobItem turSNJobItem) {
        return turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.DELETE);
    }

    private void ignoreIndexingRulesStatus(TurSNJobItem turSNJobItem,
                                           TurConnectorSession session,
                                           boolean standalone) {
        getContentFromRepo(turSNJobItem, session)
                .ifPresentOrElse(indexingList -> {
                    ignoreIndexingRulesLog(turSNJobItem, session);
                    indexingList.forEach(
                            indexing ->
                                    indexingService.update(turSNJobItem, session, standalone, indexing));
                }, () -> {
                    ignoreIndexingRulesLog(turSNJobItem, session);
                    indexingService.save(turSNJobItem, session, TurIndexingStatus.IGNORED, standalone);
                });
        setSuccessStatus(turSNJobItem, session, TurIndexingStatus.IGNORED);
    }


    private static void ignoreIndexingRulesLog(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        log.debug("{} object ({} - {} - {}) was ignored by Indexing Rules. transactionId = {}",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(),
                turSNJobItem.getEnvironment(), session.getTransactionId());
    }

    private boolean indexingRuleIgnore(TurConnectorSession turConnectorSession, TurSNJobItem turSNJobItem) {
        return turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.CREATE) &&
                indexingRuleService.getIndexingRules(turConnectorSession).stream()
                        .anyMatch(rule -> ignoredJobItem(turSNJobItem, rule));
    }


    private static boolean ignoredJobItem(TurSNJobItem turSNJobItem, TurConnectorIndexingRuleModel rule) {
        for (String ruleValue : rule.getValues()) {
            if (StringUtils.isNotBlank(ruleValue)) {
                if (!turSNJobItem.containsAttribute(rule.getAttribute())) {
                    return false;
                }
                if (Pattern
                        .compile(ruleValue)
                        .matcher(turSNJobItem.getStringAttribute(rule.getAttribute()))
                        .lookingAt()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addJobToMessageQueue(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        turSNJobItems.add(turSNJobItem);
        sendToMessageQueueWhenMaxSize(session);
        getInfoQueue();
    }

    private void sendToMessageQueue(TurConnectorSession session) {
        if (turSNJobItems.getTuringDocuments().isEmpty()) {
            return;
        }
        // To avoid concurrency in for statement, was created a copy of List
        List<TurSNJobItem> turSNJobItemsClone = new ArrayList<>(turSNJobItems.getTuringDocuments());
        if (log.isDebugEnabled()) {
            for (TurSNJobItem turSNJobItem : turSNJobItemsClone) {
                log.debug("TurSNJobItem Id: {}", turSNJobItem.getAttributes().get(ID));
            }
        }
        for (TurSNJobItem turSNJobItem : turSNJobItemsClone) {
            setSuccessStatus(turSNJobItem, session, TurIndexingStatus.SENT_TO_QUEUE);
        }
        this.jmsMessagingTemplate.convertAndSend(CONNECTOR_INDEXING_QUEUE, turSNJobItems);
        turSNJobItems.clear();
    }

    private void getInfoQueue() {
        log.debug("Total Job Item: {}", Iterators.size(turSNJobItems.iterator()));
        log.debug("Queue Size: {}", (long) queueLinks.size());
    }

    private void sendToMessageQueueWhenMaxSize(TurConnectorSession session) {
        if (turSNJobItems.size() >= jobSize) {
            sendToMessageQueue(session);
            turSNJobItems.clear();
        }
    }

    private void unchangedLog(TurSNJobItem turSNJobItem,
                              TurConnectorSession session) {
        getContentFromRepo(turSNJobItem, session)
                .ifPresent(turAemIndexingsList ->
                        log.debug("Unchanged {} object ({} - {} - {}) and transactionId = {}",
                                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(),
                                turSNJobItem.getEnvironment(), session.getTransactionId()));
    }

    private void reindexLog(TurSNJobItem turSNJobItem,
                            TurConnectorSession session) {
        getContentFromRepo(turSNJobItem, session)
                .ifPresent(indexingList -> log.debug(
                        "ReIndexed {} object ({} - {} - {}) from {} to {} and transactionId = {}",
                        turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(),
                        turSNJobItem.getEnvironment(),
                        indexingList.getFirst().getChecksum(),
                        turSNJobItem.getChecksum(),
                        session.getTransactionId()));
    }

    private Optional<List<TurConnectorIndexingModel>> getContentFromRepo(TurSNJobItem turSNJobItem,
                                                                         TurConnectorSession session) {
        return indexingService.getList(turSNJobItem, session);
    }


    private void validateAndReprocessObjects(TurConnectorSession session) {
        List<TurConnectorIndexingModel> deindexedItems = getContentsShouldBeDeIndexed(session);
        if (deindexedItems.isEmpty())
            return;
        turConnectorPlugin.sentToIndexByIdList(session.getSource(),
                deindexedItems.stream().map(TurConnectorIndexingModel::getObjectId).toList());
    }

    private List<TurConnectorIndexingModel> getContentsShouldBeDeIndexed(TurConnectorSession session) {
        return indexingService.getShouldBeDeIndexedList(session);
    }


    private void modifyStatus(TurSNJobItem turSNJobItem,
                              TurConnectorSession turConnectorSession,
                              TurIndexingStatus status,
                              boolean standalone) {
        getContentFromRepo(turSNJobItem, turConnectorSession)
                .filter(turConnectorIndexingList -> !turConnectorIndexingList.isEmpty())
                .ifPresent(turConnectorIndexingList -> {
                    if (turConnectorIndexingList.size() > 1) {
                        recreateDuplicatedStatus(turSNJobItem, turConnectorSession, standalone);
                    } else {
                        updateStatus(turSNJobItem, turConnectorSession, turConnectorIndexingList, status, standalone);
                    }
                });
    }

    private void recreateDuplicatedStatus(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone) {
        indexingService.delete(session, turSNJobItem);
        log.debug("Removed duplicated status {} object ({} - {} - {})",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(), turSNJobItem.getEnvironment());
        indexingService.save(turSNJobItem, session, TurIndexingStatus.PREPARE_FORCED_REINDEX, standalone);
        setSuccessStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_FORCED_REINDEX);
        log.debug("Recreated status {} object ({} - {} - {}) and transactionId() = {}",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(), turSNJobItem.getEnvironment(),
                session.getTransactionId());
    }


    private void updateStatus(TurSNJobItem turSNJobItem, TurConnectorSession session,
                              List<TurConnectorIndexingModel> turConnectorIndexingList,
                              TurIndexingStatus status,
                              boolean standalone) {
        indexingService.update(turSNJobItem, session, turConnectorIndexingList, status, standalone);
        log.debug("Updated status {} object ({} - {} - {}) transactionId() = {}",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(), turSNJobItem.getEnvironment(),
                session.getTransactionId());
    }


    private void createStatus(TurSNJobItem turSNJobItem,
                              TurConnectorSession session,
                              boolean standalone) {
        indexingService.save(turSNJobItem, session, TurIndexingStatus.PREPARE_INDEX, standalone);
        log.debug("Created status {} object ({} - {} - {})", turSNJobItem.getId(), session.getSource(),
                turSNJobItem.getLocale(), turSNJobItem.getEnvironment());
        setSuccessStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_INDEX);
    }


    private boolean objectNeedBeIndexed(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return (StringUtils.isNotEmpty(turSNJobItem.getId()) &&
                !indexingService.exists(turSNJobItem, session));
    }


    private boolean isChecksumDifferent(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return indexingService.isChecksumDifferent(turSNJobItem, session);
    }


    private boolean hasIndexingButNotSolr(TurSNJobItem turSNJobItem, TurConnectorSession session) {

        return indexingService.exists(turSNJobItem, session) &&
                !turConnectorSolr.hasContentIdAtSolr(turSNJobItem.getId(), session.getSource());
    }

    private boolean objectNeedBeReIndexed(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return isChecksumDifferent(turSNJobItem, session) ||
                hasIndexingButNotSolr(turSNJobItem, session);
    }
}