/*
 * Copyright (C) 2016-2022 the original author or authors.
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

package com.viglet.turing.client.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TurClientUtils.
 *
 * @author Alexandre Oliveira
 * @since 0.3.9
 */
class TurClientUtilsTest {

    @Test
    void testConstructorThrowsException() {
        // Use reflection to test private constructor
        assertThrows(IllegalStateException.class, () -> {
            try {
                var constructor = TurClientUtils.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            } catch (Exception e) {
                if (e.getCause() instanceof IllegalStateException) {
                    throw (IllegalStateException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testAuthenticationWithValidApiKey() {
        HttpPost httpPost = new HttpPost("http://example.com/api");
        String apiKey = "test-api-key-123";

        TurClientUtils.authentication(httpPost, apiKey);

        assertThat(httpPost.getFirstHeader("Key")).isNotNull();
        assertThat(httpPost.getFirstHeader("Key").getValue()).isEqualTo(apiKey);
    }

    @Test
    void testAuthenticationWithNullApiKey() {
        HttpPost httpPost = new HttpPost("http://example.com/api");

        TurClientUtils.authentication(httpPost, null);

        // Should not add the Key header when apiKey is null
        assertThat(httpPost.getFirstHeader("Key")).isNull();
    }

    @Test
    void testAuthenticationWithEmptyApiKey() {
        HttpPost httpPost = new HttpPost("http://example.com/api");
        String apiKey = "";

        TurClientUtils.authentication(httpPost, apiKey);

        // Should add header even with empty string (non-null)
        assertThat(httpPost.getFirstHeader("Key")).isNotNull();
        assertThat(httpPost.getFirstHeader("Key").getValue()).isEmpty();
    }

    @Test
    void testAuthenticationOverwritesExistingHeader() {
        HttpPost httpPost = new HttpPost("http://example.com/api");

        // Set initial header
        httpPost.setHeader("Key", "old-api-key");

        // Overwrite with new key
        String newApiKey = "new-api-key-456";
        TurClientUtils.authentication(httpPost, newApiKey);

        assertThat(httpPost.getFirstHeader("Key")).isNotNull();
        assertThat(httpPost.getFirstHeader("Key").getValue()).isEqualTo(newApiKey);

        // Should only have one Key header
        assertThat(httpPost.getHeaders("Key")).hasSize(1);
    }

    @Test
    void testAuthenticationWithSpecialCharacters() {
        HttpPost httpPost = new HttpPost("http://example.com/api");
        String specialApiKey = "api-key-with-special-chars!@#$%^&*()_+";

        TurClientUtils.authentication(httpPost, specialApiKey);

        assertThat(httpPost.getFirstHeader("Key")).isNotNull();
        assertThat(httpPost.getFirstHeader("Key").getValue()).isEqualTo(specialApiKey);
    }

    @Test
    void testAuthenticationDoesNotAffectOtherHeaders() {
        HttpPost httpPost = new HttpPost("http://example.com/api");

        // Set other headers
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");

        // Add authentication
        String apiKey = "test-key";
        TurClientUtils.authentication(httpPost, apiKey);

        // Verify authentication header is added
        assertThat(httpPost.getFirstHeader("Key")).isNotNull();
        assertThat(httpPost.getFirstHeader("Key").getValue()).isEqualTo(apiKey);

        // Verify other headers remain unchanged
        assertThat(httpPost.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(httpPost.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    void testAuthenticationWithLongApiKey() {
        HttpPost httpPost = new HttpPost("http://example.com/api");

        // Create a very long API key
        String longApiKey = "a".repeat(1000);

        TurClientUtils.authentication(httpPost, longApiKey);

        assertThat(httpPost.getFirstHeader("Key")).isNotNull();
        assertThat(httpPost.getFirstHeader("Key").getValue()).isEqualTo(longApiKey);
        assertThat(httpPost.getFirstHeader("Key").getValue()).hasSize(1000);
    }
}