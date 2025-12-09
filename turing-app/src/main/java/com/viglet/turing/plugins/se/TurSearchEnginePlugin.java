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

import com.viglet.turing.se.result.TurSEResults;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;

import java.util.Optional;

/**
 * Interface for search engine plugins that can be used by Turing.
 * Implementations include Solr, Elasticsearch, etc.
 *
 * @author Alexandre Oliveira
 * @since 2025.4.4
 */
public interface TurSearchEnginePlugin {

    /**
     * Retrieves search results from the search engine based on the given context.
     *
     * @param context the search context containing site name, locale, and search parameters
     * @return Optional containing search results if successful, empty otherwise
     */
    Optional<TurSEResults> retrieveSearchResults(TurSNSiteSearchContext context);

    /**
     * Retrieves facet results for a specific facet field.
     *
     * @param context the search context containing site name, locale, and search parameters
     * @param facetName the name of the facet to retrieve
     * @return Optional containing search results with facets if successful, empty otherwise
     */
    Optional<TurSEResults> retrieveFacetResults(TurSNSiteSearchContext context, String facetName);

    /**
     * Gets the type identifier of this search engine plugin.
     *
     * @return the plugin type (e.g., "solr", "elasticsearch")
     */
    String getPluginType();
}
