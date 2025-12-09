/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.plugins.se;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating and selecting search engine plugins based on configuration.
 *
 * @author Alexandre Oliveira
 * @since 2025.4.4
 */
@Slf4j
@Component
public class TurSearchEnginePluginFactory {

    private final Map<String, TurSearchEnginePlugin> pluginMap;
    private final String defaultEngineType;

    public TurSearchEnginePluginFactory(
            List<TurSearchEnginePlugin> plugins,
            @Value("${turing.search.engine.type:solr}") String defaultEngineType) {
        this.pluginMap = plugins.stream()
                .collect(Collectors.toMap(
                        plugin -> plugin.getPluginType().toLowerCase(),
                        Function.identity()));
        this.defaultEngineType = defaultEngineType;
        log.info("Initialized TurSearchEnginePluginFactory with {} plugins. Default engine: {}",
                pluginMap.size(), defaultEngineType);
        pluginMap.keySet().forEach(type -> log.info("Registered plugin: {}", type));
    }

    /**
     * Gets the configured default search engine plugin.
     *
     * @return the default search engine plugin
     * @throws IllegalStateException if the configured plugin is not found
     */
    public TurSearchEnginePlugin getDefaultPlugin() {
        return getPlugin(defaultEngineType);
    }

    /**
     * Gets a specific search engine plugin by type.
     *
     * @param engineType the type of search engine (e.g., "solr", "elasticsearch")
     * @return the search engine plugin
     * @throws IllegalStateException if the plugin is not found
     */
    public TurSearchEnginePlugin getPlugin(String engineType) {
        TurSearchEnginePlugin plugin = pluginMap.get(engineType.toLowerCase());
        if (plugin == null) {
            throw new IllegalStateException(
                    "Search engine plugin '" + engineType + "' not found. Available plugins: " + pluginMap.keySet());
        }
        return plugin;
    }

    /**
     * Gets the default engine type configured.
     *
     * @return the default engine type
     */
    public String getDefaultEngineType() {
        return defaultEngineType;
    }
}
