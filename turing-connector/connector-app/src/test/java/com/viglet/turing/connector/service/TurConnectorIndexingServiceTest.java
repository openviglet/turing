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
package com.viglet.turing.connector.service;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.domain.TurSNSiteLocale;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TurConnectorIndexingService.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@ExtendWith(MockitoExtension.class)
class TurConnectorIndexingServiceTest {

    @Mock
    private TurConnectorIndexingRepository repository;

    @InjectMocks
    private TurConnectorIndexingService service;

    @Test
    void testFindByDependencies() {
        String source = "test-source";
        String provider = "test-provider";
        List<String> referenceIds = Arrays.asList("ref1", "ref2");
        List<String> expectedObjectIds = Arrays.asList("obj1", "obj2");

        when(repository.findObjectIdsByDependencies(source, provider, referenceIds))
                .thenReturn(expectedObjectIds);

        List<String> result = service.findByDependencies(source, provider, referenceIds);

        assertThat(result).isEqualTo(expectedObjectIds);
        verify(repository, times(1)).findObjectIdsByDependencies(source, provider, referenceIds);
    }

    @Test
    void testDelete() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);

        when(jobItem.getId()).thenReturn("test-id");
        when(jobItem.getEnvironment()).thenReturn("test-env");
        when(session.getSource()).thenReturn("test-source");
        when(session.getProviderName()).thenReturn("test-provider");

        service.delete(jobItemWithSession);

        verify(repository, times(1)).deleteByObjectIdAndSourceAndEnvironmentAndProvider(
                "test-id", "test-source", "test-env", "test-provider");
    }

    @Test
    void testDeleteByProvider() {
        String provider = "test-provider";

        service.deleteByProvider(provider);

        verify(repository, times(1)).deleteByProvider(provider);
    }

    @Test
    void testDeleteByProviderAndSourceAndObjectIdIn() {
        String provider = "test-provider";
        String source = "test-source";
        Collection<String> objectIds = Arrays.asList("id1", "id2", "id3");

        service.deleteByProviderAndSourceAndObjectIdIn(provider, source, objectIds);

        verify(repository, times(1)).deleteByProviderAndSourceAndObjectIdIn(provider, source, objectIds);
    }

    @Test
    void testDeleteByProviderAndSource() {
        String provider = "test-provider";
        String source = "test-source";

        service.deleteByProviderAndSource(provider, source);

        verify(repository, times(1)).deleteByProviderAndSource(provider, source);
    }

    @Test
    void testDeleteContentsToBeDeIndexed() {
        TurConnectorSession session = mock(TurConnectorSession.class);
        when(session.getSource()).thenReturn("test-source");
        when(session.getProviderName()).thenReturn("test-provider");
        when(session.getTransactionId()).thenReturn("test-transaction-id");

        service.deleteContentsToBeDeIndexed(session);

        verify(repository, times(1)).deleteBySourceAndProviderAndTransactionIdNot(
                "test-source", "test-provider", "test-transaction-id");
    }

    @Test
    void testUpdateWithSingleIndexing() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);
        TurConnectorIndexingModel indexing = mock(TurConnectorIndexingModel.class);

        when(jobItem.getChecksum()).thenReturn("test-checksum");
        when(session.getTransactionId()).thenReturn("test-transaction-id");
        when(jobItem.getSiteNames()).thenReturn(Arrays.asList("site1"));

        service.update(jobItemWithSession, indexing);

        verify(repository, times(1)).save(any(TurConnectorIndexingModel.class));
    }

    @Test
    void testUpdateWithIndexingList() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);
        
        TurConnectorIndexingModel indexing1 = mock(TurConnectorIndexingModel.class);
        TurConnectorIndexingModel indexing2 = mock(TurConnectorIndexingModel.class);
        List<TurConnectorIndexingModel> indexingList = Arrays.asList(indexing1, indexing2);

        when(indexing1.getId()).thenReturn(1);
        when(indexing2.getId()).thenReturn(2);
        when(repository.findById(1)).thenReturn(Optional.of(indexing1));
        when(repository.findById(2)).thenReturn(Optional.of(indexing2));
        when(jobItem.getChecksum()).thenReturn("test-checksum");
        when(session.getTransactionId()).thenReturn("test-transaction-id");
        when(jobItem.getSiteNames()).thenReturn(Arrays.asList("site1"));

        service.update(jobItemWithSession, indexingList, TurIndexingStatus.INDEXED);

        verify(repository, times(2)).save(any(TurConnectorIndexingModel.class));
    }

    @Test
    void testSave() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);

        when(jobItem.getId()).thenReturn("test-id");
        when(jobItem.getLocale()).thenReturn(Locale.ENGLISH);
        when(jobItem.getChecksum()).thenReturn("test-checksum");
        when(jobItem.getSiteNames()).thenReturn(Arrays.asList("site1"));
        when(jobItem.getEnvironment()).thenReturn("test-env");
        when(session.getSource()).thenReturn("test-source");
        when(session.getTransactionId()).thenReturn("test-transaction-id");
        when(session.getProviderName()).thenReturn("test-provider");

        service.save(jobItemWithSession, TurIndexingStatus.INDEXED);

        ArgumentCaptor<TurConnectorIndexingModel> captor = ArgumentCaptor.forClass(TurConnectorIndexingModel.class);
        verify(repository, times(1)).save(captor.capture());
        
        TurConnectorIndexingModel saved = captor.getValue();
        assertThat(saved.getObjectId()).isEqualTo("test-id");
        assertThat(saved.getSource()).isEqualTo("test-source");
        assertThat(saved.getProvider()).isEqualTo("test-provider");
        assertThat(saved.getStatus()).isEqualTo(TurIndexingStatus.INDEXED);
    }

    @Test
    void testExists() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);

        when(jobItem.getId()).thenReturn("test-id");
        when(jobItem.getEnvironment()).thenReturn("test-env");
        when(session.getSource()).thenReturn("test-source");
        when(session.getProviderName()).thenReturn("test-provider");
        when(repository.existsByObjectIdAndSourceAndEnvironmentAndProvider(
                "test-id", "test-source", "test-env", "test-provider")).thenReturn(true);

        boolean result = service.exists(jobItemWithSession);

        assertThat(result).isTrue();
        verify(repository, times(1)).existsByObjectIdAndSourceAndEnvironmentAndProvider(
                "test-id", "test-source", "test-env", "test-provider");
    }

    @Test
    void testGetList() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);
        List<TurConnectorIndexingModel> expectedList = Arrays.asList(mock(TurConnectorIndexingModel.class));

        when(jobItem.getId()).thenReturn("test-id");
        when(jobItem.getEnvironment()).thenReturn("test-env");
        when(session.getSource()).thenReturn("test-source");
        when(session.getProviderName()).thenReturn("test-provider");
        when(repository.findByObjectIdAndSourceAndEnvironmentAndProvider(
                "test-id", "test-source", "test-env", "test-provider")).thenReturn(expectedList);

        List<TurConnectorIndexingModel> result = service.getList(jobItemWithSession);

        assertThat(result).isEqualTo(expectedList);
    }

    @Test
    void testGetShouldBeDeIndexedList() {
        TurConnectorSession session = mock(TurConnectorSession.class);
        List<TurConnectorIndexingModel> expectedList = Arrays.asList(mock(TurConnectorIndexingModel.class));

        when(session.getSource()).thenReturn("test-source");
        when(session.getProviderName()).thenReturn("test-provider");
        when(session.getTransactionId()).thenReturn("test-transaction-id");
        when(repository.findBySourceAndProviderAndTransactionIdNotAndStandalone(
                "test-source", "test-provider", "test-transaction-id", false)).thenReturn(expectedList);

        List<TurConnectorIndexingModel> result = service.getShouldBeDeIndexedList(session);

        assertThat(result).isEqualTo(expectedList);
    }

    @Test
    void testFindAll() {
        List<TurConnectorIndexingModel> expectedList = Arrays.asList(
                mock(TurConnectorIndexingModel.class),
                mock(TurConnectorIndexingModel.class)
        );

        when(repository.findAllByOrderByModificationDateDesc(Limit.of(50))).thenReturn(expectedList);

        List<TurConnectorIndexingModel> result = service.findAll();

        assertThat(result).isEqualTo(expectedList);
        verify(repository, times(1)).findAllByOrderByModificationDateDesc(Limit.of(50));
    }

    @Test
    void testGetAllSources() {
        String provider = "test-provider";
        List<String> expectedSources = Arrays.asList("source1", "source2");

        when(repository.findAllSources(provider)).thenReturn(expectedSources);

        List<String> result = service.getAllSources(provider);

        assertThat(result).isEqualTo(expectedSources);
        verify(repository, times(1)).findAllSources(provider);
    }

    @Test
    void testIsChecksumDifferent() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        TurConnectorSession session = mock(TurConnectorSession.class);
        TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem, session, new HashSet<>(), false);

        when(jobItem.getId()).thenReturn("test-id");
        when(jobItem.getEnvironment()).thenReturn("test-env");
        when(jobItem.getChecksum()).thenReturn("test-checksum");
        when(session.getSource()).thenReturn("test-source");
        when(repository.existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(
                "test-id", "test-source", "test-env", "test-checksum")).thenReturn(true);

        boolean result = service.isChecksumDifferent(jobItemWithSession);

        assertThat(result).isTrue();
    }

    @Test
    void testGetBySourceAndProvider() {
        String source = "test-source";
        String provider = "test-provider";
        List<TurConnectorIndexingModel> expectedList = Arrays.asList(mock(TurConnectorIndexingModel.class));

        when(repository.findAllBySourceAndProviderOrderByModificationDateDesc(source, provider, Limit.of(50)))
                .thenReturn(expectedList);

        List<TurConnectorIndexingModel> result = service.getBySourceAndProvider(source, provider);

        assertThat(result).isEqualTo(expectedList);
    }

    @Test
    void testGetConnectorIndexing() {
        TurConnectorIndexingModel model = mock(TurConnectorIndexingModel.class);
        
        when(model.getChecksum()).thenReturn("test-checksum");
        when(model.getCreated()).thenReturn(new Date());
        when(model.getEnvironment()).thenReturn("test-env");
        when(model.getId()).thenReturn(1);
        when(model.getLocale()).thenReturn(Locale.ENGLISH);
        when(model.getModificationDate()).thenReturn(new Date());
        when(model.getSource()).thenReturn("test-source");
        when(model.getObjectId()).thenReturn("test-object-id");
        when(model.getSites()).thenReturn(Arrays.asList("site1"));
        when(model.getStatus()).thenReturn(TurIndexingStatus.INDEXED);
        when(model.getTransactionId()).thenReturn("test-transaction-id");

        TurConnectorIndexing result = service.getConnectorIndexing(model);

        assertThat(result).isNotNull();
        assertThat(result.getChecksum()).isEqualTo("test-checksum");
        assertThat(result.getEnvironment()).isEqualTo("test-env");
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getLocale()).isEqualTo(Locale.ENGLISH);
        assertThat(result.getSource()).isEqualTo("test-source");
        assertThat(result.getObjectId()).isEqualTo("test-object-id");
        assertThat(result.getSites()).containsExactly("site1");
        assertThat(result.getStatus()).isEqualTo(TurIndexingStatus.INDEXED);
        assertThat(result.getTransactionId()).isEqualTo("test-transaction-id");
    }

    @Test
    void testGetIndexingItem() {
        String objectId = "test-object-id";
        String source = "test-source";
        String provider = "test-provider";
        
        TurConnectorIndexingModel model = mock(TurConnectorIndexingModel.class);
        when(model.getId()).thenReturn(1);
        when(model.getObjectId()).thenReturn(objectId);
        when(model.getSource()).thenReturn(source);
        when(model.getChecksum()).thenReturn("checksum");
        when(model.getEnvironment()).thenReturn("env");
        when(model.getLocale()).thenReturn(Locale.ENGLISH);
        when(model.getCreated()).thenReturn(new Date());
        when(model.getModificationDate()).thenReturn(new Date());
        when(model.getSites()).thenReturn(Arrays.asList("site1"));
        when(model.getStatus()).thenReturn(TurIndexingStatus.INDEXED);
        when(model.getTransactionId()).thenReturn("transaction-id");

        when(repository.findByObjectIdAndSourceAndProvider(objectId, source, provider))
                .thenReturn(Arrays.asList(model));

        List<TurConnectorIndexing> result = service.getIndexingItem(objectId, source, provider);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getObjectId()).isEqualTo(objectId);
        assertThat(result.get(0).getSource()).isEqualTo(source);
    }
}