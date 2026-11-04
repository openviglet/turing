/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.testsupport.research;

import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.resilience.llm.TurLlmModelFactory;

import reactor.core.publisher.Flux;

/**
 * T1220b — a {@code @Primary} model factory whose every chat model answers by
 * the prompt it is given: the research planning prompt with fixed {@code STEP:}
 * lines, the composing prompt with {@code SECTION:} lines citing {@code [1]},
 * {@code [2]} and the out-of-range {@code [99]}, and anything else (the
 * copilot's facet parse) with nothing, which the planner treats as no filters.
 *
 * <p>Lives under {@code com.viglet.testsupport} so it reaches only the IT that
 * imports it, never the shared context.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@TestConfiguration
public class TurResearchStubLlmSupport {

    /** A sub-question on words the fixture does not hold, so it finds nothing. */
    public static final String EMPTY_STEP = "Which starships carry them?";

    public static final String PLAN = """
            SUMMARY: what the mythology says about dragons and sea monsters
            STEP: Which dragons breathe fire? | dragon fire
            STEP: Which sea monsters attack ships? | kraken sea monster
            STEP: %s | zzqxv starship hyperdrive
            """.formatted(EMPTY_STEP);

    public static final String COMPOSITION = """
            SECTION: Dragons
            Dragons breathe fire and guard treasure [1][99].
            SECTION: Sea monsters
            Sea monsters drag ships under [2].
            SECTION: Opinion
            Monsters are fascinating.
            """;

    @Bean
    @Primary
    public TurLlmModelFactory researchStubLlmModelFactory() {
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                String text = prompt.getContents();
                String reply = text.contains("Split the question into") ? PLAN
                        : text.contains("Write an in-depth overview") ? COMPOSITION : "";
                return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }

            @Override
            public ChatOptions getOptions() {
                return ChatOptions.builder().build();
            }
        };
        return new TurLlmModelFactory(null, null, null, null, null) {
            @Override
            public ChatModel createChatModel(TurLLMInstance llmInstance, String decryptedApiKey) {
                return model;
            }
        };
    }
}
