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

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.TurSNFieldType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * What other modules may call on {@code TurSNSiteFieldExtRepository}, under its own names, so the
 * repository stays internal to sn (T1312, ADR 0005). Each method delegates.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Service
public class TurSNSiteFieldExts {
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    public TurSNSiteFieldExts(TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    public List<TurSNSiteFieldExt> findAll() {
        return turSNSiteFieldExtRepository.findAll();
    }

    public List<TurSNSiteFieldExt> findByTurSNSite(Sort sort, TurSNSite turSNSite) {
        return turSNSiteFieldExtRepository.findByTurSNSite(sort, turSNSite);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndName(TurSNSite turSNSite, String name) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndName(turSNSite, name);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndEnabled(TurSNSite turSNSite, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(turSNSite, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndFacetAndEnabled(TurSNSite turSNSite, int facet, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabled(turSNSite, facet, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndNameAndFacetAndEnabled(TurSNSite turSNSite, String name, int facet, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndNameAndFacetAndEnabled(turSNSite, name, facet, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(TurSNSite turSNSite, int facet, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(turSNSite, facet, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndFacetAndEnabledAndType(TurSNSite turSNSite, int facet, int enabled, TurSEFieldType type) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndFacetAndEnabledAndType(turSNSite, facet, enabled, type);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndHlAndEnabled(TurSNSite turSNSite, int hl, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndHlAndEnabled(turSNSite, hl, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndMltAndEnabled(TurSNSite turSNSite, int mlt, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndMltAndEnabled(turSNSite, mlt, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndRequiredAndEnabled(TurSNSite turSNSite, int required, int enabled) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndRequiredAndEnabled(turSNSite, required, enabled);
    }

    public List<TurSNSiteFieldExt> findByTurSNSiteAndSnType(TurSNSite turSNSite, TurSNFieldType turSNFieldType) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndSnType(turSNSite, turSNFieldType);
    }

    public Optional<TurSNSiteFieldExt> findById(String id) {
        return turSNSiteFieldExtRepository.findById(id);
    }

    public TurSNSiteFieldExt save(TurSNSiteFieldExt entity) {
        return turSNSiteFieldExtRepository.save(entity);
    }
}