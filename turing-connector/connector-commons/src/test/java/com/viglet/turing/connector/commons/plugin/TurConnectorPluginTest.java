/*
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
package com.viglet.turing.connector.commons.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TurConnectorPlugin interface.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@ExtendWith(MockitoExtension.class)
class TurConnectorPluginTest {

    @Mock
    private TurConnectorPlugin plugin;

    @Test
    void testCrawlMethodExists() {
        // Test that the crawl method can be called
        doNothing().when(plugin).crawl();
        
        plugin.crawl();
        
        verify(plugin, times(1)).crawl();
    }

    @Test
    void testGetProviderNameReturnsValue() {
        String expectedProviderName = "test-provider";
        when(plugin.getProviderName()).thenReturn(expectedProviderName);
        
        String actualProviderName = plugin.getProviderName();
        
        assertThat(actualProviderName).isEqualTo(expectedProviderName);
        verify(plugin, times(1)).getProviderName();
    }

    @Test
    void testGetProviderNameReturnsNull() {
        when(plugin.getProviderName()).thenReturn(null);
        
        String providerName = plugin.getProviderName();
        
        assertThat(providerName).isNull();
        verify(plugin, times(1)).getProviderName();
    }

    @Test
    void testIndexAllWithSource() {
        String source = "test-source";
        doNothing().when(plugin).indexAll(source);
        
        plugin.indexAll(source);
        
        verify(plugin, times(1)).indexAll(source);
    }

    @Test
    void testIndexAllWithNullSource() {
        doNothing().when(plugin).indexAll(null);
        
        plugin.indexAll(null);
        
        verify(plugin, times(1)).indexAll(null);
    }

    @Test
    void testIndexByIdWithMultipleIds() {
        String source = "test-source";
        List<String> contentIds = Arrays.asList("id1", "id2", "id3");
        doNothing().when(plugin).indexById(source, contentIds);
        
        plugin.indexById(source, contentIds);
        
        verify(plugin, times(1)).indexById(source, contentIds);
    }

    @Test
    void testIndexByIdWithSingleId() {
        String source = "test-source";
        List<String> contentIds = Collections.singletonList("single-id");
        doNothing().when(plugin).indexById(source, contentIds);
        
        plugin.indexById(source, contentIds);
        
        verify(plugin, times(1)).indexById(source, contentIds);
    }

    @Test
    void testIndexByIdWithEmptyList() {
        String source = "test-source";
        List<String> contentIds = Collections.emptyList();
        doNothing().when(plugin).indexById(source, contentIds);
        
        plugin.indexById(source, contentIds);
        
        verify(plugin, times(1)).indexById(source, contentIds);
    }

    @Test
    void testIndexByIdWithNullParameters() {
        doNothing().when(plugin).indexById(null, null);
        
        plugin.indexById(null, null);
        
        verify(plugin, times(1)).indexById(null, null);
    }

    @Test
    void testMultipleCalls() {
        String providerName = "multi-provider";
        String source = "multi-source";
        List<String> contentIds = Arrays.asList("multi-id1", "multi-id2");
        
        when(plugin.getProviderName()).thenReturn(providerName);
        doNothing().when(plugin).crawl();
        doNothing().when(plugin).indexAll(source);
        doNothing().when(plugin).indexById(source, contentIds);
        
        // Call multiple methods
        String actualProviderName = plugin.getProviderName();
        plugin.crawl();
        plugin.indexAll(source);
        plugin.indexById(source, contentIds);
        
        // Verify all calls
        assertThat(actualProviderName).isEqualTo(providerName);
        verify(plugin, times(1)).getProviderName();
        verify(plugin, times(1)).crawl();
        verify(plugin, times(1)).indexAll(source);
        verify(plugin, times(1)).indexById(source, contentIds);
    }

    @Test
    void testInterfaceMethodSignatures() {
        // Test that interface methods have the expected signatures
        // This is more of a compilation test to ensure interface contracts are maintained
        
        TurConnectorPlugin testPlugin = new TurConnectorPlugin() {
            @Override
            public void crawl() {
                // Test implementation
            }

            @Override
            public String getProviderName() {
                return "test-implementation";
            }

            @Override
            public void indexAll(String source) {
                // Test implementation
            }

            @Override
            public void indexById(String source, List<String> contentId) {
                // Test implementation
            }
        };
        
        // Verify that all methods can be called
        testPlugin.crawl();
        String providerName = testPlugin.getProviderName();
        testPlugin.indexAll("test");
        testPlugin.indexById("test", Arrays.asList("id1"));
        
        assertThat(providerName).isEqualTo("test-implementation");
    }

    @Test
    void testPluginChainedOperations() {
        String source = "chained-source";
        List<String> ids = Arrays.asList("chain1", "chain2");
        
        when(plugin.getProviderName()).thenReturn("chain-provider");
        doNothing().when(plugin).crawl();
        doNothing().when(plugin).indexAll(source);
        doNothing().when(plugin).indexById(source, ids);
        
        // Simulate a typical plugin usage pattern
        String providerName = plugin.getProviderName();
        if ("chain-provider".equals(providerName)) {
            plugin.crawl();
            plugin.indexAll(source);
            plugin.indexById(source, ids);
        }
        
        verify(plugin, times(1)).getProviderName();
        verify(plugin, times(1)).crawl();
        verify(plugin, times(1)).indexAll(source);
        verify(plugin, times(1)).indexById(source, ids);
    }
}