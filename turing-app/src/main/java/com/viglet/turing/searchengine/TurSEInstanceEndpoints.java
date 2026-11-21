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

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1337 — the endpoint URL an SE instance serves, by the instance's id. The
 * Solr, Elasticsearch and Lucene clients are keyed by endpoint and need nothing
 * else from the row, and an SN site names its SE instance by id (T1315), so the
 * endpoint is a cached string rather than a primary-key read per search.
 * {@code TurSEInstanceEvictionListener} clears the cache after any SE instance
 * write commits.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSEInstanceEndpoints {

    /** Service-level cache: SE instance id to its endpoint URL ("" when it has none). */
    public static final String CACHE_NAME = "turSEInstanceEndpoint";

    private final TurSEInstanceRepository seInstanceRepository;

    public TurSEInstanceEndpoints(TurSEInstanceRepository seInstanceRepository) {
        this.seInstanceRepository = seInstanceRepository;
    }

    /**
     * The endpoint URL of the SE instance {@code seInstanceId}; the empty string
     * when the instance is gone or has none, so nothing null is cached.
     */
    @Cacheable(cacheNames = CACHE_NAME)
    public String of(String seInstanceId) {
        return seInstanceRepository.findById(seInstanceId)
                .map(TurSEInstance::getEndpointUrl)
                .orElse("");
    }

    /**
     * The endpoint of the site's SE instance, or {@code null} when it names none
     * or none is set. Static and given the bean, because a call to {@link #of}
     * through {@code this} would bypass the cache proxy.
     */
    public static String ofSite(TurSEInstanceEndpoints endpoints, TurSNSite site) {
        return site == null ? null : ofInstance(endpoints, site.getTurSEInstanceId());
    }

    /**
     * The endpoint of SE instance {@code seInstanceId}, or {@code null} when the id
     * is null or the instance has none; static for the same reason as
     * {@link #ofSite}.
     */
    public static String ofInstance(TurSEInstanceEndpoints endpoints, String seInstanceId) {
        if (seInstanceId == null) {
            return null;
        }
        String endpoint = endpoints.of(seInstanceId);
        return endpoint == null || endpoint.isBlank() ? null : endpoint;
    }
}
