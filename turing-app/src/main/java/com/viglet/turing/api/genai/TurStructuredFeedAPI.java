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

package com.viglet.turing.api.genai;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.genai.feed.TurStructuredFeedIngestService.IngestResult;
import com.viglet.turing.genai.feed.TurStructuredFeedTrigger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * T1229 — {@code POST /api/genai/structured-feed/{sourceId}/ingest}: pull one
 * configured structured feed now. Authenticated like the rest of
 * {@code /api/genai/**}; a CI job sends an API key in the {@code Key} header.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@RestController
@RequestMapping("/api/genai/structured-feed")
@Tag(name = "Structured Feed", description = "Pull-based ingest of structured feeds into SN sites")
public class TurStructuredFeedAPI {

    private final TurStructuredFeedTrigger trigger;

    public TurStructuredFeedAPI(TurStructuredFeedTrigger trigger) {
        this.trigger = trigger;
    }

    /**
     * @return the run's outcome; {@code deIndexBaseline=false} means this node had
     *         no previous run of the source, so ids removed from the feed were not
     *         looked for and {@code deIndexed=0} is not a clean sync
     */
    @Operation(summary = "Ingest one configured structured feed now")
    @PostMapping("/{sourceId}/ingest")
    public IngestResult ingest(@PathVariable String sourceId) {
        return trigger.ingest(sourceId);
    }
}
