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
package com.viglet.turing.connector.commons;

import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
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
 * Unit tests for TurConnectorContext interface.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@ExtendWith(MockitoExtension.class)
class TurConnectorContextTest {

    @Mock
    private TurConnectorContext context;

    @Mock
    private TurJobItemWithSession jobItemWithSession;

    @Mock
    private TurConnectorSession session;

    @Mock
    private TurConnectorIndexing indexingItem;

    @Test
    void testAddJobItem() {
        doNothing().when(context).addJobItem(jobItemWithSession);
        
        context.addJobItem(jobItemWithSession);
        
        verify(context, times(1)).addJobItem(jobItemWithSession);
    }

    @Test
    void testAddJobItemWithNull() {
        doNothing().when(context).addJobItem(null);
        
        context.addJobItem(null);
        
        verify(context, times(1)).addJobItem(null);
    }

    @Test
    void testFinishIndexingStandalone() {
        doNothing().when(context).finishIndexing(session, true);
        
        context.finishIndexing(session, true);
        
        verify(context, times(1)).finishIndexing(session, true);
    }

    @Test
    void testFinishIndexingNonStandalone() {
        doNothing().when(context).finishIndexing(session, false);
        
        context.finishIndexing(session, false);
        
        verify(context, times(1)).finishIndexing(session, false);
    }

    @Test
    void testFinishIndexingWithNullSession() {
        doNothing().when(context).finishIndexing(null, true);
        
        context.finishIndexing(null, true);
        
        verify(context, times(1)).finishIndexing(null, true);
    }

    @Test
    void testGetIndexingItemWithResults() {
        String objectId = "test-object-id";
        String source = "test-source";
        String provider = "test-provider";
        List<TurConnectorIndexing> expectedResults = Arrays.asList(indexingItem);
        
        when(context.getIndexingItem(objectId, source, provider)).thenReturn(expectedResults);
        
        List<TurConnectorIndexing> actualResults = context.getIndexingItem(objectId, source, provider);
        
        assertThat(actualResults).isEqualTo(expectedResults);
        verify(context, times(1)).getIndexingItem(objectId, source, provider);
    }

    @Test
    void testGetIndexingItemWithEmptyResults() {
        String objectId = "empty-object-id";
        String source = "empty-source";
        String provider = "empty-provider";
        List<TurConnectorIndexing> emptyResults = Collections.emptyList();
        
        when(context.getIndexingItem(objectId, source, provider)).thenReturn(emptyResults);
        
        List<TurConnectorIndexing> actualResults = context.getIndexingItem(objectId, source, provider);
        
        assertThat(actualResults).isEmpty();
        verify(context, times(1)).getIndexingItem(objectId, source, provider);
    }

    @Test
    void testGetIndexingItemWithNullParameters() {
        when(context.getIndexingItem(null, null, null)).thenReturn(null);
        
        List<TurConnectorIndexing> results = context.getIndexingItem(null, null, null);
        
        assertThat(results).isNull();
        verify(context, times(1)).getIndexingItem(null, null, null);
    }

    @Test
    void testGetObjectIdByDependency() {
        String source = "dependency-source";
        String provider = "dependency-provider";
        List<String> dependenciesObjectIdList = Arrays.asList("dep1", "dep2", "dep3");
        List<String> expectedObjectIds = Arrays.asList("obj1", "obj2");
        
        when(context.getObjectIdByDependency(source, provider, dependenciesObjectIdList))
                .thenReturn(expectedObjectIds);
        
        List<String> actualObjectIds = context.getObjectIdByDependency(source, provider, dependenciesObjectIdList);
        
        assertThat(actualObjectIds).isEqualTo(expectedObjectIds);
        verify(context, times(1)).getObjectIdByDependency(source, provider, dependenciesObjectIdList);
    }

    @Test
    void testGetObjectIdByDependencyWithEmptyDependencies() {
        String source = "empty-dep-source";
        String provider = "empty-dep-provider";
        List<String> emptyDependencies = Collections.emptyList();
        List<String> emptyObjectIds = Collections.emptyList();
        
        when(context.getObjectIdByDependency(source, provider, emptyDependencies))
                .thenReturn(emptyObjectIds);
        
        List<String> actualObjectIds = context.getObjectIdByDependency(source, provider, emptyDependencies);
        
        assertThat(actualObjectIds).isEmpty();
        verify(context, times(1)).getObjectIdByDependency(source, provider, emptyDependencies);
    }

