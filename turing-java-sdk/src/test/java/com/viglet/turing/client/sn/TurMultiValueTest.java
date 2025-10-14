/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.viglet.turing.client.sn;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TurMultiValue.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurMultiValueTest {

    @Test
    void testDefaultConstructor() {
        TurMultiValue multiValue = new TurMultiValue();

        assertThat(multiValue).isEmpty();
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testConstructorWithOverride() {
        TurMultiValue multiValue = new TurMultiValue(true);

        assertThat(multiValue).isEmpty();
        assertThat(multiValue.isOverride()).isTrue();
    }

    @Test
    void testConstructorWithCollection() {
        List<String> items = Arrays.asList("item1", "item2", "item3");
        TurMultiValue multiValue = new TurMultiValue(items);

        assertThat(multiValue).hasSize(3);
        assertThat(multiValue).containsExactly("item1", "item2", "item3");
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testConstructorWithCollectionAndOverride() {
        List<String> items = Arrays.asList("item1", "item2");
        TurMultiValue multiValue = new TurMultiValue(items, true);

        assertThat(multiValue).hasSize(2);
        assertThat(multiValue).containsExactly("item1", "item2");
        assertThat(multiValue.isOverride()).isTrue();
    }

    @Test
    void testSingleItemString() {
        TurMultiValue multiValue = TurMultiValue.singleItem("test");

        assertThat(multiValue).hasSize(1);
        assertThat(multiValue).containsExactly("test");
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testSingleItemStringWithOverride() {
        TurMultiValue multiValue = TurMultiValue.singleItem("test", true);

        assertThat(multiValue).hasSize(1);
        assertThat(multiValue).containsExactly("test");
        assertThat(multiValue.isOverride()).isTrue();
    }

    @Test
    void testSingleItemBoolean() {
        TurMultiValue multiValueTrue = TurMultiValue.singleItem(true);
        TurMultiValue multiValueFalse = TurMultiValue.singleItem(false);

        assertThat(multiValueTrue).hasSize(1);
        assertThat(multiValueTrue).containsExactly("true");
        assertThat(multiValueTrue.isOverride()).isFalse();

        assertThat(multiValueFalse).hasSize(1);
        assertThat(multiValueFalse).containsExactly("false");
        assertThat(multiValueFalse.isOverride()).isFalse();
    }

    @Test
    void testSingleItemBooleanObject() {
        TurMultiValue multiValue = TurMultiValue.singleItem(Boolean.TRUE, true);

        assertThat(multiValue).hasSize(1);
        assertThat(multiValue).containsExactly("true");
        assertThat(multiValue.isOverride()).isTrue();
    }

    @Test
    void testSingleItemDate() {
        Date testDate = new Date(1609459200000L); // 2021-01-01 00:00:00 UTC
        TurMultiValue multiValue = TurMultiValue.singleItem(testDate);

        assertThat(multiValue).hasSize(1);
        assertThat(multiValue.get(0)).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testSingleItemDateWithOverride() {
        Date testDate = new Date(1609459200000L); // 2021-01-01 00:00:00 UTC
        TurMultiValue multiValue = TurMultiValue.singleItem(testDate, true);

        assertThat(multiValue).hasSize(1);
        assertThat(multiValue.get(0)).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(multiValue.isOverride()).isTrue();
    }

    @Test
    void testSingleItemNullDate() {
        Date nullDate = null;
        TurMultiValue multiValue = TurMultiValue.singleItem(nullDate, false);

        assertThat(multiValue).isEmpty();
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testSingleItemGenericObject() {
        Integer number = 42;
        TurMultiValue multiValue = TurMultiValue.singleItem(number);

        assertThat(multiValue).hasSize(1);
        assertThat(multiValue).containsExactly("42");
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testFromList() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        TurMultiValue multiValue = new TurMultiValue(list);

        assertThat(multiValue).hasSize(4);
        assertThat(multiValue).containsExactly("a", "b", "c", "d");
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testFromEmptyList() {
        List<String> emptyList = new ArrayList<>();
        TurMultiValue multiValue = new TurMultiValue(emptyList);

        assertThat(multiValue).isEmpty();
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testEmpty() {
        TurMultiValue multiValue = TurMultiValue.empty();

        assertThat(multiValue).isEmpty();
        assertThat(multiValue.isOverride()).isFalse();
    }

    @Test
    void testEqualsAndHashCode() {
        List<String> items1 = Arrays.asList("item1", "item2");
        List<String> items2 = Arrays.asList("item1", "item2");

        TurMultiValue multiValue1 = new TurMultiValue(items1);
        TurMultiValue multiValue2 = new TurMultiValue(items2);

        assertThat(multiValue1).isEqualTo(multiValue2);
        assertThat(multiValue1.hashCode()).isEqualTo(multiValue2.hashCode());
    }

    @Test
    void testNotEquals() {
        TurMultiValue multiValue1 = TurMultiValue.singleItem("item1");
        TurMultiValue multiValue2 = TurMultiValue.singleItem("item2");

        assertThat(multiValue1).isNotEqualTo(multiValue2);
    }

    @Test
    void testArrayListBehavior() {
        TurMultiValue multiValue = new TurMultiValue();

        // Test ArrayList functionality
        multiValue.add("first");
        multiValue.add("second");
        multiValue.add(1, "middle");

        assertThat(multiValue).hasSize(3);
        assertThat(multiValue).containsExactly("first", "middle", "second");
        assertThat(multiValue.get(1)).isEqualTo("middle");

        // Test removal
        multiValue.remove("middle");
        assertThat(multiValue).containsExactly("first", "second");
    }

    @Test
    void testDateFormatConstants() {
        assertThat(TurMultiValue.DATE_FORMAT).isEqualTo("yyyy-MM-dd'T'HH:mm:ss'Z'");
        assertThat(TurMultiValue.UTC).isEqualTo("UTC");
    }

    @Test
    void testDateFormattingConsistency() {
        // Test that the same date always produces the same formatted string
        Date testDate = new Date(1609459200000L); // 2021-01-01 00:00:00 UTC

        TurMultiValue multiValue1 = TurMultiValue.singleItem(testDate);
        TurMultiValue multiValue2 = TurMultiValue.singleItem(testDate);

        assertThat(multiValue1.get(0)).isEqualTo(multiValue2.get(0));
    }

    @Test
    void testCollectionWithNulls() {
        List<String> listWithNulls = Arrays.asList("item1", null, "item3");
        TurMultiValue multiValue = new TurMultiValue(listWithNulls);

        assertThat(multiValue).hasSize(3);
        assertThat(multiValue).containsExactly("item1", null, "item3");
    }

    @Test
    void testOverrideBehaviorPreservation() {
        // Test that override flag is preserved through operations
        TurMultiValue multiValue = new TurMultiValue(true);
        multiValue.add("item1");
        multiValue.add("item2");

        assertThat(multiValue.isOverride()).isTrue();
        assertThat(multiValue).hasSize(2);
    }

    @Test
    void testGenericTypeHandling() {
        // Test various generic types
        Double doubleValue = 3.14159;
        TurMultiValue multiValueDouble = TurMultiValue.singleItem(doubleValue);

        Long longValue = 123456789L;
        TurMultiValue multiValueLong = TurMultiValue.singleItem(longValue);

        assertThat(multiValueDouble).containsExactly("3.14159");
        assertThat(multiValueLong).containsExactly("123456789");
    }
}
