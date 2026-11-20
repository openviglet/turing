package com.viglet.turing.agent.persistence.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.viglet.turing.agent.event.TurAIAgentChangedEvent;

/** T1316 — every agent write is announced as an event, carrying the agent's id. */
class TurAIAgentChangeListenerTest {

    @Test
    void aWrittenAgentIsPublishedWithItsId() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        TurAIAgent agent = new TurAIAgent();
        agent.setId("agent-1");

        new TurAIAgentChangeListener(publisher).onChange(agent);

        verify(publisher).publishEvent(new TurAIAgentChangedEvent("agent-1"));
    }

    @Test
    void aNoArgInstanceFallsBackToThePublisherSpringBuilt() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        new TurAIAgentChangeListener(publisher);

        new TurAIAgentChangeListener().onChange(new TurAIAgent());

        verify(publisher).publishEvent(new TurAIAgentChangedEvent(null));
    }
}