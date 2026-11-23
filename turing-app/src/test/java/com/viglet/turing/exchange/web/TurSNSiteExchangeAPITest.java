/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.exchange.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.viglet.turing.answer.exchange.TurSNSiteContentExchangeService;
import com.viglet.turing.exchange.sn.TurSNSiteExport;
import com.viglet.turing.sn.api.TurSNSiteAgents;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * T1322 — the export and reindex endpoints, moved out of sn's site API.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNSiteExchangeAPITest {

    private final TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
    private final TurSNSiteExport export = mock(TurSNSiteExport.class);
    private final TurSNSiteAgents siteAgents = mock(TurSNSiteAgents.class);
    private final TurSNSiteExchangeAPI api = new TurSNSiteExchangeAPI(siteRepository, export,
            mock(TurSNSiteContentExchangeService.class), siteAgents);

    @Test
    void aSiteOnTheDefaultAgentCanStartAReindex() {
        // T1151 — the gate reads the effective agent, as the reindex does.
        TurSNSite site = new TurSNSite();
        site.setId("site");
        site.setTurSNSiteGenAi(new TurSNSiteGenAi());
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(siteAgents.isRagReady(site.getTurSNSiteGenAi())).thenReturn(true);

        var response = api.turSNSiteGenAiReindex("site", "task-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsValue("task-1");
    }

    @Test
    void aSiteWithoutRagIsRefusedAReindex() {
        TurSNSite site = new TurSNSite();
        site.setId("site");
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));

        var response = api.turSNSiteGenAiReindex("site", "task-1");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void theExportReturnsTheExportersStreamingBody() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StreamingResponseBody expected = outputStream -> outputStream.write(new byte[0]);
        when(export.exportAll(response)).thenReturn(expected);

        assertThat(api.turSNSiteExportAll(response)).isSameAs(expected);
    }
}
