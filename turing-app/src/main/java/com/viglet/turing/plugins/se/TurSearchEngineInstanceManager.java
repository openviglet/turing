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

import java.net.URL;
import java.util.Locale;
import java.util.Optional;

/**
 * Interface for managing search engine instances.
 * Implementations handle the initialization and lifecycle of search engine connections.
 *
 * @author Alexandre Oliveira
 * @since 2025.4.4
 */
public interface TurSearchEngineInstanceManager {

    /**
     * Initializes a search engine instance for the given site and locale.
     *
     * @param siteName the name of the semantic navigation site
     * @param locale the locale for the search
     * @return Optional containing the search engine instance if successful, empty otherwise
     */
    Optional<Object> initInstance(String siteName, Locale locale);

    /**
     * Gets the search engine URL for a given site and locale.
     *
     * @param siteName the name of the semantic navigation site
     * @param locale the locale for the search
     * @return Optional containing the URL if found, empty otherwise
     */
    Optional<URL> getSearchEngineUrl(String siteName, Locale locale);

    /**
     * Gets the type of search engine this manager handles.
     *
     * @return the search engine type (e.g., "solr", "elasticsearch")
     */
    String getEngineType();
}
