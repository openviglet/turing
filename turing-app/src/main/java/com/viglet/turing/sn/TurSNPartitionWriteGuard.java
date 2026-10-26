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
package com.viglet.turing.sn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshotService;

/**
 * T1133 — keeps a partitioned key's writes inside its partition. A document is
 * named by id alone, so before a CREATE replaces one or a DELETE removes one,
 * the document already stored under that id must be absent or in the key's
 * partition. A DELETE without an id (by type) spans partitions and is refused.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Service
public class TurSNPartitionWriteGuard {

    private final TurSNSiteSearchSnapshotService snapshotService;
    private final TurSearchEnginePluginFactory searchEnginePluginFactory;

    public TurSNPartitionWriteGuard(TurSNSiteSearchSnapshotService snapshotService,
            TurSearchEnginePluginFactory searchEnginePluginFactory) {
        this.snapshotService = snapshotService;
        this.searchEnginePluginFactory = searchEnginePluginFactory;
    }

    /** One (site, locale) index and the ids a batch writes into it. */
    private record Target(String site, Locale locale) {
    }

    /**
     * @throws AccessDeniedException when an item would change a document of
     *                               another partition
     */
    public void check(TurSEPartition partition, TurSNJobItems items) {
        Map<Target, List<String>> ids = new LinkedHashMap<>();
        for (TurSNJobItem item : items) {
            if (item == null || item.getTurSNJobAction() == TurSNJobAction.COMMIT) {
                continue;
            }
            Object id = item.getAttributes() == null ? null : item.getAttributes().get(TurSNFieldName.ID);
            if (id == null) {
                if (item.getTurSNJobAction() == TurSNJobAction.DELETE) {
                    throw new AccessDeniedException(
                            "An API key bound to a partition deletes by id only");
                }
                continue;
            }
            for (String site : Objects.requireNonNullElse(item.getSiteNames(), List.<String>of())) {
                ids.computeIfAbsent(new Target(site, item.getLocale()), target -> new ArrayList<>())
                        .add(id.toString());
            }
        }
        ids.forEach((target, targetIds) -> checkStored(partition, target, targetIds));
    }

    private void checkStored(TurSEPartition partition, Target target, List<String> ids) {
        snapshotService.getSnapshot(target.site(), target.locale()).ifPresent(snap -> {
            TurSNSiteLocale siteLocale = TurSNDuplicateClusteringService.resolveLocale(snap, target.locale());
            if (siteLocale == null) {
                return;
            }
            searchEnginePluginFactory.getPlugin(snap.pluginType()).getStoredDocumentsByIds(siteLocale, ids)
                    .stream()
                    .filter(stored -> !inPartition(stored.get(partition.field()), partition.value()))
                    .findFirst()
                    .ifPresent(stored -> {
                        throw new AccessDeniedException("Document " + stored.get(TurSNFieldName.ID)
                                + " belongs to another partition of site " + target.site());
                    });
        });
    }

    /** A stored value is in the partition when it is the value, alone or in a list. */
    static boolean inPartition(Object stored, String value) {
        if (stored instanceof Collection<?> values) {
            return !values.isEmpty() && values.stream().allMatch(v -> value.equals(String.valueOf(v)));
        }
        return stored != null && value.equals(String.valueOf(stored));
    }
}
