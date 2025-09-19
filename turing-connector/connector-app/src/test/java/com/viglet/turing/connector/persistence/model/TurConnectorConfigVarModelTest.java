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
package com.viglet.turing.connector.persistence.model;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurConnectorConfigVarModel.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurConnectorConfigVarModelTest {

    @Test
    void testDefaultConstructor() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();

        assertThat(model.getId()).isNull();
        assertThat(model.getPath()).isNull();
        assertThat(model.getValue()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        String id = "TEST_CONFIG";
        String path = "/system/config";
        String value = "test-value";

        model.setId(id);
        model.setPath(path);
        model.setValue(value);

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getPath()).isEqualTo(path);
        assertThat(model.getValue()).isEqualTo(value);
    }

    @Test
    void testWithFirstTimeConfiguration() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        model.setId("FIRST_TIME");
        model.setPath("/system");
        model.setValue("true");

        assertThat(model.getId()).isEqualTo("FIRST_TIME");
        assertThat(model.getPath()).isEqualTo("/system");
        assertThat(model.getValue()).isEqualTo("true");
    }

    @Test
    void testWithCustomConfiguration() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        model.setId("CUSTOM_SETTING");
        model.setPath("/application/settings");
        model.setValue("custom-configuration-value");

        assertThat(model.getId()).isEqualTo("CUSTOM_SETTING");
        assertThat(model.getPath()).isEqualTo("/application/settings");
        assertThat(model.getValue()).isEqualTo("custom-configuration-value");
    }

    @Test
    void testWithNullValues() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        model.setId(null);
        model.setPath(null);
        model.setValue(null);

        assertThat(model.getId()).isNull();
        assertThat(model.getPath()).isNull();
        assertThat(model.getValue()).isNull();
    }

    @Test
    void testWithEmptyValues() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        model.setId("");
        model.setPath("");
        model.setValue("");

        assertThat(model.getId()).isEqualTo("");
        assertThat(model.getPath()).isEqualTo("");
        assertThat(model.getValue()).isEqualTo("");
    }

    @Test
    void testSerializableInterface() {
        assertThat(TurConnectorConfigVarModel.class).isAssignableTo(Serializable.class);
    }

    @Test
    void testSerialVersionUID() throws NoSuchFieldException {
        assertThat(TurConnectorConfigVarModel.class.getDeclaredField("serialVersionUID"))
                .isNotNull();
    }

    @Test
    void testEntityAnnotation() {
        assertThat(TurConnectorConfigVarModel.class
                .isAnnotationPresent(jakarta.persistence.Entity.class))
                .isTrue();
    }

    @Test
    void testTableAnnotation() {
        assertThat(TurConnectorConfigVarModel.class
                .isAnnotationPresent(jakarta.persistence.Table.class))
                .isTrue();

        jakarta.persistence.Table tableAnnotation = 
                TurConnectorConfigVarModel.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(tableAnnotation.name()).isEqualTo("con_config");
    }

    @Test
    void testIdFieldAnnotation() throws NoSuchFieldException {
        assertThat(TurConnectorConfigVarModel.class.getDeclaredField("id")
                .isAnnotationPresent(jakarta.persistence.Id.class))
                .isTrue();
        
        assertThat(TurConnectorConfigVarModel.class.getDeclaredField("id")
                .isAnnotationPresent(jakarta.persistence.Column.class))
                .isTrue();
    }

    @Test
    void testPathFieldAnnotation() throws NoSuchFieldException {
        assertThat(TurConnectorConfigVarModel.class.getDeclaredField("path")
                .isAnnotationPresent(jakarta.persistence.Column.class))
                .isTrue();
    }

    @Test
    void testValueFieldAnnotation() throws NoSuchFieldException {
        assertThat(TurConnectorConfigVarModel.class.getDeclaredField("value")
                .isAnnotationPresent(jakarta.persistence.Column.class))
                .isTrue();
    }

    @Test
    void testIdConstraints() throws NoSuchFieldException {
        jakarta.persistence.Column columnAnnotation = 
                TurConnectorConfigVarModel.class.getDeclaredField("id")
                        .getAnnotation(jakarta.persistence.Column.class);
        
        assertThat(columnAnnotation.unique()).isTrue();
        assertThat(columnAnnotation.nullable()).isFalse();
        assertThat(columnAnnotation.length()).isEqualTo(25);
    }

    @Test
    void testLombokAnnotations() {
        assertThat(TurConnectorConfigVarModel.class
                .isAnnotationPresent(lombok.Getter.class))
                .isTrue();
        
        assertThat(TurConnectorConfigVarModel.class
                .isAnnotationPresent(lombok.Setter.class))
                .isTrue();
    }

    @Test
    void testFieldModification() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        // Set initial values
        model.setId("INITIAL");
        model.setPath("/initial");
        model.setValue("initial-value");
        
        // Verify initial state
        assertThat(model.getId()).isEqualTo("INITIAL");
        assertThat(model.getPath()).isEqualTo("/initial");
        assertThat(model.getValue()).isEqualTo("initial-value");
        
        // Modify values
        model.setId("MODIFIED");
        model.setPath("/modified");
        model.setValue("modified-value");
        
        // Verify modified state
        assertThat(model.getId()).isEqualTo("MODIFIED");
        assertThat(model.getPath()).isEqualTo("/modified");
        assertThat(model.getValue()).isEqualTo("modified-value");
    }

    @Test
    void testIdLengthConstraint() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        // Test with maximum length (25 characters)
        String maxLengthId = "A".repeat(25);
        model.setId(maxLengthId);
        
        assertThat(model.getId()).isEqualTo(maxLengthId);
        assertThat(model.getId()).hasSize(25);
        
        // Test with longer string (this should still work at model level, 
        // but would fail at database level)
        String longerId = "A".repeat(30);
        model.setId(longerId);
        
        assertThat(model.getId()).isEqualTo(longerId);
        assertThat(model.getId()).hasSize(30);
    }

    @Test
    void testTypicalConfigurationScenarios() {
        // Test different types of configuration variables
        
        // Boolean configuration
        TurConnectorConfigVarModel booleanConfig = new TurConnectorConfigVarModel();
        booleanConfig.setId("FEATURE_ENABLED");
        booleanConfig.setPath("/features");
        booleanConfig.setValue("true");
        
        assertThat(booleanConfig.getValue()).isEqualTo("true");
        
        // Numeric configuration
        TurConnectorConfigVarModel numericConfig = new TurConnectorConfigVarModel();
        numericConfig.setId("MAX_CONNECTIONS");
        numericConfig.setPath("/system/limits");
        numericConfig.setValue("100");
        
        assertThat(numericConfig.getValue()).isEqualTo("100");
        
        // String configuration
        TurConnectorConfigVarModel stringConfig = new TurConnectorConfigVarModel();
        stringConfig.setId("DEFAULT_LOCALE");
        stringConfig.setPath("/i18n");
        stringConfig.setValue("en_US");
        
        assertThat(stringConfig.getValue()).isEqualTo("en_US");
    }

    @Test
    void testPathVariations() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        String[] paths = {
                "/system",
                "/application/settings",
                "/features/experimental",
                "/db/connection",
                "relative/path",
                "",
                "/"
        };
        
        for (String path : paths) {
            model.setPath(path);
            assertThat(model.getPath()).isEqualTo(path);
        }
    }

    @Test
    void testValueVariations() {
        TurConnectorConfigVarModel model = new TurConnectorConfigVarModel();
        
        String[] values = {
                "simple-value",
                "complex.value.with.dots",
                "value_with_underscores",
                "value-with-hyphens",
                "123456",
                "true",
                "false",
                "",
                "very long value with spaces and special characters !@#$%",
                "{\"json\": \"value\"}"
        };
        
        for (String value : values) {
            model.setValue(value);
            assertThat(model.getValue()).isEqualTo(value);
        }
    }
}