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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * T1179 — a logged search is grouped by its folded form and shown as typed: an
 * accented and an unaccented spelling count as one search under the spelling
 * typed most, and a Cyrillic or Japanese search keeps its letters.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
class TurSNSiteMetricTermsAsTypedIT extends AbstractTuringSpringIT {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TurSNSiteMetricAccessRepository repository;

    private TurSNSite site;
    private Instant start;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        // The H2 test DB is reused across runs, so every fixture is its own site.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        start = Instant.now().minus(1, ChronoUnit.HOURS);
        site = tx.execute(status -> {
            TurSEVendor vendor = new TurSEVendor();
            vendor.setId(suffix);
            vendor.setTitle("As typed vendor");
            entityManager.persist(vendor);
            TurSEInstance instance = new TurSEInstance();
            instance.setId(UUID.randomUUID().toString());
            instance.setTitle("As typed instance");
            instance.setEndpointUrl("http://localhost:0");
            instance.setEnabled(1);
            instance.setTurSEVendor(vendor);
            entityManager.persist(instance);
            TurSNSite created = new TurSNSite();
            created.setId(UUID.randomUUID().toString());
            created.setName("as-typed-" + suffix);
            created.setTurSEInstance(instance);
            entityManager.persist(created);
            Instant when = start.plus(1, ChronoUnit.MINUTES);
            for (String term : List.of("emergência", "emergência", "Emergência ", "emergencia", "палатка",
                    "テント")) {
                entityManager.persist(access(created, term, when));
            }
            return created;
        });
    }

    private static TurSNSiteMetricAccess access(TurSNSite site, String term, Instant when) {
        TurSNSiteMetricAccess access = new TurSNSiteMetricAccess();
        access.setTurSNSite(site);
        access.setTerm(term);
        access.setAccessDate(when);
        access.setNumFound(0);
        return access;
    }

    @Test
    void aSearchIsCountedByItsFoldedFormAndShownAsTyped() {
        List<TurSNSiteMetricAccessTerm> rows = TurSNSiteMetricTerms.asTyped(repository, site,
                repository.zeroResultTermsBetweenDates(site, start, Instant.now(), PageRequest.of(0, 10)));

        Map<String, Long> counted = rows.stream()
                .collect(Collectors.toMap(TurSNSiteMetricAccessTerm::getTerm, TurSNSiteMetricAccessTerm::getTotal));
        assertThat(counted).containsExactlyInAnyOrderEntriesOf(Map.of(
                "emergência", 4L,
                "палатка", 1L,
                "テント", 1L));
    }
}
