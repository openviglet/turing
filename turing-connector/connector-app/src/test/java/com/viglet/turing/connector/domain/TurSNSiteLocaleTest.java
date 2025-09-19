/*
 * Copyright (C) 2016-2025 the original author or authors.
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
package com.viglet.turing.connector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.Serializable;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TurSNSiteLocale.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurSNSiteLocaleTest {

    @Test
    void testDefaultConstructor() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();

        assertThat(siteLocale.getLanguage()).isNull();
        assertThat(siteLocale.getCore()).isNull();
        assertThat(siteLocale.getTurSNSite()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();
        Locale language = Locale.ENGLISH;
        String core = "test-core";
        TurSNSite turSNSite = new TurSNSite();

        siteLocale.setLanguage(language);
        siteLocale.setCore(core);
        siteLocale.setTurSNSite(turSNSite);

        assertThat(siteLocale.getLanguage()).isEqualTo(language);
        assertThat(siteLocale.getCore()).isEqualTo(core);
        assertThat(siteLocale.getTurSNSite()).isEqualTo(turSNSite);
    }

    @Test
    void testWithDifferentLocales() {
        TurSNSiteLocale englishSite = new TurSNSiteLocale();
        englishSite.setLanguage(Locale.ENGLISH);
        englishSite.setCore("english-core");

        TurSNSiteLocale frenchSite = new TurSNSiteLocale();
        frenchSite.setLanguage(Locale.FRENCH);
        frenchSite.setCore("french-core");

        TurSNSiteLocale germanSite = new TurSNSiteLocale();
        germanSite.setLanguage(Locale.GERMAN);
        germanSite.setCore("german-core");

        assertThat(englishSite.getLanguage()).isEqualTo(Locale.ENGLISH);
        assertThat(englishSite.getCore()).isEqualTo("english-core");

        assertThat(frenchSite.getLanguage()).isEqualTo(Locale.FRENCH);
        assertThat(frenchSite.getCore()).isEqualTo("french-core");

        assertThat(germanSite.getLanguage()).isEqualTo(Locale.GERMAN);
        assertThat(germanSite.getCore()).isEqualTo("german-core");
    }

    @Test
    void testWithCustomLocale() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();
        Locale customLocale = new Locale("pt", "BR"); // Portuguese Brazil

        siteLocale.setLanguage(customLocale);
        siteLocale.setCore("portuguese-brazil-core");

        assertThat(siteLocale.getLanguage()).isEqualTo(customLocale);
        assertThat(siteLocale.getLanguage().getLanguage()).isEqualTo("pt");
        assertThat(siteLocale.getLanguage().getCountry()).isEqualTo("BR");
        assertThat(siteLocale.getCore()).isEqualTo("portuguese-brazil-core");
    }

    @Test
    void testSerializableInterface() {
        assertThat(TurSNSiteLocale.class).isAssignableTo(Serializable.class);
    }

    @Test
    void testWithNullValues() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();

        siteLocale.setLanguage(null);
        siteLocale.setCore(null);
        siteLocale.setTurSNSite(null);

        assertThat(siteLocale.getLanguage()).isNull();
        assertThat(siteLocale.getCore()).isNull();
        assertThat(siteLocale.getTurSNSite()).isNull();
    }

    @Test
    void testWithEmptyStrings() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();

        siteLocale.setCore("");

        assertThat(siteLocale.getCore()).isEqualTo("");
        assertThat(siteLocale.getCore()).isEmpty();
    }

    @Test
    void testRelationshipWithTurSNSite() {
        TurSNSite turSNSite = new TurSNSite();
        // Assuming TurSNSite has a name field based on typical patterns

        TurSNSiteLocale siteLocale = new TurSNSiteLocale();
        siteLocale.setLanguage(Locale.JAPANESE);
        siteLocale.setCore("japanese-core");
        siteLocale.setTurSNSite(turSNSite);

        assertThat(siteLocale.getTurSNSite()).isSameAs(turSNSite);
        assertThat(siteLocale.getLanguage()).isEqualTo(Locale.JAPANESE);
        assertThat(siteLocale.getCore()).isEqualTo("japanese-core");
    }

    @Test
    void testMultipleSiteLocalesWithSameSite() {
        TurSNSite sharedSite = new TurSNSite();

        TurSNSiteLocale locale1 = new TurSNSiteLocale();
        locale1.setLanguage(Locale.ENGLISH);
        locale1.setCore("en-core");
        locale1.setTurSNSite(sharedSite);

        TurSNSiteLocale locale2 = new TurSNSiteLocale();
        locale2.setLanguage(Locale.FRENCH);
        locale2.setCore("fr-core");
        locale2.setTurSNSite(sharedSite);

        assertThat(locale1.getTurSNSite()).isSameAs(locale2.getTurSNSite());
        assertThat(locale1.getLanguage()).isNotEqualTo(locale2.getLanguage());
        assertThat(locale1.getCore()).isNotEqualTo(locale2.getCore());
    }

    @Test
    void testCoreNamingConventions() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();

        // Test various core naming patterns
        String[] coreNames = {"simple-core", "core_with_underscores", "CoreWithCamelCase",
                "core123", "site.core.name", "production-en-US-core"};

        for (String coreName : coreNames) {
            siteLocale.setCore(coreName);
            assertThat(siteLocale.getCore()).isEqualTo(coreName);
        }
    }

    @Test
    void testLombokGeneratedMethods() {
        TurSNSiteLocale siteLocale1 = new TurSNSiteLocale();
        siteLocale1.setLanguage(Locale.ENGLISH);
        siteLocale1.setCore("test-core");

        TurSNSiteLocale siteLocale2 = new TurSNSiteLocale();
        siteLocale2.setLanguage(Locale.ENGLISH);
        siteLocale2.setCore("test-core");

        // Note: Lombok @Getter and @Setter don't generate equals/hashCode by default
        // These objects will use Object's equals/hashCode unless explicitly added
        assertThat(siteLocale1).isNotEqualTo(siteLocale2); // Different instances
        assertThat(siteLocale1.getLanguage()).isEqualTo(siteLocale2.getLanguage());
        assertThat(siteLocale1.getCore()).isEqualTo(siteLocale2.getCore());
    }

    @Test
    void testCompleteConfiguration() {
        TurSNSite site = new TurSNSite();
        Locale locale = new Locale("zh", "CN"); // Chinese China
        String core = "chinese-china-production-core";

        TurSNSiteLocale siteLocale = new TurSNSiteLocale();
        siteLocale.setTurSNSite(site);
        siteLocale.setLanguage(locale);
        siteLocale.setCore(core);

        assertThat(siteLocale.getTurSNSite()).isEqualTo(site);
        assertThat(siteLocale.getLanguage().getLanguage()).isEqualTo("zh");
        assertThat(siteLocale.getLanguage().getCountry()).isEqualTo("CN");
        assertThat(siteLocale.getCore()).isEqualTo(core);
    }

    @Test
    void testFieldModification() {
        TurSNSiteLocale siteLocale = new TurSNSiteLocale();

        // Set initial values
        siteLocale.setLanguage(Locale.ENGLISH);
        siteLocale.setCore("initial-core");

        // Modify values
        siteLocale.setLanguage(Locale.FRENCH);
        siteLocale.setCore("modified-core");

        // Verify modifications
        assertThat(siteLocale.getLanguage()).isEqualTo(Locale.FRENCH);
        assertThat(siteLocale.getCore()).isEqualTo("modified-core");
    }
}
