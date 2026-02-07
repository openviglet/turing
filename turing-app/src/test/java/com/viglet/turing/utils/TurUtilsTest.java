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

package com.viglet.turing.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TurUtils.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
class TurUtilsTest {

    @Test
    void testConstructorThrowsException() {
        assertThatThrownBy(() -> {
            var constructor = TurUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Utility class");
    }

    @Test
    void testGetUrlTemplateWithBasicUrl() {
        String serviceUrl = "http://example.com";
        String id = "12345";
        
        String result = TurUtils.getUrlTemplate(serviceUrl, id);
        
        assertThat(result).isEqualTo("http://example.com/12345");
    }

    @Test
    void testGetUrlTemplateWithTrailingSlash() {
        String serviceUrl = "http://example.com/";
        String id = "12345";
        
        String result = TurUtils.getUrlTemplate(serviceUrl, id);
        
        assertThat(result).isEqualTo("http://example.com//12345");
    }

    @Test
    void testGetUrlTemplateWithPath() {
        String serviceUrl = "http://example.com/api";
        String id = "item-123";
        
        String result = TurUtils.getUrlTemplate(serviceUrl, id);
        
        assertThat(result).isEqualTo("http://example.com/api/item-123");
    }

    @Test
    void testGetUrlTemplateWithEmptyId() {
        String serviceUrl = "http://example.com";
        String id = "";
        
        String result = TurUtils.getUrlTemplate(serviceUrl, id);
        
        assertThat(result).isEqualTo("http://example.com/");
    }

    @Test
    void testGetUrlTemplateWithSpecialCharacters() {
        String serviceUrl = "http://example.com/api";
        String id = "item-with-dashes_and_underscores";
        
        String result = TurUtils.getUrlTemplate(serviceUrl, id);
        
        assertThat(result).isEqualTo("http://example.com/api/item-with-dashes_and_underscores");
    }

    @Test
    void testGetUrlTemplateWithNumericId() {
        String serviceUrl = "http://localhost:8080/service";
        String id = "999";
        
        String result = TurUtils.getUrlTemplate(serviceUrl, id);
        
        assertThat(result).isEqualTo("http://localhost:8080/service/999");
    }
}
