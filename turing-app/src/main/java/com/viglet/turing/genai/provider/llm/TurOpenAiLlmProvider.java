package com.viglet.turing.genai.provider.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.llm.TurLLMInstance;

@Component
public class TurOpenAiLlmProvider implements TurGenAiLlmProvider {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_CHAT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";

    @Override
    public String getPluginType() {
        return "openai";
    }

    @Override
    public ChatModel createChatModel(TurLLMInstance turLLMInstance, String decryptedApiKey) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(resolveBaseUrl(turLLMInstance.getUrl()))
                .apiKey(requireApiKey(decryptedApiKey, turLLMInstance))
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(resolveChatModelName(turLLMInstance.getModelName()));

        if (turLLMInstance.getTemperature() != null) {
            optionsBuilder.temperature(turLLMInstance.getTemperature());
        }
        if (turLLMInstance.getTopP() != null) {
            optionsBuilder.topP(turLLMInstance.getTopP());
        }
        if (turLLMInstance.getSeed() != null) {
            optionsBuilder.seed(turLLMInstance.getSeed());
        }

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(TurLLMInstance turLLMInstance, String decryptedApiKey) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(resolveBaseUrl(turLLMInstance.getUrl()))
                .apiKey(requireApiKey(decryptedApiKey, turLLMInstance))
                .build();

        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(resolveEmbeddingModelName(turLLMInstance.getModelName()))
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, embeddingOptions);
    }

    private String requireApiKey(String decryptedApiKey, TurLLMInstance turLLMInstance) {
        if (!StringUtils.hasText(decryptedApiKey)) {
            throw new IllegalStateException(
                    "Missing API key for OpenAI provider in LLM instance: " + turLLMInstance.getId());
        }
        return decryptedApiKey;
    }

    private String resolveBaseUrl(String configuredUrl) {
        return StringUtils.hasText(configuredUrl) ? configuredUrl : DEFAULT_BASE_URL;
    }

    private String resolveChatModelName(String configuredModel) {
        return StringUtils.hasText(configuredModel) ? configuredModel : DEFAULT_CHAT_MODEL;
    }

    private String resolveEmbeddingModelName(String configuredModel) {
        return StringUtils.hasText(configuredModel) ? configuredModel : DEFAULT_EMBEDDING_MODEL;
    }
}
