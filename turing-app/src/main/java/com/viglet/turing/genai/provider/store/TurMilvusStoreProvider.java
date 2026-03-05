package com.viglet.turing.genai.provider.store;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.store.TurStoreInstance;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;

@Component
public class TurMilvusStoreProvider implements TurGenAiStoreProvider {

    private static final String DEFAULT_COLLECTION = "turing";

    @Override
    public String getPluginType() {
        return "milvus";
    }

    @Override
    public VectorStore createVectorStore(TurStoreInstance turStoreInstance,
            EmbeddingModel embeddingModel,
            String decryptedCredential) {

        ConnectParam.Builder connectBuilder = ConnectParam.newBuilder()
                .withUri(turStoreInstance.getUrl());

        if (StringUtils.hasText(decryptedCredential)) {
            connectBuilder.withToken(decryptedCredential);
        }

        MilvusServiceClient milvusServiceClient = new MilvusServiceClient(connectBuilder.build());

        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(resolveCollectionName(turStoreInstance.getCollectionName()))
                .initializeSchema(true)
                .build();
    }

    private String resolveCollectionName(String collectionName) {
        return StringUtils.hasText(collectionName) ? collectionName : DEFAULT_COLLECTION;
    }
}
