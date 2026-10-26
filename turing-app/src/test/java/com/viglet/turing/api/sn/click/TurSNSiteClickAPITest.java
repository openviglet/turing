/*
 * Copyright (C) 2016-2026 the original author or authors.
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

package com.viglet.turing.api.sn.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricClick;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickRepository;

/**
 * T1128 — a click sent as a beacon ({@code text/plain} JSON) is recorded like the
 * JSON one.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNSiteClickAPITest {

    private final TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
    private final TurSNSiteMetricClickRepository clickRepository = mock(TurSNSiteMetricClickRepository.class);
    private final TurSNSiteClickAPI api = new TurSNSiteClickAPI(siteRepository, clickRepository, true);

    @Test
    void aBeaconClickIsRecorded() {
        when(siteRepository.findByNameIgnoreCase("Sample")).thenReturn(Optional.of(new TurSNSite()));

        var response = api.trackClickBeacon("Sample",
                "{\"term\":\"workshop\",\"documentId\":\"post-7\",\"position\":2,\"locale\":\"en_US\"}");

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        ArgumentCaptor<TurSNSiteMetricClick> saved = ArgumentCaptor.forClass(TurSNSiteMetricClick.class);
        verify(clickRepository).save(saved.capture());
        assertThat(saved.getValue().getDocumentId()).isEqualTo("post-7");
        assertThat(saved.getValue().getTerm()).isEqualTo("workshop");
        assertThat(saved.getValue().getPosition()).isEqualTo(2);
    }

    /** T1131 — with metrics off the click is dropped, and the answer says so. */
    @Test
    void aClickWithMetricsOffSaysItWasNotKept() {
        TurSNSiteClickAPI disabled = new TurSNSiteClickAPI(siteRepository, clickRepository, false);

        var response = disabled.trackClick("Sample",
                new TurSNSiteClickAPI.TurSNClickRequest("workshop", "post-7", 2, null, null));

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getHeaders().getFirst(TurSNSiteClickAPI.METRICS_HEADER)).isEqualTo("disabled");
        verify(clickRepository, never()).save(any());
    }

    @Test
    void aBeaconThatIsNotJsonIsRefused() {
        assertThat(api.trackClickBeacon("Sample", "documentId=post-7").getStatusCode().value()).isEqualTo(400);
        verify(clickRepository, never()).save(any());
    }
}
