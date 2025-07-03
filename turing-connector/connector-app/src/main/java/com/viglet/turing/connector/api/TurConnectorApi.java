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

import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
import com.viglet.turing.connector.domain.TurConnectorValidateDifference;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.service.TurConnectorIndexingService;
import com.viglet.turing.connector.service.TurConnectorSolrService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/connector")
@Tag(name = "Connector API", description = "Connector API")
public class TurConnectorApi {
    private final TurConnectorIndexingService indexingService;
    private final TurConnectorSolrService turConnectorSolr;
    private final TurConnectorPlugin plugin;

    @Autowired
    public TurConnectorApi(TurConnectorIndexingService indexingService, TurConnectorSolrService turConnectorSolr,
                           TurConnectorPlugin plugin) {
        this.indexingService = indexingService;
        this.turConnectorSolr = turConnectorSolr;
        this.plugin = plugin;
    }

    @GetMapping("status")
    public Map<String, String> status() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }

    private static Map<String, String> statusSent() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "sent");
        return status;
    }

    @GetMapping("validate/{provider}/{source}")
    public TurConnectorValidateDifference validateSource(@PathVariable String provider, @PathVariable String source) {
        return TurConnectorValidateDifference.builder()
                .missing(turConnectorSolr.solrMissingContent(source, provider.toUpperCase()))
                .extra(turConnectorSolr.solrExtraContent(source, provider.toUpperCase()))
                .build();
    }

    @GetMapping("monitoring/index/{provider}/{source}")
    public ResponseEntity<List<TurConnectorIndexingModel>> monitoryIndexByName(@PathVariable String provider,
                                                                               @PathVariable String source) {
        List<TurConnectorIndexingModel> indexingModelList = indexingService.getBySourceAndProvider(source, provider);
        return indexingModelList.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(indexingModelList);
    }

    @GetMapping("index/{name}/all")
    public ResponseEntity<Map<String, String>> indexAll(@PathVariable String name) {
        plugin.indexAll(name);
        return ResponseEntity.ok(statusSent());
    }

    @GetMapping("reindex/{name}/all")
    public ResponseEntity<Map<String, String>> reindexAll(@PathVariable String name) {
        indexingService.deleteByProvider(plugin.getProviderName());
        plugin.indexAll(name);
        return ResponseEntity.ok(statusSent());
    }
}
