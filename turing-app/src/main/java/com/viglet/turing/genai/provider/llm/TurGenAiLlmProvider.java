package com.viglet.turing.genai.provider.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import com.viglet.turing.persistence.model.llm.TurLLMInstance;

public interface TurGenAiLlmProvider {

    String getPluginType();

    ChatModel createChatModel(TurLLMInstance turLLMInstance, String decryptedApiKey);

    EmbeddingModel createEmbeddingModel(TurLLMInstance turLLMInstance, String decryptedApiKey);
}
