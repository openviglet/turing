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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for TurSNQuery.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurSNQueryTest {

    @Test
    void testDefaultConstructor() {
        TurSNQuery query = new TurSNQuery();

        assertThat(query.getQuery()).isNull();
        assertThat(query.getRows()).isZero();
        assertThat(query.getGroupBy()).isNull();
        assertThat(query.getSortField()).isNull();
        assertThat(query.getBetweenDates()).isNull();
        assertThat(query.getFieldQueries()).isNull();
        assertThat(query.getTargetingRules()).isNull();
        assertThat(query.getPageNumber()).isZero();
        assertThat(query.isPopulateMetrics()).isFalse();
    }

    @Test
    void testQuerySetterAndGetter() {
        TurSNQuery query = new TurSNQuery();

        query.setQuery("test query");

        assertThat(query.getQuery()).hasToString("test query");
    }

    @Test
    void testRowsSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();

        query.setRows(50);

        assertThat(query.getRows()).isEqualTo(50);
    }

    @Test
    void testGroupBySetterAndGetter() {
        TurSNQuery query = new TurSNQuery();

        query.setGroupBy("category");

        assertThat(query.getGroupBy()).isEqualTo("category");
    }

    @Test
    void testSortFieldSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();
        TurSNSortField sortField = new TurSNSortField();

        query.setSortField(sortField);

        assertThat(query.getSortField()).isEqualTo(sortField);
    }

    @Test
    void testSetSortFieldWithFieldAndOrder() {
        TurSNQuery query = new TurSNQuery();

        query.setSortField("title", TurSNQuery.Order.asc);

        assertThat(query.getSortField()).isNotNull();
        assertThat(query.getSortField().getField()).isEqualTo("title");
        assertThat(query.getSortField().getSort()).isEqualTo(TurSNQuery.Order.asc);
    }

    @Test
    void testSetSortFieldWithOrderOnly() {
        TurSNQuery query = new TurSNQuery();

        query.setSortField(TurSNQuery.Order.desc);

        assertThat(query.getSortField()).isNotNull();
        assertThat(query.getSortField().getField()).isNull();
        assertThat(query.getSortField().getSort()).isEqualTo(TurSNQuery.Order.desc);
    }

    @Test
    void testSetSortFieldOverwritesExisting() {
        TurSNQuery query = new TurSNQuery();

        // Set initial sort field
        query.setSortField("date", TurSNQuery.Order.desc);
        assertThat(query.getSortField().getField()).isEqualTo("date");
        assertThat(query.getSortField().getSort()).isEqualTo(TurSNQuery.Order.desc);

        // Overwrite with new field
        query.setSortField("title", TurSNQuery.Order.asc);
        assertThat(query.getSortField().getField()).isEqualTo("title");
        assertThat(query.getSortField().getSort()).isEqualTo(TurSNQuery.Order.asc);
    }

    @Test
    void testBetweenDatesSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();
        Date now = new Date();
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("testField", now, now);

        query.setBetweenDates(betweenDates);

        assertThat(query.getBetweenDates()).isEqualTo(betweenDates);
    }

    @Test
    void testSetBetweenDatesWithParameters() {
        TurSNQuery query = new TurSNQuery();
        Date startDate = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        Date endDate = new Date();

        query.setBetweenDates("publishDate", startDate, endDate);

        assertThat(query.getBetweenDates()).isNotNull();
        assertThat(query.getBetweenDates().getField()).isEqualTo("publishDate");
        assertThat(query.getBetweenDates().getStartDate()).isEqualTo(startDate);
        assertThat(query.getBetweenDates().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void testAddFilterQuerySingle() {
        TurSNQuery query = new TurSNQuery();

        query.addFilterQuery("category:news");

        assertThat(query.getFieldQueries()).hasSize(1);
        assertThat(query.getFieldQueries()).contains("category:news");
    }

    @Test
    void testAddFilterQueryMultiple() {
        TurSNQuery query = new TurSNQuery();

        query.addFilterQuery("category:news", "status:published", "author:john");

        assertThat(query.getFieldQueries()).hasSize(3);
        assertThat(query.getFieldQueries()).containsExactly("category:news", "status:published", "author:john");
    }

    @Test
    void testAddFilterQueryMultipleCalls() {
        TurSNQuery query = new TurSNQuery();

        query.addFilterQuery("category:news");
        query.addFilterQuery("status:published");

        assertThat(query.getFieldQueries()).hasSize(2);
        assertThat(query.getFieldQueries()).containsExactly("category:news", "status:published");
    }

    @Test
    void testFieldQueriesSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();
        List<String> fieldQueries = Arrays.asList("type:article", "lang:en");

        query.setFieldQueries(fieldQueries);

        assertThat(query.getFieldQueries()).isEqualTo(fieldQueries);
    }

    @Test
    void testAddTargetingRuleSingle() {
        TurSNQuery query = new TurSNQuery();

        query.addTargetingRule("location:US");

        assertThat(query.getTargetingRules()).hasSize(1);
        assertThat(query.getTargetingRules()).contains("location:US");
    }

    @Test
    void testAddTargetingRuleMultiple() {
        TurSNQuery query = new TurSNQuery();

        query.addTargetingRule("location:US", "age:adult", "interest:tech");

        assertThat(query.getTargetingRules()).hasSize(3);
        assertThat(query.getTargetingRules()).containsExactly("location:US", "age:adult", "interest:tech");
    }

    @Test
    void testTargetingRulesSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();
        List<String> targetingRules = Arrays.asList("segment:premium", "tier:gold");

        query.setTargetingRules(targetingRules);

        assertThat(query.getTargetingRules()).isEqualTo(targetingRules);
    }

    @Test
    void testPageNumberSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();

        query.setPageNumber(5);

        assertThat(query.getPageNumber()).isEqualTo(5);
    }

    @Test
    void testPopulateMetricsSetterAndGetter() {
        TurSNQuery query = new TurSNQuery();

        query.setPopulateMetrics(true);

        assertThat(query.isPopulateMetrics()).isTrue();
    }

    @Test
    void testCompleteQueryConfiguration() {
        TurSNQuery query = new TurSNQuery();

        // Configure all aspects of the query
        query.setQuery("search term");
        query.setRows(20);
        query.setGroupBy("category");
        query.setSortField("date", TurSNQuery.Order.desc);
        query.setBetweenDates("publishDate", new Date(0), new Date());
        query.addFilterQuery("status:active", "type:article");
        query.addTargetingRule("location:US");
        query.setPageNumber(2);
        query.setPopulateMetrics(true);

        // Verify all settings
        assertThat(query.getQuery()).isEqualTo("search term");
        assertThat(query.getRows()).isEqualTo(20);
        assertThat(query.getGroupBy()).isEqualTo("category");
        assertThat(query.getSortField().getField()).isEqualTo("date");
        assertThat(query.getSortField().getSort()).isEqualTo(TurSNQuery.Order.desc);
        assertThat(query.getBetweenDates()).isNotNull();
        assertThat(query.getFieldQueries()).hasSize(2);
        assertThat(query.getTargetingRules()).hasSize(1);
        assertThat(query.getPageNumber()).isEqualTo(2);
        assertThat(query.isPopulateMetrics()).isTrue();
    }

    @Test
    void testOrderEnum() {
        // Test enum values
        assertThat(TurSNQuery.Order.asc).isNotNull();
        assertThat(TurSNQuery.Order.desc).isNotNull();

        // Test enum string representations
        assertThat(TurSNQuery.Order.asc.toString()).hasToString("asc");
        assertThat(TurSNQuery.Order.desc.toString()).hasToString("desc");
    }

    @Test
    void testNullHandling() {
        TurSNQuery query = new TurSNQuery();

        // Test setting null values
        query.setQuery(null);
        query.setGroupBy(null);
        query.setSortField((TurSNSortField) null);
        query.setBetweenDates(null);
        query.setFieldQueries(null);
        query.setTargetingRules(null);

        assertThat(query.getQuery()).isNull();
        assertThat(query.getGroupBy()).isNull();
        assertThat(query.getSortField()).isNull();
        assertThat(query.getBetweenDates()).isNull();
        assertThat(query.getFieldQueries()).isNull();
        assertThat(query.getTargetingRules()).isNull();
    }
}