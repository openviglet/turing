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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSiteView;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshot;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshotService;

/**
 * T1133 — a partitioned key cannot replace or delete another partition's document.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@ExtendWith(MockitoExtension.class)
class TurSNPartitionWriteGuardTest {

    private static final TurSEPartition A = new TurSEPartition("site_key", "a.example.org");

    @Mock
    private TurSNSiteSearchSnapshotService snapshotService;
    @Mock
    private TurSearchEnginePluginFactory pluginFactory;
    @Mock
    private TurSearchEnginePlugin plugin;

    private TurSNPartitionWriteGuard guard;

    @BeforeEach
    void setUp() {
        guard = new TurSNPartitionWriteGuard(snapshotService, pluginFactory);
        TurSNSite site = new TurSNSite();
        site.setId("site-1");
        site.setName("shared");
        TurSNSiteSearchSnapshot snapshot = new TurSNSiteSearchSnapshot(TurSNSiteSearchSiteView.from(site, "solr"),
                Locale.US, "solr", false, List.of(), java.util.Set.of(), Map.of(), Map.of(),
                List.of(new TurSNSiteSearchSnapshot.LocaleView(Locale.US, "core", 0)), List.of());
        lenient().when(snapshotService.getSnapshot("shared", Locale.US)).thenReturn(Optional.of(snapshot));
        lenient().when(pluginFactory.getPlugin("solr")).thenReturn(plugin);
    }

    private static TurSNJobItems item(TurSNJobAction action, Map<String, Object> attributes) {
        return new TurSNJobItems(new TurSNJobItem(action, List.of("shared"), Locale.US,
                new HashMap<>(attributes), null));
    }

    private void stored(Map<String, Object> document) {
        when(plugin.getStoredDocumentsByIds(any(), anyList())).thenReturn(List.of(document));
    }

    @Test
    void writesANewIdOrOneOfItsOwn() {
        when(plugin.getStoredDocumentsByIds(any(), anyList())).thenReturn(List.of());
        assertThatCode(() -> guard.check(A, item(TurSNJobAction.CREATE, Map.of("id", "new"))))
                .doesNotThrowAnyException();

        stored(Map.of("id", "mine", "site_key", "a.example.org"));
        assertThatCode(() -> guard.check(A, item(TurSNJobAction.DELETE, Map.of("id", "mine"))))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesToReplaceOrDeleteAnotherPartitionsDocument() {
        stored(Map.of("id", "theirs", "site_key", "b.example.org"));

        assertThatThrownBy(() -> guard.check(A, item(TurSNJobAction.CREATE, Map.of("id", "theirs"))))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guard.check(A, item(TurSNJobAction.DELETE, Map.of("id", "theirs"))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void refusesADocumentStoredWithoutAPartition() {
        stored(Map.of("id", "legacy"));

        assertThatThrownBy(() -> guard.check(A, item(TurSNJobAction.DELETE, Map.of("id", "legacy"))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void refusesADeleteByType() {
        assertThatThrownBy(() -> guard.check(A, item(TurSNJobAction.DELETE, Map.of("type", "post"))))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** T1112 — a delete by fields stays inside the partition; naming the partition is its delete-all. */
    @Test
    void confinesADeleteByFieldsToThePartition() {
        TurSNJobItems deleteAll = item(TurSNJobAction.DELETE, Map.of("site_key", "a.example.org"));
        guard.check(A, deleteAll);
        assertThat(deleteAll.getTuringDocuments().get(0).getAttributes())
                .containsEntry("site_key", "a.example.org");

        TurSNJobItems posts = item(TurSNJobAction.DELETE, Map.of("post_type", "post"));
        guard.check(A, posts);
        assertThat(posts.getTuringDocuments().get(0).getAttributes())
                .containsEntry("site_key", "a.example.org").containsEntry("post_type", "post");

        assertThatThrownBy(() -> guard.check(A, item(TurSNJobAction.DELETE, Map.of("site_key", "b.example.org"))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void readsAMultiValuedPartitionField() {
        assertThat(TurSNPartitionWriteGuard.inPartition(List.of("a.example.org"), "a.example.org")).isTrue();
        assertThat(TurSNPartitionWriteGuard.inPartition(List.of("a.example.org", "b"), "a.example.org")).isFalse();
        assertThat(TurSNPartitionWriteGuard.inPartition(List.of(), "a.example.org")).isFalse();
    }
}
