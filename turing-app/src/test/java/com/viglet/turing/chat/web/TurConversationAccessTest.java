package com.viglet.turing.chat.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.model.TurChatCitationRecord;
import com.viglet.turing.agent.persistence.model.TurChatFlowState;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.agent.persistence.repository.TurChatCitationRecordRepository;
import com.viglet.turing.agent.persistence.repository.TurChatFlowStateRepository;
import com.viglet.turing.agent.persistence.repository.TurChatFlowSubmissionRepository;
import com.viglet.turing.shared.exception.TurNotFoundException;
import com.viglet.turing.tenant.TurTenantContext;

/**
 * T1334 — a conversation id opens a conversation only to a caller whose tenant
 * a tenant-scoped record ties it to.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurConversationAccessTest {

    private final TurTenantContext tenantContext = mock(TurTenantContext.class);
    private final TurChatFlowStateRepository states = mock(TurChatFlowStateRepository.class);
    private final TurChatFlowSubmissionRepository submissions = mock(TurChatFlowSubmissionRepository.class);
    private final TurChatCitationRecordRepository citations = mock(TurChatCitationRecordRepository.class);
    private final TurAIAgentRepository agents = mock(TurAIAgentRepository.class);
    private final com.viglet.turing.chat.memory.TurChatMemoryService memory =
            mock(com.viglet.turing.chat.memory.TurChatMemoryService.class);
    private final TurConversationAccess access =
            new TurConversationAccess(tenantContext, states, submissions, citations, agents, memory);

    @Test
    void withTenancyOffEveryConversationIsVisible() {
        when(tenantContext.isTenancyEnabled()).thenReturn(false);

        assertThat(access.isVisible("c1")).isTrue();
        verifyNoInteractions(states, submissions, citations, agents, memory);
    }

    @Test
    void aConversationWithAFlowStateOfTheCallersTenantIsVisible() {
        when(tenantContext.isTenancyEnabled()).thenReturn(true);
        when(states.findByConversationId("c1")).thenReturn(List.of(new TurChatFlowState()));

        assertThat(access.isVisible("c1")).isTrue();
    }

    @Test
    void aConversationProvenOnlyByACitationOfAVisibleAgentIsVisible() {
        when(tenantContext.isTenancyEnabled()).thenReturn(true);
        TurChatCitationRecord record = new TurChatCitationRecord();
        record.setAgentId("agent-mine");
        when(citations.findByConversationIdOrderByAnswerProducedAtAsc("c1")).thenReturn(List.of(record));
        when(agents.existsById("agent-mine")).thenReturn(true);

        assertThat(access.isVisible("c1")).isTrue();
    }

    @Test
    void anotherTenantsConversationAnswersAsNotFound() {
        when(tenantContext.isTenancyEnabled()).thenReturn(true);
        TurChatCitationRecord record = new TurChatCitationRecord();
        record.setAgentId("agent-theirs");
        when(citations.findByConversationIdOrderByAnswerProducedAtAsc("c2")).thenReturn(List.of(record));
        when(agents.existsById("agent-theirs")).thenReturn(false);

        assertThatThrownBy(() -> access.requireVisible("c2")).isInstanceOf(TurNotFoundException.class);
    }

    /** T1336 — a plain RAG chat is opened through the agent its memory names. */
    @Test
    void aConversationProvenOnlyByItsMemoryIsVisibleToItsAgentsTenant() {
        when(tenantContext.isTenancyEnabled()).thenReturn(true);
        when(memory.findAgentId("c3")).thenReturn(java.util.Optional.of("agent-mine"));
        when(memory.findAgentId("c4")).thenReturn(java.util.Optional.of("agent-theirs"));
        when(agents.existsById("agent-mine")).thenReturn(true);
        when(agents.existsById("agent-theirs")).thenReturn(false);

        assertThat(access.isVisible("c3")).isTrue();
        assertThat(access.isVisible("c4")).isFalse();
    }
}
