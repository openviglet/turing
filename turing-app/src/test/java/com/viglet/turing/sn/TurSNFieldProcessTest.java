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

package com.viglet.turing.sn;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TurSNFieldProcess.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSNFieldProcessTest {

    @Mock
    private TurSNSiteRepository turSNSiteRepository;

    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    private TurSNFieldProcess turSNFieldProcess;

    @BeforeEach
    void setUp() {
        turSNFieldProcess = new TurSNFieldProcess(turSNSiteRepository, turSNSiteFieldExtRepository);
    }

    @Test
    void testGetTurSNSiteFieldOrderingWithValidSite() {
        String snSiteId = "site-123";
        TurSNSite mockSite = new TurSNSite();
        mockSite.setId(snSiteId);

        TurSNSiteFieldExt field1 = new TurSNSiteFieldExt();
        TurSNSiteFieldExt field2 = new TurSNSiteFieldExt();
        List<TurSNSiteFieldExt> expectedFields = Arrays.asList(field1, field2);

        when(turSNSiteRepository.findById(snSiteId)).thenReturn(Optional.of(mockSite));
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                mockSite, 1, 1)).thenReturn(expectedFields);

        Optional<List<TurSNSiteFieldExt>> result = turSNFieldProcess.getTurSNSiteFieldOrdering(snSiteId);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
        assertThat(result.get()).containsExactly(field1, field2);

        verify(turSNSiteRepository).findById(snSiteId);
        verify(turSNSiteFieldExtRepository).findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                mockSite, 1, 1);
    }

    @Test
    void testGetTurSNSiteFieldOrderingWithNonExistentSite() {
        String snSiteId = "non-existent-site";

        when(turSNSiteRepository.findById(snSiteId)).thenReturn(Optional.empty());

        Optional<List<TurSNSiteFieldExt>> result = turSNFieldProcess.getTurSNSiteFieldOrdering(snSiteId);

        assertThat(result).isEmpty();

        verify(turSNSiteRepository).findById(snSiteId);
        verify(turSNSiteFieldExtRepository, never()).findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                any(), anyInt(), anyInt());
    }

    @Test
    void testGetTurSNSiteFieldOrderingWithEmptyFieldList() {
        String snSiteId = "site-456";
        TurSNSite mockSite = new TurSNSite();
        mockSite.setId(snSiteId);

        when(turSNSiteRepository.findById(snSiteId)).thenReturn(Optional.of(mockSite));
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                mockSite, 1, 1)).thenReturn(Collections.emptyList());

        Optional<List<TurSNSiteFieldExt>> result = turSNFieldProcess.getTurSNSiteFieldOrdering(snSiteId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();

        verify(turSNSiteRepository).findById(snSiteId);
        verify(turSNSiteFieldExtRepository).findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                mockSite, 1, 1);
    }

    @Test
    void testGetTurSNSiteFieldOrderingWithNullSiteId() {
        when(turSNSiteRepository.findById(null)).thenReturn(Optional.empty());

        Optional<List<TurSNSiteFieldExt>> result = turSNFieldProcess.getTurSNSiteFieldOrdering(null);

        assertThat(result).isEmpty();

        verify(turSNSiteRepository).findById(null);
        verify(turSNSiteFieldExtRepository, never()).findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                any(), anyInt(), anyInt());
    }

    @Test
    void testGetTurSNSiteFieldOrderingReturnTypeNotNull() {
        String snSiteId = "site-789";

        when(turSNSiteRepository.findById(snSiteId)).thenReturn(Optional.empty());

        Optional<List<TurSNSiteFieldExt>> result = turSNFieldProcess.getTurSNSiteFieldOrdering(snSiteId);

        assertThat(result).isNotNull();
    }

    @Test
    void testGetTurSNSiteFieldOrderingWithSingleField() {
        String snSiteId = "single-field-site";
        TurSNSite mockSite = new TurSNSite();
        mockSite.setId(snSiteId);

        TurSNSiteFieldExt field = new TurSNSiteFieldExt();
        List<TurSNSiteFieldExt> singleFieldList = Collections.singletonList(field);

        when(turSNSiteRepository.findById(snSiteId)).thenReturn(Optional.of(mockSite));
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(
                mockSite, 1, 1)).thenReturn(singleFieldList);

        Optional<List<TurSNSiteFieldExt>> result = turSNFieldProcess.getTurSNSiteFieldOrdering(snSiteId);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0)).isEqualTo(field);
    }
}
