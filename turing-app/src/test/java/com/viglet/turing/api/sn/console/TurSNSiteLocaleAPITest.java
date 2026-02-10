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

package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.sn.template.TurSNTemplate;

/**
 * Unit tests for TurSNSiteLocaleAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteLocaleAPITest {

    @Test
    void testLocaleListReturnsEmptyWhenMissing() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNTemplate template = mock(TurSNTemplate.class);
        TurSNSiteLocaleAPI api = new TurSNSiteLocaleAPI(siteRepository, localeRepository, template);

        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        List<TurSNSiteLocale> result = api.turSNSiteLocaleList("site");

        assertThat(result).isEqualTo(Collections.emptyList());
    }

    @Test
    void testLocaleAddSetsCore() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNTemplate template = mock(TurSNTemplate.class);
        TurSNSiteLocaleAPI api = new TurSNSiteLocaleAPI(siteRepository, localeRepository, template);
        TurSNSite site = new TurSNSite();
        TurSNSiteLocale locale = new TurSNSiteLocale();

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(template.createSolrCore(locale, "admin")).thenReturn("core");

        TurSNSiteLocale result = api.turSNSiteLocaleAdd(locale, (Principal) () -> "admin", "site");

        assertThat(result.getCore()).isEqualTo("core");
        verify(localeRepository).save(locale);
    }

    @Test
    void testLocaleDeleteReturnsFalseWhenMissing() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNTemplate template = mock(TurSNTemplate.class);
        TurSNSiteLocaleAPI api = new TurSNSiteLocaleAPI(siteRepository, localeRepository, template);

        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        boolean result = api.turSNSiteLocaleDelete("id", "site");

        assertThat(result).isFalse();
    }

    @Test
    void testLocaleStructureDefaultsLanguage() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNTemplate template = mock(TurSNTemplate.class);
        TurSNSiteLocaleAPI api = new TurSNSiteLocaleAPI(siteRepository, localeRepository, template);
        TurSNSite site = new TurSNSite();

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));

        TurSNSiteLocale result = api.turSNSiteLocaleStructure("site");

        assertThat(result.getLanguage()).isEqualTo(Locale.US);
        assertThat(result.getTurSNSite()).isSameAs(site);
    }
}
