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

package com.viglet.turing.searchengine.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.spi.TurSEIndexField;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

/**
 * Tests for TurSolrDocumentHandler.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@ExtendWith(MockitoExtension.class)
class TurSolrDocumentHandlerTest {

        private static final String LINE_SEPARATOR = System.lineSeparator();

        @Mock
        private TurSESiteSearchConfig siteSearchConfig;

        @Mock
        private HttpJdkSolrClient httpJdkSolrClient;

        private static final String SITE_ID = "site-1";

        @Mock
        private TurDecimalFieldNormalizer turDecimalFieldNormalizer;

        private TurSolrDocumentHandler turSolrDocumentHandler;
        private CapturingSolrClient capturingSolrClient;
        private TurSolrInstance turSolrInstance;

        @BeforeEach
        void setUp() throws MalformedURLException {
                turSolrDocumentHandler = new TurSolrDocumentHandler(1000, siteSearchConfig,
                                turDecimalFieldNormalizer);
                capturingSolrClient = new CapturingSolrClient();
                turSolrInstance = new TurSolrInstance(httpJdkSolrClient,
                                URI.create("http://localhost:8983/solr").toURL(),
                                "core");
                turSolrInstance.setSolrClient(capturingSolrClient);
        }

        @Test
        void testIndexingBuildsDocumentWithExpectedFields() {
                TurSEIndexField multiValuedField = TurSEIndexField.builder()
                                .name("tags")
                                .multiValued(true)
                                .build();
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("tags", multiValuedField);
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("title", new JSONArray(List.of("first", "second")));
                attributes.put("tags", new JSONArray(List.of("alpha", "beta")));
                attributes.put("count", 42);
                attributes.put(TurSolrConstants.SCORE, 1.0);
                attributes.put(TurSolrConstants.VERSION, 3);
                attributes.put(TurSolrConstants.BOOST, 2.5);

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                List<SolrInputDocument> documents = updateRequest.getDocuments();
                assertThat(documents).hasSize(1);

                SolrInputDocument document = documents.getFirst();
                assertThat(document.getFieldValue("title"))
                                .isEqualTo("first" + LINE_SEPARATOR + "second");
                assertThat(document.getFieldValues("tags"))
                                .containsExactly("alpha", "beta");
                assertThat(document.getFieldValue("count")).isEqualTo(42);
                assertThat(document.getFieldValue(TurSolrConstants.SCORE)).isNull();
                assertThat(document.getFieldValue(TurSolrConstants.VERSION)).isNull();
                assertThat(document.getFieldValue(TurSolrConstants.BOOST)).isNull();
        }

        @Test
        void testIndexingFormatsCurrencyAsAmountAndIsoCode() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("preco_produto")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("preco_produto", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("preco_produto", "150.00,BRL");

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();

                assertThat(document.getFieldValue("preco_produto")).isEqualTo("150.00,BRL");
        }

        @Test
        void testIndexingRemovesScoreVersionAndBoostKeys() {
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("id", "1");
                attributes.put(TurSolrConstants.SCORE, 5.0);
                attributes.put(TurSolrConstants.VERSION, 99);
                attributes.put(TurSolrConstants.BOOST, 1.5);

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue(TurSolrConstants.SCORE)).isNull();
                assertThat(document.getFieldValue(TurSolrConstants.VERSION)).isNull();
                assertThat(document.getFieldValue(TurSolrConstants.BOOST)).isNull();
                assertThat(document.getFieldValue("id")).isEqualTo("1");
        }

        @Test
        void testIndexingHandlesNullAttributesThrowsNpe() {
                // The indexing method calls attributes.remove() before null-checking,
                // so null attributes cause a NullPointerException
                assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                                NullPointerException.class,
                                () -> turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, null)))
                                .isNotNull();
        }

        @Test
        void testIndexingHandlesEmptyAttributes() {
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(new HashMap<>());

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, new HashMap<>());

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldNames()).isEmpty();
        }

        @Test
        void testIndexingSingleValuedJsonArrayConcatenatesValues() {
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("desc", TurSEIndexField.builder().name("desc").multiValued(false).build());
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("desc", new org.json.JSONArray(List.of("line1", "line2")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                String value = (String) document.getFieldValue("desc");
                assertThat(value)
                                .contains("line1")
                                .contains("line2");
        }

        @Test
        void testIndexingSingleValuedArrayListConcatenatesValues() {
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("body", TurSEIndexField.builder().name("body").multiValued(false).build());
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("body", new ArrayList<>(List.of("para1", "para2")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                String value = (String) document.getFieldValue("body");
                assertThat(value)
                                .contains("para1")
                                .contains("para2");
        }

        @Test
        void testIndexingMultiValuedArrayListAddsEachValue() {
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("authors", TurSEIndexField.builder().name("authors").multiValued(true).build());
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("authors", new ArrayList<>(List.of("Alice", "Bob")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValues("authors")).containsExactly("Alice", "Bob");
        }

        @Test
        void testIndexingIntegerFieldPreservesType() {
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("count", 42);

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("count")).isEqualTo(42);
        }

        @Test
        void testIndexingNullAttributeValueIsSkipped() {
                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("id", "1");
                attributes.put("nullField", null);

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("id")).isEqualTo("1");
                assertThat(document.getFieldValue("nullField")).isNull();
        }

        @ParameterizedTest(name = "{0}=[{1}] -> {2}")
        @CsvSource(nullValues = "null", value = {
                        "price,  invalid,      null",
                        "amount, '99.50,USD',  '99.50,USD'",
                        "amount, '100.00,brl', '100.00,BRL'"
        })
        void testCurrencyValueIsNormalizedOrSkipped(String fieldName, String input, String expected) {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name(fieldName)
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put(fieldName, currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put(fieldName, input);

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                if (expected == null) {
                        assertThat(document.getFieldValue(fieldName)).isNull();
                } else {
                        assertThat(document.getFieldValue(fieldName)).isEqualTo(expected);
                }
        }

        @Test
        void testCurrencyMissingCodeIsInvalid() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", "100.00,");

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isNull();
        }

        @Test
        void testDeIndexingCallsDeleteById() {
                turSolrDocumentHandler.deIndexing(turSolrInstance, "doc-123");

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                assertThat(updateRequest).isNotNull();
                assertThat(updateRequest.getDeleteById()).contains("doc-123");
        }

        /** T1247b — with an update side, a delete goes to its client and never to the query client. */
        @Test
        void testDeIndexingGoesThroughTheUpdateSide() throws MalformedURLException {
                CapturingSolrClient query = new CapturingSolrClient();
                CapturingSolrClient update = new CapturingSolrClient();
                TurSolrInstance split = new TurSolrInstance(query, update,
                                URI.create("http://localhost:8983/solr").toURL(), "core");

                turSolrDocumentHandler.deIndexing(split, "doc-123");

                assertThat(query.getLastRequest()).isNull();
                assertThat(((UpdateRequest) update.getLastRequest()).getDeleteById()).contains("doc-123");
        }

        @Test
        void testDeIndexingByTypeCallsDeleteByQuery() {
                turSolrDocumentHandler.deIndexingByType(turSolrInstance, "article");

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                assertThat(updateRequest).isNotNull();
                // The deleteByQuery sets query "type:article"
                assertThat(updateRequest.getDeleteQuery()).contains("type:article");
        }

        @Test
        void testCurrencyNullValueIsSkipped() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", null);

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isNull();
        }

        @Test
        void testCurrencyMultiValuedJsonArray() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("prices")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(true)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("prices", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("prices", new org.json.JSONArray(List.of("100.00,USD", "200.00,EUR")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValues("prices")).containsExactly("100.00,USD", "200.00,EUR");
        }

        @Test
        void testCurrencySingleValuedJsonArrayUsesFirst() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", new org.json.JSONArray(List.of("50.00,GBP", "75.00,EUR")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isEqualTo("50.00,GBP");
        }

        @Test
        void testCurrencyEmptyJsonArrayIsSkipped() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", new org.json.JSONArray());

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isNull();
        }

        @Test
        void testCurrencyMultiValuedArrayList() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("prices")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(true)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("prices", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("prices", new ArrayList<>(List.of("100.00,USD", "200.00,EUR")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValues("prices")).containsExactly("100.00,USD", "200.00,EUR");
        }

        @Test
        void testCurrencySingleValuedArrayListUsesFirst() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", new ArrayList<>(List.of("50.00,GBP", "75.00,EUR")));

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isEqualTo("50.00,GBP");
        }

        @Test
        void testCurrencyEmptyArrayListIsSkipped() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", new ArrayList<>());

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isNull();
        }

        @Test
        void testDecimalFieldNonCurrencyUsesNormalizer() {
                TurSEIndexField floatField = TurSEIndexField.builder()
                                .name("weight")
                                .type(TurSEFieldType.FLOAT)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("weight", floatField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.FLOAT)).thenReturn(true);
                when(turDecimalFieldNormalizer.normalizeNumericValue(
                                org.mockito.ArgumentMatchers.eq(TurSEFieldType.FLOAT), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(java.util.Optional.of(3.14));

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("weight", "3,14");

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("weight")).isEqualTo(3.14);
        }

        @Test
        void testDecimalFieldInvalidValueIsSkipped() {
                TurSEIndexField doubleField = TurSEIndexField.builder()
                                .name("weight")
                                .type(TurSEFieldType.DOUBLE)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("weight", doubleField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.DOUBLE)).thenReturn(true);
                when(turDecimalFieldNormalizer.normalizeNumericValue(
                                org.mockito.ArgumentMatchers.eq(TurSEFieldType.DOUBLE), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(java.util.Optional.empty());

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("weight", "not-a-number");

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("weight")).isNull();
        }

        @Test
        void testCurrencyWithCommaDecimalIsNormalized() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                // "19,92,BRL" - comma decimal in amount part should be normalized to dot
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", "19,92,BRL");

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isEqualTo("19.92,BRL");
        }

        @Test
        void testCurrencyInvalidCodeLengthIsInvalid() {
                TurSEIndexField currencyField = TurSEIndexField.builder()
                                .name("price")
                                .type(TurSEFieldType.CURRENCY)
                                .multiValued(false)
                                .build();

                Map<String, TurSEIndexField> fieldMap = new HashMap<>();
                fieldMap.put("price", currencyField);

                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(fieldMap);
                when(turDecimalFieldNormalizer.isDecimalFieldType(TurSEFieldType.CURRENCY)).thenReturn(true);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("price", "100.00,ABCD");

                turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes);

                UpdateRequest updateRequest = (UpdateRequest) capturingSolrClient.getLastRequest();
                SolrInputDocument document = updateRequest.getDocuments().getFirst();
                assertThat(document.getFieldValue("price")).isNull();
        }

        @Test
        void testIndexingPropagatesIoFailureInsteadOfSwallowing() {
                // Regression guard: a swallowed Solr/IO error used to falsely report the
                // document as "Indexed" while it was actually lost. It must now surface so
                // the resilience layer / circuit breaker can react.
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(new HashMap<>());
                turSolrInstance.setSolrClient(new ThrowingSolrClient());

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("id", "doc-io-fail");

                org.junit.jupiter.api.Assertions.assertThrows(
                                TurSolrDocumentHandler.TurSolrIndexException.class,
                                () -> turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes));
        }

        /** T1343 — a 400 is Solr refusing this document, named so the queue logs it. */
        @Test
        void testIndexingNamesADocumentSolrRefuses() {
                when(siteSearchConfig.indexFields(SITE_ID)).thenReturn(new HashMap<>());
                turSolrInstance.setSolrClient(new RefusingSolrClient());

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("id", "doc-bad-date");

                var refused = org.junit.jupiter.api.Assertions.assertThrows(
                                com.viglet.turing.searchengine.TurSEDocumentRefusedException.class,
                                () -> turSolrDocumentHandler.indexing(turSolrInstance, SITE_ID, attributes));
                org.assertj.core.api.Assertions.assertThat(refused.getDocId()).isEqualTo("doc-bad-date");
                org.assertj.core.api.Assertions.assertThat(refused.getMessage()).contains("Invalid Date String");
        }

        /** Solr client stub that refuses every request with a 400. */
        private static final class RefusingSolrClient extends SolrClient {
                @Override
                public NamedList<Object> request(SolrRequest<?> request, String collection) {
                        throw new org.apache.solr.common.SolrException(
                                        org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST,
                                        "Invalid Date String:'2017T00:00:00Z'");
                }

                @Override
                public void close() {
                        // No resources to close for this test stub.
                }
        }

        @Test
        void testDeIndexingPropagatesIoFailureInsteadOfSwallowing() {
                turSolrInstance.setSolrClient(new ThrowingSolrClient());

                org.junit.jupiter.api.Assertions.assertThrows(
                                TurSolrDocumentHandler.TurSolrIndexException.class,
                                () -> turSolrDocumentHandler.deIndexing(turSolrInstance, "doc-io-fail"));
        }

        private static final class CapturingSolrClient extends SolrClient {
                private SolrRequest<?> lastRequest;

                @Override
                public NamedList<Object> request(SolrRequest<?> request, String collection)
                                throws SolrServerException, IOException {
                        this.lastRequest = request;
                        return new NamedList<>();
                }

                @Override
                public void close() {
                        // No resources to close for this test stub.
                }

                public SolrRequest<?> getLastRequest() {
                        return lastRequest;
                }
        }

        /** Solr client stub that simulates an IO failure on every request. */
        private static final class ThrowingSolrClient extends SolrClient {
                @Override
                public NamedList<Object> request(SolrRequest<?> request, String collection)
                                throws SolrServerException, IOException {
                        throw new IOException("disk full");
                }

                @Override
                public void close() {
                        // No resources to close for this test stub.
                }
        }
}
