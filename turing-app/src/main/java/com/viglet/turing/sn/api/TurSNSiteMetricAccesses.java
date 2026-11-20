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

package com.viglet.turing.sn.api;

import com.viglet.turing.sn.persistence.repository.metric.TurSNSiteMetricTerms;

import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.repository.metric.TurSNSiteMetricAccessRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * What other modules may call on {@code TurSNSiteMetricAccessRepository}, under its own names, so the
 * repository stays internal to sn (T1312, ADR 0005). Each method delegates.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Service
public class TurSNSiteMetricAccesses {
    private final TurSNSiteMetricAccessRepository turSNSiteMetricAccessRepository;

    public TurSNSiteMetricAccesses(TurSNSiteMetricAccessRepository turSNSiteMetricAccessRepository) {
        this.turSNSiteMetricAccessRepository = turSNSiteMetricAccessRepository;
    }

    public List<TurSNSiteMetricAccessTerm> topTerms(TurSNSite turSNSite, Pageable pageable) {
        return turSNSiteMetricAccessRepository.topTerms(turSNSite, pageable);
    }

    public List<TurSNSiteMetricAccessTerm> topTermsBetweenDates(TurSNSite turSNSite, Instant startDate, Instant endDate, Pageable pageable) {
        return turSNSiteMetricAccessRepository.topTermsBetweenDates(turSNSite, startDate, endDate, pageable);
    }

    public int countTerms(TurSNSite turSNSite) {
        return turSNSiteMetricAccessRepository.countTerms(turSNSite);
    }

    public int countTermsByPeriod(TurSNSite turSNSite, Instant startDate, Instant endDate) {
        return turSNSiteMetricAccessRepository.countTermsByPeriod(turSNSite, startDate, endDate);
    }

    /** The terms with each one's most-typed spelling, as {@code TurSNSiteMetricTerms#asTyped} reads them. */
    public List<TurSNSiteMetricAccessTerm> asTyped(TurSNSite site, List<TurSNSiteMetricAccessTerm> terms) {
        return TurSNSiteMetricTerms.asTyped(turSNSiteMetricAccessRepository, site, terms);
    }
}
