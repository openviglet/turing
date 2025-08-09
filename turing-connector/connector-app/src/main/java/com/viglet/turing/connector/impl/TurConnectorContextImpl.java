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
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.service.TurConnectorIndexingRuleService;
import com.viglet.turing.connector.service.TurConnectorIndexingService;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRuleModel;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

import static com.viglet.turing.client.sn.TurSNConstants.ID_ATTR;
import static com.viglet.turing.client.sn.TurSNConstants.SOURCE_APPS_ATTR;
import static com.viglet.turing.client.sn.job.TurSNJobAction.CREATE;
import static com.viglet.turing.client.sn.job.TurSNJobAction.DELETE;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.*;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;
import static com.viglet.turing.connector.constant.TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
import static com.viglet.turing.connector.commons.logging.TurConnectorLoggingUtils.setSuccessStatus;

@Slf4j
@Component
public class TurConnectorContextImpl implements TurConnectorContext {

    private final TurConnectorIndexingService indexingService;
    private final TurConnectorIndexingRuleService indexingRuleService;
    private final TurSNJobItems turSNJobItems = new TurSNJobItems();
    private final Queue<TurSNJobItem> queueLinks = new LinkedList<>();
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final int jobSize;

    @Autowired
    public TurConnectorContextImpl(@Value("${turing.connector.job.size:50}") int jobSize,
                                   TurConnectorIndexingService turConnectorIndexingService,
                                   TurConnectorIndexingRuleService indexingRuleService,
                                   JmsMessagingTemplate jmsMessagingTemplate) {
        this.indexingService = turConnectorIndexingService;
        this.indexingRuleService = indexingRuleService;
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.jobSize = jobSize;

    }

    @Override
    public void addJobItem(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone) {
        if (turSNJobItem != null) {
            log.info("Adding {} object to payload.", turSNJobItem.getId());
            queueLinks.offer(turSNJobItem);
            processRemainingJobs(session, standalone);
        }
    }

    @Override
    public void finishIndexing(TurConnectorSession session, boolean standalone) {
        if (turSNJobItems.size() > 0) {
            log.info("Sending job to connector queue.");
            sendToMessageQueue(session);
            getInfoQueue();
        } else {
            log.info("No job to send to connector queue.");
        }
        if (!standalone) {
            deIndexObjects(session);
        }
        queueLinks.clear();
    }

    @Override
    public List<TurConnectorIndexing> getIndexingItem(String objectId, String source, String provider) {
        return indexingService.getIndexingItem(objectId, source, provider);
    }

    private void processRemainingJobs(TurConnectorSession session, boolean standalone) {
        while (!queueLinks.isEmpty()) {
            TurSNJobItem turSNJobItem = queueLinks.poll();
            if (isJobItemToDeIndex(turSNJobItem)) {
                indexingService.delete(session, turSNJobItem);
                addJobToMessageQueue(turSNJobItem, session);
                setSuccessStatus(turSNJobItem, session, DEINDEXED);
                continue;
            }
            if (indexingRuleIgnore(session, turSNJobItem)) {
                ignoreIndexingRulesStatus(turSNJobItem, session, standalone);
                createJobDeleteFromCreate(session, turSNJobItem)
                        .ifPresent(deIndexJobItem -> {
                            addJobToMessageQueue(deIndexJobItem, session);
                            setSuccessStatus(turSNJobItem, session, DEINDEXED);
                        });
                continue;
            }
            if (objectNeedBeIndexed(turSNJobItem, session)) {
                createIndexing(turSNJobItem, session, standalone);
                addJobToMessageQueue(turSNJobItem, session);
            } else {
                if (objectNeedBeReIndexed(turSNJobItem, session)) {
                    reindexLog(turSNJobItem, session);
                    addJobToMessageQueue(turSNJobItem, session);
                    modifyIndexing(turSNJobItem, session, PREPARE_REINDEX, standalone);
                    setSuccessStatus(turSNJobItem, session, PREPARE_REINDEX);
                } else {
                    unchangedLog(turSNJobItem, session);
                    modifyIndexing(turSNJobItem, session, PREPARE_UNCHANGED, standalone);
                    setSuccessStatus(turSNJobItem, session, PREPARE_UNCHANGED);
                }
            }
        }
    }


