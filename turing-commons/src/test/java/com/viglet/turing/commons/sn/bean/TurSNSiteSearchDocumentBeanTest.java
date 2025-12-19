/*
 * Copyright (C) 2016-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viglet.turing.commons.sn.bean;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurSNSiteSearchDocumentBean.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurSNSiteSearchDocumentBeanTest {

    @Test
    void testBuilderPattern() {
        List<TurSNSiteSearchDocumentMetadataBean> metadata = Arrays.asList(
                TurSNSiteSearchDocumentMetadataBean.builder().build()
        );
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "Test Document");
        fields.put("content", "Test content");
        
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder()
                .source("test-source")
                .elevate(true)
                .metadata(metadata)
                .fields(fields)
                .build();
        
        assertThat(document.getSource()).isEqualTo("test-source");
        assertThat(document.isElevate()).isTrue();
        assertThat(document.getMetadata()).isEqualTo(metadata);
        assertThat(document.getFields()).isEqualTo(fields);
    }

    @Test
    void testSettersAndGetters() {
        // Use builder since there's no default constructor
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        List<TurSNSiteSearchDocumentMetadataBean> metadata = new ArrayList<>();
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "123");
        fields.put("score", 0.95);
        
        document.setSource("web-source");
        document.setElevate(false);
        document.setMetadata(metadata);
        document.setFields(fields);
        
        assertThat(document.getSource()).isEqualTo("web-source");
        assertThat(document.isElevate()).isFalse();
        assertThat(document.getMetadata()).isEqualTo(metadata);
        assertThat(document.getFields()).isEqualTo(fields);
    }

    @Test
    void testDefaultConstructor() {
        // Use builder since there's no default constructor
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        assertThat(document.getSource()).isNull();
        assertThat(document.isElevate()).isFalse(); // default boolean value
        assertThat(document.getMetadata()).isNull();
        assertThat(document.getFields()).isNull();
    }

    @Test
    void testBuilderWithNullValues() {
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder()
                .source(null)
                .elevate(false)
                .metadata(null)
                .fields(null)
                .build();
        
        assertThat(document.getSource()).isNull();
        assertThat(document.isElevate()).isFalse();
        assertThat(document.getMetadata()).isNull();
        assertThat(document.getFields()).isNull();
    }

    @Test
    void testImplementsSerializable() {
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        assertThat(document).isInstanceOf(Serializable.class);
    }

    @Test
    void testFieldsManipulation() {
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        Map<String, Object> fields = new HashMap<>();
        document.setFields(fields);
        
        // Add fields
        document.getFields().put("title", "Document Title");
        document.getFields().put("author", "John Doe");
        document.getFields().put("date", new Date());
        
        assertThat(document.getFields()).hasSize(3);
        assertThat(document.getFields()).containsKey("title");
        assertThat(document.getFields()).containsKey("author");
        assertThat(document.getFields()).containsKey("date");
        assertThat(document.getFields()).containsEntry("title", "Document Title");
    }

    @Test
    void testMetadataManipulation() {
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        List<TurSNSiteSearchDocumentMetadataBean> metadata = new ArrayList<>();
        document.setMetadata(metadata);
        
        // Add metadata
        TurSNSiteSearchDocumentMetadataBean meta1 = TurSNSiteSearchDocumentMetadataBean.builder().build();
        TurSNSiteSearchDocumentMetadataBean meta2 = TurSNSiteSearchDocumentMetadataBean.builder().build();
        
        document.getMetadata().add(meta1);
        document.getMetadata().add(meta2);
        
        assertThat(document.getMetadata()).hasSize(2);
        assertThat(document.getMetadata()).contains(meta1, meta2);
    }

    @Test
    void testElevateToggle() {
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        // Default should be false
        assertThat(document.isElevate()).isFalse();
        
        // Set to true
        document.setElevate(true);
        assertThat(document.isElevate()).isTrue();
        
        // Set back to false
        document.setElevate(false);
        assertThat(document.isElevate()).isFalse();
    }

    @Test
    void testComplexFieldTypes() {
        TurSNSiteSearchDocumentBean document = TurSNSiteSearchDocumentBean.builder().build();
        
        Map<String, Object> fields = new HashMap<>();
        
        // Test various field types
        fields.put("stringField", "text value");
        fields.put("intField", 42);
        fields.put("doubleField", 3.14);
        fields.put("booleanField", true);
        fields.put("listField", Arrays.asList("item1", "item2", "item3"));
        fields.put("mapField", Collections.singletonMap("nested", "value"));
        
        document.setFields(fields);
        assertThat(document.getFields())
                .containsEntry("stringField", "text value")
                .containsEntry("intField", 42)
                .containsEntry("doubleField", 3.14)
                .containsEntry("booleanField",true);
        assertThat(document.getFields().get("listField")).isInstanceOf(List.class);
        assertThat(document.getFields().get("mapField")).isInstanceOf(Map.class);
    }
}