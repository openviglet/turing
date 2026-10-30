/*
 * Copyright (C) 2016-2022 the original author or authors. 
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
package com.viglet.turing.persistence.repository.sn.metric;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricAccess;

/**
 * @author Alexandre Oliveira
 * @since 0.3.6
 */
public interface TurSNSiteMetricAccessRepository extends JpaRepository<TurSNSiteMetricAccess, String> {
	@Query("SELECT t FROM TurSNSiteMetricAccess t WHERE t.turSNSite.id = :siteId AND t.accessDate >= :since ORDER BY t.accessDate ASC")
	List<TurSNSiteMetricAccess> findLastMinuteMetrics(@Param("siteId") String siteId, @Param("since") Instant since);

	@Query(value = "select distinct new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, max(accessDate)) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 and language = ?2 and userId = ?3 GROUP BY sanatizedTerm ORDER BY MAX(accessDate) DESC")
	List<TurSNSiteMetricAccessTerm> findLatestSearches(TurSNSite turSNSite,
			String language, String userId, Pageable pageable);

	@Query(value = "select distinct new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double))) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 GROUP BY sanatizedTerm ORDER BY COUNT(sanatizedTerm) DESC")
	List<TurSNSiteMetricAccessTerm> topTerms(TurSNSite turSNSite, Pageable pageable);

	@Query(value = "select distinct new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double))) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 AND accessDate BETWEEN ?2 AND ?3 GROUP BY sanatizedTerm ORDER BY COUNT(sanatizedTerm) DESC")
	List<TurSNSiteMetricAccessTerm> topTermsBetweenDates(TurSNSite turSNSite, Instant startDate, Instant endDate,
			Pageable pageable);

	// T252 — terms that ALWAYS returned zero results (MAX numFound across all
	// their accesses is 0): the literal "users searched this and we had nothing".
	// Ordered by how often the failed term was searched.
	@Query(value = "select distinct new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double))) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 GROUP BY sanatizedTerm HAVING MAX(numFound) = 0 ORDER BY COUNT(sanatizedTerm) DESC")
	List<TurSNSiteMetricAccessTerm> topZeroResultTerms(TurSNSite turSNSite, Pageable pageable);

	// T1119 — the two queries above inside a window, each with the term's last
	// search, for the failed-searches and content-gaps REST metrics. Count ties
	// come back newest first (T1148), so `rows` cuts the oldest one-off terms.
	@Query(value = "select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double)), MAX(accessDate)) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 AND accessDate BETWEEN ?2 AND ?3 GROUP BY sanatizedTerm HAVING MAX(numFound) = 0 ORDER BY COUNT(sanatizedTerm) DESC, MAX(accessDate) DESC")
	List<TurSNSiteMetricAccessTerm> zeroResultTermsBetweenDates(TurSNSite turSNSite, Instant startDate,
			Instant endDate, Pageable pageable);

	@Query(value = "select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double)), MAX(accessDate)) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 AND accessDate BETWEEN ?2 AND ?3 GROUP BY sanatizedTerm ORDER BY COUNT(sanatizedTerm) DESC, MAX(accessDate) DESC")
	List<TurSNSiteMetricAccessTerm> termsWithLastSeenBetweenDates(TurSNSite turSNSite, Instant startDate,
			Instant endDate, Pageable pageable);

	// T1131 — the most searched terms in a window, optionally for one locale
	// (null reads every locale) and (T1133) one partition of a shared site (null
	// reads every partition), for the click-through report.
	@Query(value = "select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double)), MAX(accessDate)) from "
			+ "TurSNSiteMetricAccess where turSNSite = :site AND accessDate BETWEEN :start AND :end "
			+ "AND (:locale is null or language = :locale) AND (:partition is null or partitionKey = :partition) "
			+ "GROUP BY sanatizedTerm ORDER BY COUNT(sanatizedTerm) DESC")
	List<TurSNSiteMetricAccessTerm> termsBetweenDatesForLocale(@Param("site") TurSNSite site,
			@Param("start") Instant start, @Param("end") Instant end, @Param("locale") Locale locale,
			@Param("partition") String partitionKey, Pageable pageable);

	@Query(value = "select COUNT(sanatizedTerm) from TurSNSiteMetricAccess where turSNSite = :site "
			+ "AND accessDate BETWEEN :start AND :end AND (:locale is null or language = :locale) "
			+ "AND (:partition is null or partitionKey = :partition)")
	long countSearchesForLocale(@Param("site") TurSNSite site, @Param("start") Instant start,
			@Param("end") Instant end, @Param("locale") Locale locale, @Param("partition") String partitionKey);

	// T1133 — the T1119 reports for one partition of a shared site.
	@Query(value = "select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double)), MAX(accessDate)) from "
			+ "TurSNSiteMetricAccess where turSNSite = :site AND accessDate BETWEEN :start AND :end "
			+ "AND partitionKey = :partition GROUP BY sanatizedTerm HAVING MAX(numFound) = 0 ORDER BY COUNT(sanatizedTerm) DESC, MAX(accessDate) DESC")
	List<TurSNSiteMetricAccessTerm> zeroResultTermsBetweenDatesInPartition(@Param("site") TurSNSite site,
			@Param("start") Instant start, @Param("end") Instant end, @Param("partition") String partitionKey,
			Pageable pageable);

	@Query(value = "select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessTerm(sanatizedTerm, COUNT(sanatizedTerm), AVG(CAST(numFound as double)), MAX(accessDate)) from "
			+ "TurSNSiteMetricAccess where turSNSite = :site AND accessDate BETWEEN :start AND :end "
			+ "AND partitionKey = :partition GROUP BY sanatizedTerm ORDER BY COUNT(sanatizedTerm) DESC, MAX(accessDate) DESC")
	List<TurSNSiteMetricAccessTerm> termsWithLastSeenBetweenDatesInPartition(@Param("site") TurSNSite site,
			@Param("start") Instant start, @Param("end") Instant end, @Param("partition") String partitionKey,
			Pageable pageable);

	@Query(value = "select COUNT(sanatizedTerm) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1")
	int countTerms(TurSNSite turSNSite);

	@Query(value = "select COUNT(sanatizedTerm) from "
			+ "TurSNSiteMetricAccess where turSNSite = ?1 AND accessDate BETWEEN ?2 AND ?3")
	int countTermsByPeriod(TurSNSite turSNSite, Instant startDate, Instant endDate);

	/**
	 * T1179 — every spelling typed for each of the grouped {@code keys}, with its
	 * count, which {@link TurSNSiteMetricTerms#asTyped} reads to show a key as typed.
	 */
	@Query(value = "select new com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricTermSpelling(sanatizedTerm, term, COUNT(term)) from "
			+ "TurSNSiteMetricAccess where turSNSite = :site AND sanatizedTerm in :keys GROUP BY sanatizedTerm, term")
	List<TurSNSiteMetricTermSpelling> spellings(@Param("site") TurSNSite site,
			@Param("keys") java.util.Collection<String> keys);

	List<TurSNSiteMetricAccess> findByTurSNSiteAndLanguage(TurSNSite turSNSite, String language);

	List<TurSNSiteMetricAccess> findByTurSNSite(TurSNSite turSNSite);
}
