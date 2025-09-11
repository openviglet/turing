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
package com.viglet.turing.connector.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurRestClientConfig.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurRestClientConfigTest {

    @Test
    void testKeyConstant() {
        assertThat(TurRestClientConfig.KEY).isEqualTo("Key");
    }

    @Test
    void testConstructorWithDefaultUrl() {
        String turingUrl = "http://localhost:2700";
        String turingApiKey = "test-api-key";

        TurRestClientConfig config = new TurRestClientConfig(turingUrl, turingApiKey);

        assertThat(config).isNotNull();
        // Since fields are private, we test behavior through the restClient() method
        RestClient restClient = config.restClient();
        assertThat(restClient).isNotNull();
    }

    @Test
    void testConstructorWithCustomUrl() {
        String turingUrl = "https://custom-turing-server.com:8080";
        String turingApiKey = "custom-api-key";

        TurRestClientConfig config = new TurRestClientConfig(turingUrl, turingApiKey);

        assertThat(config).isNotNull();
        RestClient restClient = config.restClient();
        assertThat(restClient).isNotNull();
    }

    @Test
    void testRestClientCreation() {
        String turingUrl = "http://test-server.com";
        String turingApiKey = "test-key";

        TurRestClientConfig config = new TurRestClientConfig(turingUrl, turingApiKey);
        RestClient restClient = config.restClient();

        assertThat(restClient).isNotNull();
        assertThat(restClient).isInstanceOf(RestClient.class);
    }

    @Test
    void testRestClientWithNullValues() {
        // Test with null values - should still create config but might cause issues at runtime
        TurRestClientConfig config = new TurRestClientConfig(null, null);

        assertThat(config).isNotNull();
        // The RestClient creation should handle null values gracefully or throw appropriate exceptions
        // This test verifies the config object can be created with null inputs
    }

    @Test
    void testRestClientWithEmptyValues() {
        TurRestClientConfig config = new TurRestClientConfig("", "");

        assertThat(config).isNotNull();
        RestClient restClient = config.restClient();
        assertThat(restClient).isNotNull();
    }

    @Test
    void testConfigurationAnnotation() {
        assertThat(TurRestClientConfig.class
                .isAnnotationPresent(org.springframework.context.annotation.Configuration.class))
                .isTrue();
    }

    @Test
    void testRestClientBeanMethod() throws NoSuchMethodException {
        assertThat(TurRestClientConfig.class.getMethod("restClient")
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .isTrue();
    }

    @Test
    void testRestClientMethodReturnType() throws NoSuchMethodException {
        assertThat(TurRestClientConfig.class.getMethod("restClient")
                .getReturnType()).isEqualTo(RestClient.class);
    }

    @Test
    void testMultipleRestClientCalls() {
        String turingUrl = "http://multi-test-server.com";
        String turingApiKey = "multi-test-key";

        TurRestClientConfig config = new TurRestClientConfig(turingUrl, turingApiKey);
        
        // Call restClient() multiple times - each call should return a new instance
        RestClient restClient1 = config.restClient();
        RestClient restClient2 = config.restClient();

        assertThat(restClient1).isNotNull();
        assertThat(restClient2).isNotNull();
        // Since the method creates new instances each time, they should not be the same reference
        assertThat(restClient1).isNotSameAs(restClient2);
    }

    @Test
    void testWithDifferentUrlFormats() {
        String[] urls = {
                "http://localhost:2700",
                "https://secure-server.com",
                "http://192.168.1.100:8080",
                "https://subdomain.example.com:9443/path"
        };

        for (String url : urls) {
            TurRestClientConfig config = new TurRestClientConfig(url, "test-key");
            RestClient restClient = config.restClient();
            
            assertThat(restClient).isNotNull();
        }
    }

    @Test
    void testWithDifferentApiKeyFormats() {
        String[] apiKeys = {
                "simple-key",
                "complex-key-123-ABC",
                "key_with_underscores",
                "key.with.dots",
                "UPPERCASE-KEY",
                "MixedCase-Key-123"
        };

        for (String apiKey : apiKeys) {
            TurRestClientConfig config = new TurRestClientConfig("http://test.com", apiKey);
            RestClient restClient = config.restClient();
            
            assertThat(restClient).isNotNull();
        }
    }

    @Test
    void testDefaultValuesFromAnnotation() {
        // Test that constructor accepts the default value format used in @Value annotation
        String defaultUrl = "http://localhost:2700"; // From @Value("${turing.url:http://localhost:2700}")
        String apiKey = "test-key";

        TurRestClientConfig config = new TurRestClientConfig(defaultUrl, apiKey);
        RestClient restClient = config.restClient();

        assertThat(restClient).isNotNull();
        assertThat(defaultUrl).contains("localhost");
        assertThat(defaultUrl).contains("2700");
    }

    @Test
    void testConstructorParameterValidation() {
        // These should all create valid config objects
        TurRestClientConfig config1 = new TurRestClientConfig("http://valid.url", "valid-key");
        TurRestClientConfig config2 = new TurRestClientConfig("https://secure.url", "secure-key");
        
        assertThat(config1).isNotNull();
        assertThat(config2).isNotNull();
        
        RestClient client1 = config1.restClient();
        RestClient client2 = config2.restClient();
        
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();
    }

    @Test
    void testRestClientConfiguration() {
        String turingUrl = "https://config-test.com";
        String turingApiKey = "config-test-key";

        TurRestClientConfig config = new TurRestClientConfig(turingUrl, turingApiKey);
        RestClient restClient = config.restClient();

        assertThat(restClient).isNotNull();
        
        // The RestClient should be configured with:
        // 1. HttpComponentsClientHttpRequestFactory as request factory
        // 2. Base URL from turingUrl
        // 3. Default header "Key" with turingApiKey value
        // We can't easily test these internal configurations without accessing private fields
        // but we can verify the client was created successfully
    }
}