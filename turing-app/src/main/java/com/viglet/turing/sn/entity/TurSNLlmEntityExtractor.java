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

package com.viglet.turing.sn.entity;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import com.viglet.turing.genai.TurChatToolOptions;
import com.viglet.turing.genai.TurToolExecutionLoop;
import com.viglet.turing.genai.tool.TurToolCallbackPipeline;
import com.viglet.turing.observability.TurMeterNames;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.resilience.llm.TurLlmModelFactory;
import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.system.security.TurSecretCryptoService;

import lombok.extern.slf4j.Slf4j;

/**
 * T1228 — asks the site's LLM for the entities a document names through tool
 * calling: the model calls {@code record_entities} once with three lists, and the
 * call's arguments are the answer. No JSON is parsed out of prose (structured
 * output is a non-goal); a model that never calls the tool found nothing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@Component
public class TurSNLlmEntityExtractor implements TurSNEntityExtractor {

    private static final String SYSTEM = """
            You read one document and record the people, organisations and places it names, \
            by calling the record_entities tool exactly once. Copy each name as the document \
            writes it. Record only names the document contains. Then answer "done".""";

    private final TurLlmModelFactory llmModelFactory;
    private final TurSecretCryptoService secretCryptoService;
    private final TurToolCallbackPipeline toolCallbackPipeline;
    private final TurToolExecutionLoop toolExecutionLoop;
    private final TurLLMTokenUsageService tokenUsageService;

    public TurSNLlmEntityExtractor(TurLlmModelFactory llmModelFactory,
            TurSecretCryptoService secretCryptoService, TurToolCallbackPipeline toolCallbackPipeline,
            TurToolExecutionLoop toolExecutionLoop, TurLLMTokenUsageService tokenUsageService) {
        this.llmModelFactory = llmModelFactory;
        this.secretCryptoService = secretCryptoService;
        this.toolCallbackPipeline = toolCallbackPipeline;
        this.toolExecutionLoop = toolExecutionLoop;
        this.tokenUsageService = tokenUsageService;
    }

    @Override
    public TurSNEntities extract(String siteName, TurLLMInstance llm, String text) {
        ChatModel chatModel = llmModelFactory.createChatModel(llm,
                secretCryptoService.decrypt(llm.getApiKeyEncrypted()));
        RecordEntitiesTool tool = new RecordEntitiesTool();
        ToolCallback[] callbacks = toolCallbackPipeline.decorate(MethodToolCallbackProvider.builder()
                .toolObjects(tool).build().getToolCallbacks());
        Prompt prompt = new Prompt(List.of(new SystemMessage(SYSTEM), new UserMessage(text)),
                TurChatToolOptions.builderFrom(chatModel).toolCallbacks(callbacks).build());
        ChatResponse response = toolExecutionLoop.call(chatModel, prompt);
        try {
            tokenUsageService.recordUsage(llm, response, null, null, TurMeterNames.STAGE_CHAT_BACKGROUND,
                    null, siteName);
        } catch (RuntimeException e) {
            log.debug("[T1228] token usage record failed: {}", e.getMessage());
        }
        TurSNEntities recorded = tool.recorded.get();
        return recorded == null ? TurSNEntities.NONE : recorded;
    }

    /** The one tool the model is given; its arguments are the extraction. */
    static final class RecordEntitiesTool {

        private final AtomicReference<TurSNEntities> recorded = new AtomicReference<>();

        @Tool(name = "record_entities", description = ".")
        public String recordEntities(
                @ToolParam(description = "People named in the document", required = false) List<String> people,
                @ToolParam(description = "Organisations named in the document", required = false)
                List<String> organizations,
                @ToolParam(description = "Places named in the document", required = false) List<String> places) {
            recorded.set(new TurSNEntities(people, organizations, places));
            return "recorded";
        }

        TurSNEntities recorded() {
            return recorded.get();
        }
    }
}
