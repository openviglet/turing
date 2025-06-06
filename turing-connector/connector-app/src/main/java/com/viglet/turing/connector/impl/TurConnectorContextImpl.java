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
import com.viglet.turing.connector.persistence.model.TurConnectorIndexing;
import com.viglet.turing.connector.persistence.model.TurConnectorStatus;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;
import static com.viglet.turing.connector.TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;

@Slf4j
@Component
public class TurConnectorContextImpl implements TurConnectorContext {
    private final TurSNJobItems turSNJobItems = new TurSNJobItems();
    private final Queue<TurSNJobItem> queueLinks = new LinkedList<>();
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;
    private final int jobSize;

    public TurConnectorContextImpl(@Value("${turing.connector.job.size:50}") int jobSize,
                                   JmsMessagingTemplate jmsMessagingTemplate,
                                   TurConnectorIndexingRepository turConnectorIndexingRepository) {
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.jobSize = jobSize;
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;

    }

    @Override
    public void addJobItem(TurSNJobItem turSNJobItem, TurConnectorSession source) {
        if (turSNJobItem != null) {
            log.info("Adding {} object to payload.", turSNJobItem.getId());
            queueLinks.offer(turSNJobItem);
            processRemainingJobs(source);
        }
    }

    @Override
    public void finishIndexing(TurConnectorSession source) {
        if (turSNJobItems.size() > 0) {
            log.info("Sending job to connector queue.");
            sendToMessageQueue();
            getInfoQueue();
        } else {
            log.info("No job to send to connector queue.");
        }
        deIndexObjects(source);
        queueLinks.clear();
    }

    private void processRemainingJobs(TurConnectorSession turConnectorSession) {
        while (!queueLinks.isEmpty()) {
            TurSNJobItem turSNJobItem = queueLinks.poll();
            if (objectNeedBeIndexed(turSNJobItem, turConnectorSession)) {
                createStatus(turSNJobItem, turConnectorSession);
                addJobToMessageQueue(turSNJobItem);
            } else {
                if (objectNeedBeReIndexed(turSNJobItem, turConnectorSession)) {
                    reindexLog(turSNJobItem, turConnectorSession);
                    addJobToMessageQueue(turSNJobItem);
                    modifyStatus(turSNJobItem, turConnectorSession, TurConnectorStatus.REINDEX);
                } else {
                    unchangedLog(turSNJobItem, turConnectorSession);
                    modifyStatus(turSNJobItem, turConnectorSession, TurConnectorStatus.UNCHANGED);
                }
            }
        }
    }

    private void addJobToMessageQueue(TurSNJobItem turSNJobItem) {
        turSNJobItems.add(turSNJobItem);
        sendToMessageQueueWhenMaxSize();
        getInfoQueue();
    }

    private void sendToMessageQueue() {
        if (turSNJobItems.getTuringDocuments().isEmpty()) {
            return;
        }
        if (log.isDebugEnabled()) {
            for (TurSNJobItem turSNJobItem : turSNJobItems) {
                log.debug("TurSNJobItem Id: {}", turSNJobItem.getAttributes().get(ID));
            }
        }
        this.jmsMessagingTemplate.convertAndSend(CONNECTOR_INDEXING_QUEUE, turSNJobItems);
        turSNJobItems.clear();
    }

    private void getInfoQueue() {
        log.info("Total Job Item: {}", Iterators.size(turSNJobItems.iterator()));
        log.info("Queue Size: {}", (long) queueLinks.size());
    }

    private void sendToMessageQueueWhenMaxSize() {
        if (turSNJobItems.size() >= jobSize) {
            sendToMessageQueue();
            turSNJobItems.clear();
        }
    }

    private void unchangedLog(TurSNJobItem turSNJobItem,
                              TurConnectorSession turConnectorSession) {
        getContentFromRepo(turSNJobItem, turConnectorSession)
                .ifPresent(turAemIndexingsList ->
                        log.info("Unchanged {} object ({}) and transactionId = {}",
                                turSNJobItem.getId(), turConnectorSession.getSystemId(),
                                turConnectorSession.getTransactionId()));
    }

    private void reindexLog(TurSNJobItem turSNJobItem,
                            TurConnectorSession turConnectorSession) {
        getContentFromRepo(turSNJobItem, turConnectorSession)
                .ifPresent(indexingList ->
                        log.info("ReIndexed {} object ({}) from {} to {} and transactionId = {}",
                                turSNJobItem.getId(), turConnectorSession.getSystemId(),
                                indexingList.getFirst().getChecksum(),
                                turSNJobItem.getChecksum(), turConnectorSession.getTransactionId()));
    }

    private Optional<List<TurConnectorIndexing>> getContentFromRepo(TurSNJobItem turSNJobItem,
                                                                    TurConnectorSession turConnectorSession) {
        return turConnectorIndexingRepository.findByObjectIdAndNameAndEnvironment(turSNJobItem.getId(),
                turConnectorSession.getSystemId(), turSNJobItem.getEnvironment());
    }

    private void deIndexObjects(TurConnectorSession turConnectorSession) {
        getContentsShouldBeDeIndexed(turConnectorSession)
                .ifPresent(contents -> {
                            contents.forEach(content ->
                                    addDeIndexItemToJob(turConnectorSession, content));
                            removeDeIndexItemsFromRepo(turConnectorSession);
                            sendToMessageQueue();
                        }
                );
    }

