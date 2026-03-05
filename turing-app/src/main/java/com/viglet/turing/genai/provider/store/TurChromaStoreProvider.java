package com.viglet.turing.genai.provider.store;

import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.store.TurStoreInstance;

@Component
public class TurChromaStoreProvider implements TurGenAiStoreProvider {

    private static final String DEFAULT_COLLECTION = "turing";

    @Override
    public String getPluginType() {
        return "chroma";
    }

    @Override
    public VectorStore createVectorStore(TurStoreInstance turStoreInstance,
            EmbeddingModel embeddingModel,
            String decryptedCredential) {

        ChromaApi chromaApi = ChromaApi.builder()
                .baseUrl(turStoreInstance.getUrl())
                .build();

        if (StringUtils.hasText(decryptedCredential)) {
            if (decryptedCredential.startsWith("basic:")) {
                String[] credentials = decryptedCredential.substring("basic:".length()).split(":", 2);
                if (credentials.length == 2) {
                    chromaApi = chromaApi.withBasicAuthCredentials(credentials[0], credentials[1]);
                }
            } else if (decryptedCredential.startsWith("token:")) {
                chromaApi = chromaApi.withKeyToken(decryptedCredential.substring("token:".length()));
            } else {
                chromaApi = chromaApi.withKeyToken(decryptedCredential);
            }
        }

        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName(resolveCollectionName(turStoreInstance.getCollectionName()))
                .initializeSchema(true)
                .build();
    }

    private String resolveCollectionName(String collectionName) {
        return StringUtils.hasText(collectionName) ? collectionName : DEFAULT_COLLECTION;
    }
}
