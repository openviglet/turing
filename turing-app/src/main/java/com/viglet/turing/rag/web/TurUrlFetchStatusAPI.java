/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.rag.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.system.urlfetch.TurUrlFetchService;
import com.viglet.turing.system.urlfetch.TurUrlFetchService.TurBrowserlessStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The URL-fetch status probe of the Global Settings page. It keeps its path
 * under {@code /api/system/global-settings}, but lives with the URL fetcher it
 * probes, so the system module does not depend on rag (T1311, ADR 0005).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/system/global-settings")
@Tag(name = "Global Settings", description = "Global Settings API")
public class TurUrlFetchStatusAPI {
    private final TurUrlFetchService turUrlFetchService;

    public TurUrlFetchStatusAPI(TurUrlFetchService turUrlFetchService) {
        this.turUrlFetchService = turUrlFetchService;
    }

    /**
     * T739 / §XLVIII — probes whether the configured {@code browserless} sidecar
     * is reachable, so the admin can validate it before switching the URL-fetch
     * mode to {@code HEADLESS}/{@code AUTO}. Never sends anything sensitive — only
     * an availability flag, the reported browser version, and an error string.
     */
    @Operation(summary = "Check whether the browserless sidecar is reachable for headless URL fetching")
    @GetMapping("/url-fetch/browserless/status")
    public TurBrowserlessStatus getBrowserlessStatus() {
        return turUrlFetchService.checkBrowserless();
    }
}
