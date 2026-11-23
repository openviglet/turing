/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.llm.gateway;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * T1341 — the subset of OpenAI's Files and Batch wire the gateway speaks, in
 * {@code snake_case} like {@link TurOpenAiWire}: a client that batches against
 * OpenAI points {@code base_url} at Turing and batches the same way.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurOpenAiBatchWire {

    private TurOpenAiBatchWire() {
    }

    /** A {@code file} object, as {@code POST /v1/files} and {@code GET /v1/files/{id}} answer. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FileObject(String id, String object, long bytes, long created_at, String filename,
            String purpose) {
    }

    /** The {@code POST /v1/batches} body. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BatchCreateRequest(String input_file_id, String endpoint, String completion_window,
            Map<String, String> metadata) {
    }

    /** The {@code request_counts} of a batch. */
    public record RequestCounts(long total, long completed, long failed) {
    }

    /** A {@code batch} object. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BatchObject(String id, String object, String endpoint, String input_file_id,
            String completion_window, String status, String output_file_id, long created_at,
            Long completed_at, RequestCounts request_counts, Map<String, String> metadata) {
    }
}
