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
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import com.viglet.turing.sn.persistence.repository.locale.TurSNSiteLocaleRepository;

/**
 * The locales of the SN sites as other modules see them, under the repository's
 * own names, so the repository stays internal to sn (T1312, ADR 0005).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Service
public class TurSNSiteLocales {
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;

    public TurSNSiteLocales(TurSNSiteLocaleRepository turSNSiteLocaleRepository) {
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
    }

    public Optional<TurSNSiteLocale> findById(String id) {
        return turSNSiteLocaleRepository.findById(id);
    }

    public TurSNSiteLocale findByTurSNSiteAndLanguage(TurSNSite turSNSite, Locale language) {
        return turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(turSNSite, language);
    }

    public List<TurSNSiteLocale> findByTurSNSite(TurSNSite turSNSite) {
        return turSNSiteLocaleRepository.findByTurSNSite(turSNSite);
    }

    public List<TurSNSiteLocale> findByTurSNSite(Sort sort, TurSNSite turSNSite) {
        return turSNSiteLocaleRepository.findByTurSNSite(sort, turSNSite);
    }

    public TurSNSiteLocale findFirstByTurSNSiteOrderByPositionAsc(TurSNSite turSNSite) {
        return turSNSiteLocaleRepository.findFirstByTurSNSiteOrderByPositionAsc(turSNSite);
    }

    public List<TurSNSiteLocale> findByCore(String core) {
        return turSNSiteLocaleRepository.findByCore(core);
    }

    public List<TurSNSiteLocale> findAll() {
        return turSNSiteLocaleRepository.findAll();
    }

    public TurSNSiteLocale save(TurSNSiteLocale turSNSiteLocale) {
        return turSNSiteLocaleRepository.save(turSNSiteLocale);
    }
}
