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

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository;

/**
 * T1315 — which engine plugin an SE instance runs, by the instance's id. An SN
 * site names its SE instance by id, and every search resolves the site's plugin
 * from it, so the answer is a cached string rather than a query per request.
 * {@code TurSEInstanceEvictionListener} clears the cache after any SE instance
 * write commits.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSEInstancePluginTypes {

    /** Service-level cache: SE instance id to its vendor's plugin type. */
    public static final String CACHE_NAME = "turSEInstancePluginType";

    /** What a site with no resolvable SE instance runs, as before T1315. */
    public static final String DEFAULT_PLUGIN_TYPE = "solr";

    private final TurSEInstanceRepository seInstanceRepository;

    public TurSEInstancePluginTypes(TurSEInstanceRepository seInstanceRepository) {
        this.seInstanceRepository = seInstanceRepository;
    }

    /** The plugin type of the SE instance {@code seInstanceId}; Solr when it is gone. */
    @Cacheable(cacheNames = CACHE_NAME)
    public String of(String seInstanceId) {
        return seInstanceRepository.findById(seInstanceId)
                .map(TurSEInstancePluginTypes::pluginTypeOf)
                .orElse(DEFAULT_PLUGIN_TYPE);
    }

    /** The plugin type an SE instance's vendor names: its plugin, else its id, else Solr. */
    public static String pluginTypeOf(TurSEInstance instance) {
        if (instance == null || instance.getTurSEVendor() == null) {
            return DEFAULT_PLUGIN_TYPE;
        }
        var vendor = instance.getTurSEVendor();
        return StringUtils.isNotBlank(vendor.getPlugin()) ? vendor.getPlugin() : vendor.getId();
    }
}
