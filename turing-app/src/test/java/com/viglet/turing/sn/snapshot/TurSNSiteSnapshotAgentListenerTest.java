package com.viglet.turing.sn.snapshot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.event.TurAIAgentChangedEvent;

/** T1316 — a changed agent clears the SN snapshot as any SN write does. */
class TurSNSiteSnapshotAgentListenerTest {

    @Test
    void anAgentChangeEvictsTheSnapshot() {
        TurSNSiteSnapshotEvictionListener eviction = mock(TurSNSiteSnapshotEvictionListener.class);
        TurAIAgentChangedEvent event = new TurAIAgentChangedEvent("agent-1");

        new TurSNSiteSnapshotAgentListener(eviction).onAgentChanged(event);

        verify(eviction).onChange(event);
    }
}