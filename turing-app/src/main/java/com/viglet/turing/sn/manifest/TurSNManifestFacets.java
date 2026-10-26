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

package com.viglet.turing.sn.manifest;

import com.viglet.turing.persistence.model.sn.field.TurSNSiteFacetFieldEnum;

/**
 * The site-level facet behaviour an SN manifest declares (T1135). Every
 * component is optional: {@code null} leaves the live setting alone.
 *
 * @param enabled       whether the site returns facets at all
 * @param type          how facets of different fields combine: {@code AND}
 *                      narrows (drill-down), {@code OR} broadens
 * @param itemType      how the values of one field combine: {@code OR} is
 *                      multi-select and keeps an applied facet visible, {@code AND}
 *                      narrows, {@code DEFAULT} follows {@code type}
 * @param itemsPerFacet how many values each facet lists
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public record TurSNManifestFacets(Boolean enabled, TurSNSiteFacetFieldEnum type,
        TurSNSiteFacetFieldEnum itemType, Integer itemsPerFacet) {
}
