/*
 * Copyright (C) 2016-2022 the original author or authors.
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

package com.viglet.turing.api.sn.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.viglet.turing.api.ocr.TurTikaFileAttributes;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.genai.TurSNGenAi;
import com.viglet.turing.logging.TurLoggingUtils;
import com.viglet.turing.domain.sn.TurSNSiteRepositoryPort;
import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.sn.TurSNConstants;
import com.viglet.turing.sn.TurSNPartitionWriteGuard;
import com.viglet.turing.spring.security.auth.TurApiKeyPartitionContext;
import com.viglet.turing.spring.utils.TurSpringUtils;
import com.viglet.turing.utils.TurFileUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RestController
@RequestMapping("/api/sn/import")
@Tag(name = "Semantic Navigation Import", description = "Semantic Navigation Import API")
public class TurSNImportAPI {
    private final JmsMessagingTemplate jmsMessagingTemplate;
    private final TurSNSiteRepositoryPort turSNSiteRepositoryPort;
    private final TurSNGenAi turGenAi;
    private final com.viglet.turing.tenant.TurJmsTenantPropagation turJmsTenantPropagation;
    private final TurSNBulkImportService turSNBulkImportService;
    private final TurSNPartitionWriteGuard turSNPartitionWriteGuard;
    private final TurDefaultAgentResolver turDefaultAgentResolver;

    public TurSNImportAPI(JmsMessagingTemplate jmsMessagingTemplate,
            TurSNSiteRepositoryPort turSNSiteRepositoryPort,
            TurSNGenAi turGenAi,
            com.viglet.turing.tenant.TurJmsTenantPropagation turJmsTenantPropagation,
            TurSNBulkImportService turSNBulkImportService,
            TurSNPartitionWriteGuard turSNPartitionWriteGuard,
            TurDefaultAgentResolver turDefaultAgentResolver) {
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.turSNSiteRepositoryPort = turSNSiteRepositoryPort;
        this.turGenAi = turGenAi;
        this.turJmsTenantPropagation = turJmsTenantPropagation;
        this.turSNBulkImportService = turSNBulkImportService;
        this.turSNPartitionWriteGuard = turSNPartitionWriteGuard;
        this.turDefaultAgentResolver = turDefaultAgentResolver;
    }

    @Operation(summary = "Queue documents for indexing",
            description = "Body: {\"turingDocuments\": [{\"turSNJobAction\": \"CREATE\", \"siteNames\": [\"mysite\"], "
                    + "\"locale\": \"en_US\", \"attributes\": {\"id\": \"1\", \"title\": \"...\"}}]}. "
                    + "\"snJobItems\" is accepted as an alias; a body with no items answers 400. "
                    + "A DELETE names an \"id\", a \"type\", or (T1112) the fields every deleted document "
                    + "holds, e.g. {\"turSNJobAction\": \"DELETE\", \"attributes\": {\"site_key\": \"a.example.org\"}}; "
                    + "one that names nothing, or the id \"*\", answers 400.")
    @PostMapping
    public boolean turSNImportBroker(@RequestBody TurSNJobItems turSNJobItems) {
        requireItems(turSNJobItems);
        applyPartition(turSNJobItems);
        send(turSNJobItems);
        return true;
    }

    /**
     * T1133 — a key bound to a partition of a shared site writes only into it:
     * each document gets the partition's value stamped on, and the write guard
     * refuses any item that would replace or delete another partition's document.
     */
    private void applyPartition(TurSNJobItems turSNJobItems) {
        TurApiKeyPartitionContext.current().ifPresent(partition -> {
            stamp(partition, turSNJobItems);
            turSNPartitionWriteGuard.check(partition, turSNJobItems);
        });
    }

    /**
     * Stamps the partition's value on every CREATE, and refuses one that already
     * carries a different value.
     */
    static void stamp(TurSEPartition partition, TurSNJobItems turSNJobItems) {
        turSNJobItems.forEach(item -> {
            if (item == null || item.getTurSNJobAction() != TurSNJobAction.CREATE
                    || item.getAttributes() == null) {
                return;
            }
            Object carried = item.getAttributes().get(partition.field());
            if (carried != null && !partition.value().equals(String.valueOf(carried))) {
                throw new AccessDeniedException("Document " + item.getAttributes().get(TurSNFieldName.ID)
                        + " carries " + partition.field() + "=" + carried + ", outside this key's partition");
            }
            item.getAttributes().put(partition.field(), partition.value());
        });
    }

    /**
     * T1062 — a body Jackson could not map to any item (an empty list, or the
     * items under an unknown key, which Jackson 3 ignores) used to answer 200 and
     * index nothing. Refuse it with a 400 that names the property expected.
     */
    private static void requireItems(TurSNJobItems turSNJobItems) {
        if (turSNJobItems == null || turSNJobItems.size() == 0) {
            throw new IllegalArgumentException(
                    "Import body has no items: send them as {\"turingDocuments\": [ ... ]}");
        }
        turSNJobItems.forEach(TurSNImportAPI::validateDelete);
    }

    /** A field a DELETE may name: the engines take it unquoted. */
    private static final java.util.regex.Pattern DELETE_FIELD =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_.\\-]{1,255}$");

    /**
     * T1112 — a DELETE names an id, a type, or fields whose values every deleted
     * document holds. One that names nothing, an id of {@code *} (which used to
     * delete nothing and answer 200) and a field value that is not a single value
     * are refused with 400.
     */
    static void validateDelete(TurSNJobItem item) {
        if (item == null || item.getTurSNJobAction() != TurSNJobAction.DELETE) {
            return;
        }
        Map<String, Object> attributes = item.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException(
                    "A DELETE item names nothing to delete: send an id, a type, or the fields to delete by");
        }
        if ("*".equals(String.valueOf(attributes.get(TurSNFieldName.ID)))) {
            throw new IllegalArgumentException("A DELETE with id \"*\" deletes nothing: to delete a set of "
                    + "documents, name the fields they share instead, e.g. {\"site_key\": \"a.example.org\"}");
        }
        if (attributes.containsKey(TurSNFieldName.ID) || attributes.containsKey(TurSNFieldName.TYPE)) {
            return;
        }
        attributes.forEach((field, value) -> {
            if (!DELETE_FIELD.matcher(field).matches()) {
                throw new IllegalArgumentException("A DELETE cannot name the field '" + field
                        + "': use letters, digits, '_', '.' or '-'");
            }
            if (value == null || value instanceof java.util.Collection<?> || value instanceof Map<?, ?>) {
                throw new IllegalArgumentException(
                        "A DELETE by field takes one value per field; '" + field + "' has " + value);
            }
        });
    }

    /**
     * T807 / §LV.5 (Block BG) — opt-in direct bulk-index endpoint for very large
     * <em>vectorless</em> reindexes. When the {@code turing.sn.import.bulk-direct}
     * fast path is enabled and every targeted site is RAG-disabled, the documents
     * are written straight to the search engine (in bounded chunks, reusing the
     * T803 batching) and the JMS queue is skipped. Otherwise it transparently
     * falls back to the ordered queue — so an ineligible request is never
     * rejected, just routed the default way.
     */
    @PostMapping("bulk")
    public boolean turSNImportBulkBroker(@RequestBody TurSNJobItems turSNJobItems) {
        requireItems(turSNJobItems);
        applyPartition(turSNJobItems);
        if (turSNBulkImportService.isEligible(turSNJobItems)) {
            int processed = turSNBulkImportService.importDirect(turSNJobItems);
            log.info("Direct bulk import wrote {} items, bypassing the indexing queue", processed);
            return true;
        }
        send(turSNJobItems);
        return true;
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

    @PostMapping("zip")
    public boolean turSNImportZipFileBroker(@RequestParam("file") MultipartFile multipartFile) {
        File extractFolder = TurSpringUtils.extractZipFile(multipartFile);

        ObjectMapper mapper = JsonMapper.builder().build();

        try (FileInputStream fileInputStream = new FileInputStream(
                extractFolder.getAbsolutePath().concat(File.separator).concat(TurSNConstants.EXPORT_FILE))) {
            TurSNJobItems turSNJobItems = mapper.readValue(fileInputStream, TurSNJobItems.class);

            turSNJobItems.forEach(turSNJobItem -> turSNJobItem.getAttributes().entrySet()
                    .forEach(attribute -> extractTextOfFileAttribute(extractFolder, attribute)));

            FileUtils.deleteDirectory(extractFolder);
            return turSNImportBroker(turSNJobItems);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private void extractTextOfFileAttribute(File extractFolder, Map.Entry<String, Object> attribute) {
        if (attribute.getValue().toString().startsWith(TurSNConstants.FILE_PROTOCOL)) {
            String fileName = attribute.getValue().toString().replace(TurSNConstants.FILE_PROTOCOL, "");
            File file = new File(extractFolder.getAbsolutePath().concat(File.separator).concat(fileName));
            TurTikaFileAttributes turTikaFileAttributes = TurFileUtils.parseFile(file);
            Optional.ofNullable(turTikaFileAttributes)
                    .map(TurTikaFileAttributes::getContent)
                    .ifPresent(content -> attribute.setValue(TurCommonsUtils.cleanTextContent(content)));
        }
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

    private static boolean isValidJobItem(TurSNJobItem turJobItem) {
        return turJobItem != null && turJobItem.getAttributes() != null &&
                turJobItem.getAttributes().containsKey(TurSNFieldName.ID);
    }

    @NotNull
    private static String actionType(TurSNJobItem turJobItem) {
        return switch (turJobItem.getTurSNJobAction()) {
            case CREATE -> "index";
            case DELETE -> "deIndex";
            case COMMIT -> "commit";
        };
    }
}
