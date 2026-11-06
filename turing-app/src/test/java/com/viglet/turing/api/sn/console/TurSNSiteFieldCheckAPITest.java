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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.api.sn.bean.TurSNFieldExtCheck;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.plugins.se.TurSEFieldSchema;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;

/**
 * Unit tests for TurSNSiteFieldCheckAPI. T1243 — the site's engine plugin answers
 * for the schema, so the tests stub the plugin rather than Solr.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteFieldCheckAPITest {

    private final TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
    private final TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
    private final TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
    private final TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
    private final TurSEInstance instance = new TurSEInstance();
    private final TurSNSite site = new TurSNSite();

    private TurSNSiteFieldCheckAPI api(boolean indexExists, boolean hasSchema) {
        lenient().when(plugin.indexExists(any(), any())).thenReturn(indexExists);
        lenient().when(plugin.hasFieldSchema()).thenReturn(hasSchema);
        TurSearchEnginePluginFactory factory = mock(TurSearchEnginePluginFactory.class);
        lenient().when(factory.getPluginForSite(any())).thenReturn(plugin);
        return new TurSNSiteFieldCheckAPI(siteRepository, fieldExtRepository, localeRepository, factory);
    }

    private TurSNSiteFieldExt field(String core, String name, TurSEFieldType type, int multiValued, int facet) {
        site.setTurSEInstance(instance);
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setCore(core);
        TurSNSiteFieldExt fieldExt = new TurSNSiteFieldExt();
        fieldExt.setId("id-" + name);
        fieldExt.setExternalId("ext-" + name);
        fieldExt.setName(name);
        fieldExt.setType(type);
        fieldExt.setMultiValued(multiValued);
        fieldExt.setFacet(facet);
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findByTurSNSite(any(), org.mockito.ArgumentMatchers.eq(site)))
                .thenReturn(List.of(fieldExt));
        when(localeRepository.findByTurSNSite(site)).thenReturn(List.of(locale));
        return fieldExt;
    }

    @Test
    void testFieldCheckReturnsEmptyWhenSiteMissing() {
        TurSNSiteFieldCheckAPI api = api(true, true);
        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        assertThat(api.turSNSiteFieldExtCheckList("site")).isNotNull();
    }

    @Test
    void testFieldCheckReturnsEmptyListsWhenNoFieldsOrLocales() {
        TurSNSiteFieldCheckAPI api = api(true, true);
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findByTurSNSite(any(), org.mockito.ArgumentMatchers.eq(site)))
                .thenReturn(Collections.emptyList());
        when(localeRepository.findByTurSNSite(site)).thenReturn(Collections.emptyList());

        TurSNFieldExtCheck result = api.turSNSiteFieldExtCheckList("site");

        assertThat(result.getCores()).isEmpty();
        assertThat(result.getFields()).isEmpty();
    }

    @Test
    void testFieldCheckReturnsCoreAndCorrectFieldStatusWhenSchemaMatches() {
        TurSNSiteFieldCheckAPI api = api(true, true);
        field("core_en", "title", TurSEFieldType.STRING, 1, 0);
        when(plugin.fieldSchema(instance, "core_en", "title"))
                .thenReturn(Optional.of(new TurSEFieldSchema("string", TurSEFieldType.STRING, true)));

        TurSNFieldExtCheck result = api.turSNSiteFieldExtCheckList("site");

        assertThat(result.getCores()).hasSize(1);
        assertThat(result.getCores().get(0).isExists()).isTrue();
        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0).isCorrect()).isTrue();
        assertThat(result.getFields().get(0).isFacetIsCorrect()).isTrue();
        assertThat(result.getFields().get(0).getCores().get(0).getType()).isEqualTo("string");
        assertThat(result.getFields().get(0).getCores().get(0).isCorrect()).isTrue();
    }

    @Test
    void testFieldCheckMarksIncorrectWhenFieldMissingAndFacetInvalid() {
        TurSNSiteFieldCheckAPI api = api(false, true);
        field("core_pt", "content", TurSEFieldType.TEXT, 0, 1);
        when(plugin.fieldSchema(instance, "core_pt", "content")).thenReturn(Optional.empty());

        TurSNFieldExtCheck result = api.turSNSiteFieldExtCheckList("site");

        assertThat(result.getCores().get(0).isExists()).isFalse();
        assertThat(result.getFields().get(0).isCorrect()).isFalse();
        assertThat(result.getFields().get(0).isFacetIsCorrect()).isFalse();
        assertThat(result.getFields().get(0).getCores().get(0).isExists()).isFalse();
    }

    @Test
    void testFieldCheckMarksIncorrectWhenTypeOrMultivaluedMismatch() {
        TurSNSiteFieldCheckAPI api = api(true, true);
        field("core_es", "summary", TurSEFieldType.STRING, 1, 0);
        when(plugin.fieldSchema(instance, "core_es", "summary"))
                .thenReturn(Optional.of(new TurSEFieldSchema("text_general", TurSEFieldType.TEXT, false)));

        TurSNFieldExtCheck result = api.turSNSiteFieldExtCheckList("site");

        assertThat(result.getFields().get(0).isCorrect()).isFalse();
        assertThat(result.getFields().get(0).getCores().get(0).isTypeIsCorrect()).isFalse();
        assertThat(result.getFields().get(0).getCores().get(0).isMultiValuedIsCorrect()).isFalse();
    }

    @Test
    void testFieldCheckDoesNotThrowWhenTheEngineTypeIsUnknown() {
        TurSNSiteFieldCheckAPI api = api(true, true);
        field("core_ca", "author", TurSEFieldType.STRING, 0, 0);
        when(plugin.fieldSchema(instance, "core_ca", "author"))
                .thenReturn(Optional.of(new TurSEFieldSchema(null, null, false)));

        TurSNFieldExtCheck result = api.turSNSiteFieldExtCheckList("site");

        assertThat(result.getFields().get(0).isCorrect()).isFalse();
        assertThat(result.getFields().get(0).getCores().get(0).isTypeIsCorrect()).isFalse();
    }

    /** T1243 — an engine without a schema (Lucene, Elasticsearch) reports every field correct. */
    @Test
    void anEngineWithoutASchemaReportsTheFieldCorrect() {
        TurSNSiteFieldCheckAPI api = api(true, false);
        field("lucene_pt", "author", TurSEFieldType.STRING, 1, 0);

        TurSNFieldExtCheck result = api.turSNSiteFieldExtCheckList("site");

        assertThat(result.getFields().get(0).isCorrect()).isTrue();
        org.mockito.Mockito.verify(plugin, org.mockito.Mockito.never()).fieldSchema(any(), any(), any());
    }
}