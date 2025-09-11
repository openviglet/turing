/*
 * Copyright (C) 2016-2021 the original author or authors.
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

package com.viglet.turing.client.sn;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurSNSortField.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurSNSortFieldTest {

    @Test
    void testDefaultConstructor() {
        TurSNSortField sortField = new TurSNSortField();
        
        assertThat(sortField.getField()).isNull();
        assertThat(sortField.getSort()).isNull();
    }

    @Test
    void testFieldSetterAndGetter() {
        TurSNSortField sortField = new TurSNSortField();
        
        sortField.setField("title");
        
        assertThat(sortField.getField()).isEqualTo("title");
    }

    @Test
    void testSortSetterAndGetter() {
        TurSNSortField sortField = new TurSNSortField();
        
        sortField.setSort(TurSNQuery.ORDER.asc);
        
        assertThat(sortField.getSort()).isEqualTo(TurSNQuery.ORDER.asc);
    }

    @Test
    void testCompleteConfiguration() {
        TurSNSortField sortField = new TurSNSortField();
        
        sortField.setField("publishDate");
        sortField.setSort(TurSNQuery.ORDER.desc);
        
        assertThat(sortField.getField()).isEqualTo("publishDate");
        assertThat(sortField.getSort()).isEqualTo(TurSNQuery.ORDER.desc);
    }

    @Test
    void testNullValues() {
        TurSNSortField sortField = new TurSNSortField();
        
        sortField.setField(null);
        sortField.setSort(null);
        
        assertThat(sortField.getField()).isNull();
        assertThat(sortField.getSort()).isNull();
    }

    @Test
    void testFieldUpdate() {
        TurSNSortField sortField = new TurSNSortField();
        
        // Set initial value
        sortField.setField("title");
        assertThat(sortField.getField()).isEqualTo("title");
        
        // Update value
        sortField.setField("date");
        assertThat(sortField.getField()).isEqualTo("date");
    }

    @Test
    void testSortOrderUpdate() {
        TurSNSortField sortField = new TurSNSortField();
        
        // Set initial order
        sortField.setSort(TurSNQuery.ORDER.asc);
        assertThat(sortField.getSort()).isEqualTo(TurSNQuery.ORDER.asc);
        
        // Update order
        sortField.setSort(TurSNQuery.ORDER.desc);
        assertThat(sortField.getSort()).isEqualTo(TurSNQuery.ORDER.desc);
    }

    @Test
    void testEmptyStringField() {
        TurSNSortField sortField = new TurSNSortField();
        
        sortField.setField("");
        
        assertThat(sortField.getField()).isEmpty();
    }

    @Test
    void testSpecialCharactersInField() {
        TurSNSortField sortField = new TurSNSortField();
        
        sortField.setField("field_with_underscores");
        assertThat(sortField.getField()).isEqualTo("field_with_underscores");
        
        sortField.setField("field-with-dashes");
        assertThat(sortField.getField()).isEqualTo("field-with-dashes");
        
        sortField.setField("field.with.dots");
        assertThat(sortField.getField()).isEqualTo("field.with.dots");
    }
}