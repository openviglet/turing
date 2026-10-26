/*
 * Copyright (C) 2016-2026 the original author or authors.
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
package com.viglet.turing.persistence.repository.sn.metric;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricClick;

/**
 * Repository for {@link TurSNSiteMetricClick}. Clicks are append-only
 * telemetry: the only writer is the {@code /click} endpoint, and the reads are
 * the aggregates the click-through report (T1131) needs. A {@code null}
 * {@code locale} reads every locale, and a {@code null} partition key (T1133)
 * every partition.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.7
 */
public interface TurSNSiteMetricClickRepository extends JpaRepository<TurSNSiteMetricClick, String> {

    @Query("select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickTerm("
            + "c.sanatizedTerm, COUNT(c), AVG(CAST(c.position as double))) from TurSNSiteMetricClick c "
            + "where c.turSNSite = :site and c.accessDate between :start and :end "
            + "and (:locale is null or c.language = :locale) and (:partition is null or c.partitionKey = :partition) "
            + "and c.sanatizedTerm in :terms group by c.sanatizedTerm")
    List<TurSNSiteMetricClickTerm> clicksByTerm(@Param("site") TurSNSite site, @Param("start") Instant start,
            @Param("end") Instant end, @Param("locale") Locale locale, @Param("partition") String partitionKey,
            @Param("terms") Collection<String> terms);

    @Query("select COUNT(c) from TurSNSiteMetricClick c where c.turSNSite = :site "
            + "and c.accessDate between :start and :end and (:locale is null or c.language = :locale) "
            + "and (:partition is null or c.partitionKey = :partition)")
    long countClicks(@Param("site") TurSNSite site, @Param("start") Instant start, @Param("end") Instant end,
            @Param("locale") Locale locale, @Param("partition") String partitionKey);
}
