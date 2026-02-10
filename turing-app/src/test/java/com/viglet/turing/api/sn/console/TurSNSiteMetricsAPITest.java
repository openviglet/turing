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
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.api.sn.bean.TurSNSiteMetricsTopTermsBean;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;

/**
 * Unit tests for TurSNSiteMetricsAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteMetricsAPITest {

    @Test
    void testTopTermsAllTimeReturnsDefaultWhenMissing() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteMetricAccessRepository metricRepository = mock(TurSNSiteMetricAccessRepository.class);
        TurSNSiteMetricsAPI api = new TurSNSiteMetricsAPI(siteRepository, metricRepository);

        when(siteRepository.findById("site")).thenReturn(Optional.empty());

        TurSNSiteMetricsTopTermsBean result = api.turSNSiteMetricsTopTermsAllTime("site", 10);

        assertThat(result.getTopTerms()).isEmpty();
        assertThat(result.getTotalTermsPeriod()).isEqualTo(0);
    }

    @Test
    void testTopTermsAllTimeReturnsMetricsWhenFound() {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteMetricAccessRepository metricRepository = mock(TurSNSiteMetricAccessRepository.class);
        TurSNSiteMetricsAPI api = new TurSNSiteMetricsAPI(siteRepository, metricRepository);
        TurSNSite site = new TurSNSite();

        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(metricRepository.topTerms(org.mockito.ArgumentMatchers.eq(site), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());
        when(metricRepository.countTerms(site)).thenReturn(5);

        TurSNSiteMetricsTopTermsBean result = api.turSNSiteMetricsTopTermsAllTime("site", 10);

        assertThat(result.getTotalTermsPeriod()).isEqualTo(5);
    }
}
