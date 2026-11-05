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

package com.viglet.turing.genai.feed;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.viglet.turing.api.exception.TurConflictException;
import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.genai.feed.TurStructuredFeedIngestService.IngestResult;
import com.viglet.turing.properties.TurStructuredFeedProperty;
import com.viglet.turing.properties.TurStructuredFeedProperty.Source;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

/**
 * T1229 — ingests one configured structured feed now, for a publisher whose feed
 * just changed or an operator re-syncing after a fix, instead of waiting for the
 * hourly sweep. It takes the sweep's own ShedLock, so the two never run at once
 * anywhere in the cluster: a request during a sweep (or another trigger) is
 * refused with 409 rather than queued.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@Component
public class TurStructuredFeedTrigger {

    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(30);

    private final TurStructuredFeedProperty property;
    private final TurStructuredFeedIngestService ingestService;
    private final ObjectProvider<LockProvider> lockProvider;

    public TurStructuredFeedTrigger(TurStructuredFeedProperty property,
            TurStructuredFeedIngestService ingestService, ObjectProvider<LockProvider> lockProvider) {
        this.property = property;
        this.ingestService = ingestService;
        this.lockProvider = lockProvider;
    }

    /**
     * Ingests the source whose {@code id} (or, without one, site name) is
     * {@code sourceId}.
     *
     * @throws TurNotFoundException when no configured source has that id
     * @throws TurConflictException when a sweep or another trigger holds the lock
     */
    public IngestResult ingest(String sourceId) {
        Source source = find(sourceId).orElseThrow(() -> new TurNotFoundException(
                "No structured feed source '" + sourceId + "' is configured"));
        LockProvider provider = lockProvider.getIfAvailable();
        if (provider == null) {
            return ingestService.ingestSource(source);
        }
        Optional<SimpleLock> lock = provider.lock(new LockConfiguration(Instant.now(),
                TurStructuredFeedIngestService.LOCK_NAME, LOCK_AT_MOST_FOR, Duration.ZERO));
        if (lock.isEmpty()) {
            throw new TurConflictException("A structured feed ingest is already running; try again when it ends");
        }
        try {
            IngestResult result = ingestService.ingestSource(source);
            log.info("[StructuredFeed] on-demand ingest of '{}' → {}", sourceId, result);
            return result;
        } finally {
            lock.get().unlock();
        }
    }

    private Optional<Source> find(String sourceId) {
        List<Source> sources = property.getSources();
        if (sources == null || StringUtils.isBlank(sourceId)) {
            return Optional.empty();
        }
        return sources.stream()
                .filter(source -> source != null
                        && sourceId.equals(StringUtils.defaultIfBlank(source.getId(), source.getSiteName())))
                .findFirst();
    }
}
