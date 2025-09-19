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

import com.viglet.turing.connector.persistence.model.TurConnectorConfigVarModel;
import com.viglet.turing.connector.persistence.repository.TurConnectorConfigVarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TurConnectorConfigVarService.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@ExtendWith(MockitoExtension.class)
class TurConnectorConfigVarServiceTest {

    @Mock
    private TurConnectorConfigVarRepository repository;

    @InjectMocks
    private TurConnectorConfigVarService service;

    @Test
    void testFirstTimeConstant() {
        assertThat(TurConnectorConfigVarService.FIRST_TIME).isEqualTo("FIRST_TIME");
    }

    @Test
    void testHasNotFirstTimeWhenFirstTimeRecordExists() {
        TurConnectorConfigVarModel firstTimeModel = new TurConnectorConfigVarModel();
        firstTimeModel.setId(TurConnectorConfigVarService.FIRST_TIME);
        
        when(repository.findById(TurConnectorConfigVarService.FIRST_TIME))
                .thenReturn(Optional.of(firstTimeModel));

        boolean result = service.hasNotFirstTime();

        assertThat(result).isFalse();
        verify(repository, times(1)).findById(TurConnectorConfigVarService.FIRST_TIME);
    }

    @Test
    void testHasNotFirstTimeWhenFirstTimeRecordDoesNotExist() {
        when(repository.findById(TurConnectorConfigVarService.FIRST_TIME))
                .thenReturn(Optional.empty());

        boolean result = service.hasNotFirstTime();

        assertThat(result).isTrue();
        verify(repository, times(1)).findById(TurConnectorConfigVarService.FIRST_TIME);
    }

    @Test
    void testSave() {
        TurConnectorConfigVarModel configVar = new TurConnectorConfigVarModel();
        configVar.setId("test-id");
        configVar.setPath("/test/path");
        configVar.setValue("test-value");

        when(repository.save(configVar)).thenReturn(configVar);

        service.save(configVar);

        verify(repository, times(1)).save(configVar);
    }

    @Test
    void testSaveWithNullModel() {
        service.save(null);

        verify(repository, times(1)).save(null);
    }

    @Test
    void testSaveFirstTime() {
        ArgumentCaptor<TurConnectorConfigVarModel> captor = 
                ArgumentCaptor.forClass(TurConnectorConfigVarModel.class);

        service.saveFirstTime();

        verify(repository, times(1)).save(captor.capture());
        
        TurConnectorConfigVarModel capturedModel = captor.getValue();
        assertThat(capturedModel.getId()).isEqualTo(TurConnectorConfigVarService.FIRST_TIME);
        assertThat(capturedModel.getPath()).isEqualTo("/system");
        assertThat(capturedModel.getValue()).isEqualTo("true");
    }

    @Test
    void testSaveFirstTimeCallsSaveMethod() {
        // Use spy to verify internal method call
        TurConnectorConfigVarService spyService = spy(service);
        doNothing().when(spyService).save(any(TurConnectorConfigVarModel.class));

        spyService.saveFirstTime();

        verify(spyService, times(1)).save(any(TurConnectorConfigVarModel.class));
    }

    @Test
    void testConstructorWithRepository() {
        TurConnectorConfigVarRepository mockRepo = mock(TurConnectorConfigVarRepository.class);
        
        TurConnectorConfigVarService newService = new TurConnectorConfigVarService(mockRepo);
        
        assertThat(newService).isNotNull();
        // We can't easily access the private field, but we can test behavior
        when(mockRepo.findById(TurConnectorConfigVarService.FIRST_TIME))
                .thenReturn(Optional.empty());
        
        boolean result = newService.hasNotFirstTime();
        assertThat(result).isTrue();
        verify(mockRepo, times(1)).findById(TurConnectorConfigVarService.FIRST_TIME);
    }

    @Test
    void testServiceIsAnnotatedWithService() {
        assertThat(TurConnectorConfigVarService.class
                .isAnnotationPresent(org.springframework.stereotype.Service.class))
                .isTrue();
    }

    @Test
    void testSaveFirstTimeCreatesCorrectModel() {
        ArgumentCaptor<TurConnectorConfigVarModel> captor = 
                ArgumentCaptor.forClass(TurConnectorConfigVarModel.class);

        service.saveFirstTime();

        verify(repository).save(captor.capture());
        TurConnectorConfigVarModel model = captor.getValue();

        assertThat(model).isNotNull();
        assertThat(model.getId()).isNotNull();
        assertThat(model.getPath()).isNotNull();
        assertThat(model.getValue()).isNotNull();
        assertThat(model.getId()).isEqualTo("FIRST_TIME");
        assertThat(model.getPath()).isEqualTo("/system");
        assertThat(model.getValue()).isEqualTo("true");
    }

    @Test
    void testMultipleSaveCalls() {
        TurConnectorConfigVarModel model1 = new TurConnectorConfigVarModel();
        model1.setId("id1");
        
        TurConnectorConfigVarModel model2 = new TurConnectorConfigVarModel();
        model2.setId("id2");

        service.save(model1);
        service.save(model2);

        verify(repository, times(2)).save(any(TurConnectorConfigVarModel.class));
        verify(repository, times(1)).save(model1);
        verify(repository, times(1)).save(model2);
    }

    @Test
    void testHasNotFirstTimeMultipleCalls() {
        when(repository.findById(TurConnectorConfigVarService.FIRST_TIME))
                .thenReturn(Optional.empty());

        boolean result1 = service.hasNotFirstTime();
        boolean result2 = service.hasNotFirstTime();

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        verify(repository, times(2)).findById(TurConnectorConfigVarService.FIRST_TIME);
    }

    @Test
    void testRepositoryInteractionPattern() {
        // Test typical usage pattern
        when(repository.findById(TurConnectorConfigVarService.FIRST_TIME))
                .thenReturn(Optional.empty());
        
        // Check if it's first time
        boolean isFirstTime = service.hasNotFirstTime();
        
        if (isFirstTime) {
            // Save first time configuration
            service.saveFirstTime();
        }

        assertThat(isFirstTime).isTrue();
        verify(repository, times(1)).findById(TurConnectorConfigVarService.FIRST_TIME);
        verify(repository, times(1)).save(any(TurConnectorConfigVarModel.class));
    }
}