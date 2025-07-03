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
import com.viglet.turing.connector.domain.TurConnectorMonitoring;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.service.TurConnectorIndexingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/connector/monitoring/indexing/{provider}")
@Tag(name = "Connector API", description = "Connector API")
public class TurConnectorMonitoringApi {
    private final TurConnectorIndexingService indexingService;

    @Inject
    public TurConnectorMonitoringApi(TurConnectorIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping
    public ResponseEntity<TurConnectorMonitoring> monitoringIndexing(@PathVariable String provider) {
        List<TurConnectorIndexingModel> indexing = indexingService.findAll();
        return indexing.isEmpty() ? ResponseEntity.notFound().build() :
                ResponseEntity.ok(getMonitoring(provider, indexing));
    }

    @GetMapping("{source}")
    public ResponseEntity<TurConnectorMonitoring> monitoringIndexingBySource(@PathVariable String provider,
                                                                             @PathVariable String source) {
        List<TurConnectorIndexingModel> indexing = indexingService.getBySourceAndProvider(source, provider);
        return indexing.isEmpty() ? ResponseEntity.notFound().build() :
                ResponseEntity.ok(getMonitoring(provider.toUpperCase(), indexing));
    }

    private TurConnectorMonitoring getMonitoring(String provider, List<TurConnectorIndexingModel> indexing) {
        return TurConnectorMonitoring.builder()
                .sources(indexingService.getAllSources(provider.toUpperCase()))
                .indexing(indexing)
                .build();
    }
}
