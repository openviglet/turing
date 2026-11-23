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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * T1324 — the registry job republishes what is incomplete past its grace period and
 * purges old completed publications, each under its own cluster lock.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurEventPublicationJobTest {

    private final IncompleteEventPublications incomplete = mock(IncompleteEventPublications.class);
    private final CompletedEventPublications completed = mock(CompletedEventPublications.class);
    private final TurEventPublicationJob job = new TurEventPublicationJob(incomplete, completed,
            Duration.ofMinutes(5), Duration.ofDays(7));

    @Test
    void incompletePublicationsPastTheGracePeriodAreDeliveredAgain() {
        job.republishIncomplete();

        verify(incomplete).resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));
    }

    @Test
    void aFailedRepublishDoesNotStopTheSchedule() {
        doThrow(new IllegalStateException("db down")).when(incomplete)
                .resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));

        job.republishIncomplete();
    }

    @Test
    void completedPublicationsPastTheRetentionArePurged() {
        job.purgeCompleted();

        verify(completed).deletePublicationsOlderThan(Duration.ofDays(7));
    }

    @Test
    void eachRunHoldsAClusterLock() throws Exception {
        assertThat(TurEventPublicationJob.class.getMethod("republishIncomplete")
                .getAnnotation(SchedulerLock.class).name()).isEqualTo("eventPublicationRepublish");
        assertThat(TurEventPublicationJob.class.getMethod("purgeCompleted")
                .getAnnotation(SchedulerLock.class).name()).isEqualTo("eventPublicationPurge");
    }
}
