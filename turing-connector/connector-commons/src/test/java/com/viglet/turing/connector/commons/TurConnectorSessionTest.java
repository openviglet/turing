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
package com.viglet.turing.connector.commons;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurConnectorSession.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurConnectorSessionTest {

    @Test
    void testConstructorWithValidParameters() {
        String source = "test-source";
        Collection<String> sites = Arrays.asList("site1", "site2");
        String providerName = "test-provider";
        Locale locale = Locale.ENGLISH;

        TurConnectorSession session = new TurConnectorSession(source, sites, providerName, locale);

        assertThat(session.getSource()).isEqualTo(source);
        assertThat(session.getSites()).isEqualTo(sites);
        assertThat(session.getProviderName()).isEqualTo(providerName);
        assertThat(session.getLocale()).isEqualTo(locale);
        assertThat(session.getTransactionId()).isNotNull().isNotEmpty();
    }

    @Test
    void testConstructorGeneratesUniqueTransactionId() {
        String source = "test-source";
        Collection<String> sites = Collections.singletonList("site1");
        String providerName = "test-provider";
        Locale locale = Locale.FRENCH;

        TurConnectorSession session1 = new TurConnectorSession(source, sites, providerName, locale);
        TurConnectorSession session2 = new TurConnectorSession(source, sites, providerName, locale);

        assertThat(session1.getTransactionId()).isNotEqualTo(session2.getTransactionId());
    }

    @Test
    void testConstructorWithEmptySites() {
        String source = "test-source";
        Collection<String> sites = Collections.emptyList();
        String providerName = "test-provider";
        Locale locale = Locale.GERMAN;

        TurConnectorSession session = new TurConnectorSession(source, sites, providerName, locale);

        assertThat(session.getSource()).isEqualTo(source);
        assertThat(session.getSites()).isEmpty();
        assertThat(session.getProviderName()).isEqualTo(providerName);
        assertThat(session.getLocale()).isEqualTo(locale);
        assertThat(session.getTransactionId()).isNotNull().isNotEmpty();
    }

    @Test
    void testConstructorWithNullValues() {
        TurConnectorSession session = new TurConnectorSession(null, null, null, null);

        assertThat(session.getSource()).isNull();
        assertThat(session.getSites()).isNull();
        assertThat(session.getProviderName()).isNull();
        assertThat(session.getLocale()).isNull();
        assertThat(session.getTransactionId()).isNotNull().isNotEmpty();
    }

    @Test
    void testLombokGeneratedMethods() {
        String source = "test-source";
        Collection<String> sites = Arrays.asList("site1", "site2");
        String providerName = "test-provider";
        Locale locale = Locale.ENGLISH;

        TurConnectorSession session = new TurConnectorSession(source, sites, providerName, locale);
        
        // Test setters
        session.setSource("new-source");
        session.setProviderName("new-provider");
        session.setLocale(Locale.JAPANESE);
        session.setTransactionId("custom-transaction-id");
        
        assertThat(session.getSource()).isEqualTo("new-source");
        assertThat(session.getProviderName()).isEqualTo("new-provider");
        assertThat(session.getLocale()).isEqualTo(Locale.JAPANESE);
        assertThat(session.getTransactionId()).isEqualTo("custom-transaction-id");
    }

    @Test
    void testEquals() {
        String source = "test-source";
        Collection<String> sites = Arrays.asList("site1", "site2");
        String providerName = "test-provider";
        Locale locale = Locale.ENGLISH;

        TurConnectorSession session1 = new TurConnectorSession(source, sites, providerName, locale);
        TurConnectorSession session2 = new TurConnectorSession(source, sites, providerName, locale);
        
        // Set same transaction ID to make them equal
        session2.setTransactionId(session1.getTransactionId());

        assertThat(session1).isEqualTo(session2);
        assertThat(session1.hashCode()).isEqualTo(session2.hashCode());
    }

    @Test
    void testToString() {
        String source = "test-source";
        Collection<String> sites = Arrays.asList("site1", "site2");
        String providerName = "test-provider";
        Locale locale = Locale.ENGLISH;

        TurConnectorSession session = new TurConnectorSession(source, sites, providerName, locale);
        String toString = session.toString();

        assertThat(toString).contains("TurConnectorSession");
        assertThat(toString).contains("test-source");
        assertThat(toString).contains("test-provider");
        assertThat(toString).contains(session.getTransactionId());
    }
}