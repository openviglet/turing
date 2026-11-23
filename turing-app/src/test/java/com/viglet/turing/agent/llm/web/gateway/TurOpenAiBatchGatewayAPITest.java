/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent.llm.web.gateway;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.viglet.turing.agent.llm.gateway.TurGatewayBatchService;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.FileObject;

/**
 * T1341 — the batch wire's HTTP shape: an OpenAI SDK's multipart upload binds,
 * output comes back as JSONL, and errors use the gateway's OpenAI envelope.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurOpenAiBatchGatewayAPITest {

    private final TurGatewayBatchService service = mock(TurGatewayBatchService.class);
    private final MockMvc mvc = standaloneSetup(new TurOpenAiBatchGatewayAPI(service)).build();

    @Test
    void anSdkUploadBindsTheFileAndThePurpose() throws Exception {
        when(service.upload(eq("games.jsonl"), eq("batch"), any()))
                .thenReturn(new FileObject("file-f1", "file", 10, 1L, "games.jsonl", "batch"));

        mvc.perform(multipart("/v1/files")
                        .file(new MockMultipartFile("file", "games.jsonl", "application/jsonl", "{}".getBytes()))
                        .param("purpose", "batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo("file-f1")))
                .andExpect(jsonPath("$.object", equalTo("file")));
    }

    @Test
    void outputIsJsonlAndAnUnknownIdIs404AndABadRequest400() throws Exception {
        when(service.fileContent("file-out-j1")).thenReturn("{\"custom_id\":\"a\"}\n");
        when(service.batch("batch_nope")).thenThrow(new NoSuchElementException("No such batch: batch_nope"));
        when(service.fileContent("file-out-j2")).thenThrow(new IllegalArgumentException("Batch is in_progress"));

        mvc.perform(get("/v1/files/file-out-j1/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/jsonl"))
                .andExpect(content().string("{\"custom_id\":\"a\"}\n"));
        mvc.perform(get("/v1/batches/batch_nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", equalTo("not_found")));
        mvc.perform(get("/v1/files/file-out-j2/content"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type", equalTo("invalid_request_error")));
    }
}
