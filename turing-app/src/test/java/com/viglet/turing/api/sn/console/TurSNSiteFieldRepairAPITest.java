/*
 * Copyright (C) 2016-2025 the original author or authors.
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

package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.api.sn.bean.TurSNFieldRepairPayload;
import com.viglet.turing.api.sn.bean.TurSNFieldRepairType;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.plugins.se.TurSEFieldSchema;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;

/**
 * Unit tests for TurSNSiteFieldRepairAPI. T1243 — every repair writes through the
 * site's engine plugin.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteFieldRepairAPITest {

    private final TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
    private final TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
    private final TurSEInstanceRepository instanceRepository = mock(TurSEInstanceRepository.class);
    private final TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
    private final TurSEInstance instance = new TurSEInstance();
    private final TurSNSiteFieldRepairAPI api;

    TurSNSiteFieldRepairAPITest() {
        TurSearchEnginePluginFactory factory = mock(TurSearchEnginePluginFactory.class);
        when(factory.getPluginForSite(any())).thenReturn(plugin);
        api = new TurSNSiteFieldRepairAPI(siteRepository, fieldExtRepository, instanceRepository, factory);
    }

    private TurSNSiteFieldExt field(TurSEFieldType type, int multiValued) {
        instance.setId("se");
        TurSNSite site = new TurSNSite();
        site.setTurSEInstance(instance);
        TurSNSiteFieldExt fieldExt = new TurSNSiteFieldExt();
        fieldExt.setName("title");
        fieldExt.setType(type);
        fieldExt.setMultiValued(multiValued);
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findById("field")).thenReturn(Optional.of(fieldExt));
        when(instanceRepository.findById("se")).thenReturn(Optional.of(instance));
        return fieldExt;
    }

    private static TurSNFieldRepairPayload payload(TurSNFieldRepairType type) {
        TurSNFieldRepairPayload payload = new TurSNFieldRepairPayload();
        payload.setId("field");
        payload.setCore("core");
        payload.setRepairType(type);
        return payload;
    }

    @Test
    void testFieldRepairReturnsOkWhenSiteMissing() {
        assertThat(api.turSNSiteFieldRepair("site", payload(TurSNFieldRepairType.SE_CREATE_FIELD))).isEqualTo("ok");
        verify(plugin, never()).addOrUpdateField(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void testFieldRepairCreatesFieldWhenRequested() {
        field(TurSEFieldType.STRING, 1);

        assertThat(api.turSNSiteFieldRepair("site", payload(TurSNFieldRepairType.SE_CREATE_FIELD))).isEqualTo("ok");

        verify(plugin).addOrUpdateField(instance, "core", "title", TurSEFieldType.STRING, true, true, true);
    }

    @Test
    void changingTheTypeKeepsTheEnginesCardinality() {
        field(TurSEFieldType.STRING, 0);
        when(plugin.fieldSchema(instance, "core", "title"))
                .thenReturn(Optional.of(new TurSEFieldSchema("text_general", TurSEFieldType.TEXT, true)));

        api.turSNSiteFieldRepair("site", payload(TurSNFieldRepairType.SE_CHANGE_TYPE));

        verify(plugin).addOrUpdateField(instance, "core", "title", TurSEFieldType.STRING, true, true, false);
    }

    @Test
    void syncingTheCardinalityKeepsTheEnginesType() {
        field(TurSEFieldType.STRING, 1);
        when(plugin.fieldSchema(instance, "core", "title"))
                .thenReturn(Optional.of(new TurSEFieldSchema("text_general", TurSEFieldType.TEXT, false)));

        api.turSNSiteFieldRepair("site", payload(TurSNFieldRepairType.SE_ENABLE_MULTI_VALUE));

        verify(plugin).addOrUpdateField(instance, "core", "title", TurSEFieldType.TEXT, true, true, false);
    }

    @Test
    void repairAllReplacesAMismatchedFieldAndLeavesAMatchingOne() {
        field(TurSEFieldType.STRING, 1);
        when(plugin.fieldSchema(instance, "core", "title"))
                .thenReturn(Optional.of(new TurSEFieldSchema("string", TurSEFieldType.STRING, false)));

        api.turSNSiteFieldRepair("site", payload(TurSNFieldRepairType.REPAIR_ALL));

        verify(plugin).addOrUpdateField(instance, "core", "title", TurSEFieldType.STRING, true, true, false);
    }

    @Test
    void repairAllCreatesAFieldTheSchemaLacks() {
        field(TurSEFieldType.STRING, 0);
        when(plugin.fieldSchema(instance, "core", "title")).thenReturn(Optional.empty());

        api.turSNSiteFieldRepair("site", payload(TurSNFieldRepairType.REPAIR_ALL));

        verify(plugin).addOrUpdateField(instance, "core", "title", TurSEFieldType.STRING, true, false, true);
    }
}