package com.viglet.turing.agent;

import com.viglet.turing.rag.TurGenAiContext;
import com.viglet.turing.rag.TurRagContextBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.llm.resilience.TurLlmModelFactory;
import com.viglet.turing.llm.persistence.model.embedding.TurEmbeddingModel;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;
import com.viglet.turing.llm.persistence.model.store.TurStoreInstance;
import com.viglet.turing.system.security.TurSecretCryptoService;

@ExtendWith(MockitoExtension.class)
class TurGenAiContextFactoryTest {

    private final com.viglet.turing.agent.InMemoryAgentLlmInstances agentLlms =
            new com.viglet.turing.agent.InMemoryAgentLlmInstances();

    @org.mockito.Spy
    private com.viglet.turing.agent.TurAgentLlmInstances agentLlmInstances = agentLlms.service();

    @Mock
    private TurLlmModelFactory llmModelFactory;

    @Mock
    private TurSecretCryptoService turSecretCryptoService;

    @Mock
    private TurRagContextBuilder turRagContextBuilder;

    @Mock
    private TurDefaultAgentResolver turDefaultAgentResolver;

    @Mock
    private com.viglet.turing.agent.TurAgentRagInfra agentRagInfra;

    @InjectMocks
    private TurGenAiContextFactory factory;

    private TurSNSiteGenAi turSNSiteGenAi;

    /** T1315 — the binding names its agent by id; the mocked resolver answers it from here. */
    private final java.util.Map<String, com.viglet.turing.agent.persistence.model.TurAIAgent> boundAgents =
            new java.util.HashMap<>();

    private void bindAgent(TurSNSiteGenAi genAi, com.viglet.turing.agent.persistence.model.TurAIAgent agent) {
        if (agent.getId() == null) {
            agent.setId("agent-" + (boundAgents.size() + 1));
        }
        boundAgents.put(agent.getId(), agent);
        genAi.setTurAIAgentId(agent.getId());
    }

    @BeforeEach
    void setUp() {
        turSNSiteGenAi = new TurSNSiteGenAi();
        // Mirror the real resolver with no global default agent set: the effective
        // agent is simply the binding's own agent (T622 fallback is inert here).
        lenient().when(turDefaultAgentResolver.resolveEffectiveAgent(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    TurSNSiteGenAi g = inv.getArgument(0);
                    return g == null ? null : boundAgents.get(g.getTurAIAgentId());
                });
    }

    @Test
    void testBuild_disabled_whenNoAgent() {
        // No agent attached → context is disabled.
        TurGenAiContext context = factory.build(turSNSiteGenAi);
        assertFalse(context.isEnabled());
    }

    @Test
    void testBuild_disabled_whenAgentRagDisabled() {
        TurAIAgent agent = new TurAIAgent();
        agent.setEnabled(1);
        agent.setRagEnabled(false);
        bindAgent(turSNSiteGenAi, agent);
        TurGenAiContext context = factory.build(turSNSiteGenAi);
        assertFalse(context.isEnabled());
    }

    @Test
    void testBuild_disabled_whenAgentHasNoLlm() {
        TurAIAgent agent = new TurAIAgent();
        agent.setEnabled(1);
        agent.setRagEnabled(true);
        // No LLM instances on the agent.
        bindAgent(turSNSiteGenAi, agent);
        TurGenAiContext context = factory.build(turSNSiteGenAi);
        assertFalse(context.isEnabled());
    }

    @Test
    void testBuild_disabled_whenVectorlessStructured_evenWithRagReadyAgent() {
        // T790 — a fully RAG-ready agent, but the site is explicitly in
        // VECTORLESS_STRUCTURED mode: the factory must return a disabled context
        // (no embeddings), so neither the batch reindex nor the per-doc index path
        // ever embeds. The gate short-circuits before any LLM/store lookup.
        TurAIAgent agent = new TurAIAgent();
        agent.setEnabled(1);
        agent.setRagEnabled(true);
        bindAgent(turSNSiteGenAi, agent);
        turSNSiteGenAi.setKnowledgeBaseMode(
                com.viglet.turing.sn.persistence.model.genai.TurSNKnowledgeBaseMode.VECTORLESS_STRUCTURED);

        TurGenAiContext context = factory.build(turSNSiteGenAi);

        assertFalse(context.isEnabled());
        // Never reached the secret decrypt / chat-model creation stage.
        verifyNoInteractions(turSecretCryptoService, llmModelFactory, turRagContextBuilder);
    }

