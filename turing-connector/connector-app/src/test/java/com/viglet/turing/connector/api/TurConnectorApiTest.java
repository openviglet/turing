/*
 * Copyright (C) 2016-2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */
package com.viglet.turing.connector.api;

import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
import com.viglet.turing.connector.domain.TurConnectorValidateDifference;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.service.TurConnectorIndexingService;
import com.viglet.turing.connector.service.TurConnectorSolrService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TurConnectorApi.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@ExtendWith(MockitoExtension.class)
class TurConnectorApiTest {

    @Mock
    private TurConnectorIndexingService indexingService;

    @Mock
    private TurConnectorSolrService turConnectorSolr;

    @Mock
    private TurConnectorPlugin plugin;

    @InjectMocks
    private TurConnectorApi api;

    @Test
    void testStatus() {
        Map<String, String> result = api.status();

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("status", "ok");
        assertThat(result).hasSize(1);
    }

    @Test
    void testValidateSource() {
        String source = "test-source";
        String providerName = "test-provider";
        List<String> missingContent = Arrays.asList("missing1", "missing2");
        List<String> extraContent = Arrays.asList("extra1", "extra2");

        when(plugin.getProviderName()).thenReturn(providerName);
        when(turConnectorSolr.solrMissingContent(source, providerName)).thenReturn(missingContent);
        when(turConnectorSolr.solrExtraContent(source, providerName)).thenReturn(extraContent);

        TurConnectorValidateDifference result = api.validateSource(source);

        assertThat(result).isNotNull();
        assertThat(result.getMissing()).isEqualTo(missingContent);
        assertThat(result.getExtra()).isEqualTo(extraContent);
        
        verify(plugin, times(1)).getProviderName();
        verify(turConnectorSolr, times(1)).solrMissingContent(source, providerName);
        verify(turConnectorSolr, times(1)).solrExtraContent(source, providerName);
    }

    @Test
    void testValidateSourceWithEmptyResults() {
        String source = "empty-source";
        String providerName = "test-provider";
        List<String> emptyList = Collections.emptyList();

        when(plugin.getProviderName()).thenReturn(providerName);
        when(turConnectorSolr.solrMissingContent(source, providerName)).thenReturn(emptyList);
        when(turConnectorSolr.solrExtraContent(source, providerName)).thenReturn(emptyList);

        TurConnectorValidateDifference result = api.validateSource(source);

        assertThat(result).isNotNull();
        assertThat(result.getMissing()).isEmpty();
        assertThat(result.getExtra()).isEmpty();
    }

    @Test
    void testMonitoryIndexByNameWithResults() {
        String source = "test-source";
        String providerName = "test-provider";
        List<TurConnectorIndexingModel> indexingModels = Arrays.asList(
                mock(TurConnectorIndexingModel.class),
                mock(TurConnectorIndexingModel.class)
        );

        when(plugin.getProviderName()).thenReturn(providerName);
        when(indexingService.getBySourceAndProvider(source, providerName)).thenReturn(indexingModels);

        ResponseEntity<List<TurConnectorIndexingModel>> result = api.monitoryIndexByName(source);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(indexingModels);
        assertThat(result.getBody()).hasSize(2);
        
        verify(plugin, times(1)).getProviderName();
        verify(indexingService, times(1)).getBySourceAndProvider(source, providerName);
    }

    @Test
    void testMonitoryIndexByNameWithEmptyResults() {
        String source = "empty-source";
        String providerName = "test-provider";
        List<TurConnectorIndexingModel> emptyList = Collections.emptyList();

        when(plugin.getProviderName()).thenReturn(providerName);
        when(indexingService.getBySourceAndProvider(source, providerName)).thenReturn(emptyList);

        ResponseEntity<List<TurConnectorIndexingModel>> result = api.monitoryIndexByName(source);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNull();
        
        verify(plugin, times(1)).getProviderName();
        verify(indexingService, times(1)).getBySourceAndProvider(source, providerName);
    }

    @Test
    void testIndexAll() {
        String name = "test-name";
        
        doNothing().when(plugin).indexAll(name);

        ResponseEntity<Map<String, String>> result = api.indexAll(name);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).containsEntry("status", "sent");
        
