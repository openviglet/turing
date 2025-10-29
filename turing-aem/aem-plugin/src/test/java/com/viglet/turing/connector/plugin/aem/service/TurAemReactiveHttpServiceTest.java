/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.plugin.aem.service;

import org.junit.jupiter.api.Test;

import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;

import reactor.core.publisher.Mono;

/**
 * Simple test for TurAemReactiveHttpService to verify basic functionality
 * 
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurAemReactiveHttpServiceTest {

    @Test
    void testFetchResponseBodyReactive_BasicConfiguration() {
        // Given
        TurAemReactiveHttpService service = new TurAemReactiveHttpService();
        TurAemConfiguration context = new TurAemConfiguration();
        context.setUsername("admin");
        context.setPassword("admin");

        // When
        Mono<String> result = service.fetchResponseBodyReactive("http://httpbin.org/status/404", context);

        // Then - should handle error gracefully and return empty string
        String response = result.block();
        assert response != null;
        assert response.isEmpty(); // Should return empty on error
    }
}