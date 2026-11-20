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

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;

/**
 * The SN sites as other modules see them: the lookups and writes they need,
 * under the repository's own names, so the repository stays internal to sn
 * (T1312, ADR 0005). Each method delegates; transactions and lazy loading are
 * the repository's.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Service
public class TurSNSites {
    private final TurSNSiteRepository turSNSiteRepository;

    public TurSNSites(TurSNSiteRepository turSNSiteRepository) {
        this.turSNSiteRepository = turSNSiteRepository;
    }

    public Optional<TurSNSite> findById(String id) {
        return turSNSiteRepository.findById(id);
    }

    public Optional<TurSNSite> findByName(String name) {
        return turSNSiteRepository.findByName(name);
    }

    public Optional<TurSNSite> findByNameIgnoreCase(String name) {
        return turSNSiteRepository.findByNameIgnoreCase(name);
    }

    public Optional<TurSNSiteGenAi> findGenAiByNameIgnoreCase(String name) {
        return turSNSiteRepository.findGenAiByNameIgnoreCase(name);
    }

    public Optional<TurSNSite> findByIdWithGenAi(String id) {
        return turSNSiteRepository.findByIdWithGenAi(id);
    }

    public Optional<TurSNSite> findByNameWithGenAi(String name) {
        return turSNSiteRepository.findByNameWithGenAi(name);
    }

    public List<TurSNSite> findAll() {
        return turSNSiteRepository.findAll();
    }

    public List<TurSNSite> findAll(Sort sort) {
        return turSNSiteRepository.findAll(sort);
    }

    public long count() {
        return turSNSiteRepository.count();
    }

    public TurSNSite save(TurSNSite turSNSite) {
        return turSNSiteRepository.save(turSNSite);
    }

    public TurSNSite saveAndFlush(TurSNSite turSNSite) {
        return turSNSiteRepository.saveAndFlush(turSNSite);
    }

    public void delete(TurSNSite turSNSite) {
        turSNSiteRepository.delete(turSNSite);
    }

    public void flush() {
        turSNSiteRepository.flush();
    }
}
