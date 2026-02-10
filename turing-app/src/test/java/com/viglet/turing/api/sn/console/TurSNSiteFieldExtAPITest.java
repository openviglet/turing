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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteField;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.sn.TurSNFieldType;
import com.viglet.turing.sn.template.TurSNTemplate;

/**
 * Unit tests for TurSNSiteFieldExtAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteFieldExtAPITest {

    @Test
    void testFieldExtListReturnsEmptyWhenSiteMissing() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                mock(TurSNTemplate.class));

        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        List<TurSNSiteFieldExt> result = api.turSNSiteFieldExtList("site");

        assertThat(result).isEmpty();
    }

    @Test
    void testFieldExtGetReturnsFacetLocales() {
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteFieldExtFacetRepository facetRepository = mock(TurSNSiteFieldExtFacetRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                fieldExtRepository, facetRepository, mock(TurSNSiteFieldRepository.class),
                mock(TurSNSiteLocaleRepository.class), mock(TurSNTemplate.class));

        when(fieldExtRepository.findById("id")).thenReturn(Optional.empty());

        TurSNSiteFieldExt result = api.turSNSiteFieldExtGet("site", "id");

        assertThat(result).isNotNull();
        verify(facetRepository).findByTurSNSiteFieldExt(result);
    }

    @Test
    void testFieldExtUpdateSetsFacetPositionWhenMissing() {
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                fieldRepository, mock(TurSNSiteLocaleRepository.class),
                mock(TurSNTemplate.class));

        TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
        existing.setSnType(TurSNFieldType.SE);
        existing.setExternalId("ext");
        TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
        payload.setName("title");
        payload.setFacet(1);
        payload.setSnType(TurSNFieldType.SE);
        payload.setExternalId("ext");

        when(fieldExtRepository.findById("id")).thenReturn(Optional.of(existing));
        when(fieldExtRepository.findMaxFacetPosition()).thenReturn(Optional.of(3));
        when(fieldRepository.findById("ext")).thenReturn(Optional.empty());

        TurSNSiteFieldExt result = api.turSNSiteFieldExtUpdate("site", "id", payload);

        assertThat(result.getFacetPosition()).isEqualTo(4);
    }

    @Test
    void testFieldExtDeleteDeletesExternalFieldWhenSE() {
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                fieldRepository, mock(TurSNSiteLocaleRepository.class), mock(TurSNTemplate.class));
        TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
        existing.setSnType(TurSNFieldType.SE);
        existing.setExternalId("ext");

        when(fieldExtRepository.findById("id")).thenReturn(Optional.of(existing));

        boolean result = api.turSNSiteFieldExtDelete("site", "id");

        assertThat(result).isTrue();
        verify(fieldRepository).delete("ext");
        verify(fieldExtRepository).delete("id");
    }

    @Test
    void testFieldExtAddReturnsDefaultWhenSiteMissing() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                mock(TurSNTemplate.class));

        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        TurSNSiteFieldExt result = api.turSNSiteFieldExtAdd("site", new TurSNSiteFieldExt());

        assertThat(result.getId()).isNull();
    }

    @Test
    void testFieldExtStructureSetsSite() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                mock(TurSNTemplate.class));
        TurSNSite site = new TurSNSite();

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));

        TurSNSiteFieldExt result = api.urSNSiteFieldExtStructure("site");

        assertThat(result.getTurSNSite()).isSameAs(site);
    }

    @Test
    void testFieldExtAddCreatesSEFieldWhenSiteFound() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                fieldRepository, mock(TurSNSiteLocaleRepository.class),
                mock(TurSNTemplate.class));
        TurSNSite site = new TurSNSite();
        TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
        payload.setName("title");
        payload.setType(com.viglet.turing.commons.se.field.TurSEFieldType.STRING);

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(fieldRepository.save(org.mockito.ArgumentMatchers.any(TurSNSiteField.class)))
                .thenAnswer(invocation -> {
                    TurSNSiteField field = invocation.getArgument(0);
                    field.setId("field-id");
                    return field;
                });

        TurSNSiteFieldExt result = api.turSNSiteFieldExtAdd("site", payload);

        assertThat(result.getSnType()).isEqualTo(TurSNFieldType.SE);
        assertThat(result.getExternalId()).isEqualTo("field-id");
    }
}
