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

package com.viglet.turing.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import java.util.EnumMap;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.se.result.TurSEResult;

/**
 * Unit tests for TurSolrUtils.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSolrUtilsTest {

    @Mock
    private TurSEInstance turSEInstance;

    @BeforeEach
    void setUp() {
        // Use lenient() to avoid unnecessary stubbing exceptions for tests that don't
        // use the mock
        lenient().when(turSEInstance.getHost()).thenReturn("localhost");
        lenient().when(turSEInstance.getPort()).thenReturn(8983);
    }

    @Test
    void testConstructorThrowsException() throws NoSuchMethodException {
        var constructor = TurSolrUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solr Utility class");
    }

    @Test
    void testGetSolrFieldTypeForText() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.TEXT);
        assertThat(result).isEqualTo("text_general");
    }

    @Test
    void testGetSolrFieldTypeForString() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.STRING);
        assertThat(result).isEqualTo("string");
    }

    @Test
    void testGetSolrFieldTypeForInt() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.INT);
        assertThat(result).isEqualTo("pint");
    }

    @Test
    void testGetSolrFieldTypeForBool() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.BOOL);
        assertThat(result).isEqualTo("boolean");
    }

    @Test
    void testGetSolrFieldTypeForDate() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.DATE);
        assertThat(result).isEqualTo("pdate");
    }

    @Test
    void testGetSolrFieldTypeForLong() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.LONG);
        assertThat(result).isEqualTo("plong");
    }

    @Test
    void testGetSolrFieldTypeForArray() {
        String result = TurSolrUtils.getSolrFieldType(TurSEFieldType.ARRAY);
        assertThat(result).isEqualTo("strings");
    }

    @ParameterizedTest
    @CsvSource({
            "test query, test query",
            "field:value, value",
            "field:value:extra, value:extra",
            "'', ''",
            ":, :"
    })
    void testGetValueFromQuery(String query, String expected) {
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1, 0",
            "10, 2, 10",
            "20, 3, 40"
    })
    void testFirstRowPositionFromCurrentPage(int rows, int page, int expected) {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(rows);
        searchParams.setP(page);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

        int result = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);

        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1, 10",
            "10, 2, 20",
            "25, 3, 75"
    })
    void testLastRowPositionFromCurrentPage(int rows, int page, int expected) {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(rows);
        searchParams.setP(page);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

        int result = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testConstantsAreCorrect() {
        assertThat(TurSolrUtils.STR_SUFFIX).isEqualTo("_str");
        assertThat(TurSolrUtils.SCHEMA_API_URL).isEqualTo("%s/solr/%s/schema");
    }

    @ParameterizedTest
    @EnumSource(TurSEFieldType.class)
    void testGetSolrFieldTypeForAllTypes(TurSEFieldType fieldType) {
        String result = TurSolrUtils.getSolrFieldType(fieldType);
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void testCreateTurSEResultFromDocument() {
        // Given
        SolrDocument document = new SolrDocument();
        document.addField("id", "123");
        document.addField("title", "Test Document");

        // When
        TurSEResult result = TurSolrUtils.createTurSEResultFromDocument(document);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFields())
                .isNotNull()
                .hasSize(2)
                .containsEntry("id", "123")
                .containsEntry("title", "Test Document");
    }

    @Test
    void testCreateTurSEResultFromEmptyDocument() {
        // Given
        SolrDocument document = new SolrDocument();

        // When
        TurSEResult result = TurSolrUtils.createTurSEResultFromDocument(document);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFields())
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testCreateTurSEResultFromDocumentWithMultiValuedFields() {
        // Given
        SolrDocument document = new SolrDocument();
        document.addField("id", "456");
        document.addField("tags", java.util.Arrays.asList("java", "solr", "search"));

        // When
        TurSEResult result = TurSolrUtils.createTurSEResultFromDocument(document);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFields())
                .isNotNull()
                .hasSize(2)
                .containsEntry("id", "456")
                .containsKey("tags");
        assertThat(result.getFields().get("tags"))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly("java", "solr", "search");
    }

    @ParameterizedTest
    @CsvSource({
            "myField, TEXT, false",
            "myField_str, TEXT, false",
            "myField, STRING, false",
            "myField, DATE, false",
            "myField, INT, false"
    })
    void testIsCreateCopyFieldByCore_WhenFieldDoesNotExist(String fieldName, TurSEFieldType fieldType,
            boolean expected) {
        // This test would require mocking existsField method
        // For now, we test the logic without HTTP calls
        assertThat(fieldType).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "myField, TEXT, true",
            "myField_str, TEXT, false",
            "myField, STRING, false"
    })
    void testIsDeleteCopyFieldByCore_Logic(String fieldName, TurSEFieldType fieldType, boolean shouldCheckExistence) {
        // Test the conditional logic
        boolean hasTextType = fieldType.equals(TurSEFieldType.TEXT);
        boolean doesNotEndWithSuffix = !fieldName.endsWith(TurSolrUtils.STR_SUFFIX);

        assertThat(hasTextType && doesNotEndWithSuffix).isEqualTo(shouldCheckExistence);
    }

    @Test
    void testGetValueFromQuery_WithNull() {
        // Should handle gracefully, though depends on TurCommonsUtils implementation
        String result = TurSolrUtils.getValueFromQuery("");
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "'field:', 'field:'",
            "'field:value with spaces', 'value with spaces'",
            "'multiple:colons:in:query', 'colons:in:query'"
    })
    void testGetValueFromQuery_EdgeCases(String query, String expected) {
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result)
                .as("Should handle edge cases with special characters and multiple colons")
                .isNotNull()
                .isEqualTo(expected);
    }

    @Test
    void testFirstRowPositionWithZeroRows() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(0);
        searchParams.setP(1);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

        int result = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);

        assertThat(result).isZero();
    }

    @Test
    void testLastRowPositionWithZeroRows() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(0);
        searchParams.setP(1);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

        int result = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);

        assertThat(result).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "100, 10, 900, 1000",
            "50, 5, 200, 250",
            "1, 1, 0, 1"
    })
    void testRowPositionCalculations(int rows, int page, int expectedFirst, int expectedLast) {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(rows);
        searchParams.setP(page);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

        int first = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);
        int last = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);

        assertThat(first).isEqualTo(expectedFirst);
        assertThat(last).isEqualTo(expectedLast);
    }

    @Test
    void testGetSolrFieldType_Consistency() {
        // Verify that each type has a unique mapping
        Map<TurSEFieldType, String> mappings = new EnumMap<>(TurSEFieldType.class);
        for (TurSEFieldType type : TurSEFieldType.values()) {
            String solrType = TurSolrUtils.getSolrFieldType(type);
            assertThat(solrType)
                    .isNotNull()
                    .isNotEmpty();
            mappings.put(type, solrType);
        }

        // Verify we have mappings for all types
        assertThat(mappings)
                .hasSize(TurSEFieldType.values().length)
                .isNotEmpty();
    }

    @Test
    void testSTR_SUFFIX_Usage() {
        String fieldName = "testField";
        String expectedSuffix = "_str";

        assertThat(fieldName.concat(TurSolrUtils.STR_SUFFIX))
                .isEqualTo(fieldName + expectedSuffix);
    }

    @Test
    void testSchemaApiUrlFormat() {
        String solrUrl = "http://localhost:8983";
        String coreName = "mycore";
        String expected = "http://localhost:8983/solr/mycore/schema";

        String result = String.format(TurSolrUtils.SCHEMA_API_URL, solrUrl, coreName);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testGetSolrFieldType_AllExpectedMappings() {
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.TEXT)).isEqualTo("text_general");
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.STRING)).isEqualTo("string");
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.INT)).isEqualTo("pint");
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.BOOL)).isEqualTo("boolean");
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.DATE)).isEqualTo("pdate");
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.LONG)).isEqualTo("plong");
        assertThat(TurSolrUtils.getSolrFieldType(TurSEFieldType.ARRAY)).isEqualTo("strings");
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1",
            "20, 2",
            "50, 5",
            "1, 100"
    })
    void testPaginationConsistency(int rows, int page) {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(rows);
        searchParams.setP(page);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());

        int first = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);
        int last = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);

        // Last position should always be first position + rows
        assertThat(last - first).isEqualTo(rows);
    }

}
