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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/connector")
@Tag(name = "Connector API", description = "Connector API")
public class TurConnectorApi {
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;

    @Inject
    public TurConnectorApi(TurConnectorIndexingRepository turConnectorIndexingRepository) {
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
    }

    @GetMapping("status")
    public Map<String, String> status() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }

    @GetMapping("monitoring/index/{name}")
    public ResponseEntity<List<TurConnectorIndexing>> monitoryIndexByName(@PathVariable String name) {
        return turConnectorIndexingRepository.findAllByNameOrderByModificationDateDesc(name, Limit.of(50))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());

    }
}
