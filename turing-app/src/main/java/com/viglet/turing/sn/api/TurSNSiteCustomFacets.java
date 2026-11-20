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

package com.viglet.turing.sn.api;

import com.viglet.turing.sn.persistence.model.field.TurSNSiteCustomFacet;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteCustomFacetRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * What other modules may call on {@code TurSNSiteCustomFacetRepository}, under its own names, so the
 * repository stays internal to sn (T1312, ADR 0005). Each method delegates.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Service
public class TurSNSiteCustomFacets {
    private final TurSNSiteCustomFacetRepository turSNSiteCustomFacetRepository;

    public TurSNSiteCustomFacets(TurSNSiteCustomFacetRepository turSNSiteCustomFacetRepository) {
        this.turSNSiteCustomFacetRepository = turSNSiteCustomFacetRepository;
    }

    public List<TurSNSiteCustomFacet> findByFieldExtsWithDetails(Collection<TurSNSiteFieldExt> fieldExts) {
        return turSNSiteCustomFacetRepository.findByFieldExtsWithDetails(fieldExts);
    }
}