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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.persistence.model.se.TurSEInstance;

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
        // Setup mock instance if needed for some tests
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

}