    private Optional<List<TurConnectorIndexing>> getContentsShouldBeDeIndexed(TurConnectorSession turConnectorSession) {
        return turConnectorIndexingRepository.findContentsShouldBeDeIndexed(turConnectorSession.getSystemId(),
                turConnectorSession.getTransactionId());
    }

    private void removeDeIndexItemsFromRepo(TurConnectorSession turConnectorSession) {
        turConnectorIndexingRepository.deleteContentsWereDeIndexed(turConnectorSession.getSystemId(),
                turConnectorSession.getTransactionId());
    }

    private void addDeIndexItemToJob(TurConnectorSession turConnectorSession,
                                     TurConnectorIndexing turConnectorIndexing) {
        log.info("DeIndex {} object from {} systemId and {} transactionId",
                turConnectorIndexing.getObjectId(), turConnectorSession.getSystemId(),
                turConnectorSession.getTransactionId());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(TurSNConstants.ID_ATTR, turConnectorIndexing.getObjectId());
        attributes.put(TurSNConstants.SOURCE_APPS_ATTR,
                turConnectorSession.getProviderName());
        addJobToMessageQueue(new TurSNJobItem(TurSNJobAction.DELETE,
                turConnectorIndexing.getSites(), turConnectorIndexing.getLocale(), attributes));
    }

    private void modifyStatus(TurSNJobItem turSNJobItem, TurConnectorSession turConnectorSession,
                              TurConnectorStatus status) {
        getContentFromRepo(turSNJobItem, turConnectorSession)
                .filter(turConnectorIndexingList -> !turConnectorIndexingList.isEmpty())
                .ifPresent(turConnectorIndexingList -> {
                    if (turConnectorIndexingList.size() > 1) {
                        recreateDuplicatedStatus(turSNJobItem, turConnectorSession);
                    } else {
                        updateStatus(turSNJobItem, turConnectorSession, turConnectorIndexingList, status);
                    }
                });
    }

    private void recreateDuplicatedStatus(TurSNJobItem turSNJobItem, TurConnectorSession source) {
        turConnectorIndexingRepository.deleteByObjectIdAndNameAndEnvironment(turSNJobItem.getId(),
                source.getSystemId(), turSNJobItem.getEnvironment());
        log.info("Removed duplicated status {} object ({})",
                turSNJobItem.getId(), source.getSystemId());
        turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, source,
                TurConnectorStatus.RECREATE));
        log.info("Recreated status {} object ({}) and transactionId() = {}",
                turSNJobItem.getId(), source.getSystemId(), source.getTransactionId());
    }

    private void updateStatus(TurSNJobItem turSNJobItem, TurConnectorSession turConnectorSession,
                              List<TurConnectorIndexing> turConnectorIndexingList, TurConnectorStatus status) {
        turConnectorIndexingRepository.save(updateTurConnectorIndexing(turConnectorIndexingList.getFirst(),
                turSNJobItem, turConnectorSession, status));
        log.info("Updated status {} object ({}) transactionId() = {}",
                turSNJobItem.getId(), turConnectorSession.getSystemId(), turConnectorSession.getTransactionId());
    }

    private void createStatus(TurSNJobItem turSNJobItem,
                              TurConnectorSession source) {
        turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, source,
                TurConnectorStatus.NEW));
        log.info("Created status {} object ({})", turSNJobItem.getId(), source.getSystemId());
    }

    private TurConnectorIndexing createTurConnectorIndexing(TurSNJobItem turSNJobItem,
                                                            TurConnectorSession turConnectorSession,
                                                            TurConnectorStatus status) {
        return TurConnectorIndexing.builder()
                .objectId(turSNJobItem.getId())
                .name(turConnectorSession.getSystemId())
                .transactionId(turConnectorSession.getTransactionId())
                .locale(turSNJobItem.getLocale())
                .checksum(turSNJobItem.getChecksum())
                .created(new Date())
                .sites(turSNJobItem.getSiteNames())
                .environment(turSNJobItem.getEnvironment())
                .status(status)
                .build();
    }

    private static TurConnectorIndexing updateTurConnectorIndexing(TurConnectorIndexing turConnectorIndexing,
                                                                   TurSNJobItem turSNJobItem,
                                                                   TurConnectorSession turConnectorSession,
                                                                   TurConnectorStatus status) {
        return turConnectorIndexing
                .setChecksum(turSNJobItem.getChecksum())
                .setTransactionId(turConnectorSession.getTransactionId())
                .setModificationDate(new Date())
                .setStatus(status)
                .setSites(turSNJobItem.getSiteNames());
    }

    private boolean objectNeedBeIndexed(TurSNJobItem turSNJobItem, TurConnectorSession turConnectorSession) {
        return (StringUtils.isNotEmpty(turSNJobItem.getId()) &&
                !turConnectorIndexingRepository.existsByObjectIdAndNameAndEnvironment(turSNJobItem.getId(),
                        turConnectorSession.getSystemId(), turSNJobItem.getEnvironment()));
    }

    private boolean objectNeedBeReIndexed(TurSNJobItem turSNJobItem, TurConnectorSession turConnectorSession) {
        return StringUtils.isNotEmpty(turSNJobItem.getId()) &&
                turConnectorIndexingRepository.existsByObjectIdAndNameAndEnvironmentAndChecksumNot(turSNJobItem.getId(),
                        turConnectorSession.getSystemId(), turSNJobItem.getEnvironment(), turSNJobItem.getChecksum());
    }
}