        verify(plugin, times(1)).indexAll(name);
    }

    @Test
    void testIndexContentId() {
        String name = "test-name";
        List<String> contentIds = Arrays.asList("id1", "id2", "id3");
        
        doNothing().when(plugin).indexById(name, contentIds);

        ResponseEntity<Map<String, String>> result = api.indexContentId(name, contentIds);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).containsEntry("status", "sent");
        
        verify(plugin, times(1)).indexById(name, contentIds);
    }

    @Test
    void testIndexContentIdWithEmptyList() {
        String name = "test-name";
        List<String> emptyContentIds = Collections.emptyList();
        
        doNothing().when(plugin).indexById(name, emptyContentIds);

        ResponseEntity<Map<String, String>> result = api.indexContentId(name, emptyContentIds);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("status", "sent");
        
        verify(plugin, times(1)).indexById(name, emptyContentIds);
    }

    @Test
    void testReindexAll() {
        String name = "test-name";
        String providerName = "test-provider";
        
        when(plugin.getProviderName()).thenReturn(providerName);
        doNothing().when(indexingService).deleteByProviderAndSource(providerName, name);
        doNothing().when(plugin).indexAll(name);

        ResponseEntity<Map<String, String>> result = api.reindexAll(name);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).containsEntry("status", "sent");
        
        verify(plugin, times(1)).getProviderName();
        verify(indexingService, times(1)).deleteByProviderAndSource(providerName, name);
        verify(plugin, times(1)).indexAll(name);
    }

    @Test
    void testReindexSpecificContent() {
        String name = "test-name";
        String providerName = "test-provider";
        List<String> contentIds = Arrays.asList("id1", "id2");
        
        when(plugin.getProviderName()).thenReturn(providerName);
        doNothing().when(indexingService).deleteByProviderAndSourceAndObjectIdIn(providerName, name, contentIds);
        doNothing().when(plugin).indexById(name, contentIds);

        ResponseEntity<Map<String, String>> result = api.reindexAll(name, contentIds);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("status", "sent");
        
        verify(plugin, times(1)).getProviderName();
        verify(indexingService, times(1)).deleteByProviderAndSourceAndObjectIdIn(providerName, name, contentIds);
        verify(plugin, times(1)).indexById(name, contentIds);
    }

    @Test
    void testApiClassAnnotations() {
        // Test that the class has expected annotations
        assertThat(TurConnectorApi.class.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class))
                .isTrue();
        assertThat(TurConnectorApi.class.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class))
                .isTrue();
    }

    @Test
    void testStatusMethodAnnotation() throws NoSuchMethodException {
        assertThat(TurConnectorApi.class.getMethod("status")
                .isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class))
                .isTrue();
    }

    @Test
    void testValidateSourceMethodAnnotation() throws NoSuchMethodException {
        assertThat(TurConnectorApi.class.getMethod("validateSource", String.class)
                .isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class))
                .isTrue();
    }

    @Test
    void testIndexAllMethodAnnotation() throws NoSuchMethodException {
        assertThat(TurConnectorApi.class.getMethod("indexAll", String.class)
                .isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class))
                .isTrue();
    }

    @Test
    void testIndexContentIdMethodAnnotation() throws NoSuchMethodException {
        assertThat(TurConnectorApi.class.getMethod("indexContentId", String.class, List.class)
                .isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class))
                .isTrue();
    }

    @Test
    void testConstructorInitializesFields() {
        TurConnectorIndexingService mockIndexingService = mock(TurConnectorIndexingService.class);
        TurConnectorSolrService mockSolrService = mock(TurConnectorSolrService.class);
        TurConnectorPlugin mockPlugin = mock(TurConnectorPlugin.class);

        TurConnectorApi newApi = new TurConnectorApi(mockIndexingService, mockSolrService, mockPlugin);

        assertThat(newApi).isNotNull();
        // We can't access private fields directly, but we can test behavior
        Map<String, String> status = newApi.status();
        assertThat(status).containsEntry("status", "ok");
    }

    @Test
    void testStatusSentResponse() {
        String name = "test-name";
        doNothing().when(plugin).indexAll(name);

        ResponseEntity<Map<String, String>> result = api.indexAll(name);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody()).containsKey("status");
        assertThat(result.getBody().get("status")).isEqualTo("sent");
    }

    @Test
    void testMultipleOperationsFlow() {
        String source = "integration-source";
        String providerName = "integration-provider";
        List<String> contentIds = Arrays.asList("content1", "content2");

        when(plugin.getProviderName()).thenReturn(providerName);
        
        // Test complete flow: status -> validate -> index -> reindex
        Map<String, String> status = api.status();
        assertThat(status).containsEntry("status", "ok");

        when(turConnectorSolr.solrMissingContent(source, providerName)).thenReturn(Collections.emptyList());
        when(turConnectorSolr.solrExtraContent(source, providerName)).thenReturn(Collections.emptyList());
        
        TurConnectorValidateDifference validation = api.validateSource(source);
        assertThat(validation.getMissing()).isEmpty();
        assertThat(validation.getExtra()).isEmpty();

        doNothing().when(plugin).indexById(source, contentIds);
        ResponseEntity<Map<String, String>> indexResponse = api.indexContentId(source, contentIds);
        assertThat(indexResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        doNothing().when(indexingService).deleteByProviderAndSourceAndObjectIdIn(providerName, source, contentIds);
        ResponseEntity<Map<String, String>> reindexResponse = api.reindexAll(source, contentIds);
        assertThat(reindexResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify all interactions happened
        verify(plugin, times(3)).getProviderName(); // Called in validate, and both reindex operations
        verify(plugin, times(2)).indexById(source, contentIds); // Called in index and reindex
        verify(indexingService, times(1)).deleteByProviderAndSourceAndObjectIdIn(providerName, source, contentIds);
    }
}