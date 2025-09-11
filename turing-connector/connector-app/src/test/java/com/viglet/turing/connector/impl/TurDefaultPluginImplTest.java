/*
 * Copyright (C) 2016-2024 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.viglet.turing.connector.impl;

import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TurDefaultPluginImpl.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurDefaultPluginImplTest {

    private final TurDefaultPluginImpl plugin = new TurDefaultPluginImpl();

    @Test
    void testImplementsConnectorPlugin() {
        assertThat(plugin).isInstanceOf(TurConnectorPlugin.class);
    }

    @Test
    void testGetProviderName() {
        String providerName = plugin.getProviderName();
        
        assertThat(providerName).isEqualTo("DEFAULT");
    }

    @Test
    void testCrawlThrowsUnsupportedOperationException() {
        assertThatThrownBy(() -> plugin.crawl())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexAllThrowsUnsupportedOperationException() {
        String source = "test-source";
        
        assertThatThrownBy(() -> plugin.indexAll(source))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexAllWithNullSourceThrowsUnsupportedOperationException() {
        assertThatThrownBy(() -> plugin.indexAll(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexAllWithEmptySourceThrowsUnsupportedOperationException() {
        assertThatThrownBy(() -> plugin.indexAll(""))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexByIdThrowsUnsupportedOperationException() {
        String source = "test-source";
        List<String> contentIds = Arrays.asList("id1", "id2");
        
        assertThatThrownBy(() -> plugin.indexById(source, contentIds))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexByIdWithEmptyListThrowsUnsupportedOperationException() {
        String source = "test-source";
        List<String> emptyContentIds = Collections.emptyList();
        
        assertThatThrownBy(() -> plugin.indexById(source, emptyContentIds))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexByIdWithNullParametersThrowsUnsupportedOperationException() {
        assertThatThrownBy(() -> plugin.indexById(null, null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testIndexByIdWithSingleIdThrowsUnsupportedOperationException() {
        String source = "test-source";
        List<String> singleId = Collections.singletonList("single-id");
        
        assertThatThrownBy(() -> plugin.indexById(source, singleId))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("This method is only a placeholder");
    }

    @Test
    void testComponentAnnotation() {
        assertThat(TurDefaultPluginImpl.class
                .isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isTrue();
        
        org.springframework.stereotype.Component componentAnnotation = 
                TurDefaultPluginImpl.class.getAnnotation(org.springframework.stereotype.Component.class);
        assertThat(componentAnnotation.value()).isEqualTo("default");
    }

    @Test
    void testSlf4jAnnotation() {
        assertThat(TurDefaultPluginImpl.class
                .isAnnotationPresent(lombok.extern.slf4j.Slf4j.class))
                .isTrue();
    }

    @Test
    void testAllMethodsThrowUnsupportedOperationException() {
        // Verify that all interface methods throw UnsupportedOperationException
        // This confirms the class serves as a placeholder implementation
        
        assertThatThrownBy(() -> plugin.crawl())
                .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> plugin.indexAll("test"))
                .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> plugin.indexById("test", Arrays.asList("id")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testProviderNameIsConstant() {
        // Call getProviderName multiple times to ensure it returns the same value
        String name1 = plugin.getProviderName();
        String name2 = plugin.getProviderName();
        String name3 = plugin.getProviderName();
        
        assertThat(name1).isEqualTo("DEFAULT");
        assertThat(name2).isEqualTo("DEFAULT");
        assertThat(name3).isEqualTo("DEFAULT");
        assertThat(name1).isEqualTo(name2);
        assertThat(name2).isEqualTo(name3);
    }

    @Test
    void testProviderNameNotNull() {
        String providerName = plugin.getProviderName();
        
        assertThat(providerName).isNotNull();
        assertThat(providerName).isNotEmpty();
    }

    @Test
    void testExceptionMessages() {
        // Verify all methods throw with the same consistent message
        String expectedMessage = "This method is only a placeholder";
        
        assertThatThrownBy(() -> plugin.crawl())
                .hasMessage(expectedMessage);
        
        assertThatThrownBy(() -> plugin.indexAll("test"))
                .hasMessage(expectedMessage);
        
        assertThatThrownBy(() -> plugin.indexById("test", Arrays.asList("id")))
                .hasMessage(expectedMessage);
    }

    @Test
    void testObjectInstantiation() {
        // Test that multiple instances can be created
        TurDefaultPluginImpl plugin1 = new TurDefaultPluginImpl();
        TurDefaultPluginImpl plugin2 = new TurDefaultPluginImpl();
        
        assertThat(plugin1).isNotNull();
        assertThat(plugin2).isNotNull();
        assertThat(plugin1).isNotSameAs(plugin2);
        
        // Both should have the same provider name
        assertThat(plugin1.getProviderName()).isEqualTo(plugin2.getProviderName());
    }

    @Test
    void testInterfaceContractCompliance() {
        // Verify that all TurConnectorPlugin interface methods are implemented
        assertThat(plugin).isInstanceOf(TurConnectorPlugin.class);
        
        // Test that all methods are callable (even though they throw exceptions)
        try {
            plugin.crawl();
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("This method is only a placeholder");
        }
        
        try {
            plugin.indexAll("test");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("This method is only a placeholder");
        }
        
        try {
            plugin.indexById("test", Arrays.asList("id"));
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage()).isEqualTo("This method is only a placeholder");
        }
        
        // getProviderName should not throw
        String providerName = plugin.getProviderName();
        assertThat(providerName).isEqualTo("DEFAULT");
    }

    @Test
    void testComponentName() {
        // Verify the component name is "default" for Spring context
        org.springframework.stereotype.Component componentAnnotation = 
                TurDefaultPluginImpl.class.getAnnotation(org.springframework.stereotype.Component.class);
        
        assertThat(componentAnnotation).isNotNull();
        assertThat(componentAnnotation.value()).isEqualTo("default");
    }

    @Test
    void testDefaultPluginBehavior() {
        // This plugin serves as a fallback/default when no other plugin is available
        // It should identify itself as "DEFAULT" and throw exceptions for operational methods
        
        assertThat(plugin.getProviderName()).isEqualTo("DEFAULT");
        
        // All operational methods should be unsupported
        assertThatThrownBy(() -> plugin.crawl()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> plugin.indexAll("any")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> plugin.indexById("any", Arrays.asList("any"))).isInstanceOf(UnsupportedOperationException.class);
    }
}