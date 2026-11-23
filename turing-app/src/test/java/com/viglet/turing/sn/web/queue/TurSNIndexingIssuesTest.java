/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.web.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/**
 * T1343 — the per-site tally the console reads: what the engine dropped and
 * refused, within the last day.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNIndexingIssuesTest {

    /** A clock the test moves by hand. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-23T10:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void countsAddUpPerSiteIgnoringCase() {
        TurSNIndexingIssues issues = new TurSNIndexingIssues(new MovableClock());
        issues.droppedValues("Jogos", 2);
        issues.droppedValues("jogos", 1);
        issues.refusedDocument("JOGOS");
        issues.droppedValues("other", 0);

        assertThat(issues.countsFor("jogos")).isEqualTo(new TurSNIndexingIssues.Counts(3, 1));
        assertThat(issues.countsFor("other")).isEqualTo(new TurSNIndexingIssues.Counts(0, 0));
        assertThat(issues.countsFor("unknown")).isEqualTo(new TurSNIndexingIssues.Counts(0, 0));
    }

    @Test
    void anIssueOlderThanTheWindowNoLongerCounts() {
        MovableClock clock = new MovableClock();
        TurSNIndexingIssues issues = new TurSNIndexingIssues(clock);
        issues.droppedValues("jogos", 5);
        clock.now = clock.now.plus(Duration.ofHours(23));
        issues.refusedDocument("jogos");

        assertThat(issues.countsFor("jogos")).isEqualTo(new TurSNIndexingIssues.Counts(5, 1));

        clock.now = clock.now.plus(Duration.ofHours(2));
        assertThat(issues.countsFor("jogos")).isEqualTo(new TurSNIndexingIssues.Counts(0, 1));
    }
}
