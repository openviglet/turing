package com.viglet.turing.genai.provider.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.genai.provider.TurProviderOptionsParser;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;

@ExtendWith(MockitoExtension.class)
class TurGeminiLlmProviderTest {

    @Mock
    private TurProviderOptionsParser optionsParser;

    @InjectMocks
    private TurGeminiLlmProvider provider;

    private TurLLMInstance instance;

    @BeforeEach
    void setUp() {
        instance = new TurLLMInstance();
        instance.setId("test-id");
        instance.setUrl("https://us-central1-aiplatform.googleapis.com");
    }

    @Test
    void testGetPluginType() {
        assertEquals("gemini", provider.getPluginType());
    }

    @Test
    void testCreateChatModel_missingProjectId() {
        when(optionsParser.parse(any())).thenReturn(Map.of());
        when(optionsParser.stringValue(any(), eq("projectId"))).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> provider.createChatModel(instance, "dummy-key"));
    }

    @Test
    void testCreateEmbeddingModel_unsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> provider.createEmbeddingModel(instance, "dummy-key"));
    }
}
