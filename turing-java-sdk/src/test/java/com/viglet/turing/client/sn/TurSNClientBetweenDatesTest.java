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

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurSNClientBetweenDates.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurSNClientBetweenDatesTest {

    @Test
    void testConstructorWithParameters() {
        Date startDate = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        Date endDate = new Date();
        
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("publishDate", startDate, endDate);
        
        assertThat(betweenDates.getField()).isEqualTo("publishDate");
        assertThat(betweenDates.getStartDate()).isEqualTo(startDate);
        assertThat(betweenDates.getEndDate()).isEqualTo(endDate);
    }

    @Test
    void testFieldSetterAndGetter() {
        Date now = new Date();
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("initialField", now, now);
        
        betweenDates.setField("modifiedDate");
        
        assertThat(betweenDates.getField()).isEqualTo("modifiedDate");
    }

    @Test
    void testStartDateSetterAndGetter() {
        Date initialDate = new Date(0);
        Date newStartDate = new Date(System.currentTimeMillis() - 86400000L);
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("date", initialDate, initialDate);
        
        betweenDates.setStartDate(newStartDate);
        
        assertThat(betweenDates.getStartDate()).isEqualTo(newStartDate);
    }

    @Test
    void testEndDateSetterAndGetter() {
        Date initialDate = new Date(0);
        Date newEndDate = new Date();
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("date", initialDate, initialDate);
        
        betweenDates.setEndDate(newEndDate);
        
        assertThat(betweenDates.getEndDate()).isEqualTo(newEndDate);
    }

    @Test
    void testNullValues() {
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates(null, null, null);
        
        assertThat(betweenDates.getField()).isNull();
        assertThat(betweenDates.getStartDate()).isNull();
        assertThat(betweenDates.getEndDate()).isNull();
    }

    @Test
    void testSetNullValues() {
        Date now = new Date();
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("field", now, now);
        
        betweenDates.setField(null);
        betweenDates.setStartDate(null);
        betweenDates.setEndDate(null);
        
        assertThat(betweenDates.getField()).isNull();
        assertThat(betweenDates.getStartDate()).isNull();
        assertThat(betweenDates.getEndDate()).isNull();
    }

    @Test
    void testEmptyFieldName() {
        Date now = new Date();
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("", now, now);
        
        assertThat(betweenDates.getField()).isEmpty();
    }

    @Test
    void testSameStartAndEndDates() {
        Date sameDate = new Date();
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("createdAt", sameDate, sameDate);
        
        assertThat(betweenDates.getStartDate()).isEqualTo(betweenDates.getEndDate());
    }

    @Test
    void testStartDateAfterEndDate() {
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("date", startDate, endDate);
        
        assertThat(betweenDates.getStartDate()).isAfter(betweenDates.getEndDate());
    }

    @Test
    void testFieldWithSpecialCharacters() {
        Date now = new Date();
        String fieldWithSpecialChars = "field_with-special.chars";
        
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates(fieldWithSpecialChars, now, now);
        
        assertThat(betweenDates.getField()).isEqualTo(fieldWithSpecialChars);
    }

    @Test
    void testDateRangeUpdate() {
        Date initialStart = new Date(0);
        Date initialEnd = new Date(86400000L);
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("date", initialStart, initialEnd);
        
        // Update to new range
        Date newStart = new Date(System.currentTimeMillis() - 86400000L);
        Date newEnd = new Date();
        
        betweenDates.setStartDate(newStart);
        betweenDates.setEndDate(newEnd);
        
        assertThat(betweenDates.getStartDate()).isEqualTo(newStart);
        assertThat(betweenDates.getEndDate()).isEqualTo(newEnd);
    }

    @Test
    void testTypicalUsageScenario() {
        // Simulate typical usage: last 30 days
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));
        Date now = new Date();
        
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("publishDate", thirtyDaysAgo, now);
        
        assertThat(betweenDates.getField()).isEqualTo("publishDate");
        assertThat(betweenDates.getStartDate()).isBefore(betweenDates.getEndDate());
        assertThat(betweenDates.getStartDate()).isBefore(now);
        assertThat(betweenDates.getEndDate()).isAfter(thirtyDaysAgo);
    }

    @Test
    void testVeryOldDates() {
        Date epochDate = new Date(0); // 1970-01-01
        Date oldDate = new Date(946684800000L); // 2000-01-01
        
        TurSNClientBetweenDates betweenDates = new TurSNClientBetweenDates("legacyDate", epochDate, oldDate);
        
        assertThat(betweenDates.getStartDate()).isEqualTo(epochDate);
        assertThat(betweenDates.getEndDate()).isEqualTo(oldDate);
    }
}