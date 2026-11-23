/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.sn.web.queue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

/**
 * T1343 — how many field values an engine dropped and how many documents it
 * refused for each SN site over the last {@link #WINDOW}, so the console can
 * flag a client that sends malformed data without anyone reading the indexing
 * log. An import is a stream of batches with no run boundary, so a rolling
 * window stands in for "the last import".
 *
 * <p>Held in memory on the node that indexed, like the queue size beside it on
 * the monitoring endpoint; a restart starts the window over.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSNIndexingIssues {

    /** How far back the counts look. */
    public static final Duration WINDOW = Duration.ofHours(24);

    /** The counts for one site within the window. */
    public record Counts(long droppedValues, long refusedDocuments) {
    }

    private record Event(Instant at, long droppedValues, long refusedDocuments) {
    }

    private final Map<String, Deque<Event>> bySite = new ConcurrentHashMap<>();
    private final Clock clock;

    public TurSNIndexingIssues() {
        this(Clock.systemUTC());
    }

    TurSNIndexingIssues(Clock clock) {
        this.clock = clock;
    }

    /** Records values the engine dropped from one document of {@code siteName}. */
    public void droppedValues(String siteName, int count) {
        if (count > 0) {
            add(siteName, new Event(clock.instant(), count, 0));
        }
    }

    /** Records one document of {@code siteName} the engine refused. */
    public void refusedDocument(String siteName) {
        add(siteName, new Event(clock.instant(), 0, 1));
    }

    /** The site's counts within the window. */
    public Counts countsFor(String siteName) {
        Deque<Event> events = bySite.get(key(siteName));
        if (events == null) {
            return new Counts(0, 0);
        }
        prune(events);
        long dropped = 0;
        long refused = 0;
        for (Event event : events) {
            dropped += event.droppedValues();
            refused += event.refusedDocuments();
        }
        return new Counts(dropped, refused);
    }

    private void add(String siteName, Event event) {
        if (siteName == null) {
            return;
        }
        Deque<Event> events = bySite.computeIfAbsent(key(siteName), k -> new ConcurrentLinkedDeque<>());
        events.addLast(event);
        prune(events);
    }

    private void prune(Deque<Event> events) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!events.isEmpty() && events.peekFirst().at().isBefore(cutoff)) {
            events.pollFirst();
        }
    }

    private static String key(String siteName) {
        return siteName == null ? "" : siteName.toLowerCase(Locale.ROOT);
    }
}
