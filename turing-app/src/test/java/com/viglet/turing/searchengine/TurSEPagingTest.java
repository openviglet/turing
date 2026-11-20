/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.searchengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;

/** T1310 - moved from TurSolrUtilsTest and TurSolrQueryBuilderTest with the logic they cover. */
class TurSEPagingTest {

    @Test
    void firstRowPositionFromCurrentPageShouldCalculateCorrectly() {
        TurSEParameters params = createParams(2, 10);
        assertEquals(10, TurSEPaging.firstRow(params));
    }

    @Test
    void firstRowPositionFromFirstPageShouldBeZero() {
        TurSEParameters params = createParams(1, 10);
        assertEquals(0, TurSEPaging.firstRow(params));
    }

    @Test
    void firstRowPositionPage3Rows5ShouldBe10() {
        TurSEParameters params = createParams(3, 5);
        assertEquals(10, TurSEPaging.firstRow(params));
    }

    @Test
    void lastRowPositionFromCurrentPageShouldCalculateCorrectly() {
        TurSEParameters params = createParams(2, 10);
        assertEquals(20, TurSEPaging.lastRow(params));
    }

    @Test
    void lastRowPositionPage1Rows10ShouldBe10() {
        TurSEParameters params = createParams(1, 10);
        assertEquals(10, TurSEPaging.lastRow(params));
    }

    @Test
    void lastRowPositionPage5Rows3ShouldBe15() {
        TurSEParameters params = createParams(5, 3);
        assertEquals(15, TurSEPaging.lastRow(params));
    }

    @Test
    void firstRowPositionPage0Rows10ShouldBeClampedToZero() {
        // A client-supplied page <= 0 must not produce a negative Solr 'start'
        // (Solr rejects it with "'start' parameter cannot be negative").
        TurSEParameters params = createParams(0, 10);
        assertEquals(0, TurSEPaging.firstRow(params));
    }

    @Test
    void firstRowPositionNegativePageShouldBeClampedToZero() {
        TurSEParameters params = createParams(-3, 10);
        assertEquals(0, TurSEPaging.firstRow(params));
    }

    @Test
    void firstRowPositionLargePageShouldCalculate() {
        TurSEParameters params = createParams(100, 25);
        assertEquals(2475, TurSEPaging.firstRow(params));
    }

    @Test
    void lastRowPositionPage0Rows10ShouldBeZero() {
        TurSEParameters params = createParams(0, 10);
        assertEquals(0, TurSEPaging.lastRow(params));
    }

    @Test
    void hasGroupWhenGroupIsPresent() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setGroup("group");
        TurSEParameters parameters = new TurSEParameters(
                searchParams);

        assertThat(TurSEPaging.hasGroup(parameters)).isTrue();
    }

    @Test
    void hasGroupWhenGroupIsMissing() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        TurSEParameters parameters = new TurSEParameters(
                searchParams);

        assertThat(TurSEPaging.hasGroup(parameters)).isFalse();
    }

    @Test
    void hasGroupWithBlankStringReturnsFalse() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setGroup("   ");
        TurSEParameters parameters = new TurSEParameters(searchParams);

        // StringUtils.hasText returns false for blank strings
        assertThat(TurSEPaging.hasGroup(parameters)).isFalse();
    }

    private TurSEParameters createParams(int page, int rows) {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setP(page);
        searchParams.setRows(rows);
        return new TurSEParameters(searchParams);
    }
}
