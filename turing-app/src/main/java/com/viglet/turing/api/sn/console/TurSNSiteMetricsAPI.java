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
package com.viglet.turing.api.sn.console;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.api.sn.bean.TurSNSiteMetricsTopTermsBean;
import com.viglet.turing.sn.insights.TurSNSearchInsightsService;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricAccess;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Alexandre Oliveira
 * @since 0.3.6
 */

@RestController
@RequestMapping("/api/sn/{snSiteId}/metrics")
@Tag(name = "Semantic Navigation Metrics", description = "Semantic Navigation Metrics API")
public class TurSNSiteMetricsAPI {
	private final TurSNSiteRepository turSNSiteRepository;
	private final TurSNSiteMetricAccessRepository turSNSiteMetricAccessRepository;
	private final TurSNSearchInsightsService turSNSearchInsightsService;

	public TurSNSiteMetricsAPI(TurSNSiteRepository turSNSiteRepository,
			TurSNSiteMetricAccessRepository turSNSiteMetricAccessRepository,
			TurSNSearchInsightsService turSNSearchInsightsService) {
		this.turSNSiteRepository = turSNSiteRepository;
		this.turSNSiteMetricAccessRepository = turSNSiteMetricAccessRepository;
		this.turSNSearchInsightsService = turSNSearchInsightsService;
	}

	@GetMapping("live")
	public List<Map<String, Object>> getLiveMetrics(@PathVariable String snSiteId) {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant sixtySecondsAgo = now.minusSeconds(60);

		// Busca os dados reais do modelo TurSNSiteMetricAccess
		List<TurSNSiteMetricAccess> rawData = turSNSiteMetricAccessRepository
				.findLastMinuteMetrics(snSiteId, sixtySecondsAgo);

		// 1. Criamos os baldes de 60 segundos
		Map<Long, Long> buckets = new LinkedHashMap<>();
		for (int i = 0; i < 60; i++) {
			buckets.put(sixtySecondsAgo.plusSeconds(i).getEpochSecond(), 0L);
		}

		// 2. Preenchemos com os acessos reais
		rawData.forEach(m -> {
			long second = m.getAccessDate().truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
			buckets.computeIfPresent(second, (k, v) -> v + 1);
		});

		// 3. Conversão segura para evitar o erro de Type Mismatch
		return buckets.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(e -> {
					Map<String, Object> item = new HashMap<>();
					item.put("time", e.getKey()); // Epoch Second
					item.put("displayTime", Instant.ofEpochSecond(e.getKey()).toString().substring(11, 19));
					item.put("accesses", e.getValue());
					return item;
				})
				.toList();
	}

