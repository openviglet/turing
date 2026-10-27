/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.persistence.repository.sn.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.se.TurSEVendor;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricAccess;
import com.viglet.turing.testutil.AbstractTuringSpringIT;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * T1148 — failed-search and last-seen window queries break count ties newest
 * first, so a new one-off gap is not cut off by older one-off terms.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNSiteMetricAccessTieOrderIT extends AbstractTuringSpringIT {

    private static final int ONE_OFF_TERMS = 6;
    private static final int ROWS = 3;
    private static final String PARTITION = "tenant-a";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TurSNSiteMetricAccessRepository repository;

    private TransactionTemplate tx;
    private TurSNSite site;
    private Instant start;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        // The H2 test DB is reused across runs, so every fixture is its own site.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        start = Instant.now().minus(1, ChronoUnit.HOURS);
        site = tx.execute(status -> {
            TurSEVendor vendor = new TurSEVendor();
            vendor.setId(suffix);
            vendor.setTitle("Tie order vendor");
            entityManager.persist(vendor);
            TurSEInstance instance = new TurSEInstance();
            instance.setId(UUID.randomUUID().toString());
            instance.setTitle("Tie order instance");
            instance.setEndpointUrl("http://localhost:0");
            instance.setEnabled(1);
            instance.setTurSEVendor(vendor);
            entityManager.persist(instance);
            TurSNSite created = new TurSNSite();
            created.setId(UUID.randomUUID().toString());
            created.setName("tie-order-" + suffix);
            created.setTurSEInstance(instance);
            entityManager.persist(created);
            // One search each, a minute apart: term-0 is the oldest, term-5 the newest.
            for (int i = 0; i < ONE_OFF_TERMS; i++) {
                Instant when = start.plus(i + 1L, ChronoUnit.MINUTES);
                entityManager.persist(access(created, "term-" + i, when, null));
                entityManager.persist(access(created, "term-" + i, when, PARTITION));
            }
            return created;
        });
    }

    private static TurSNSiteMetricAccess access(TurSNSite site, String term, Instant when, String partition) {
        TurSNSiteMetricAccess access = new TurSNSiteMetricAccess();
        access.setTurSNSite(site);
        access.setTerm(term);
        access.setAccessDate(when);
        access.setNumFound(0);
        access.setPartitionKey(partition);
        return access;
    }

    private static List<String> terms(List<TurSNSiteMetricAccessTerm> rows) {
        return rows.stream().map(TurSNSiteMetricAccessTerm::getTerm).toList();
    }

    private static final List<String> NEWEST = List.of("term-5", "term-4", "term-3");

    @Test
    void failedSearchTiesComeBackNewestFirst() {
        Instant end = Instant.now();
        assertThat(terms(repository.zeroResultTermsBetweenDates(site, start, end, PageRequest.of(0, ROWS))))
                .containsExactlyElementsOf(NEWEST);
        assertThat(terms(repository.termsWithLastSeenBetweenDates(site, start, end, PageRequest.of(0, ROWS))))
                .containsExactlyElementsOf(NEWEST);
    }

    @Test
    void partitionedFailedSearchTiesComeBackNewestFirst() {
        Instant end = Instant.now();
        assertThat(terms(repository.zeroResultTermsBetweenDatesInPartition(site, start, end, PARTITION,
                PageRequest.of(0, ROWS)))).containsExactlyElementsOf(NEWEST);
        assertThat(terms(repository.termsWithLastSeenBetweenDatesInPartition(site, start, end, PARTITION,
                PageRequest.of(0, ROWS)))).containsExactlyElementsOf(NEWEST);
    }
}
