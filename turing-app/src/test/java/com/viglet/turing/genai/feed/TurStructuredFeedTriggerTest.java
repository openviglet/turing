package com.viglet.turing.genai.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import com.viglet.turing.api.exception.TurConflictException;
import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.genai.feed.TurStructuredFeedIngestService.IngestResult;
import com.viglet.turing.properties.TurStructuredFeedProperty;
import com.viglet.turing.properties.TurStructuredFeedProperty.Source;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

/**
 * T1229 — an on-demand ingest runs the named source under the sweep's lock,
 * answers 404 for a source nobody configured and 409 while the lock is held.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurStructuredFeedTriggerTest {

    private final TurStructuredFeedIngestService ingestService = mock(TurStructuredFeedIngestService.class);
    private final LockProvider lockProvider = mock(LockProvider.class);
    private final TurStructuredFeedProperty property = new TurStructuredFeedProperty();
    private TurStructuredFeedTrigger trigger;
    private Source source;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        source = new Source();
        source.setId("model-catalog");
        source.setSiteName("model-catalog");
        source.setFeedUrl("https://example.com/catalog.ndjson");
        property.setSources(List.of(source));
        ObjectProvider<LockProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(lockProvider);
        trigger = new TurStructuredFeedTrigger(property, ingestService, provider);
    }

    @Test
    void runsTheSourceUnderTheSweepsLockAndReleasesIt() {
        SimpleLock lock = mock(SimpleLock.class);
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
        IngestResult result = new IngestResult(true, 1238, 0, 0, 0, null, false);
        when(ingestService.ingestSource(source)).thenReturn(result);

        assertThat(trigger.ingest("model-catalog")).isSameAs(result);

        ArgumentCaptor<LockConfiguration> config = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockProvider).lock(config.capture());
        assertThat(config.getValue().getName()).isEqualTo(TurStructuredFeedIngestService.LOCK_NAME);
        verify(lock).unlock();
    }

    @Test
    void aHeldLockIsAConflictAndNothingRuns() {
        when(lockProvider.lock(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trigger.ingest("model-catalog")).isInstanceOf(TurConflictException.class);
        verify(ingestService, never()).ingestSource(any());
    }

    @Test
    void anUnknownSourceIsNotFound() {
        assertThatThrownBy(() -> trigger.ingest("nope")).isInstanceOf(TurNotFoundException.class);
        verify(lockProvider, never()).lock(any());
    }
}
