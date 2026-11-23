/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.system.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * T1324 / ADR 0005 — makes cross-module listeners at-least-once. Spring Modulith's
 * event publication registry stores each event an asynchronous module listener has to
 * handle and marks it complete when the listener returns. This job, run by one node of
 * a cluster at a time under ShedLock, delivers again every publication still incomplete
 * after {@code turing.events.republish-after} (the listener failed, or the node died
 * before it ran), and deletes completed publications older than
 * {@code turing.events.completed-retention}. Republishing on restart stays off, since
 * every node of a cluster would do it at once.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurEventPublicationJob {

    private final IncompleteEventPublications incomplete;
    private final CompletedEventPublications completed;
    private final Duration republishAfter;
    private final Duration completedRetention;

    public TurEventPublicationJob(IncompleteEventPublications incomplete, CompletedEventPublications completed,
            @Value("${turing.events.republish-after:PT5M}") Duration republishAfter,
            @Value("${turing.events.completed-retention:P7D}") Duration completedRetention) {
        this.incomplete = incomplete;
        this.completed = completed;
        this.republishAfter = republishAfter;
        this.completedRetention = completedRetention;
    }

    @Scheduled(fixedDelayString = "${turing.events.republish-interval:PT1M}",
            initialDelayString = "${turing.events.republish-interval:PT1M}")
    @SchedulerLock(name = "eventPublicationRepublish", lockAtMostFor = "PT10M", lockAtLeastFor = "PT5S")
    public void republishIncomplete() {
        try {
            incomplete.resubmitIncompletePublicationsOlderThan(republishAfter);
        } catch (RuntimeException e) {
            log.warn("[Events] republishing incomplete event publications failed: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${turing.events.purge-cron:0 20 4 * * *}")
    @SchedulerLock(name = "eventPublicationPurge", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5S")
    public void purgeCompleted() {
        completed.deletePublicationsOlderThan(completedRetention);
    }
}
