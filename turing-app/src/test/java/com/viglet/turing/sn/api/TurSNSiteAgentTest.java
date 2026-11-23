/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.viglet.turing.llm.persistence.model.TurLLMInstance;

/**
 * T1322h — the active LLM instance rule the media indexer relied on before it
 * read the site's agent through {@link TurSNSiteAgents}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNSiteAgentTest {

    private static TurLLMInstance llm(String id, int enabled) {
        TurLLMInstance instance = new TurLLMInstance();
        instance.setId(id);
        instance.setEnabled(enabled);
        return instance;
    }

    @Test
    void theFirstEnabledInstanceIsActive() {
        TurLLMInstance off = llm("a", 0);
        TurLLMInstance on = llm("b", 1);

        assertThat(new TurSNSiteAgent("t", true, List.of(off, on)).activeLlmInstance()).contains(on);
    }

    @Test
    void withNoneEnabledTheFirstIsActive() {
        TurLLMInstance first = llm("a", 0);

        assertThat(new TurSNSiteAgent("t", true, List.of(first, llm("b", 0))).activeLlmInstance())
                .contains(first);
    }

    @Test
    void anAgentWithoutInstancesHasNone() {
        assertThat(new TurSNSiteAgent("t", true, null).activeLlmInstance()).isEmpty();
    }
}
