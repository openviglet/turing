/*
 * Copyright (C) 2016-2025 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.persistence.model.sn.metric;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TurSNSiteMetricAccess.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteMetricAccessTest {

    @Mock
    private TurSNSite turSNSite;

    private TurSNSiteMetricAccess turSNSiteMetricAccess;

    @BeforeEach
    void setUp() {
        turSNSiteMetricAccess = new TurSNSiteMetricAccess();
    }

    @Test
    void testGettersAndSetters() {
        String id = "metric-id-123";
        String userId = "user-123";
        Instant accessDate = Instant.now();
        String term = "Search Term";
        Locale language = Locale.ENGLISH;
        long numFound = 42L;

        turSNSiteMetricAccess.setId(id);
        turSNSiteMetricAccess.setUserId(userId);
        turSNSiteMetricAccess.setAccessDate(accessDate);
        turSNSiteMetricAccess.setTerm(term);
        turSNSiteMetricAccess.setLanguage(language);
        turSNSiteMetricAccess.setTurSNSite(turSNSite);
        turSNSiteMetricAccess.setNumFound(numFound);

        assertThat(turSNSiteMetricAccess.getId()).isEqualTo(id);
        assertThat(turSNSiteMetricAccess.getUserId()).isEqualTo(userId);
        assertThat(turSNSiteMetricAccess.getAccessDate()).isEqualTo(accessDate);
        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getLanguage()).isEqualTo(language);
        assertThat(turSNSiteMetricAccess.getTurSNSite()).isEqualTo(turSNSite);
        assertThat(turSNSiteMetricAccess.getNumFound()).isEqualTo(numFound);
    }

    @Test
    void testDefaultConstructor() {
        assertThat(turSNSiteMetricAccess).isNotNull();
        assertThat(turSNSiteMetricAccess.getId()).isNull();
        assertThat(turSNSiteMetricAccess.getUserId()).isNull();
        assertThat(turSNSiteMetricAccess.getAccessDate()).isNull();
        assertThat(turSNSiteMetricAccess.getTerm()).isNull();
        assertThat(turSNSiteMetricAccess.getLanguage()).isNull();
        assertThat(turSNSiteMetricAccess.getTurSNSite()).isNull();
    }

    @Test
    void testSetTermSanitization() {
        String originalTerm = "Café Français";
        turSNSiteMetricAccess.setTerm(originalTerm);

        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(originalTerm);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("cafe francais");
    }

    @Test
    void testSetTermWithMultipleSpaces() {
        String term = "search   with    multiple     spaces";
        turSNSiteMetricAccess.setTerm(term);

        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("search with multiple spaces");
    }

    @Test
    void testSetTermWithSpecialCharacters() {
        String term = "Søren Kierkegård";
        turSNSiteMetricAccess.setTerm(term);

        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isNotNull();
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).doesNotContain("ø");
    }

    @Test
    void testSetTermWithUpperCase() {
        String term = "UPPER CASE SEARCH";
        turSNSiteMetricAccess.setTerm(term);

        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("upper case search");
    }

    @Test
    void testSetTermWithTrailingSpaces() {
        String term = "  search term  ";
        turSNSiteMetricAccess.setTerm(term);

        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("search term");
    }

    @Test
    void testTargetingRulesInitialization() {
        assertThat(turSNSiteMetricAccess.getTargetingRules()).isNotNull();
    }

    @Test
    void testSetTargetingRules() {
        Set<String> rules = new HashSet<>();
        rules.add("rule1");
        rules.add("rule2");
        rules.add("rule3");

        turSNSiteMetricAccess.setTargetingRules(rules);

        assertThat(turSNSiteMetricAccess.getTargetingRules()).hasSize(3);
        assertThat(turSNSiteMetricAccess.getTargetingRules()).containsExactlyInAnyOrder("rule1", "rule2", "rule3");
    }

    @Test
    void testSetTargetingRulesEmpty() {
        Set<String> emptyRules = new HashSet<>();
        turSNSiteMetricAccess.setTargetingRules(emptyRules);

        assertThat(turSNSiteMetricAccess.getTargetingRules()).isEmpty();
    }

    @Test
    void testAccessDateField() {
        Instant now = Instant.now();
        turSNSiteMetricAccess.setAccessDate(now);

        assertThat(turSNSiteMetricAccess.getAccessDate()).isEqualTo(now);
    }

    @Test
    void testLanguageField() {
        turSNSiteMetricAccess.setLanguage(Locale.FRENCH);
        assertThat(turSNSiteMetricAccess.getLanguage()).isEqualTo(Locale.FRENCH);

        turSNSiteMetricAccess.setLanguage(Locale.GERMAN);
        assertThat(turSNSiteMetricAccess.getLanguage()).isEqualTo(Locale.GERMAN);
    }

    @Test
    void testNumFoundField() {
        turSNSiteMetricAccess.setNumFound(100L);
        assertThat(turSNSiteMetricAccess.getNumFound()).isEqualTo(100L);

        turSNSiteMetricAccess.setNumFound(0L);
        assertThat(turSNSiteMetricAccess.getNumFound()).isEqualTo(0L);

        turSNSiteMetricAccess.setNumFound(999999L);
        assertThat(turSNSiteMetricAccess.getNumFound()).isEqualTo(999999L);
    }

    @Test
    void testUserIdField() {
        String userId1 = "user-abc-123";
        turSNSiteMetricAccess.setUserId(userId1);
        assertThat(turSNSiteMetricAccess.getUserId()).isEqualTo(userId1);

        String userId2 = "user-xyz-789";
        turSNSiteMetricAccess.setUserId(userId2);
        assertThat(turSNSiteMetricAccess.getUserId()).isEqualTo(userId2);
    }

    @Test
    void testSiteRelationship() {
        turSNSiteMetricAccess.setTurSNSite(turSNSite);
        assertThat(turSNSiteMetricAccess.getTurSNSite()).isEqualTo(turSNSite);
    }

    @Test
    void testCompleteMetricAccess() {
        String id = "metric-complete-123";
        String userId = "user-complete";
        Instant accessDate = Instant.parse("2025-01-10T10:15:30.00Z");
        String term = "Complete Search Term";
        Locale language = Locale.US;
        long numFound = 50L;
        Set<String> rules = new HashSet<>();
        rules.add("rule-A");
        rules.add("rule-B");

        turSNSiteMetricAccess.setId(id);
        turSNSiteMetricAccess.setUserId(userId);
        turSNSiteMetricAccess.setAccessDate(accessDate);
        turSNSiteMetricAccess.setTerm(term);
        turSNSiteMetricAccess.setLanguage(language);
        turSNSiteMetricAccess.setTurSNSite(turSNSite);
        turSNSiteMetricAccess.setNumFound(numFound);
        turSNSiteMetricAccess.setTargetingRules(rules);

        assertThat(turSNSiteMetricAccess.getId()).isEqualTo(id);
        assertThat(turSNSiteMetricAccess.getUserId()).isEqualTo(userId);
        assertThat(turSNSiteMetricAccess.getAccessDate()).isEqualTo(accessDate);
        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("complete search term");
        assertThat(turSNSiteMetricAccess.getLanguage()).isEqualTo(language);
        assertThat(turSNSiteMetricAccess.getTurSNSite()).isEqualTo(turSNSite);
        assertThat(turSNSiteMetricAccess.getNumFound()).isEqualTo(numFound);
        assertThat(turSNSiteMetricAccess.getTargetingRules()).hasSize(2);
    }

    @Test
    void testSetTermMultipleTimes() {
        turSNSiteMetricAccess.setTerm("First Term");
        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo("First Term");
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("first term");

        turSNSiteMetricAccess.setTerm("Second Term");
        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo("Second Term");
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("second term");
    }

    @Test
    void testSanitizedTermWithNumbers() {
        String term = "Product 123 Model 456";
        turSNSiteMetricAccess.setTerm(term);

        assertThat(turSNSiteMetricAccess.getTerm()).isEqualTo(term);
        assertThat(turSNSiteMetricAccess.getSanatizedTerm()).isEqualTo("product 123 model 456");
    }

    @Test
    void testEmptyTargetingRules() {
        Set<String> rules = new HashSet<>();
        turSNSiteMetricAccess.setTargetingRules(rules);

        assertThat(turSNSiteMetricAccess.getTargetingRules()).isNotNull().isEmpty();
    }
}
