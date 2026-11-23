/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent.llm.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.viglet.turing.batch.TurBatchChatRequest;
import com.viglet.turing.batch.TurBatchChatResult;
import com.viglet.turing.batch.TurBatchInferenceService;
import com.viglet.turing.batch.TurBatchState;
import com.viglet.turing.batch.persistence.model.TurBatchJob;
import com.viglet.turing.batch.persistence.repository.TurBatchJobRepository;
import com.viglet.turing.llm.gateway.TurGatewayContext;
import com.viglet.turing.llm.gateway.TurGatewayKeyService;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.BatchCreateRequest;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.llm.persistence.model.gateway.TurGatewayBatchFile;
import com.viglet.turing.llm.persistence.model.gateway.TurGatewayKey;
import com.viglet.turing.llm.persistence.repository.gateway.TurGatewayBatchFileRepository;

/**
 * T1341 — the gateway's batch wire: what a file must hold, which lines reach the
 * vendor, how a refused line comes back, and that one key cannot see another's.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurGatewayBatchServiceTest {

    private final TurGatewayBatchFileRepository files = mock(TurGatewayBatchFileRepository.class);
    private final TurBatchJobRepository jobs = mock(TurBatchJobRepository.class);
    private final TurBatchInferenceService batches = mock(TurBatchInferenceService.class);
    private final TurGatewayKeyService keys = mock(TurGatewayKeyService.class);
    private final TurGatewayLlmService llm = mock(TurGatewayLlmService.class);
    private final TurGatewayBatchService service =
            new TurGatewayBatchService(files, jobs, batches, keys, llm);

    private final TurGatewayKey key = new TurGatewayKey();
    private final TurLLMInstance instance = new TurLLMInstance();

    private static String line(String customId, String model, String messages) {
        return "{\"custom_id\":\"" + customId + "\",\"method\":\"POST\",\"url\":\"/v1/chat/completions\","
                + "\"body\":{\"model\":\"" + model + "\",\"messages\":" + messages + ",\"max_tokens\":200}}";
    }

    private static final String SYSTEM_AND_USER =
            "[{\"role\":\"system\",\"content\":\"Write in pt-BR.\"},{\"role\":\"user\",\"content\":\"Describe Celeste.\"}]";

    @BeforeEach
    void setUp() {
        key.setId("key-1");
        TurGatewayContext.set(key);
        instance.setId("gpt-4o-mini");
        when(llm.resolveChatInstance(anyString())).thenReturn(instance);
        when(keys.isModelAllowed(eq(key), anyString())).thenReturn(true);
        when(batches.isEnabled()).thenReturn(true);
        when(batches.isSupported(instance)).thenReturn(true);
        when(files.save(any())).thenAnswer(inv -> {
            TurGatewayBatchFile file = inv.getArgument(0);
            file.setId("f1");
            return file;
        });
    }

    @AfterEach
    void tearDown() {
        TurGatewayContext.clear();
    }

    private TurGatewayBatchFile stored(String content) {
        TurGatewayBatchFile file = new TurGatewayBatchFile();
        file.setId("f1");
        file.setKeyId("key-1");
        file.setPurpose("batch");
        file.setContent(content);
        when(files.findById("f1")).thenReturn(Optional.of(file));
        return file;
    }

    private TurBatchJob submittedJob() {
        TurBatchJob job = new TurBatchJob();
        job.setId("j1");
        job.setPurpose(TurGatewayBatchService.PURPOSE);
        job.setTotalRequests(1);
        when(batches.submit(eq(instance), eq("gateway"), anyList(), anyString())).thenAnswer(inv -> {
            job.setContextJson(inv.getArgument(3));
            return Optional.of(job);
        });
        when(jobs.findById("j1")).thenReturn(Optional.of(job));
        return job;
    }

    @Test
    void anUploadStoresTheFileWhenEveryLineParses() {
        byte[] jsonl = (line("a", "gpt-4o-mini", SYSTEM_AND_USER) + "\n").getBytes(StandardCharsets.UTF_8);

        var file = service.upload("games.jsonl", "batch", jsonl);

        assertThat(file.id()).isEqualTo("file-f1");
        assertThat(file.purpose()).isEqualTo("batch");
        assertThat(file.bytes()).isEqualTo(jsonl.length);
    }

    @Test
    void anUploadRefusesAnotherPurposeABrokenLineAndARepeatedId() {
        byte[] ok = line("a", "m", SYSTEM_AND_USER).getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.upload("x.jsonl", "fine-tune", ok))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("purpose");
        assertThatThrownBy(() -> service.upload("x.jsonl", "batch", "{not json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Line 1");
        String twice = line("a", "m", SYSTEM_AND_USER) + "\n" + line("a", "m", SYSTEM_AND_USER);
        assertThatThrownBy(() -> service.upload("x.jsonl", "batch", twice.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("repeats custom_id a");
    }

    @Test
    void aBatchSendsTheAllowedLinesAndAnswersARefusedLineAsAnErrorLine() {
        stored(line("ok", "gpt-4o-mini", SYSTEM_AND_USER) + "\n"
                + line("denied", "gpt-5", SYSTEM_AND_USER) + "\n"
                + line("multi", "gpt-4o-mini",
                        "[{\"role\":\"user\",\"content\":\"a\"},{\"role\":\"assistant\",\"content\":\"b\"}]"));
        when(keys.isModelAllowed(key, "gpt-5")).thenReturn(false);
        TurBatchJob job = submittedJob();

        var batch = service.create(new BatchCreateRequest("file-f1", "/v1/chat/completions", "24h", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TurBatchChatRequest>> sent = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(batches).submit(eq(instance), eq("gateway"), sent.capture(), anyString());
        assertThat(sent.getValue()).extracting(TurBatchChatRequest::customId).containsExactly("ok");
        assertThat(sent.getValue().getFirst().systemPrompt()).isEqualTo("Write in pt-BR.");
        assertThat(sent.getValue().getFirst().maxTokens()).isEqualTo(200);
        assertThat(batch.id()).isEqualTo("batch_j1");
        assertThat(batch.status()).isEqualTo("validating");
        assertThat(batch.request_counts().total()).isEqualTo(3);
        assertThat(batch.request_counts().failed()).isEqualTo(2);

        job.setState(TurBatchState.COMPLETED);
        when(batches.fetchResults(job)).thenReturn(List.of(TurBatchChatResult.ok("ok", "Celeste é...", 30L, 12L)));
        String output = service.fileContent(service.batch("batch_j1").output_file_id());

        assertThat(output.lines()).hasSize(3);
        assertThat(output).contains("\"custom_id\":\"ok\"").contains("Celeste é...")
                .contains("\"status_code\":200").contains("\"total_tokens\":42");
        assertThat(output).contains("\"custom_id\":\"denied\"").contains("model_not_permitted");
        assertThat(output).contains("\"custom_id\":\"multi\"").contains("invalid_request");
    }

    @Test
    void aBatchMixingModelsIsRefused() {
        stored(line("a", "gpt-4o-mini", SYSTEM_AND_USER) + "\n" + line("b", "claude", SYSTEM_AND_USER));
        TurLLMInstance other = new TurLLMInstance();
        other.setId("claude");
        when(llm.resolveChatInstance("claude")).thenReturn(other);

        assertThatThrownBy(() -> service.create(new BatchCreateRequest("file-f1", "/v1/chat/completions", "24h", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("same model");
    }

    @Test
    void aBatchIsRefusedWhenTheTierIsOffOrTheVendorHasNone() {
        stored(line("a", "gpt-4o-mini", SYSTEM_AND_USER));
        var request = new BatchCreateRequest("file-f1", "/v1/chat/completions", "24h", null);

        when(batches.isSupported(instance)).thenReturn(false);
        assertThatThrownBy(() -> service.create(request)).hasMessageContaining("no batch tier");

        when(batches.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.create(request)).hasMessageContaining("turing.batch.enabled");
    }

    @Test
    void anotherKeyCannotSeeTheFileOrTheBatch() {
        stored(line("a", "gpt-4o-mini", SYSTEM_AND_USER));
        submittedJob();
        service.create(new BatchCreateRequest("file-f1", "/v1/chat/completions", "24h", null));

        TurGatewayKey other = new TurGatewayKey();
        other.setId("key-2");
        TurGatewayContext.set(other);

        assertThatThrownBy(() -> service.fileContent("file-f1")).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.batch("batch_j1")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void theOutputIsRefusedUntilTheBatchCompletes() {
        stored(line("a", "gpt-4o-mini", SYSTEM_AND_USER));
        submittedJob();
        service.create(new BatchCreateRequest("file-f1", "/v1/chat/completions", "24h", null));

        assertThat(service.batch("batch_j1").output_file_id()).isNull();
        assertThatThrownBy(() -> service.fileContent("file-out-j1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("validating");
    }
}
