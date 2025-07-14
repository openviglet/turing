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
import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
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
@RequestMapping("/api/v2/connector/monitoring/indexing")
@Tag(name = "Connector API", description = "Connector API")
public class TurConnectorMonitoringApi {
    private final TurConnectorIndexingService indexingService;
    private final TurConnectorPlugin plugin;

    @Inject
    public TurConnectorMonitoringApi(TurConnectorIndexingService indexingService, TurConnectorPlugin plugin) {
        this.indexingService = indexingService;
        this.plugin = plugin;
    }

    @GetMapping
    public ResponseEntity<TurConnectorMonitoring> monitoringIndexing() {
        List<TurConnectorIndexingModel> indexing = indexingService.findAll();
        return ResponseEntity.ok(indexing.isEmpty() ? new TurConnectorMonitoring() : getMonitoring(indexing));
    }

    @GetMapping("{source}")
    public ResponseEntity<TurConnectorMonitoring> monitoringIndexingBySource(@PathVariable String source) {
        List<TurConnectorIndexingModel> indexing = indexingService.getBySourceAndProvider(source,
                plugin.getProviderName());
        return ResponseEntity.ok(indexing.isEmpty() ? new TurConnectorMonitoring() : getMonitoring(indexing));
    }

    private TurConnectorMonitoring getMonitoring(List<TurConnectorIndexingModel> indexing) {
        return TurConnectorMonitoring.builder()
                .sources(indexingService.getAllSources(plugin.getProviderName()))
                .indexing(indexing)
                .build();
    }
}
