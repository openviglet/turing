package com.viglet.turing.sn.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * T1228 — the model answers by calling {@code record_entities}: the tool's
 * arguments are the extraction, and a list it leaves out is empty.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurSNLlmEntityExtractorTest {

    @Test
    void theToolCallsArgumentsAreTheExtraction() {
        TurSNLlmEntityExtractor.RecordEntitiesTool tool = new TurSNLlmEntityExtractor.RecordEntitiesTool();
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder().toolObjects(tool).build()
                .getToolCallbacks();

        assertThat(callbacks).singleElement()
                .satisfies(callback -> assertThat(callback.getToolDefinition().name()).isEqualTo("record_entities"));
        callbacks[0].call("{\"people\":[\"Antonio Prado\"],\"places\":[\"São Paulo\"]}");

        assertThat(tool.recorded()).isEqualTo(
                new TurSNEntities(List.of("Antonio Prado"), List.of(), List.of("São Paulo")));
    }

    @Test
    void aModelThatNeverCallsTheToolRecordedNothing() {
        assertThat(new TurSNLlmEntityExtractor.RecordEntitiesTool().recorded()).isNull();
    }
}
