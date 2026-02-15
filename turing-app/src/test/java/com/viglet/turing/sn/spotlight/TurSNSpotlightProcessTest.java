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

package com.viglet.turing.sn.spotlight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlight;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightDocumentRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightTermRepository;
import com.viglet.turing.solr.TurSolr;

/**
 * Unit tests for TurSNSpotlightProcess.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSNSpotlightProcessTest {

    @Mock
    private TurSNSiteSpotlightRepository turSNSiteSpotlightRepository;

    @Mock
    private TurSNSiteSpotlightTermRepository turSNSiteSpotlightTermRepository;

    @Mock
    private TurSNSiteSpotlightDocumentRepository turSNSiteSpotlightDocumentRepository;

    @Mock
    private TurSolr turSolr;

    @Mock
    private TurSpotlightCache turSpotlightCache;

    @Mock
    private TurSNSiteLocaleRepository turSNSiteLocaleRepository;

    @Test
    void testIsSpotlightJobReturnsTrueForSpotlightType() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        when(jobItem.getAttributes()).thenReturn(Map.of(TurSNFieldName.TYPE, "TUR_SPOTLIGHT"));

        TurSNSpotlightProcess process = new TurSNSpotlightProcess(turSNSiteSpotlightRepository,
                turSNSiteSpotlightTermRepository, turSNSiteSpotlightDocumentRepository, turSolr,
                turSpotlightCache, turSNSiteLocaleRepository);

        assertThat(process.isSpotlightJob(jobItem)).isTrue();
    }

    @Test
    void testDeleteUnmanagedSpotlightRemovesEntriesById() {
        TurSNJobItem jobItem = mock(TurSNJobItem.class);
        when(jobItem.getAttributes()).thenReturn(Map.of(TurSNFieldName.ID, "spotlight-id"));
        when(jobItem.getLocale()).thenReturn(Locale.US);

        TurSNSite site = new TurSNSite();
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setLanguage(Locale.US);
        when(turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(site, Locale.US))
                .thenReturn(locale);

        Set<TurSNSiteSpotlight> spotlights = Set.of(new TurSNSiteSpotlight());
        when(turSNSiteSpotlightRepository.findByUnmanagedIdAndTurSNSiteAndLanguage(
                eq("spotlight-id"), eq(site), eq(Locale.US)))
                .thenReturn(spotlights);

        TurSNSpotlightProcess process = new TurSNSpotlightProcess(turSNSiteSpotlightRepository,
                turSNSiteSpotlightTermRepository, turSNSiteSpotlightDocumentRepository, turSolr,
                turSpotlightCache, turSNSiteLocaleRepository);

        assertThat(process.deleteUnmanagedSpotlight(jobItem, site)).isTrue();
        verify(turSNSiteSpotlightRepository).deleteAllInBatch(spotlights);
    }
}
