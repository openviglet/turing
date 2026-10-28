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
package com.viglet.turing.sn.synonym;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.plugins.se.TurSESynonymApplyResult;
import com.viglet.turing.tenant.TurTenantScheduledFanOut;

import lombok.extern.slf4j.Slf4j;

/**
 * T1168 / §CXIV.2 — re-installs SN site synonyms at boot on engines that keep
 * them only in memory. Saving a synonym writes the row; Push to engine installs
 * it. Solr and Elasticsearch keep what was pushed across a restart, but the
 * embedded Lucene engine holds it in {@code TurLuceneSynonymRegistry}, which
 * starts empty, so without this sweep the query analyzer silently runs without
 * synonyms until someone pushes again.
 *
 * <p>Only sites whose engine answers {@code synonymsSurviveRestart() == false}
 * are touched. With tenancy on, the sweep runs once per active tenant through
 * {@link TurTenantScheduledFanOut}, so each pass sees that tenant's sites. A
 * site whose apply fails is logged and skipped; the sweep never stops startup.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@Component
public class TurSNSynonymStartupReapply {

    private final TurSNSiteRepository siteRepository;
    private final TurSNSynonymApplyService applyService;
    private final TurTenantScheduledFanOut tenantFanOut;

    public TurSNSynonymStartupReapply(TurSNSiteRepository siteRepository,
            TurSNSynonymApplyService applyService,
            TurTenantScheduledFanOut tenantFanOut) {
        this.siteRepository = siteRepository;
        this.applyService = applyService;
        this.tenantFanOut = tenantFanOut;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            tenantFanOut.forEachActiveTenant(this::reapplyVolatileSites);
        } catch (RuntimeException e) {
            log.warn("[Synonyms] boot re-apply skipped: {}", e.getMessage(), e);
        }
    }

    /** Re-applies synonyms for every site in the current tenant whose engine forgets them. */
    void reapplyVolatileSites() {
        List<String> siteIds = siteRepository.findAll().stream().map(TurSNSite::getId).toList();
        for (String siteId : siteIds) {
            try {
                if (!applyService.losesSynonymsOnRestart(siteId)) {
                    continue;
                }
                Map<String, TurSESynonymApplyResult> results = applyService.applyAll(siteId);
                int applied = results.values().stream().mapToInt(TurSESynonymApplyResult::applied).sum();
                log.info("[Synonyms] re-applied {} rule(s) at boot to site '{}' across {} locale(s)",
                        applied, siteId, results.size());
            } catch (RuntimeException e) {
                log.warn("[Synonyms] could not re-apply synonyms at boot for site '{}': {}",
                        siteId, e.getMessage(), e);
            }
        }
    }
}
