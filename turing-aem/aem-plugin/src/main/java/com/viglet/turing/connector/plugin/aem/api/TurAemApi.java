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
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v2/aem")
@Tag(name = "Heartbeat", description = "Heartbeat")
public class TurAemApi {
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemPluginProcess turAemPluginProcess;
    private final List<String> currentContentIdList = new ArrayList<>();

    @Inject
    public TurAemApi(TurAemSourceRepository turAemSourceRepository,
                     TurAemPluginProcess turAemPluginProcess) {
        this.turAemSourceRepository = turAemSourceRepository;
        this.turAemPluginProcess = turAemPluginProcess;
    }

    @GetMapping("status")
    public Map<String, String> status() {
        return statusOk();
    }


    @PostMapping("index/{name}")
    public ResponseEntity<Map<String, String>> indexContentId(@PathVariable String name,
                                                              @RequestBody TurAemPathList turAemPathList) {
        if (hasNonRepeatedRequest(name, turAemPathList)) {
            turAemPluginProcess.sentToIndexStandaloneAsync(name, turAemPathList);
        }
        return ResponseEntity.ok(statusSent());

    }

    private void updateCurrentRequests(String name, TurAemPathList turAemPathList) {
        currentContentIdList.clear();
        turAemPathList.getPaths()
                .forEach(path -> currentContentIdList.add(getSourceWithContentId(name, path)));
    }

    private boolean hasNonRepeatedRequest(String name, TurAemPathList turAemPathList) {
        turAemPathList.getPaths().forEach(path -> {
            String pathName = getSourceWithContentId(name, path);
            if (currentContentIdList.contains(pathName)) {
                turAemPathList.getPaths().remove(path);
                log.warn("Repeated request: {}", pathName);
            }
        });
        if (!turAemPathList.getPaths().isEmpty()) {
            updateCurrentRequests(name, turAemPathList);
        }
        return !turAemPathList.getPaths().isEmpty();
    }

    private static @NotNull String getSourceWithContentId(String name, String path) {
        return name + "-" + path;
    }

    @GetMapping("index/{name}/all")
    public ResponseEntity<Map<String, String>> indexAll(@PathVariable String name) {
        return turAemSourceRepository.findByName(name).map(turAemSource -> {
            turAemPluginProcess.indexAllAsync(turAemSource);
            return ResponseEntity.ok(statusOk());
        }).orElseGet(() -> ResponseEntity.notFound().build());

    }

    private static Map<String, String> statusOk() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }

    private static Map<String, String> statusSent() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "sent");
        return status;
    }
}
