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

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.se.result.TurSEResult;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

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
    void testConstructorThrowsException() {
        assertThatThrownBy(() -> {
            var constructor = TurSolrUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
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

    @Test
    void testGetValueFromQuerySimple() {
        String query = "test query";
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result).isEqualTo("test query");
    }

    @Test
    void testGetValueFromQueryWithColon() {
        String query = "field:value";
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result).isEqualTo("value");
    }

    @Test
    void testGetValueFromQueryWithMultipleColons() {
        String query = "field:value:extra";
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result).isEqualTo("value:extra");
    }

    @Test
    void testFirstRowPositionFromCurrentPageFirstPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(10);
        searchParams.setP(1);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());
        
        int result = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testFirstRowPositionFromCurrentPageSecondPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(10);
        searchParams.setP(2);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());
        
        int result = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);
        
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testFirstRowPositionFromCurrentPageThirdPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(20);
        searchParams.setP(3);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());
        
        int result = TurSolrUtils.firstRowPositionFromCurrentPage(parameters);
        
        assertThat(result).isEqualTo(40);
    }

    @Test
    void testLastRowPositionFromCurrentPageFirstPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(10);
        searchParams.setP(1);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());
        
        int result = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);
        
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testLastRowPositionFromCurrentPageSecondPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(10);
        searchParams.setP(2);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());
        
        int result = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);
        
        assertThat(result).isEqualTo(20);
    }

    @Test
    void testLastRowPositionFromCurrentPageThirdPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setRows(25);
        searchParams.setP(3);
        TurSEParameters parameters = new TurSEParameters(searchParams, new TurSNSitePostParamsBean());
        
        int result = TurSolrUtils.lastRowPositionFromCurrentPage(parameters);
        
        assertThat(result).isEqualTo(75);
    }

    @Test
    void testConstantsAreCorrect() {
        assertThat(TurSolrUtils.STR_SUFFIX).isEqualTo("_str");
        assertThat(TurSolrUtils.SCHEMA_API_URL).isEqualTo("%s/solr/%s/schema");
    }

    @Test
    void testGetValueFromQueryEmptyString() {
        String query = "";
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result).isEmpty();
    }

    @Test
    void testGetValueFromQueryOnlyColon() {
        String query = ":";
        String result = TurSolrUtils.getValueFromQuery(query);
        assertThat(result).isEqualTo(":");
    }
}
