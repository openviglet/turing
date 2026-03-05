package com.viglet.turing.genai.provider.llm;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.llm.TurLLMInstance;

@Component
public class TurOllamaLlmProvider implements TurGenAiLlmProvider {

    @Override
    public String getPluginType() {
        return "ollama";
    }

    @Override
    public ChatModel createChatModel(TurLLMInstance turLLMInstance, String decryptedApiKey) {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(turLLMInstance.getUrl())
                .build();

        OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                .model(turLLMInstance.getModelName());

        if (turLLMInstance.getTemperature() != null) {
            optionsBuilder.temperature(turLLMInstance.getTemperature());
        }
        if (turLLMInstance.getTopK() != null) {
            optionsBuilder.topK(turLLMInstance.getTopK());
        }
        if (turLLMInstance.getTopP() != null) {
            optionsBuilder.topP(turLLMInstance.getTopP());
        }
        if (turLLMInstance.getRepeatPenalty() != null) {
            optionsBuilder.repeatPenalty(turLLMInstance.getRepeatPenalty());
        }
        if (turLLMInstance.getSeed() != null) {
            optionsBuilder.seed(turLLMInstance.getSeed());
        }
        if (turLLMInstance.getNumPredict() != null) {
            optionsBuilder.numPredict(turLLMInstance.getNumPredict());
        }

        List<String> stopSequences = parseStopSequences(turLLMInstance.getStop());
        if (!stopSequences.isEmpty()) {
            optionsBuilder.stop(stopSequences);
        }

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(TurLLMInstance turLLMInstance, String decryptedApiKey) {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(turLLMInstance.getUrl())
                .build();

        OllamaEmbeddingOptions embeddingOptions = OllamaEmbeddingOptions.builder()
                .model(turLLMInstance.getModelName())
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(embeddingOptions)
                .build();
    }

    private List<String> parseStopSequences(String rawStop) {
        if (!StringUtils.hasText(rawStop)) {
            return List.of();
        }
        return Arrays.stream(rawStop.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
