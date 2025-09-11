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

package com.viglet.turing.client.sn;

import com.viglet.turing.commons.sn.bean.TurSNSiteSearchDocumentBean;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurSNDocument.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurSNDocumentTest {

    @Test
    void testDefaultConstructor() {
        TurSNDocument document = new TurSNDocument();
        
        assertThat(document.getContent()).isNull();
    }

    @Test
    void testSetAndGetContent() {
        TurSNDocument document = new TurSNDocument();
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder().build();
        
        document.setContent(bean);
        
        assertThat(document.getContent()).isEqualTo(bean);
    }

    @Test
    void testGetFieldValueWithNullContent() {
        TurSNDocument document = new TurSNDocument();
        
        Object value = document.getFieldValue("title");
        
        assertThat(value).isNull();
    }

    @Test
    void testGetFieldValueWithNullFields() {
        TurSNDocument document = new TurSNDocument();
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(null)
                .build();
        
        document.setContent(bean);
        
        Object value = document.getFieldValue("title");
        
        assertThat(value).isNull();
    }

    @Test
    void testGetFieldValueWithEmptyFields() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        Object value = document.getFieldValue("title");
        
        assertThat(value).isNull();
    }

    @Test
    void testGetFieldValueWithExistingField() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "Test Document Title");
        fields.put("content", "Test document content");
        fields.put("id", 123);
        
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        assertThat(document.getFieldValue("title")).isEqualTo("Test Document Title");
        assertThat(document.getFieldValue("content")).isEqualTo("Test document content");
        assertThat(document.getFieldValue("id")).isEqualTo(123);
    }

    @Test
    void testGetFieldValueWithNonExistentField() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "Test Document Title");
        
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        Object value = document.getFieldValue("nonexistent");
        
        assertThat(value).isNull();
    }

    @Test
    void testGetFieldValueWithNullValue() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "Test Document Title");
        fields.put("description", null);
        
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        assertThat(document.getFieldValue("title")).isEqualTo("Test Document Title");
        assertThat(document.getFieldValue("description")).isNull();
    }

    @Test
    void testGetFieldValueWithComplexTypes() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        
        // Add various data types
        fields.put("stringField", "string value");
        fields.put("intField", 42);
        fields.put("doubleField", 3.14159);
        fields.put("booleanField", true);
        fields.put("arrayField", new String[]{"item1", "item2", "item3"});
        
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        assertThat(document.getFieldValue("stringField")).isEqualTo("string value");
        assertThat(document.getFieldValue("intField")).isEqualTo(42);
        assertThat(document.getFieldValue("doubleField")).isEqualTo(3.14159);
        assertThat(document.getFieldValue("booleanField")).isEqualTo(true);
        assertThat(document.getFieldValue("arrayField")).isInstanceOf(String[].class);
    }

    @Test
    void testSetContentOverwrite() {
        TurSNDocument document = new TurSNDocument();
        
        // Set initial content
        Map<String, Object> fields1 = new HashMap<>();
        fields1.put("title", "First Title");
        TurSNSiteSearchDocumentBean bean1 = TurSNSiteSearchDocumentBean.builder()
                .fields(fields1)
                .build();
        
        document.setContent(bean1);
        assertThat(document.getFieldValue("title")).isEqualTo("First Title");
        
        // Overwrite with new content
        Map<String, Object> fields2 = new HashMap<>();
        fields2.put("title", "Second Title");
        TurSNSiteSearchDocumentBean bean2 = TurSNSiteSearchDocumentBean.builder()
                .fields(fields2)
                .build();
        
        document.setContent(bean2);
        assertThat(document.getFieldValue("title")).isEqualTo("Second Title");
        assertThat(document.getContent()).isEqualTo(bean2);
    }

    @Test
    void testSetContentToNull() {
        TurSNDocument document = new TurSNDocument();
        
        // Set initial content
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "Test Title");
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        assertThat(document.getContent()).isNotNull();
        assertThat(document.getFieldValue("title")).isEqualTo("Test Title");
        
        // Set content to null
        document.setContent(null);
        assertThat(document.getContent()).isNull();
        assertThat(document.getFieldValue("title")).isNull();
    }

    @Test
    void testFieldCaseSensitivity() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", "Test Title");
        fields.put("Title", "Different Title");
        fields.put("TITLE", "Another Title");
        
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        // Field names should be case-sensitive
        assertThat(document.getFieldValue("title")).isEqualTo("Test Title");
        assertThat(document.getFieldValue("Title")).isEqualTo("Different Title");
        assertThat(document.getFieldValue("TITLE")).isEqualTo("Another Title");
    }

    @Test
    void testEmptyStringFieldName() {
        TurSNDocument document = new TurSNDocument();
        Map<String, Object> fields = new HashMap<>();
        fields.put("", "Empty key value");
        fields.put("normal", "Normal value");
        
        TurSNSiteSearchDocumentBean bean = TurSNSiteSearchDocumentBean.builder()
                .fields(fields)
                .build();
        
        document.setContent(bean);
        
        assertThat(document.getFieldValue("")).isEqualTo("Empty key value");
        assertThat(document.getFieldValue("normal")).isEqualTo("Normal value");
    }
}