/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.searchengine;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.viglet.turing.system.cache.TurCacheEvictionScheduler;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * T1315 — clears {@link TurSEInstancePluginTypes#CACHE_NAME} after an SE
 * instance write commits, so a vendor change reaches the next search. Same
 * after-commit scheduling as {@code TurSNSiteSnapshotEvictionListener}, for the
 * same reason: a clear at flush could be refilled from the old row.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSEInstanceEvictionListener {

    private static final AtomicReference<TurCacheEvictionScheduler> evictionSchedulerStatic =
            new AtomicReference<>();

    private final TurCacheEvictionScheduler evictionScheduler;

    /** Used when Hibernate instantiates the listener itself, outside Spring. */
    public TurSEInstanceEvictionListener() {
        this.evictionScheduler = null;
    }

    @Autowired
    public TurSEInstanceEvictionListener(TurCacheEvictionScheduler evictionScheduler) {
        this.evictionScheduler = evictionScheduler;
        evictionSchedulerStatic.set(evictionScheduler);
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void onChange(Object entity) {
        TurCacheEvictionScheduler scheduler = evictionScheduler != null
                ? evictionScheduler
                : evictionSchedulerStatic.get();
        if (scheduler != null) {
            scheduler.clearOnce(TurSEInstancePluginTypes.CACHE_NAME);
            // T1337 — the endpoint the engine clients are built from.
            scheduler.clearOnce(TurSEInstanceEndpoints.CACHE_NAME);
        }
    }
}
