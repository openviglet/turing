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
 * Unit tests for TurConnectorRequestPlugin interface.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@ExtendWith(MockitoExtension.class)
class TurConnectorRequestPluginTest {

    @Mock
    private TurConnectorRequestPlugin requestPlugin;

    @Test
    void testSentToIndexByIdListWithMultipleIds() {
        String source = "test-source";
        List<String> idList = Arrays.asList("id1", "id2", "id3", "id4");
        
        doNothing().when(requestPlugin).sentToIndexByIdList(source, idList);
        
        requestPlugin.sentToIndexByIdList(source, idList);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(source, idList);
    }

    @Test
    void testSentToIndexByIdListWithSingleId() {
        String source = "single-source";
        List<String> idList = Collections.singletonList("single-id");
        
        doNothing().when(requestPlugin).sentToIndexByIdList(source, idList);
        
        requestPlugin.sentToIndexByIdList(source, idList);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(source, idList);
    }

    @Test
    void testSentToIndexByIdListWithEmptyList() {
        String source = "empty-source";
        List<String> idList = Collections.emptyList();
        
        doNothing().when(requestPlugin).sentToIndexByIdList(source, idList);
        
        requestPlugin.sentToIndexByIdList(source, idList);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(source, idList);
    }

    @Test
    void testSentToIndexByIdListWithNullSource() {
        List<String> idList = Arrays.asList("id1", "id2");
        
        doNothing().when(requestPlugin).sentToIndexByIdList(null, idList);
        
        requestPlugin.sentToIndexByIdList(null, idList);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(null, idList);
    }

    @Test
    void testSentToIndexByIdListWithNullIdList() {
        String source = "null-list-source";
        
        doNothing().when(requestPlugin).sentToIndexByIdList(source, null);
        
        requestPlugin.sentToIndexByIdList(source, null);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(source, null);
    }

    @Test
    void testSentToIndexByIdListWithBothNull() {
        doNothing().when(requestPlugin).sentToIndexByIdList(null, null);
        
        requestPlugin.sentToIndexByIdList(null, null);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(null, null);
    }

    @Test
    void testMultipleCalls() {
        String source1 = "source1";
        String source2 = "source2";
        List<String> idList1 = Arrays.asList("id1", "id2");
        List<String> idList2 = Arrays.asList("id3", "id4", "id5");
        
        doNothing().when(requestPlugin).sentToIndexByIdList(source1, idList1);
        doNothing().when(requestPlugin).sentToIndexByIdList(source2, idList2);
        
        requestPlugin.sentToIndexByIdList(source1, idList1);
        requestPlugin.sentToIndexByIdList(source2, idList2);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(source1, idList1);
        verify(requestPlugin, times(1)).sentToIndexByIdList(source2, idList2);
        verify(requestPlugin, times(2)).sentToIndexByIdList(anyString(), anyList());
    }

    @Test
    void testInterfaceImplementation() {
        // Test that the interface can be properly implemented
        TurConnectorRequestPlugin testPlugin = new TurConnectorRequestPlugin() {
            @Override
            public void sentToIndexByIdList(String source, List<String> idList) {
                // Test implementation - could log, validate parameters, etc.
                if (source != null && idList != null && !idList.isEmpty()) {
                    // Simulate processing
                }
            }
        };
        
        // Verify that the implementation can be called without issues
        String source = "impl-test-source";
        List<String> idList = Arrays.asList("impl-id1", "impl-id2");
        
        // This should not throw any exceptions
        testPlugin.sentToIndexByIdList(source, idList);
        testPlugin.sentToIndexByIdList(null, null);
        testPlugin.sentToIndexByIdList("", Collections.emptyList());
        
        // If we reach here, the implementation works correctly
        assertThat(testPlugin).isNotNull();
    }

    @Test
    void testMethodSignature() {
        // Verify the method signature matches expected interface contract
        TurConnectorRequestPlugin plugin = (source, idList) -> {
            // Lambda implementation for testing
        };
        
        // Test different parameter combinations
        plugin.sentToIndexByIdList("lambda-source", Arrays.asList("lambda-id"));
        plugin.sentToIndexByIdList("", Collections.emptyList());
        plugin.sentToIndexByIdList(null, null);
        
        // Verify lambda implementation works
        assertThat(plugin).isNotNull();
    }

    @Test
    void testParameterValidation() {
        // Create a mock that validates parameters
        String expectedSource = "validated-source";
        List<String> expectedIdList = Arrays.asList("val-id1", "val-id2");
        
        doNothing().when(requestPlugin).sentToIndexByIdList(expectedSource, expectedIdList);
        
        // Call with expected parameters
        requestPlugin.sentToIndexByIdList(expectedSource, expectedIdList);
        
        // Verify exact parameters were passed
        verify(requestPlugin, times(1)).sentToIndexByIdList(eq(expectedSource), eq(expectedIdList));
    }

    @Test
    void testLargeIdList() {
        String source = "large-list-source";
        
        // Create a large list of IDs to test performance/handling
        List<String> largeIdList = Arrays.asList(
            "id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10",
            "id11", "id12", "id13", "id14", "id15", "id16", "id17", "id18", "id19", "id20"
        );
        
        doNothing().when(requestPlugin).sentToIndexByIdList(source, largeIdList);
        
        requestPlugin.sentToIndexByIdList(source, largeIdList);
        
        verify(requestPlugin, times(1)).sentToIndexByIdList(source, largeIdList);
    }

    @Test
    void testPluginUsagePattern() {
        // Simulate a typical usage pattern
        String[] sources = {"source1", "source2", "source3"};
        
        for (String source : sources) {
            List<String> idList = Arrays.asList(source + "-id1", source + "-id2");
            doNothing().when(requestPlugin).sentToIndexByIdList(source, idList);
            
            requestPlugin.sentToIndexByIdList(source, idList);
            
            verify(requestPlugin, times(1)).sentToIndexByIdList(source, idList);
        }
        
        // Verify total number of calls
        verify(requestPlugin, times(3)).sentToIndexByIdList(anyString(), anyList());
    }
}