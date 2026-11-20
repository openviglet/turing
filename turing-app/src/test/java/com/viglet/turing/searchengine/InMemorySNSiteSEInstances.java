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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.mockito.quality.Strictness;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1315 — real {@link TurSNSiteSEInstances} and {@link TurSEInstancePluginTypes}
 * over an in-memory SE instance table, for unit tests: an SN site holds its SE
 * instance by id, and the instances a test attaches here are the rows those ids
 * resolve to.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class InMemorySNSiteSEInstances {

    private final Map<String, TurSEInstance> rows = new LinkedHashMap<>();
    private final TurSEInstanceRepository repository;
    private final TurSNSiteSEInstances siteSEInstances;
    private final TurSEInstancePluginTypes pluginTypes;
    private final TurSEInstanceEndpoints endpoints;

    public InMemorySNSiteSEInstances() {
        repository = mock(TurSEInstanceRepository.class, withSettings().strictness(Strictness.LENIENT));
        when(repository.findById(any())).thenAnswer(inv -> Optional.ofNullable(rows.get(inv.<String>getArgument(0))));
        when(repository.existsById(any())).thenAnswer(inv -> rows.containsKey(inv.<String>getArgument(0)));
        this.siteSEInstances = new TurSNSiteSEInstances(repository);
        this.pluginTypes = new TurSEInstancePluginTypes(repository);
        this.endpoints = new TurSEInstanceEndpoints(repository);
    }

    public TurSNSiteSEInstances service() {
        return siteSEInstances;
    }

    /** T1337 — the endpoint lookup the engine instance processes use. */
    public TurSEInstanceEndpoints endpoints() {
        return endpoints;
    }

    public TurSEInstancePluginTypes pluginTypes() {
        return pluginTypes;
    }

    /** The repository behind both, for code under test that loads SE instances itself. */
    public TurSEInstanceRepository repository() {
        return repository;
    }

    /** The plugin type the attached instance {@code seInstanceId} runs; for a plugin factory. */
    public String pluginTypeOf(String seInstanceId) {
        return pluginTypes.of(seInstanceId);
    }

    /**
     * Registers {@code seInstance} and points {@code site} at it. An instance
     * without an id gets one, since the site can only name it by id. A null
     * instance clears the site's reference.
     */
    public TurSNSite attach(TurSNSite site, TurSEInstance seInstance) {
        if (seInstance == null) {
            site.setTurSEInstanceId(null);
            return site;
        }
        if (seInstance.getId() == null) {
            seInstance.setId("se-" + (rows.size() + 1));
        }
        rows.put(seInstance.getId(), seInstance);
        site.setTurSEInstanceId(seInstance.getId());
        return site;
    }
}
