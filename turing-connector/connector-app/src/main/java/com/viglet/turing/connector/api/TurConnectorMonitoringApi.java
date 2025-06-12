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

package com.viglet.turing.connector.api;

import com.google.inject.Inject;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexing;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/connector/monitoring/indexing")
@Tag(name = "Connector API", description = "Connector API")
public class TurConnectorMonitoringApi {
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;

    @Inject
    public TurConnectorMonitoringApi(TurConnectorIndexingRepository turConnectorIndexingRepository) {
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
    }

    @GetMapping
    public ResponseEntity<TurConnectorMonitoring> monitoringIndexing() {
        return turConnectorIndexingRepository.findAllByOrderByModificationDateDesc(Limit.of(50))
                .map(indexing -> ResponseEntity.ok(getMonitoring(indexing)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TurConnectorMonitoring getMonitoring(List<TurConnectorIndexing> indexing) {
        return TurConnectorMonitoring.builder()
                .sources(turConnectorIndexingRepository.findAllSources())
                .indexing(indexing)
                .build();
    }

    @GetMapping("{source}")
    public ResponseEntity<TurConnectorMonitoring> monitoringIndexingBySource(@PathVariable String source) {
        return turConnectorIndexingRepository.findAllBySourceOrderByModificationDateDesc(source, Limit.of(50))
                .map(indexing -> ResponseEntity.ok(getMonitoring(indexing)))
                .orElseGet(() -> ResponseEntity.notFound().build());

    }
}