	@Operation(summary = "Semantic Navigation Site Metrics Top Terms")
	@GetMapping("top-terms/today/{rows}")
	public TurSNSiteMetricsTopTermsBean turSNSiteMetricsTopTermsToday(@PathVariable String snSiteId,
			@PathVariable int rows) {
		Optional<TurSNSite> turSNSite = turSNSiteRepository.findById(snSiteId);
		if (turSNSite.isPresent()) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.clear(Calendar.MINUTE);
			cal.clear(Calendar.SECOND);
			cal.clear(Calendar.MILLISECOND);

			Calendar previousBegin = Calendar.getInstance();
			previousBegin.set(Calendar.HOUR_OF_DAY, 0);
			previousBegin.clear(Calendar.MINUTE);
			previousBegin.clear(Calendar.SECOND);
			previousBegin.clear(Calendar.MILLISECOND);
			previousBegin.add(Calendar.DATE, -1);

			Calendar previousEnd = Calendar.getInstance();
			previousEnd.add(Calendar.DATE, -1);

			return getTopTermsReport(rows, turSNSite, cal, previousBegin, previousEnd);
		}
		return new TurSNSiteMetricsTopTermsBean(new ArrayList<>(), 0, 0);
	}

	private TurSNSiteMetricsTopTermsBean getTopTermsReport(int rows, Optional<TurSNSite> turSNSite, Calendar cal,
			Calendar previousBegin, Calendar previousEnd) {
		return turSNSite.map(snSite -> new TurSNSiteMetricsTopTermsBean(
				this.turSNSiteMetricAccessRepository.topTermsBetweenDates(snSite, cal.getTime().toInstant(),
						new Date().toInstant(), PageRequest.of(0, rows)),
				this.turSNSiteMetricAccessRepository.countTermsByPeriod(snSite, cal.getTime().toInstant(),
						new Date().toInstant()),
				this.turSNSiteMetricAccessRepository.countTermsByPeriod(snSite, previousBegin.getTime().toInstant(),
						previousEnd.getTime().toInstant())))
				.orElseGet(TurSNSiteMetricsTopTermsBean::new);
	}

	@GetMapping("top-terms/this-week/{rows}")
	public TurSNSiteMetricsTopTermsBean turSNSiteMetricsTopTermsThisWeek(@PathVariable String snSiteId,
			@PathVariable int rows) {
		Optional<TurSNSite> turSNSite = turSNSiteRepository.findById(snSiteId);
		if (turSNSite.isPresent()) {
			Calendar cal = Calendar.getInstance();

			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.clear(Calendar.MINUTE);
			cal.clear(Calendar.SECOND);
			cal.clear(Calendar.MILLISECOND);
			cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

			Calendar previousBegin = Calendar.getInstance();
			previousBegin.add(Calendar.DATE, -7);
			previousBegin.set(Calendar.HOUR_OF_DAY, 0);
			previousBegin.clear(Calendar.MINUTE);
			previousBegin.clear(Calendar.SECOND);
			previousBegin.clear(Calendar.MILLISECOND);
			previousBegin.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

			Calendar previousEnd = Calendar.getInstance();
			previousEnd.add(Calendar.DATE, -7);

			return getTopTermsReport(rows, turSNSite, cal, previousBegin, previousEnd);
		}
		return new TurSNSiteMetricsTopTermsBean(new ArrayList<>(), 0, 0);
	}

	@GetMapping("top-terms/this-month/{rows}")
	public TurSNSiteMetricsTopTermsBean turSNSiteMetricsTopTermsThisMonth(@PathVariable String snSiteId,
			@PathVariable int rows) {
		Optional<TurSNSite> turSNSite = turSNSiteRepository.findById(snSiteId);
		if (turSNSite.isPresent()) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.clear(Calendar.MINUTE);
			cal.clear(Calendar.SECOND);
			cal.clear(Calendar.MILLISECOND);

			cal.set(Calendar.DAY_OF_MONTH, 1);

			Calendar previousBegin = Calendar.getInstance();
			previousBegin.set(Calendar.HOUR_OF_DAY, 0);
			previousBegin.clear(Calendar.MINUTE);
			previousBegin.clear(Calendar.SECOND);
			previousBegin.clear(Calendar.MILLISECOND);
			previousBegin.add(Calendar.MONTH, -1);
			previousBegin.set(Calendar.DATE, 1);

			Calendar previousEnd = Calendar.getInstance();
			previousEnd.add(Calendar.DAY_OF_MONTH, (-1) * previousEnd.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);

			return getTopTermsReport(rows, turSNSite, cal, previousBegin, previousEnd);
		}
		return new TurSNSiteMetricsTopTermsBean(new ArrayList<>(), 0, 0);
	}

	/**
	 * T1119 — the queries that found nothing, as the MCP {@code top_failed_searches}
	 * tool reads them. {@code from}/{@code to} are ISO-8601 instants; {@code period}
	 * ({@code today}, {@code this-week}, {@code this-month}, {@code all-time}) is the
	 * shorthand for {@code from}.
	 */
	@Operation(summary = "Semantic Navigation Site searches that found nothing")
	@GetMapping("failed-searches")
	public TurSNSearchInsightsService.Report turSNSiteMetricsFailedSearches(@PathVariable String snSiteId,
			@RequestParam(required = false) String period,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(required = false) Integer rows) {
		TurSNSite site = requireSite(snSiteId);
		TurSNSearchInsightsService.Window window = turSNSearchInsightsService.window(period, from, to);
		return TurSNSearchInsightsService.Report.of(window,
				turSNSearchInsightsService.failedSearches(site, window, rows),
				turSNSearchInsightsService.metricsEnabled());
	}

	/**
	 * T1119 — the queries searched often that find little (average results below
	 * {@value TurSNSearchInsightsService#LOW_COVERAGE_THRESHOLD}), as the MCP
	 * {@code content_gaps} tool reads them. Same window as
	 * {@link #turSNSiteMetricsFailedSearches}.
	 */
	@Operation(summary = "Semantic Navigation Site content gaps")
	@GetMapping("content-gaps")
	public TurSNSearchInsightsService.Report turSNSiteMetricsContentGaps(@PathVariable String snSiteId,
			@RequestParam(required = false) String period,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(required = false) Integer rows) {
		TurSNSite site = requireSite(snSiteId);
		TurSNSearchInsightsService.Window window = turSNSearchInsightsService.window(period, from, to);
		return TurSNSearchInsightsService.Report.of(window,
				turSNSearchInsightsService.contentGaps(site, window, rows),
				turSNSearchInsightsService.metricsEnabled());
	}

	private TurSNSite requireSite(String snSiteId) {
		return turSNSiteRepository.findById(snSiteId)
				.orElseThrow(() -> new TurNotFoundException("SN site not found: " + snSiteId));
	}

	@GetMapping("top-terms/all-time/{rows}")
	public TurSNSiteMetricsTopTermsBean turSNSiteMetricsTopTermsAllTime(@PathVariable String snSiteId,
			@PathVariable int rows) {
		Optional<TurSNSite> turSNSite = turSNSiteRepository.findById(snSiteId);
		return turSNSite.map(snSite -> new TurSNSiteMetricsTopTermsBean(
				this.turSNSiteMetricAccessRepository.topTerms(snSite, PageRequest.of(0, rows)),
				this.turSNSiteMetricAccessRepository.countTerms(snSite), 0))
				.orElseGet(TurSNSiteMetricsTopTermsBean::new);
	}

}