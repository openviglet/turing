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
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.TurAemPluginProcess;
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
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemPluginProcess turAemPluginProcess;

    @Inject
    public TurAemApi(TurAemSourceRepository turAemSourceRepository,
                     TurAemPluginProcess turAemPluginProcess) {
        this.turAemSourceRepository = turAemSourceRepository;
        this.turAemPluginProcess = turAemPluginProcess;

    }

    @GetMapping
    public Map<String, String> info() {
        return statusOk();
    }

    @Transactional
    @PostMapping("index/{name}")
    public ResponseEntity<Object> indexContentId(@PathVariable String name,
                                                   @RequestBody TurAemPathList turAemPathList) {
        return turAemSourceRepository.findByName(name).map(turAemSource -> {
            TurConnectorSession turConnectorSession = TurAemPluginProcess.getTurConnectorSession(turAemSource);
            turAemPathList.paths.forEach(path ->
                    turAemPluginProcess.indexContentId(turConnectorSession, turAemSource, path));
            turAemPluginProcess.finished(turConnectorSession);
            return ResponseEntity.ok().build();

        }).orElseGet(() -> ResponseEntity.notFound().build());

    }

    @Transactional
    @GetMapping("index/{name}/all")
    public ResponseEntity<Object> indexAll(@PathVariable String name) {
        return turAemSourceRepository.findByName(name).map(turAemSource -> {
            turAemPluginProcess.indexAll(turAemSource);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());

    }

    private static Map<String, String> statusOk() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }
}