    @Test
    void testGetObjectIdByDependencyWithNullParameters() {
        when(context.getObjectIdByDependency(null, null, null)).thenReturn(null);
        
        List<String> objectIds = context.getObjectIdByDependency(null, null, null);
        
        assertThat(objectIds).isNull();
        verify(context, times(1)).getObjectIdByDependency(null, null, null);
    }

    @Test
    void testInterfaceImplementation() {
        // Test that the interface can be properly implemented
        TurConnectorContext testContext = new TurConnectorContext() {
            @Override
            public void addJobItem(TurJobItemWithSession turJobItemWithSession) {
                // Test implementation
            }

            @Override
            public void finishIndexing(TurConnectorSession session, boolean standalone) {
                // Test implementation
            }

            @Override
            public List<TurConnectorIndexing> getIndexingItem(String objectId, String source, String provider) {
                return Collections.emptyList();
            }

            @Override
            public List<String> getObjectIdByDependency(String source, String provider, 
                    List<String> dependenciesObjectIdList) {
                return Collections.emptyList();
            }
        };
        
        // Verify that all methods can be called
        testContext.addJobItem(null);
        testContext.finishIndexing(null, false);
        List<TurConnectorIndexing> indexingItems = testContext.getIndexingItem("test", "test", "test");
        List<String> objectIds = testContext.getObjectIdByDependency("test", "test", Collections.emptyList());
        
        assertThat(indexingItems).isEmpty();
        assertThat(objectIds).isEmpty();
    }

    @Test
    void testTypicalUsagePattern() {
        // Simulate a typical usage pattern
        String objectId = "usage-object-id";
        String source = "usage-source";
        String provider = "usage-provider";
        List<String> dependencies = Arrays.asList("dep1", "dep2");
        
        // Setup mocks
        when(context.getIndexingItem(objectId, source, provider))
                .thenReturn(Arrays.asList(indexingItem));
        when(context.getObjectIdByDependency(source, provider, dependencies))
                .thenReturn(Arrays.asList("related-obj1", "related-obj2"));
        doNothing().when(context).addJobItem(jobItemWithSession);
        doNothing().when(context).finishIndexing(session, true);
        
        // Typical usage flow
        List<TurConnectorIndexing> existingItems = context.getIndexingItem(objectId, source, provider);
        List<String> relatedObjectIds = context.getObjectIdByDependency(source, provider, dependencies);
        context.addJobItem(jobItemWithSession);
        context.finishIndexing(session, true);
        
        // Verify interactions
        assertThat(existingItems).hasSize(1);
        assertThat(relatedObjectIds).hasSize(2);
        verify(context, times(1)).getIndexingItem(objectId, source, provider);
        verify(context, times(1)).getObjectIdByDependency(source, provider, dependencies);
        verify(context, times(1)).addJobItem(jobItemWithSession);
        verify(context, times(1)).finishIndexing(session, true);
    }

    @Test
    void testMultipleJobItemsAddition() {
        TurJobItemWithSession jobItem1 = mock(TurJobItemWithSession.class);
        TurJobItemWithSession jobItem2 = mock(TurJobItemWithSession.class);
        TurJobItemWithSession jobItem3 = mock(TurJobItemWithSession.class);
        
        doNothing().when(context).addJobItem(any(TurJobItemWithSession.class));
        
        context.addJobItem(jobItem1);
        context.addJobItem(jobItem2);
        context.addJobItem(jobItem3);
        
        verify(context, times(3)).addJobItem(any(TurJobItemWithSession.class));
        verify(context, times(1)).addJobItem(jobItem1);
        verify(context, times(1)).addJobItem(jobItem2);
        verify(context, times(1)).addJobItem(jobItem3);
    }

    @Test
    void testParameterCombinations() {
        // Test various parameter combinations
        String[] objectIds = {"obj1", "obj2", null, ""};
        String[] sources = {"src1", "src2", null, ""};
        String[] providers = {"prov1", "prov2", null, ""};
        
        for (String objectId : objectIds) {
            for (String source : sources) {
                for (String provider : providers) {
                    when(context.getIndexingItem(objectId, source, provider))
                            .thenReturn(Collections.emptyList());
                    
                    List<TurConnectorIndexing> result = context.getIndexingItem(objectId, source, provider);
                    
                    assertThat(result).isNotNull();
                    verify(context, times(1)).getIndexingItem(objectId, source, provider);
                }
            }
        }
    }
}