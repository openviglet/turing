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
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.commons.plugin.TurConnectorContext;
import com.viglet.turing.connector.commons.plugin.dto.TurConnectorIndexingDTO;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexing;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRule;
import com.viglet.turing.connector.commons.plugin.TurConnectorIndexingRuleType;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;
import static com.viglet.turing.connector.TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
import static com.viglet.turing.connector.logging.TurConnectorLoggingUtils.setSuccessStatus;

@Slf4j
@Component
public class TurConnectorContextImpl implements TurConnectorContext {

    private final TurSNJobItems turSNJobItems = new TurSNJobItems();
    private final Queue<TurSNJobItem> queueLinks = new LinkedList<>();
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;
    private final TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository;
    private final int jobSize;

    @Autowired
    public TurConnectorContextImpl(@Value("${turing.connector.job.size:50}") int jobSize,
                                   JmsMessagingTemplate jmsMessagingTemplate,
                                   TurConnectorIndexingRepository turConnectorIndexingRepository,
                                   TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository) {
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.jobSize = jobSize;
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
        this.turConnectorIndexingRuleRepository = turConnectorIndexingRuleRepository;
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
            deIndexObjects(session);
        }
        queueLinks.clear();
    }

    private void processRemainingJobs(TurConnectorSession session, boolean standalone) {
        while (!queueLinks.isEmpty()) {
            TurSNJobItem turSNJobItem = queueLinks.poll();
            if (isJobItemToDeIndex(turSNJobItem)) {
                turConnectorIndexingRepository.deleteBySourceAndObjectId(session.getSource(), turSNJobItem.getId());
                addJobToMessageQueue(turSNJobItem, session);
                continue;
            }
            if (indexingRuleIgnore(session, turSNJobItem)) {
                ignoreIndexingRulesStatus(turSNJobItem, session, standalone);
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

    private boolean isJobItemToDeIndex(TurSNJobItem turSNJobItem) {
        return turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.DELETE);
    }

    private void ignoreIndexingRulesStatus(TurSNJobItem turSNJobItem,
                                           TurConnectorSession session,
                                           boolean standalone) {
        getContentFromRepo(turSNJobItem, session)
                .ifPresentOrElse(indexingList -> {
                    ignoreIndexingRulesLog(turSNJobItem, session);
                    indexingList.forEach(indexing ->
                            turConnectorIndexingRepository.save(updateTurConnectorIndexing(indexing,
                                    turSNJobItem, session, TurIndexingStatus.IGNORED, standalone)));
                }, () -> {
                    ignoreIndexingRulesLog(turSNJobItem, session);
                    turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, session,
                            TurIndexingStatus.IGNORED, standalone));
                });
        setSuccessStatus(turSNJobItem, session, TurIndexingStatus.IGNORED);
    }

    private static void ignoreIndexingRulesLog(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        log.debug("{} object ({} - {} - {}) was ignored by Indexing Rules. transactionId = {}",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(),
                turSNJobItem.getEnvironment(), session.getTransactionId());
    }

    private boolean indexingRuleIgnore(TurConnectorSession turConnectorSession, TurSNJobItem turSNJobItem) {
        return getIndexingRules(turConnectorSession).stream()
                .anyMatch(rule -> ignoredJobItem(turSNJobItem, rule));
    }

    private Set<TurConnectorIndexingRule> getIndexingRules(TurConnectorSession turConnectorSession) {
        return turConnectorIndexingRuleRepository
                .findBySourceAndRuleType(turConnectorSession.getSource(), TurConnectorIndexingRuleType.IGNORE);
    }

    private static boolean ignoredJobItem(TurSNJobItem turSNJobItem, TurConnectorIndexingRule rule) {
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
        if (log.isDebugEnabled()) {
            for (TurSNJobItem turSNJobItem : turSNJobItems) {
                log.debug("TurSNJobItem Id: {}", turSNJobItem.getAttributes().get(ID));
            }
        }
        for (TurSNJobItem turSNJobItem : turSNJobItems) {
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
                .ifPresent(indexingList ->
                        log.debug("ReIndexed {} object ({} - {} - {}) from {} to {} and transactionId = {}",
                                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(),
                                turSNJobItem.getEnvironment(),
                                indexingList.getFirst().getChecksum(),
                                turSNJobItem.getChecksum(),
                                session.getTransactionId()));
    }

    private Optional<List<TurConnectorIndexing>> getContentFromRepo(TurSNJobItem turSNJobItem,
                                                                    TurConnectorSession session) {
        return turConnectorIndexingRepository.findByObjectIdAndSourceAndEnvironment(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment());
    }

    private void deIndexObjects(TurConnectorSession session) {
        List<TurConnectorIndexing> deindexedItems = getContentsShouldBeDeIndexed(session);
        if (deindexedItems.isEmpty()) return;
        deindexedItems.forEach(deindexedItem -> addDeIndexItemToJob(session, deindexedItem));
        removeDeIndexItemsFromRepo(session);
        sendToMessageQueue(session);
    }

    private List<TurConnectorIndexing> getContentsShouldBeDeIndexed(TurConnectorSession turConnectorSession) {
        return turConnectorIndexingRepository.findContentsShouldBeDeIndexed(turConnectorSession.getSource(),
                turConnectorSession.getTransactionId());
    }

    private void removeDeIndexItemsFromRepo(TurConnectorSession turConnectorSession) {
        turConnectorIndexingRepository.deleteContentsWereDeIndexed(turConnectorSession.getSource(),
                turConnectorSession.getTransactionId());
    }

    public List<TurConnectorIndexingDTO> getIndexingItem(String objectId, String source) {
        List<TurConnectorIndexingDTO> dtoList = new ArrayList<>();
        turConnectorIndexingRepository.findByObjectIdAndSource(objectId, source)
                .ifPresent(indexingList ->
                        indexingList.stream()
                                .map(indexing -> TurConnectorIndexingDTO.builder()
                                        .checksum(indexing.getChecksum())
                                        .created(indexing.getCreated())
                                        .environment(indexing.getEnvironment())
                                        .id(indexing.getId())
                                        .locale(indexing.getLocale())
                                        .modificationDate(indexing.getModificationDate())
                                        .source(indexing.getSource())
                                        .objectId(indexing.getObjectId())
                                        .sites(indexing.getSites())
                                        .status(indexing.getStatus())
                                        .transactionId(indexing.getTransactionId())
                                        .build()).forEach(dtoList::add));
        return dtoList;
    }

    private void addDeIndexItemToJob(TurConnectorSession session,
                                     TurConnectorIndexing turConnectorIndexing) {
        log.debug("DeIndex {} object ({} - {} - {}) systemId and {} transactionId",
                turConnectorIndexing.getObjectId(), turConnectorIndexing.getSource(), turConnectorIndexing.getLocale(),
                turConnectorIndexing.getEnvironment(), session.getTransactionId());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(TurSNConstants.ID_ATTR, turConnectorIndexing.getObjectId());
        attributes.put(TurSNConstants.SOURCE_APPS_ATTR,
                session.getProviderName());
        addJobToMessageQueue(new TurSNJobItem(TurSNJobAction.DELETE,
                turConnectorIndexing.getSites(), turConnectorIndexing.getLocale(), attributes), session);
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
        turConnectorIndexingRepository.deleteByObjectIdAndSourceAndEnvironment(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment());
        log.debug("Removed duplicated status {} object ({} - {} - {})",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(), turSNJobItem.getEnvironment());
        turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, session,
                TurIndexingStatus.PREPARE_FORCED_REINDEX, standalone));
        setSuccessStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_FORCED_REINDEX);
        log.debug("Recreated status {} object ({} - {} - {}) and transactionId() = {}",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(), turSNJobItem.getEnvironment(),
                session.getTransactionId());
    }

    private void updateStatus(TurSNJobItem turSNJobItem, TurConnectorSession session,
                              List<TurConnectorIndexing> turConnectorIndexingList,
                              TurIndexingStatus status,
                              boolean standalone) {
        turConnectorIndexingRepository.save(updateTurConnectorIndexing(turConnectorIndexingList.getFirst(),
                turSNJobItem, session, status, standalone));
        log.debug("Updated status {} object ({} - {} - {}) transactionId() = {}",
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getLocale(), turSNJobItem.getEnvironment(),
                session.getTransactionId());
    }

    private void createStatus(TurSNJobItem turSNJobItem,
                              TurConnectorSession session,
                              boolean standalone) {
        turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, session,
                TurIndexingStatus.PREPARE_INDEX, standalone));
        log.debug("Created status {} object ({} - {} - {})", turSNJobItem.getId(), session.getSource(),
                turSNJobItem.getLocale(), turSNJobItem.getEnvironment());
        setSuccessStatus(turSNJobItem, session, TurIndexingStatus.PREPARE_INDEX);
    }

    private TurConnectorIndexing createTurConnectorIndexing(TurSNJobItem turSNJobItem,
                                                            TurConnectorSession turConnectorSession,
                                                            TurIndexingStatus status,
                                                            boolean standalone) {
        return TurConnectorIndexing.builder()
                .objectId(turSNJobItem.getId())
                .source(turConnectorSession.getSource())
                .transactionId(turConnectorSession.getTransactionId())
                .locale(turSNJobItem.getLocale())
                .checksum(turSNJobItem.getChecksum())
                .created(new Date())
                .modificationDate(new Date())
                .sites(turSNJobItem.getSiteNames())
                .environment(turSNJobItem.getEnvironment())
                .status(status)
                .standalone(standalone)
                .build();
    }

    private static TurConnectorIndexing updateTurConnectorIndexing(TurConnectorIndexing turConnectorIndexing,
                                                                   TurSNJobItem turSNJobItem,
                                                                   TurConnectorSession turConnectorSession,
                                                                   TurIndexingStatus status,
                                                                   boolean standalone) {
        return turConnectorIndexing
                .setChecksum(turSNJobItem.getChecksum())
                .setTransactionId(turConnectorSession.getTransactionId())
                .setModificationDate(new Date())
                .setStatus(status)
                .setStandalone(standalone)
                .setSites(turSNJobItem.getSiteNames());
    }

    private boolean objectNeedBeIndexed(TurSNJobItem turSNJobItem, TurConnectorSession turConnectorSession) {
        return (StringUtils.isNotEmpty(turSNJobItem.getId()) &&
                !turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironment(turSNJobItem.getId(),
                        turConnectorSession.getSource(), turSNJobItem.getEnvironment()));
    }

    private boolean objectNeedBeReIndexed(TurSNJobItem turSNJobItem, TurConnectorSession turConnectorSession) {
        return StringUtils.isNotEmpty(turSNJobItem.getId()) &&
                turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(turSNJobItem.getId(),
                        turConnectorSession.getSource(), turSNJobItem.getEnvironment(), turSNJobItem.getChecksum());
    }
}