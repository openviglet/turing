/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.genai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.genai.provider.store.TurGenAiStoreProvider;
import com.viglet.turing.genai.provider.store.TurGenAiStoreProviderFactory;
import com.viglet.turing.genai.provider.store.TurStoreChunkPage;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.store.TurStoreInstance;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.system.security.TurSecretCryptoService;

import lombok.extern.slf4j.Slf4j;

/**
 * T1151 — whether a site's content is in its agent's vector store at all.
 * Readiness ({@link TurDefaultAgentResolver#isRagReady}) only reads
 * configuration, so a site indexed before its GenAI was on read as ready while
 * holding no vectors, and chat answered that the content was missing.
 *
 * <p>Import writes a site's chunks into one collection per locale, named after
 * the locale's core, so the probe lists one chunk of each. No model is called:
 * {@link TurGenAiStoreProvider#listChunks} reads the store directly. A store
 * that cannot list answers {@link Presence#UNKNOWN}, which callers treat as
 * present, so the probe never claims a site is empty without having looked.
 * A found or unknown answer is kept for {@link #TTL}, since a status screen asks
 * on every load; an empty one is looked up again each time.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Slf4j
@Component
public class TurSNSiteVectorProbe {

    /** What the store holds for a site. */
    public enum Presence {
        PRESENT, ABSENT, UNKNOWN
    }

    static final Duration TTL = Duration.ofSeconds(60);

    private record Cached(Presence presence, Instant at) {
    }

    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurGenAiStoreProviderFactory storeProviderFactory;
    private final TurSecretCryptoService secretCryptoService;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    public TurSNSiteVectorProbe(TurSNSiteLocaleRepository turSNSiteLocaleRepository,
            TurGenAiStoreProviderFactory storeProviderFactory, TurSecretCryptoService secretCryptoService) {
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
        this.storeProviderFactory = storeProviderFactory;
        this.secretCryptoService = secretCryptoService;
    }

    void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    /**
     * @param agent the site's effective agent; {@code null} or one without a
     *              store answers {@link Presence#UNKNOWN}
     */
    public Presence probe(TurSNSite site, TurAIAgent agent) {
        TurStoreInstance store = agent == null ? null : agent.getTurStoreInstance();
        if (site == null || store == null) {
            return Presence.UNKNOWN;
        }
        String key = site.getId() + "|" + store.getId();
        Instant now = clock.instant();
        Cached cached = cache.get(key);
        if (cached != null && cached.at().plus(TTL).isAfter(now)) {
            return cached.presence();
        }
        Presence presence = look(site, store);
        // Absence is not kept: it is the state a reindex is about to end, and a
        // cached "empty" would outlive the reindex that filled the store.
        if (presence == Presence.ABSENT) {
            cache.remove(key);
        } else {
            cache.put(key, new Cached(presence, now));
        }
        return presence;
    }

    private Presence look(TurSNSite site, TurStoreInstance store) {
        List<String> collections = turSNSiteLocaleRepository.findByTurSNSite(site).stream()
                .map(TurSNSiteLocale::getCore).filter(StringUtils::hasText).distinct().toList();
        if (collections.isEmpty()) {
            return Presence.UNKNOWN;
        }
        try {
            TurGenAiStoreProvider provider = storeProviderFactory.getProvider(store);
            String credential = secretCryptoService.decrypt(store.getCredentialEncrypted());
            for (String collection : collections) {
                TurStoreChunkPage page = provider.listChunks(store, credential, collection, 0, 1, Map.of());
                if (page != null && (page.total() > 0 || !Objects.requireNonNullElse(page.chunks(),
                        List.<TurStoreChunkPage.Chunk>of()).isEmpty())) {
                    return Presence.PRESENT;
                }
            }
            return Presence.ABSENT;
        } catch (RuntimeException e) {
            // Unsupported by the provider, or the store is unreachable: not evidence of absence.
            log.debug("Vector probe for site '{}' could not read its store: {}", site.getName(), e.getMessage());
            return Presence.UNKNOWN;
        }
    }
}
