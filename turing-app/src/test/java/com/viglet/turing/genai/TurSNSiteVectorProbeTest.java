/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viglet.turing.genai.TurSNSiteVectorProbe.Presence;
import com.viglet.turing.genai.provider.store.TurGenAiStoreProvider;
import com.viglet.turing.genai.provider.store.TurGenAiStoreProviderFactory;
import com.viglet.turing.genai.provider.store.TurStoreChunkPage;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.store.TurStoreInstance;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.system.security.TurSecretCryptoService;

/**
 * T1151 — the probe says whether a site's locale collections hold any vector,
 * without calling a model, and never claims absence it could not check.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNSiteVectorProbeTest {

    private final TurSNSiteLocaleRepository localeRepository = mock(TurSNSiteLocaleRepository.class);
    private final TurGenAiStoreProviderFactory providerFactory = mock(TurGenAiStoreProviderFactory.class);
    private final TurGenAiStoreProvider provider = mock(TurGenAiStoreProvider.class);
    private final TurSNSiteVectorProbe probe = new TurSNSiteVectorProbe(localeRepository, providerFactory,
            mock(TurSecretCryptoService.class));
    private final TurSNSite site = new TurSNSite();
    private final TurAIAgent agent = new TurAIAgent();
    private final TurStoreInstance store = new TurStoreInstance();

    @BeforeEach
    void setUp() {
        site.setId("site-1");
        site.setName("Sample");
        store.setId("store-1");
        agent.setTurStoreInstance(store);
        when(localeRepository.findByTurSNSite(site)).thenReturn(List.of(locale("sample_en"), locale("sample_pt")));
        when(providerFactory.getProvider(store)).thenReturn(provider);
    }

    private static TurSNSiteLocale locale(String core) {
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setCore(core);
        return locale;
    }

    private static TurStoreChunkPage page(long total) {
        return new TurStoreChunkPage(List.of(), total);
    }

    @Test
    void emptyCollectionsReadAbsent() {
        when(provider.listChunks(eq(store), any(), any(), eq(0), eq(1), anyMap())).thenReturn(page(0));

        assertThat(probe.probe(site, agent)).isEqualTo(Presence.ABSENT);
    }

    @Test
    void anyLocaleWithAChunkReadsPresent() {
        when(provider.listChunks(eq(store), any(), eq("sample_en"), anyInt(), anyInt(), anyMap()))
                .thenReturn(page(0));
        when(provider.listChunks(eq(store), any(), eq("sample_pt"), anyInt(), anyInt(), anyMap()))
                .thenReturn(page(12));

        assertThat(probe.probe(site, agent)).isEqualTo(Presence.PRESENT);
    }

    @Test
    void aStoreThatCannotListIsUnknownNotAbsent() {
        when(provider.listChunks(any(), any(), any(), anyInt(), anyInt(), anyMap()))
                .thenThrow(new UnsupportedOperationException("no listing"));

        assertThat(probe.probe(site, agent)).isEqualTo(Presence.UNKNOWN);
    }

    @Test
    void anAgentWithoutAStoreIsUnknown() {
        assertThat(probe.probe(site, new TurAIAgent())).isEqualTo(Presence.UNKNOWN);
        assertThat(probe.probe(site, null)).isEqualTo(Presence.UNKNOWN);
    }

    @Test
    void aFoundAnswerIsKeptForItsTtl() {
        when(provider.listChunks(any(), any(), any(), anyInt(), anyInt(), anyMap())).thenReturn(page(3));
        Instant start = Instant.parse("2026-10-27T10:00:00Z");
        probe.setClockForTest(Clock.fixed(start, ZoneOffset.UTC));
        probe.probe(site, agent);
        probe.probe(site, agent);
        // The first locale answers, and the second call is served from the cache.
        verify(provider, times(1)).listChunks(any(), any(), any(), anyInt(), anyInt(), anyMap());

        probe.setClockForTest(Clock.fixed(start.plus(TurSNSiteVectorProbe.TTL), ZoneOffset.UTC));
        probe.probe(site, agent);
        verify(provider, times(2)).listChunks(any(), any(), any(), anyInt(), anyInt(), any(Map.class));
    }

    @Test
    void anEmptyAnswerIsNotKeptSoAReindexShowsAtOnce() {
        when(provider.listChunks(any(), any(), any(), anyInt(), anyInt(), anyMap()))
                .thenReturn(page(0), page(0), page(5));

        assertThat(probe.probe(site, agent)).isEqualTo(Presence.ABSENT);
        assertThat(probe.probe(site, agent)).isEqualTo(Presence.PRESENT);
    }
}
