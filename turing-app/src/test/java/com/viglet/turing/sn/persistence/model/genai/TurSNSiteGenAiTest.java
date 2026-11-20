package com.viglet.turing.sn.persistence.model.genai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.agent.persistence.model.TurAIAgent;

@ExtendWith(MockitoExtension.class)
class TurSNSiteGenAiTest {

    @Mock
    private TurAIAgent turAIAgent;

    private TurSNSiteGenAi turSNSiteGenAi;

    @BeforeEach
    void setUp() {
        turSNSiteGenAi = new TurSNSiteGenAi();
    }

    @Test
    void shouldSetAndGetAllProperties() {
        turSNSiteGenAi.setId("genai-id");
        turSNSiteGenAi.setTurAIAgentId(turAIAgent.getId());
        turSNSiteGenAi.setSitePrompt("Site description.");

        assertThat(turSNSiteGenAi.getId()).isEqualTo("genai-id");
        assertThat(turSNSiteGenAi.getTurAIAgentId()).isEqualTo(turAIAgent.getId());
        assertThat(turSNSiteGenAi.getSitePrompt()).isEqualTo("Site description.");
    }

    @Test
    void shouldExposeDefaultValuesOnNoArgsConstructor() {
        assertThat(turSNSiteGenAi.getId()).isNull();
        assertThat(turSNSiteGenAi.getTurAIAgentId()).isNull();
        assertThat(turSNSiteGenAi.getSitePrompt()).isNull();
    }
}
