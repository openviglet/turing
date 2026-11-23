/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * ADR 0005 / T1310 — the search-engine module. Its API is this package (the
 * {@link com.viglet.turing.searchengine.TurSearchEnginePlugin} contract and its factory)
 * plus the {@code result} and {@code facet} beans a search returns. The Solr, Lucene,
 * Elasticsearch and Content AI implementations under it are internal: other modules
 * reach an engine only through the plugin.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@org.springframework.modulith.ApplicationModule(displayName = "Search engines",
        allowedDependencies = {
        "shared", "shared :: *", "system", "system :: *", "observability",
        "observability :: *", "tenant", "tenant :: *", "auth", "auth :: *", "storage",
        "storage :: *" })
package com.viglet.turing.searchengine;
