/*
 * Copyright (C) 2016-2026 the original author or authors.
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
package com.viglet.turing.exchange.sn;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonym;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonymType;
import com.viglet.turing.persistence.repository.sn.synonym.TurSNSynonymRepository;
import com.viglet.turing.plugins.se.TurSESynonymApplyResult;
import com.viglet.turing.sn.synonym.TurSNSynonymApplyService;

import lombok.extern.slf4j.Slf4j;

/**
 * T1263 — carries an SN site's synonym rules through export and import. The
 * rules are their own table, not a {@link TurSNSite} collection, so the site
 * export never saw them and a site rebuilt from an export lost its synonym
 * search.
 *
 * <ul>
 * <li>{@link #export} reads a site's rules for the export file (inside the
 * export's read transaction, since {@code terms} is lazy).</li>
 * <li>{@link #snapshot} keeps a site's current rules before a re-import deletes
 * the site, for an older export that carries none.</li>
 * <li>{@link #restore} writes the rules onto the recreated site and, once the
 * import commits, pushes them to the engine, as the console's apply does.</li>
 * </ul>
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Service
public class TurSNSiteSynonymExchangeService {

    private final TurSNSynonymRepository synonymRepository;
    private final TurSNSynonymApplyService applyService;

    public TurSNSiteSynonymExchangeService(TurSNSynonymRepository synonymRepository,
            TurSNSynonymApplyService applyService) {
        this.synonymRepository = synonymRepository;
        this.applyService = applyService;
    }

    /** The site's rules as the export writes them, in a stable order. */
    public List<TurSNSynonymExchange> export(TurSNSite site) {
        return synonymRepository.findByTurSNSite(Sort.by(Sort.Direction.ASC, "id"), site).stream()
                .map(TurSNSynonymExchange::of)
                .toList();
    }

    /**
     * The site's current rules without their ids, so they can be written back
     * after the site is deleted and recreated (the delete cascades to them).
     */
    public List<TurSNSynonymExchange> snapshot(TurSNSite site) {
        return export(site).stream()
                .map(s -> new TurSNSynonymExchange(null, s.name(), s.type(), s.language(), s.input(),
                        s.terms(), s.enabled()))
                .toList();
    }

    /**
     * Writes the rules onto {@code site} and schedules the engine push for
     * after the import commits. Returns how many rules were written.
     */
    public int restore(TurSNSite site, List<TurSNSynonymExchange> synonyms) {
        if (synonyms == null || synonyms.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (TurSNSynonymExchange exchange : synonyms) {
            if (exchange == null || exchange.language() == null) {
                log.warn("Skipping a synonym of SN Site '{}' with no language", site.getName());
                continue;
            }
            synonymRepository.save(toEntity(site, exchange));
            saved++;
        }
        if (saved > 0) {
            applyAfterCommit(site.getId());
        }
        return saved;
    }

    private TurSNSynonym toEntity(TurSNSite site, TurSNSynonymExchange exchange) {
        TurSNSynonym synonym = new TurSNSynonym();
        // An id another row already holds (the same export imported into a second
        // tenant) would turn the save into an update of that row; mint a new one.
        if (exchange.id() != null && !synonymRepository.existsById(exchange.id())) {
            synonym.setId(exchange.id());
        }
        synonym.setName(exchange.name());
        synonym.setType(Objects.requireNonNullElse(exchange.type(), TurSNSynonymType.REGULAR));
        synonym.setLanguage(exchange.language());
        synonym.setInput(exchange.input());
        synonym.setTerms(exchange.terms());
        synonym.setEnabled(exchange.enabled());
        synonym.setTurSNSite(site);
        return synonym;
    }

    private void applyAfterCommit(String siteId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    apply(siteId);
                }
            });
        } else {
            apply(siteId);
        }
    }

    private void apply(String siteId) {
        try {
            Map<String, TurSESynonymApplyResult> results = applyService.applyAll(siteId);
            int applied = results.values().stream().mapToInt(TurSESynonymApplyResult::applied).sum();
            log.info("[Synonyms] applied {} imported rule(s) to site '{}' across {} locale(s)",
                    applied, siteId, results.size());
        } catch (RuntimeException e) {
            log.warn("[Synonyms] imported synonyms of site '{}' were saved but not applied: {}",
                    siteId, e.getMessage(), e);
        }
    }
}
