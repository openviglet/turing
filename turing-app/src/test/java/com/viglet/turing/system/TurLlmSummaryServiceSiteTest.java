/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import com.viglet.turing.domain.llm.TurLLMInstanceRepositoryPort;
import com.viglet.turing.observability.TurMeterNames;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.repository.llm.TurLLMInstanceRepository;
import com.viglet.turing.resilience.llm.TurLlmModelFactory;
import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.system.security.TurSecretCryptoService;

/**
 * T1150 — a summary made for a site records its spend against that site, so the
 * site's cost summary and budget count it; a cache hit records nothing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@ExtendWith(MockitoExtension.class)
class TurLlmSummaryServiceSiteTest {

    @Mock
    private TurGlobalSettingsService globalSettingsService;
    @Mock
    private TurLLMInstanceRepository llmInstanceRepository;
    @Mock
    private TurLLMInstanceRepositoryPort llmInstanceRepositoryPort;
    @Mock
    private TurLlmModelFactory llmModelFactory;
    @Mock
    private TurSecretCryptoService secretCryptoService;
    @Mock
    private TurLlmCacheService llmCacheService;
    @Mock
    private TurLLMTokenUsageService tokenUsageService;
    @Mock
    private TurStructuredCompletionPort structuredCompletion;
    @Mock
    private ChatModel chatModel;
    @InjectMocks
    private TurLlmSummaryService service;

    private final TurLLMInstance instance = new TurLLMInstance();

    @BeforeEach
    void setUp() {
        instance.setId("llm-1");
        instance.setEnabled(1);
        when(globalSettingsService.getDefaultLlmId()).thenReturn("llm-1");
        when(llmInstanceRepository.findById("llm-1")).thenReturn(Optional.of(instance));
    }

    @Test
    void aSummaryForASiteRecordsItsSpendAgainstTheSite() {
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage("summary"))));
        when(llmCacheService.get("k")).thenReturn(null);
        when(llmModelFactory.createChatModel(eq(instance), any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        var result = service.generate("k", "data", "prompt", false, null, "Sample");

        assertThat(result.success()).isTrue();
        verify(tokenUsageService).recordUsage(eq(instance), eq(response), anyString(), isNull(),
                eq(TurMeterNames.STAGE_CHAT_BACKGROUND), isNull(), eq("Sample"));
    }

    @Test
    void aCacheHitRecordsNothing() {
        when(llmCacheService.get("k")).thenReturn("cached");

        var result = service.generate("k", "data", "prompt", false, null, "Sample");

        assertThat(result.content()).isEqualTo("cached");
        verifyNoInteractions(tokenUsageService);
    }
}
