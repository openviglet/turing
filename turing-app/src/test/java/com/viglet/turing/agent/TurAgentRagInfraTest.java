package com.viglet.turing.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.llm.persistence.model.embedding.TurEmbeddingModel;
import com.viglet.turing.llm.persistence.repository.embedding.TurEmbeddingModelRepository;
import com.viglet.turing.rag.persistence.model.store.TurStoreInstance;
import com.viglet.turing.rag.persistence.repository.store.TurStoreInstanceRepository;

/** T1315 — an agent's embedding model and store are read by id from their own modules. */
class TurAgentRagInfraTest {

    private final TurEmbeddingModelRepository embeddingModels = mock(TurEmbeddingModelRepository.class);
    private final TurStoreInstanceRepository stores = mock(TurStoreInstanceRepository.class);
    private final TurAgentRagInfra infra = new TurAgentRagInfra(embeddingModels, stores);

    @Test
    void resolvesBothByTheIdsTheAgentHolds() {
        TurEmbeddingModel model = new TurEmbeddingModel();
        TurStoreInstance store = new TurStoreInstance();
        when(embeddingModels.findById("emb-1")).thenReturn(Optional.of(model));
        when(stores.findById("store-1")).thenReturn(Optional.of(store));
        TurAIAgent agent = new TurAIAgent();
        agent.setEmbeddingModelInstanceId("emb-1");
        agent.setStoreInstanceId("store-1");

        assertThat(infra.embeddingModel(agent)).containsSame(model);
        assertThat(infra.store(agent)).containsSame(store);
    }

    @Test
    void anAgentWithoutIdsOrNoAgentReadsNothing() {
        assertThat(infra.embeddingModel(new TurAIAgent())).isEmpty();
        assertThat(infra.store(null)).isEmpty();
        verifyNoInteractions(embeddingModels, stores);
    }
}
