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


    @Autowired
    public TurConnectorApi(TurConnectorIndexingService indexingService, TurConnectorSolrService turConnectorSolr) {
        this.indexingService = indexingService;
        this.turConnectorSolr = turConnectorSolr;
    }

    @GetMapping("status")
    public Map<String, String> status() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }

    @GetMapping("validate/{source}")
    public TurConnectorValidateDifference validateSource(@PathVariable String source) {
        return TurConnectorValidateDifference.builder()
                .missing(turConnectorSolr.solrMissingContent(source))
                .extra(turConnectorSolr. solrExtraContent(source))
                .build();
    }

    @GetMapping("monitoring/index/{source}")
    public ResponseEntity<List<TurConnectorIndexingModel>> monitoryIndexByName(@PathVariable String source) {
        return indexingService.getBySource(source)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


}
