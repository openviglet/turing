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

package com.viglet.turing.connector.impl;

import static com.viglet.turing.client.sn.TurSNConstants.ID_ATTR;
import static com.viglet.turing.client.sn.TurSNConstants.SOURCE_APPS_ATTR;
import static com.viglet.turing.client.sn.job.TurSNJobAction.CREATE;
import static com.viglet.turing.client.sn.job.TurSNJobAction.DELETE;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.DEINDEXED;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.IGNORED;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.PREPARE_FORCED_REINDEX;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.PREPARE_INDEX;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.PREPARE_REINDEX;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.PREPARE_UNCHANGED;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.SENT_TO_QUEUE;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;
import static com.viglet.turing.connector.commons.logging.TurConnectorLoggingUtils.setSuccessStatus;
import static com.viglet.turing.connector.constant.TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;
import com.google.common.collect.Iterators;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRuleModel;
import com.viglet.turing.connector.service.TurConnectorIndexingRuleService;
import com.viglet.turing.connector.service.TurConnectorIndexingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurConnectorContextImpl implements TurConnectorContext {

    private final TurConnectorIndexingService indexingService;
    private final TurConnectorIndexingRuleService indexingRuleService;
    private final TurSNJobItems turSNJobItems = new TurSNJobItems();
    private final Queue<TurJobItemWithSession> queueLinks = new LinkedList<>();
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final int jobSize;

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
    public List<String> getObjectIdByDependency(String source, String provider,
            List<String> dependenciesObjectIdList) {
        if (dependenciesObjectIdList == null || dependenciesObjectIdList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> dependencies =
                indexingService.findByDependencies(source, provider, dependenciesObjectIdList)
                        .stream().distinct().toList();
        if (!dependencies.isEmpty()) {
            log.info("Found dependencies for {} - {} - {}", source, provider,
                    dependenciesObjectIdList);
            dependencies.forEach(dependency -> log.info("Dependent object: {}", dependency));
        }
        return indexingService.findByDependencies(source, provider, dependenciesObjectIdList)
                .stream().distinct().toList();
    }

    @Override
    public void addJobItem(TurJobItemWithSession turJobItemWithSession) {
        if (turJobItemWithSession.turSNJobItem() != null) {
            log.info("Adding {} object to payload.", turJobItemWithSession.turSNJobItem().getId());
            queueLinks.offer(turJobItemWithSession);
            processRemainingJobs();
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
    public List<TurConnectorIndexing> getIndexingItem(String objectId, String source,
            String provider) {
        return indexingService.getIndexingItem(objectId, source, provider);
    }

    private void processRemainingJobs() {
        while (!queueLinks.isEmpty()) {
            TurJobItemWithSession turSNJobItemWithSession = queueLinks.poll();
            if (turSNJobItemWithSession.standalone()) {
                if (objectNeedBeIndexed(turSNJobItemWithSession)) {
                    indexProcess(turSNJobItemWithSession);
                } else {
                    reIndexProcess(turSNJobItemWithSession);
                }
                continue;
            }
            if (isJobItemToDeIndex(turSNJobItemWithSession)) {
                deIndexProcess(turSNJobItemWithSession);
                continue;
            }
            if (indexingRuleIgnore(turSNJobItemWithSession)) {
                indexingRuleIgnoreProcess(turSNJobItemWithSession);
                continue;
            }
            if (objectNeedBeIndexed(turSNJobItemWithSession)) {
                indexProcess(turSNJobItemWithSession);
            } else {
                if (objectNeedBeReIndexed(turSNJobItemWithSession)) {
                    reIndexProcess(turSNJobItemWithSession);
                } else {
                    unchangeProcess(turSNJobItemWithSession);
                }
            }
        }
    }

    private void indexingRuleIgnoreProcess(TurJobItemWithSession turSNJobItemWithSession) {
        ignoreIndexingRulesStatus(turSNJobItemWithSession);
        createJobDeleteFromCreate(turSNJobItemWithSession).ifPresent(deIndexJobItem -> {
            TurJobItemWithSession turSNJobItemWithSessionDeIndex =
                    new TurJobItemWithSession(deIndexJobItem, turSNJobItemWithSession.session(),
                            Collections.emptySet(), turSNJobItemWithSession.standalone());
            addJobToMessageQueue(turSNJobItemWithSessionDeIndex);
            setSuccessStatus(turSNJobItemWithSession.turSNJobItem(), DEINDEXED);
        });
    }

    private void unchangeProcess(TurJobItemWithSession turSNJobItemWithSession) {
        unchangedLog(turSNJobItemWithSession);
        modifyIndexing(turSNJobItemWithSession, PREPARE_UNCHANGED);
        setSuccessStatus(turSNJobItemWithSession.turSNJobItem(), PREPARE_UNCHANGED);
    }

    private void indexProcess(TurJobItemWithSession turSNJobItemWithSession) {
        createIndexing(turSNJobItemWithSession);
        addJobToMessageQueue(turSNJobItemWithSession);
    }

    private void deIndexProcess(TurJobItemWithSession turSNJobItemWithSession) {
        indexingService.delete(turSNJobItemWithSession);
        addJobToMessageQueue(turSNJobItemWithSession);
        setSuccessStatus(turSNJobItemWithSession, DEINDEXED);
    }

    private void reIndexProcess(TurJobItemWithSession turSNJobItemWithSession) {
        reindexLog(turSNJobItemWithSession);
        addJobToMessageQueue(turSNJobItemWithSession);
        modifyIndexing(turSNJobItemWithSession, PREPARE_REINDEX);
        setSuccessStatus(turSNJobItemWithSession, PREPARE_REINDEX);
    }


    private Optional<TurSNJobItem> createJobDeleteFromCreate(
            TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem jobItemCreate = turSNJobItemWithSession.turSNJobItem();
        if (!jobItemCreate.getTurSNJobAction().equals(CREATE))
            return Optional.empty();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ID_ATTR, jobItemCreate.getId());
        attributes.put(SOURCE_APPS_ATTR, turSNJobItemWithSession.session().getProviderName());
        return Optional.of(new TurSNJobItem(DELETE, jobItemCreate.getSiteNames(),
                jobItemCreate.getLocale(), attributes));
    }

    private boolean isJobItemToDeIndex(TurJobItemWithSession turSNJobItemWithSession) {
        return turSNJobItemWithSession.turSNJobItem().getTurSNJobAction().equals(DELETE);
    }

    private void ignoreIndexingRulesStatus(TurJobItemWithSession turSNJobItemWithSession) {
        if (indexingService.exists(turSNJobItemWithSession)) {
            ignoreIndexingRulesLog(turSNJobItemWithSession);
            indexingService.getList(turSNJobItemWithSession)
                    .forEach(indexing -> indexingService.update(turSNJobItemWithSession, indexing));
        } else {
            ignoreIndexingRulesLog(turSNJobItemWithSession);
            indexingService.save(turSNJobItemWithSession, IGNORED);
        }
        setSuccessStatus(turSNJobItemWithSession, IGNORED);
    }


    private void ignoreIndexingRulesLog(TurJobItemWithSession turSNJobItemWithSession) {
        log.info("{} was ignored by Indexing Rules.",
                getObjectDetailForLogs(turSNJobItemWithSession));
    }

    private boolean indexingRuleIgnore(TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        return turSNJobItem.getTurSNJobAction().equals(CREATE)
                && indexingRuleService.getIndexingRules(turSNJobItemWithSession.session()).stream()
                        .anyMatch(rule -> ignoredJobItem(turSNJobItem, rule));
    }

    private boolean ignoredJobItem(TurSNJobItem turSNJobItem, TurConnectorIndexingRuleModel rule) {
        for (String ruleValue : rule.getValues()) {
            if (StringUtils.isNotBlank(ruleValue)) {
                if (!turSNJobItem.containsAttribute(rule.getAttribute())) {
                    return false;
                }
                if (Pattern.compile(ruleValue)
                        .matcher(turSNJobItem.getStringAttribute(rule.getAttribute()))
                        .lookingAt()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addJobToMessageQueue(TurJobItemWithSession turSNJobItemWithSession) {
        synchronized (turSNJobItems) {
            turSNJobItems.add(turSNJobItemWithSession.turSNJobItem());
        }
        sendToMessageQueueWhenMaxSize(turSNJobItemWithSession.session());
        getInfoQueue();
    }

    private void sendToMessageQueue(TurConnectorSession session) {
        synchronized (turSNJobItems) {
            if (turSNJobItems.getTuringDocuments().isEmpty()) {
                return;
            }
            if (log.isDebugEnabled()) {
                for (TurSNJobItem turSNJobItem : turSNJobItems) {
                    log.debug("TurSNJobItem Id: {}", turSNJobItem.getAttributes().get(ID));
                }
            }
            for (TurSNJobItem turSNJobItem : turSNJobItems) {
                setSuccessStatus(turSNJobItem, session, SENT_TO_QUEUE);
            }
            this.jmsMessagingTemplate.convertAndSend(CONNECTOR_INDEXING_QUEUE, turSNJobItems);
            turSNJobItems.clear();
        }

    }

    private void getInfoQueue() {
        log.debug("Total Job Item: {}", Iterators.size(turSNJobItems.iterator()));
        log.debug("Queue Size: {}", (long) queueLinks.size());
    }

    private void sendToMessageQueueWhenMaxSize(TurConnectorSession session) {
        if (turSNJobItems.size() >= jobSize) {
            sendToMessageQueue(session);
        }
    }

    private void unchangedLog(TurJobItemWithSession turSNJobItemWithSession) {
        if (!indexingService.exists(turSNJobItemWithSession))
            return;
        log.info("Unchanged {}", getObjectDetailForLogs(turSNJobItemWithSession));
    }

    private void reindexLog(TurJobItemWithSession turSNJobItemWithSession) {
        indexingService.getList(turSNJobItemWithSession)
                .forEach(indexing -> log.info("ReIndexed {} from {} to {}",
                        getObjectDetailForLogs(turSNJobItemWithSession), indexing.getChecksum(),
                        turSNJobItemWithSession.turSNJobItem().getChecksum()));
    }

    private void deIndexObjects(TurConnectorSession session) {
        List<TurConnectorIndexingModel> deindexedItems =
                indexingService.getShouldBeDeIndexedList(session);
        if (deindexedItems.isEmpty())
            return;
        deindexedItems.forEach(deIndexedItem -> createJobDeleteFromCreate(session, deIndexedItem));
        indexingService.deleteContentsToBeDeIndexed(session);
        sendToMessageQueue(session);
    }

    private void createJobDeleteFromCreate(TurConnectorSession session,
            TurConnectorIndexingModel turConnectorIndexing) {
        log.info("DeIndex {} object ({} - {} - {}: {})", turConnectorIndexing.getObjectId(),
                turConnectorIndexing.getSource(), turConnectorIndexing.getEnvironment(),
                turConnectorIndexing.getLocale(), session.getTransactionId());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ID_ATTR, turConnectorIndexing.getObjectId());
        attributes.put(SOURCE_APPS_ATTR, session.getProviderName());
        addJobToMessageQueue(new TurJobItemWithSession(
                new TurSNJobItem(DELETE, turConnectorIndexing.getSites(),
                        turConnectorIndexing.getLocale(), attributes),
                session, Collections.emptySet(), false));
    }

    private void modifyIndexing(TurJobItemWithSession turSNJobItemWithSession,
            TurIndexingStatus status) {
        List<TurConnectorIndexingModel> indexingModelList =
                indexingService.getList(turSNJobItemWithSession);
        if (indexingModelList.size() > 1) {
            recreateDuplicatedIndexing(turSNJobItemWithSession);
        } else {
            updateIndexing(turSNJobItemWithSession, indexingModelList, status);
        }
    }

    private void recreateDuplicatedIndexing(TurJobItemWithSession turSNJobItemWithSession) {
        indexingService.delete(turSNJobItemWithSession);
        log.info("Removed duplicated status {}", getObjectDetailForLogs(turSNJobItemWithSession));
        indexingService.save(turSNJobItemWithSession, PREPARE_FORCED_REINDEX);
        setSuccessStatus(turSNJobItemWithSession, PREPARE_FORCED_REINDEX);
        log.info("Recreated status {}", getObjectDetailForLogs(turSNJobItemWithSession));
    }

    private String getObjectDetailForLogs(TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        TurConnectorSession session = turSNJobItemWithSession.session();
        return "%s object (%s - %s - %s: %s)".formatted(turSNJobItem.getId(), session.getSource(),
                turSNJobItem.getEnvironment(), turSNJobItem.getLocale(),
                session.getTransactionId());
    }

    private void updateIndexing(TurJobItemWithSession turSNJobItemWithSession,
            List<TurConnectorIndexingModel> turConnectorIndexingList, TurIndexingStatus status) {
        indexingService.update(turSNJobItemWithSession, turConnectorIndexingList, status);
        log.info("Updated status {}", getObjectDetailForLogs(turSNJobItemWithSession));
    }


    private void createIndexing(TurJobItemWithSession turSNJobItemWithSession) {
        indexingService.save(turSNJobItemWithSession, PREPARE_INDEX);
        log.info("Created status {}", getObjectDetailForLogs(turSNJobItemWithSession));
        setSuccessStatus(turSNJobItemWithSession, PREPARE_INDEX);
    }


    private boolean objectNeedBeIndexed(TurJobItemWithSession turSNJobItemWithSession) {
        return (StringUtils.isNotEmpty(turSNJobItemWithSession.turSNJobItem().getId())
                && !indexingService.exists(turSNJobItemWithSession));
    }

    private boolean objectNeedBeReIndexed(TurJobItemWithSession turSNJobItemWithSession) {
        return indexingService.isChecksumDifferent(turSNJobItemWithSession)
                || hasIgnoredStatus(turSNJobItemWithSession);
    }

    private boolean hasIgnoredStatus(TurJobItemWithSession turSNJobItemWithSession) {
        return indexingService.getList(turSNJobItemWithSession).stream()
                .anyMatch(indexing -> IGNORED.equals(indexing.getStatus()));
    }
}
