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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TurSolrInstance.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSolrInstanceTest {

    @Mock
    private HttpJdkSolrClient httpJdkSolrClient;

    private URL solrUrl;
    private String core;

    @BeforeEach
    void setUp() throws MalformedURLException {
        solrUrl = new URL("http://localhost:8983/solr");
        core = "testCore";
    }

    @Test
    void testConstructor() {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);

        assertThat(instance.getHttpJdkSolrClient()).isEqualTo(httpJdkSolrClient);
        assertThat(instance.getSolrUrl()).isEqualTo(solrUrl);
        assertThat(instance.getCore()).isEqualTo(core);
        assertThat(instance.getSolrClient()).isNotNull();
    }

    @Test
    void testConstructorCreatesNewSolrClient() {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);

        assertThat(instance.getSolrClient()).isInstanceOf(SolrClient.class);
    }

    @Test
    void testGettersAndSetters() {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);

        assertThat(instance.getHttpJdkSolrClient()).isEqualTo(httpJdkSolrClient);
        assertThat(instance.getSolrUrl()).isEqualTo(solrUrl);
        assertThat(instance.getCore()).isEqualTo(core);

        HttpJdkSolrClient newHttpClient = mock(HttpJdkSolrClient.class);
        SolrClient newSolrClient = mock(SolrClient.class);
        URL newUrl = null;
        try {
            newUrl = new URL("http://localhost:9000/solr");
        } catch (MalformedURLException e) {
            fail("Failed to create URL");
        }
        String newCore = "newCore";

        instance.setHttpJdkSolrClient(newHttpClient);
        instance.setSolrClient(newSolrClient);
        instance.setSolrUrl(newUrl);
        instance.setCore(newCore);

        assertThat(instance.getHttpJdkSolrClient()).isEqualTo(newHttpClient);
        assertThat(instance.getSolrClient()).isEqualTo(newSolrClient);
        assertThat(instance.getSolrUrl()).isEqualTo(newUrl);
        assertThat(instance.getCore()).isEqualTo(newCore);
    }

    @Test
    void testCloseWithBothClientsNotNull() throws IOException {
        SolrClient mockSolrClient = mock(SolrClient.class);
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(mockSolrClient);

        instance.close();

        verify(mockSolrClient, times(1)).close();
        verify(httpJdkSolrClient, times(1)).close();
        assertThat(instance.getSolrClient()).isNull();
        assertThat(instance.getHttpJdkSolrClient()).isNull();
    }

    @Test
    void testCloseWithOnlySolrClient() throws IOException {
        SolrClient mockSolrClient = mock(SolrClient.class);
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(mockSolrClient);
        instance.setHttpJdkSolrClient(null);

        instance.close();

        verify(mockSolrClient, times(1)).close();
        assertThat(instance.getSolrClient()).isNull();
    }

    @Test
    void testCloseWithOnlyHttpJdkClient() throws IOException {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(null);

        instance.close();

        verify(httpJdkSolrClient, times(1)).close();
        assertThat(instance.getHttpJdkSolrClient()).isNull();
    }

    @Test
    void testCloseWithNullClients() {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(null);
        instance.setHttpJdkSolrClient(null);

        assertThatCode(() -> instance.close()).doesNotThrowAnyException();
    }

    @Test
    void testCloseHandlesIOException() throws IOException {
        SolrClient mockSolrClient = mock(SolrClient.class);
        doThrow(new IOException("Test exception")).when(mockSolrClient).close();

        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(mockSolrClient);

        assertThatCode(() -> instance.close()).doesNotThrowAnyException();
    }

    @Test
    void testDestroy() throws IOException {
        SolrClient mockSolrClient = mock(SolrClient.class);
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(mockSolrClient);

        instance.destroy();

        verify(mockSolrClient, times(1)).close();
        verify(httpJdkSolrClient, times(1)).close();
        assertThat(instance.getSolrClient()).isNull();
        assertThat(instance.getHttpJdkSolrClient()).isNull();
    }

    @Test
    void testMultipleCloseCallsAreSafe() throws IOException {
        SolrClient mockSolrClient = mock(SolrClient.class);
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        instance.setSolrClient(mockSolrClient);

        instance.close();
        instance.close();

        verify(mockSolrClient, times(1)).close();
        verify(httpJdkSolrClient, times(1)).close();
    }

    @Test
    void testConstructorWithDifferentCoreName() {
        String customCore = "customCore";
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, customCore);

        assertThat(instance.getCore()).isEqualTo(customCore);
    }

    @Test
    void testConstructorWithDifferentUrl() throws MalformedURLException {
        URL customUrl = new URL("http://custom-solr:8080/solr");
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, customUrl, core);

        assertThat(instance.getSolrUrl()).isEqualTo(customUrl);
        assertThat(instance.getSolrClient()).isNotNull();
    }

    @Test
    void testCoreFieldCanBeNull() {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, null);

        assertThat(instance.getCore()).isNull();
    }

    @Test
    void testSetCoreAfterConstruction() {
        TurSolrInstance instance = new TurSolrInstance(httpJdkSolrClient, solrUrl, core);
        String newCore = "anotherCore";

        instance.setCore(newCore);

        assertThat(instance.getCore()).isEqualTo(newCore);
    }
}
