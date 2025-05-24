/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
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

package com.viglet.turing.connector.plugin.aem.api;

import com.google.inject.Inject;
import com.viglet.turing.connector.plugin.aem.TurAemPluginProcess;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginIndexingRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/aem")
@Tag(name = "Heartbeat", description = "Heartbeat")
public class TurAemApi {
    private final TurAemPluginIndexingRepository turAemIndexingRepository;
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemPluginProcess turAemPluginProcess;

    @Inject
    public TurAemApi(TurAemPluginIndexingRepository turAemIndexingRepository,
                     TurAemSourceRepository turAemSourceRepository,
                     TurAemPluginProcess turAemPluginProcess) {
        this.turAemIndexingRepository = turAemIndexingRepository;
        this.turAemSourceRepository = turAemSourceRepository;
        this.turAemPluginProcess = turAemPluginProcess;
    }

    @GetMapping
    public Map<String, String> info() {
        return statusOk();
    }

    @Transactional
    @GetMapping("reindex/{group}")
    public Map<String, String> reindex(@PathVariable String group) {
        turAemIndexingRepository.deleteByIndexGroupAndOnceFalse(group);
        return statusOk();
    }

    @Transactional
    @GetMapping("reindex/once/{group}")
    public Map<String, String> reIndexOnce(@PathVariable String group) {
        turAemIndexingRepository.deleteByIndexGroupAndOnceTrue(group);
        return statusOk();
    }

    @Transactional
    @PostMapping("reindex/{group}")
    public ResponseEntity<Object> reindexGuid(@PathVariable String group, @RequestBody TurAEMPathList turAEMPathList) {
        return turAemSourceRepository.findByGroup(group).map(turAemSource -> {
            turAEMPathList.paths.forEach(path -> {
                turAemIndexingRepository.deleteByAemIdAndIndexGroup(path, group);
                turAemPluginProcess.indexGuid(turAemSource, path);
            });
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());

    }

    private static Map<String, String> statusOk() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }
}
