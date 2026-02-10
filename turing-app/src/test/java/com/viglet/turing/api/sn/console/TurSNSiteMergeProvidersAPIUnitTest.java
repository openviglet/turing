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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProviders;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProvidersField;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.merge.TurSNSiteMergeProvidersFieldRepository;
import com.viglet.turing.persistence.repository.sn.merge.TurSNSiteMergeProvidersRepository;

/**
 * Unit tests for TurSNSiteMergeProvidersAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteMergeProvidersAPIUnitTest {

    @Test
    void testMergeListReturnsItemsWhenSiteFound() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteMergeProvidersRepository mergeRepository = mock(TurSNSiteMergeProvidersRepository.class);
        TurSNSiteMergeProvidersFieldRepository fieldRepository = mock(TurSNSiteMergeProvidersFieldRepository.class);
        TurSNSiteMergeProvidersAPI api = new TurSNSiteMergeProvidersAPI(siteRepository, mergeRepository,
                fieldRepository);
        TurSNSite site = new TurSNSite();
        TurSNSiteMergeProviders merge = new TurSNSiteMergeProviders();

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(mergeRepository.findByTurSNSite(site)).thenReturn(List.of(merge));

        List<TurSNSiteMergeProviders> result = api.turSNSiteMergeList("site");

        assertThat(result).containsExactly(merge);
    }

    @Test
    void testMergeGetPopulatesOverwrittenFields() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteMergeProvidersRepository mergeRepository = mock(TurSNSiteMergeProvidersRepository.class);
        TurSNSiteMergeProvidersFieldRepository fieldRepository = mock(TurSNSiteMergeProvidersFieldRepository.class);
        TurSNSiteMergeProvidersAPI api = new TurSNSiteMergeProvidersAPI(siteRepository, mergeRepository,
                fieldRepository);
        TurSNSiteMergeProviders merge = new TurSNSiteMergeProviders();
        TurSNSiteMergeProvidersField field = new TurSNSiteMergeProvidersField();

        when(mergeRepository.findById("id")).thenReturn(Optional.of(merge));
        when(fieldRepository.findByTurSNSiteMergeProviders(merge)).thenReturn(Collections.singleton(field));

        TurSNSiteMergeProviders result = api.turSNSiteFieldExtGet("site", "id");

        assertThat(result.getOverwrittenFields()).containsExactly(field);
    }

    @Test
    void testMergeUpdateSavesFields() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteMergeProvidersRepository mergeRepository = mock(TurSNSiteMergeProvidersRepository.class);
        TurSNSiteMergeProvidersFieldRepository fieldRepository = mock(TurSNSiteMergeProvidersFieldRepository.class);
        TurSNSiteMergeProvidersAPI api = new TurSNSiteMergeProvidersAPI(siteRepository, mergeRepository,
                fieldRepository);
        TurSNSiteMergeProviders existing = new TurSNSiteMergeProviders();
        existing.setOverwrittenFields(Collections.emptySet());
        TurSNSiteMergeProviders payload = new TurSNSiteMergeProviders();
        TurSNSiteMergeProvidersField field = new TurSNSiteMergeProvidersField();
        payload.setOverwrittenFields(Collections.singleton(field));

        when(mergeRepository.findById("id")).thenReturn(Optional.of(existing));

        TurSNSiteMergeProviders result = api.turSNSiteMergeUpdate("id", payload, "site");

        assertThat(result).isSameAs(existing);
        verify(fieldRepository).save(field);
    }

    @Test
    void testMergeDeleteAlwaysTrue() {
        TurSNSiteMergeProvidersRepository mergeRepository = mock(TurSNSiteMergeProvidersRepository.class);
        TurSNSiteMergeProvidersAPI api = new TurSNSiteMergeProvidersAPI(mock(TurSNSiteRepository.class),
                mergeRepository, mock(TurSNSiteMergeProvidersFieldRepository.class));

        boolean result = api.turSNSiteMergeDelete("id", "site");

        assertThat(result).isTrue();
        verify(mergeRepository).deleteById("id");
    }

    @Test
    void testMergeStructureDefaultsLocale() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteMergeProvidersAPI api = new TurSNSiteMergeProvidersAPI(siteRepository,
                mock(TurSNSiteMergeProvidersRepository.class), mock(TurSNSiteMergeProvidersFieldRepository.class));
        TurSNSite site = new TurSNSite();

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));

        TurSNSiteMergeProviders result = api.turSNSiteMergeStructure("site");

        assertThat(result.getLocale()).isEqualTo(Locale.US);
        assertThat(result.getTurSNSite()).isSameAs(site);
    }
}
