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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.viglet.turing.api.sn.bean.TurSNSiteMonitoringStatusBean;
import com.viglet.turing.exchange.sn.TurSNSiteExport;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.TurSNSiteFacetSortEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.sn.TurSNQueue;
import com.viglet.turing.sn.template.TurSNTemplate;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstance;
import com.viglet.turing.solr.TurSolrInstanceProcess;
import com.viglet.turing.solr.TurSolrUtils;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for TurSNSiteAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteAPITest {

    @Test
    void testSiteListUsesMultiTenantFilter() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNSiteGenAiRepository genAiRepository = mock(TurSNSiteGenAiRepository.class);
        TurSNSiteExport export = mock(TurSNSiteExport.class);
        TurSNTemplate template = mock(TurSNTemplate.class);
        TurSNQueue queue = mock(TurSNQueue.class);
        TurSolrInstanceProcess solrInstanceProcess = mock(TurSolrInstanceProcess.class);
        TurSolr turSolr = mock(TurSolr.class);
        TurConfigProperties configProperties = mock(TurConfigProperties.class);
        TurSNSiteAPI api = new TurSNSiteAPI(siteRepository, localeRepository, genAiRepository, export,
                template, queue, solrInstanceProcess, turSolr, configProperties);
        Principal principal = () -> "Admin";
        TurSNSite site = new TurSNSite();

        when(configProperties.isMultiTenant()).thenReturn(true);
        when(siteRepository.findByCreatedBy(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("admin")))
                .thenReturn(List.of(site));

        List<TurSNSite> result = api.turSNSiteList(principal);

        assertThat(result).containsExactly(site);
    }

    @Test
    void testSiteStructureHasDefaults() {
        TurSNSiteAPI api = new TurSNSiteAPI(mock(TurSNSiteRepository.class),
                mock(TurSNSiteLocaleRepository.class), mock(TurSNSiteGenAiRepository.class),
                mock(TurSNSiteExport.class), mock(TurSNTemplate.class), mock(TurSNQueue.class),
                mock(TurSolrInstanceProcess.class), mock(TurSolr.class), mock(TurConfigProperties.class));

        TurSNSite result = api.turSNSiteStructure();

        assertThat(result.getFacetSort()).isEqualTo(TurSNSiteFacetSortEnum.COUNT);
        assertThat(result.getFacetType()).isEqualTo(TurSNSiteFacetFieldEnum.AND);
        assertThat(result.getTurSEInstance()).isNotNull();
        assertThat(result.getTurSNSiteGenAi()).isNotNull();
    }

    @Test
    void testSiteGetReturnsDefaultWhenMissing() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteAPI api = new TurSNSiteAPI(siteRepository,
                mock(TurSNSiteLocaleRepository.class), mock(TurSNSiteGenAiRepository.class),
                mock(TurSNSiteExport.class), mock(TurSNTemplate.class), mock(TurSNQueue.class),
                mock(TurSolrInstanceProcess.class), mock(TurSolr.class), mock(TurConfigProperties.class));

        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        TurSNSite result = api.turSNSiteGet("site");

        assertThat(result.getId()).isNull();
    }

    @Test
    void testSiteUpdateCopiesFields() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteGenAiRepository genAiRepository = mock(TurSNSiteGenAiRepository.class);
        TurSNSiteAPI api = new TurSNSiteAPI(siteRepository,
                mock(TurSNSiteLocaleRepository.class), genAiRepository,
                mock(TurSNSiteExport.class), mock(TurSNTemplate.class), mock(TurSNQueue.class),
                mock(TurSolrInstanceProcess.class), mock(TurSolr.class), mock(TurConfigProperties.class));
        TurSNSite existing = new TurSNSite();
        TurSNSiteGenAi genAi = new TurSNSiteGenAi();
        existing.setTurSNSiteGenAi(genAi);
        TurSNSite payload = new TurSNSite();
        payload.setName("New");
        payload.setDescription("Desc");
        payload.setHl(1);
        payload.setTurSNSiteGenAi(new TurSNSiteGenAi());

        when(siteRepository.findById("site")).thenReturn(Optional.of(existing));

        TurSNSite result = api.turSNSiteUpdate("site", payload);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("Desc");
        verify(siteRepository).save(existing);
        verify(genAiRepository).save(any(TurSNSiteGenAi.class));
    }

    @Test
    void testSiteDeleteDeletesCores() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNSiteAPI api = new TurSNSiteAPI(siteRepository, localeRepository,
                mock(TurSNSiteGenAiRepository.class), mock(TurSNSiteExport.class),
                mock(TurSNTemplate.class), mock(TurSNQueue.class), mock(TurSolrInstanceProcess.class),
                mock(TurSolr.class), mock(TurConfigProperties.class));
        TurSNSite site = new TurSNSite();
        TurSEInstance instance = new TurSEInstance();
        instance.setHost("localhost");
        instance.setPort(8983);
        site.setTurSEInstance(instance);
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setCore("core1");

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(localeRepository.findByTurSNSite(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(site)))
                .thenReturn(List.of(locale));

        try (MockedStatic<TurSolrUtils> utils = org.mockito.Mockito.mockStatic(TurSolrUtils.class)) {
            boolean result = api.turSNSiteDelete("site");

            assertThat(result).isTrue();
            utils.verify(() -> TurSolrUtils.deleteCore(instance, "core1"));
            verify(siteRepository).delete("site");
        }
    }

    @Test
    void testSiteAddPersistsAndCreatesTemplate() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteGenAiRepository genAiRepository = mock(TurSNSiteGenAiRepository.class);
        TurSNTemplate template = mock(TurSNTemplate.class);
        TurSNSiteAPI api = new TurSNSiteAPI(siteRepository,
                mock(TurSNSiteLocaleRepository.class), genAiRepository,
                mock(TurSNSiteExport.class), template, mock(TurSNQueue.class),
                mock(TurSolrInstanceProcess.class), mock(TurSolr.class), mock(TurConfigProperties.class));
        TurSNSite site = new TurSNSite();
        site.setTurSNSiteGenAi(new TurSNSiteGenAi());
        Principal principal = () -> "admin";

        TurSNSite result = api.turSNSiteAdd(site, principal);

        assertThat(result).isSameAs(site);
        verify(genAiRepository).save(site.getTurSNSiteGenAi());
        verify(siteRepository).save(site);
        verify(template).createSNSite(site, "admin", Locale.US);
    }

    @Test
    void testSiteExportReturnsStreamingBody() {
        TurSNSiteExport export = mock(TurSNSiteExport.class);
        TurSNSiteAPI api = new TurSNSiteAPI(mock(TurSNSiteRepository.class),
                mock(TurSNSiteLocaleRepository.class), mock(TurSNSiteGenAiRepository.class),
                export, mock(TurSNTemplate.class), mock(TurSNQueue.class),
                mock(TurSolrInstanceProcess.class), mock(TurSolr.class), mock(TurConfigProperties.class));
        HttpServletResponse response = mock(HttpServletResponse.class);
        StreamingResponseBody expected = outputStream -> outputStream.write(new byte[0]);

        when(export.exportObject(response)).thenReturn(expected);

        StreamingResponseBody body = api.turSNSiteExport(response);

        assertThat(body).isSameAs(expected);
    }

    @Test
    void testMonitoringStatusReturnsCounts() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
        TurSNQueue queue = mock(TurSNQueue.class);
        TurSolrInstanceProcess solrInstanceProcess = mock(TurSolrInstanceProcess.class);
        TurSolr turSolr = mock(TurSolr.class);
        TurSNSiteAPI api = new TurSNSiteAPI(siteRepository, localeRepository,
                mock(TurSNSiteGenAiRepository.class), mock(TurSNSiteExport.class),
                mock(TurSNTemplate.class), queue, solrInstanceProcess, turSolr, mock(TurConfigProperties.class));
        TurSNSite site = new TurSNSite();
        TurSNSiteLocale locale = new TurSNSiteLocale();
        TurSolrInstance instance = mock(TurSolrInstance.class);

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(localeRepository.findByTurSNSite(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(site)))
                .thenReturn(List.of(locale));
        when(queue.getQueueSize()).thenReturn(2);
        when(solrInstanceProcess.initSolrInstance(locale)).thenReturn(Optional.of(instance));
        when(turSolr.getDocumentTotal(instance)).thenReturn(5L);

        TurSNSiteMonitoringStatusBean result = api.turSNSiteMonitoringStatus("site");

        assertThat(result.getQueue()).isEqualTo(2);
        assertThat(result.getDocuments()).isEqualTo(5);
    }
}