    private Optional<TurSNJobItem> createJobDeleteFromCreate(TurConnectorSession session, TurSNJobItem jobItemCreate) {
        if (!jobItemCreate.getTurSNJobAction().equals(CREATE))
            return Optional.empty();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ID_ATTR, jobItemCreate.getId());
        attributes.put(SOURCE_APPS_ATTR, session.getProviderName());
        return Optional.of(new TurSNJobItem(DELETE, jobItemCreate.getSiteNames(), jobItemCreate.getLocale(),
                attributes));
    }

    private boolean isJobItemToDeIndex(TurSNJobItem turSNJobItem) {
        return turSNJobItem.getTurSNJobAction().equals(DELETE);
    }

    private void ignoreIndexingRulesStatus(TurSNJobItem turSNJobItem,
                                           TurConnectorSession session,
                                           boolean standalone) {
        if (indexingService.exists(turSNJobItem, session)) {
            ignoreIndexingRulesLog(turSNJobItem, session);
            indexingService.getList(turSNJobItem, session).forEach(
                    indexing ->
                            indexingService.update(turSNJobItem, session, standalone, indexing));
        } else {
            ignoreIndexingRulesLog(turSNJobItem, session);
            indexingService.save(turSNJobItem, session, IGNORED, standalone);
        }
        setSuccessStatus(turSNJobItem, session, IGNORED);
    }


    private void ignoreIndexingRulesLog(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        log.info("{} was ignored by Indexing Rules.", getObjectDetailForLogs(turSNJobItem, session));
    }

    private boolean indexingRuleIgnore(TurConnectorSession turConnectorSession, TurSNJobItem turSNJobItem) {
        return turSNJobItem.getTurSNJobAction().equals(CREATE) &&
                indexingRuleService.getIndexingRules(turConnectorSession).stream()
                        .anyMatch(rule -> ignoredJobItem(turSNJobItem, rule));
    }

    private boolean ignoredJobItem(TurSNJobItem turSNJobItem, TurConnectorIndexingRuleModel rule) {
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
            setSuccessStatus(turSNJobItem, session, SENT_TO_QUEUE);
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
        if (!indexingService.exists(turSNJobItem, session)) return;
        log.info("Unchanged {}", getObjectDetailForLogs(turSNJobItem, session));
    }

    private void reindexLog(TurSNJobItem turSNJobItem,
                            TurConnectorSession session) {
        indexingService.getList(turSNJobItem, session).forEach(indexing ->
                log.info("ReIndexed {} from {} to {}",
                        getObjectDetailForLogs(turSNJobItem, session),
                        indexing.getChecksum(),
                        turSNJobItem.getChecksum()));
    }

    private void deIndexObjects(TurConnectorSession session) {
        List<TurConnectorIndexingModel> deindexedItems = indexingService.getShouldBeDeIndexedList(session);
        if (deindexedItems.isEmpty())
            return;
        deindexedItems.forEach(deIndexedItem -> createJobDeleteFromCreate(session, deIndexedItem));
        indexingService.deleteContentsToBeDeIndexed(session);
        sendToMessageQueue(session);
    }

    private void createJobDeleteFromCreate(TurConnectorSession session,
                                           TurConnectorIndexingModel turConnectorIndexing) {
        log.info("DeIndex {} object ({} - {} - {}: {})",
                turConnectorIndexing.getObjectId(),
                turConnectorIndexing.getSource(),
                turConnectorIndexing.getEnvironment(),
                turConnectorIndexing.getLocale(),
                session.getTransactionId());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ID_ATTR, turConnectorIndexing.getObjectId());
        attributes.put(SOURCE_APPS_ATTR,
                session.getProviderName());
        addJobToMessageQueue(new TurSNJobItem(DELETE,
                turConnectorIndexing.getSites(), turConnectorIndexing.getLocale(), attributes), session);
    }

    private void modifyIndexing(TurSNJobItem turSNJobItem,
                                TurConnectorSession turConnectorSession,
                                TurIndexingStatus status,
                                boolean standalone) {
        List<TurConnectorIndexingModel> indexingModelList = indexingService.getList(turSNJobItem, turConnectorSession);
        if (indexingModelList.size() > 1) {
            recreateDuplicatedIndexing(turSNJobItem, turConnectorSession, standalone);
        } else {
            updateIndexing(turSNJobItem, turConnectorSession, indexingModelList, status, standalone);
        }
    }

    private void recreateDuplicatedIndexing(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone) {
        indexingService.delete(session, turSNJobItem);
        log.info("Removed duplicated status {}", getObjectDetailForLogs(turSNJobItem, session));
        indexingService.save(turSNJobItem, session, PREPARE_FORCED_REINDEX, standalone);
        setSuccessStatus(turSNJobItem, session, PREPARE_FORCED_REINDEX);
        log.info("Recreated status {}", getObjectDetailForLogs(turSNJobItem, session));
    }

    private String getObjectDetailForLogs(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return "%s object (%s - %s - %s: %s)".formatted(
                turSNJobItem.getId(),
                session.getSource(),
                turSNJobItem.getEnvironment(),
                turSNJobItem.getLocale(),
                session.getTransactionId());
    }

    private void updateIndexing(TurSNJobItem turSNJobItem, TurConnectorSession session,
                                List<TurConnectorIndexingModel> turConnectorIndexingList,
                                TurIndexingStatus status,
                                boolean standalone) {
        indexingService.update(turSNJobItem, session, turConnectorIndexingList, status, standalone);
        log.info("Updated status {}", getObjectDetailForLogs(turSNJobItem, session));
    }


    private void createIndexing(TurSNJobItem turSNJobItem,
                                TurConnectorSession session,
                                boolean standalone) {
        indexingService.save(turSNJobItem, session,PREPARE_INDEX, standalone);
        log.info("Created status {}", getObjectDetailForLogs(turSNJobItem, session));
        setSuccessStatus(turSNJobItem, session, PREPARE_INDEX);
    }


    private boolean objectNeedBeIndexed(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return (StringUtils.isNotEmpty(turSNJobItem.getId()) &&
                !indexingService.exists(turSNJobItem, session));
    }

    private boolean objectNeedBeReIndexed(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return indexingService.isChecksumDifferent(turSNJobItem, session) || hasIgnoredStatus(turSNJobItem, session);
    }

    private boolean hasIgnoredStatus(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return indexingService.getList(turSNJobItem, session)
                .stream()
                .anyMatch(indexing -> IGNORED.equals(indexing.getStatus()));
    }
}