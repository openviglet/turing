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
package com.viglet.turing.agent.llm.web.gateway;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.viglet.turing.agent.llm.gateway.TurGatewayBatchService;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.BatchCreateRequest;
import com.viglet.turing.llm.gateway.TurOpenAiWire.ErrorEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * T1341 / §CXIII.6 — OpenAI's Files and Batch endpoints on the gateway, so a
 * bulk client pays the vendor's batch price through Turing's keys and budgets.
 * Errors use the gateway's OpenAI envelope: a bad request is 400
 * {@code invalid_request_error}, an id the key does not own is 404, and a
 * vendor that refuses the batch is 502.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@ConditionalOnProperty(prefix = "turing.gateway", name = "enabled", havingValue = "true")
@Tag(name = "LLM Gateway", description = "OpenAI-compatible governed egress (Block AZ)")
public class TurOpenAiBatchGatewayAPI {

    private static final String INVALID_REQUEST = "invalid_request_error";

    private final TurGatewayBatchService batchService;

    public TurOpenAiBatchGatewayAPI(TurGatewayBatchService batchService) {
        this.batchService = batchService;
    }

    @Operation(summary = "OpenAI-compatible file upload (purpose=batch)")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("purpose") String purpose) {
        return answer(() -> {
            try {
                return batchService.upload(file.getOriginalFilename(), purpose, file.getBytes());
            } catch (IOException e) {
                throw new IllegalArgumentException("The file could not be read");
            }
        });
    }

    @Operation(summary = "OpenAI-compatible file retrieval")
    @GetMapping(value = "/files/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> file(@PathVariable String fileId) {
        return answer(() -> batchService.file(fileId));
    }

    @Operation(summary = "OpenAI-compatible file content: a batch's input or output JSONL")
    @GetMapping(value = "/files/{fileId}/content")
    public ResponseEntity<?> fileContent(@PathVariable String fileId) {
        ResponseEntity<?> answer = answer(() -> batchService.fileContent(fileId));
        if (answer.getBody() instanceof String jsonl) {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/jsonl")).body(jsonl);
        }
        return answer;
    }

    @Operation(summary = "OpenAI-compatible batch creation over /v1/chat/completions")
    @PostMapping(value = "/batches", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createBatch(@RequestBody BatchCreateRequest request) {
        return answer(() -> batchService.create(request));
    }

    @Operation(summary = "OpenAI-compatible batch retrieval")
    @GetMapping(value = "/batches/{batchId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> batch(@PathVariable String batchId) {
        return answer(() -> batchService.batch(batchId));
    }

    @Operation(summary = "OpenAI-compatible batch cancellation")
    @PostMapping(value = "/batches/{batchId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(@PathVariable String batchId) {
        return answer(() -> batchService.cancel(batchId));
    }

    private ResponseEntity<?> answer(Supplier<Object> call) {
        try {
            return ResponseEntity.ok(call.get());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorEnvelope.of(e.getMessage(), INVALID_REQUEST, "not_found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ErrorEnvelope.of(e.getMessage(), INVALID_REQUEST, null));
        } catch (RuntimeException e) {
            log.warn("[Gateway] batch call failed: {}", e.getMessage());
            return TurOpenAiCompatibleGatewayAPI.upstreamError(e);
        }
    }
}