    @Test
    void testBuild_enabled_whenHybridMode() {
        // HYBRID still requires (and builds) the vector setup — needsVectorSetup()==true.
        TurAIAgent agent = new TurAIAgent();
        agent.setEnabled(1);
        agent.setRagEnabled(true);
        agent.setSystemPrompt("agent prompt");

        TurLLMInstance llmInstance = new TurLLMInstance();
        llmInstance.setApiKeyEncrypted("llm-encrypted");
        llmInstance.setEnabled(1);
        agentLlms.attach(agent, llmInstance);

        TurStoreInstance storeInstance = new TurStoreInstance();
        storeInstance.setId("store-1");
        agent.setStoreInstanceId(storeInstance.getId());
        org.mockito.Mockito.lenient().when(agentRagInfra.store(agent))
                .thenReturn(java.util.Optional.of(storeInstance));

        TurEmbeddingModel embeddingModel = new TurEmbeddingModel();
        embeddingModel.setId("emb-1");
        agent.setEmbeddingModelInstanceId(embeddingModel.getId());
        org.mockito.Mockito.lenient().when(agentRagInfra.embeddingModel(agent))
                .thenReturn(java.util.Optional.of(embeddingModel));

        bindAgent(turSNSiteGenAi, agent);
        turSNSiteGenAi.setKnowledgeBaseMode(
                com.viglet.turing.sn.persistence.model.genai.TurSNKnowledgeBaseMode.HYBRID);

        when(turSecretCryptoService.decrypt("llm-encrypted")).thenReturn("llm-decrypted");
        ChatModel chatModel = mock(ChatModel.class);
        when(llmModelFactory.createChatModel(llmInstance, "llm-decrypted")).thenReturn(chatModel);
        EmbeddingModel springEmbeddingModel = mock(EmbeddingModel.class);
        VectorStore vectorStore = mock(VectorStore.class);
        var ragInfra = new TurRagContextBuilder.RagInfrastructure(
                vectorStore, springEmbeddingModel, null, null, null, "turing");
        when(turRagContextBuilder.build("emb-1", "store-1", null))
                .thenReturn(Optional.of(ragInfra));

        TurGenAiContext context = factory.build(turSNSiteGenAi);

        assertTrue(context.isEnabled());
    }

    @Test
    void testBuild_success() {
        TurAIAgent agent = new TurAIAgent();
        agent.setEnabled(1);
        agent.setRagEnabled(true);
        agent.setSystemPrompt("agent prompt");

        TurLLMInstance llmInstance = new TurLLMInstance();
        llmInstance.setApiKeyEncrypted("llm-encrypted");
        llmInstance.setEnabled(1);
        agentLlms.attach(agent, llmInstance);

        TurStoreInstance storeInstance = new TurStoreInstance();
        storeInstance.setId("store-1");
        agent.setStoreInstanceId(storeInstance.getId());
        org.mockito.Mockito.lenient().when(agentRagInfra.store(agent))
                .thenReturn(java.util.Optional.of(storeInstance));

        TurEmbeddingModel embeddingModel = new TurEmbeddingModel();
        embeddingModel.setId("emb-1");
        agent.setEmbeddingModelInstanceId(embeddingModel.getId());
        org.mockito.Mockito.lenient().when(agentRagInfra.embeddingModel(agent))
                .thenReturn(java.util.Optional.of(embeddingModel));

        bindAgent(turSNSiteGenAi, agent);

        when(turSecretCryptoService.decrypt("llm-encrypted")).thenReturn("llm-decrypted");

        ChatModel chatModel = mock(ChatModel.class);
        when(llmModelFactory.createChatModel(llmInstance, "llm-decrypted")).thenReturn(chatModel);

        EmbeddingModel springEmbeddingModel = mock(EmbeddingModel.class);
        VectorStore vectorStore = mock(VectorStore.class);
        var ragInfra = new TurRagContextBuilder.RagInfrastructure(
                vectorStore, springEmbeddingModel, null, null, null, "turing");
        when(turRagContextBuilder.build("emb-1", "store-1", null))
                .thenReturn(Optional.of(ragInfra));

        TurGenAiContext context = factory.build(turSNSiteGenAi);

        assertTrue(context.isEnabled());
        assertNotNull(context.getVectorStore());
        assertNotNull(context.getEmbeddingModel());
        assertNotNull(context.getChatModel());
        assertEquals("agent prompt", context.getSystemPrompt());
    }
}
