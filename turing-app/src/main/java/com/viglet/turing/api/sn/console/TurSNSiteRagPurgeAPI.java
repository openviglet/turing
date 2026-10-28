/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.api.sn.console;

import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.genai.TurSNRagOrphanPurgeService;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * T1158 — removes the RAG chunks of documents an SN site's search engine no
 * longer holds, one result per locale. Deletes made since T1158 already remove
 * their chunks; this clears the ones stored before.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@RestController
@RequestMapping("/api/sn")
@Tag(name = "Semantic Navigation Site", description = "Semantic Navigation Site API")
public class TurSNSiteRagPurgeAPI {

    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurSNRagOrphanPurgeService purgeService;

    public TurSNSiteRagPurgeAPI(TurSNSiteRepository turSNSiteRepository,
            TurSNSiteLocaleRepository turSNSiteLocaleRepository, TurSNRagOrphanPurgeService purgeService) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
        this.purgeService = purgeService;
    }

    @Operation(summary = "Remove the RAG chunks of documents the search engine no longer holds")
    @PostMapping("/{id}/genai/purge-orphans")
    @Secured({"ROLE_ADMIN", "SN_VIEW"})
    public List<TurSNRagOrphanPurgeService.Result> purgeOrphans(@PathVariable String id) {
        TurSNSite site = turSNSiteRepository.findById(id)
                .orElseThrow(() -> new TurNotFoundException("SN site not found: " + id));
        return turSNSiteLocaleRepository.findByTurSNSite(site).stream()
                .map(siteLocale -> purgeService.purge(site, siteLocale))
                .toList();
    }
}
