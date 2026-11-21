package com.viglet.turing.eval.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.eval.distillation.TurChatFeedbackService;
import com.viglet.turing.shared.exception.TurNotFoundException;

/**
 * T1332 — feedback rows hold their agent by a plain id that no tenant filter
 * guards, so the agent is loaded through its own repository first.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurChatFeedbackAPITest {

    private final TurChatFeedbackService feedbackService = mock(TurChatFeedbackService.class);
    private final TurAIAgentRepository agentRepository = mock(TurAIAgentRepository.class);
    private final TurChatFeedbackAPI api = new TurChatFeedbackAPI(feedbackService, agentRepository);

    @Test
    void anAgentTheCallerCannotSeeHasNoDatasetAndTakesNoFeedback() {
        when(agentRepository.existsById("other-tenants-agent")).thenReturn(false);

        assertThatThrownBy(() -> api.dpoDataset("other-tenants-agent"))
                .isInstanceOf(TurNotFoundException.class);
        assertThatThrownBy(() -> api.record("other-tenants-agent",
                new TurChatFeedbackAPI.TurChatFeedbackRequest("c1", "q", "a", null)))
                .isInstanceOf(TurNotFoundException.class);
        verifyNoInteractions(feedbackService);
    }
}
