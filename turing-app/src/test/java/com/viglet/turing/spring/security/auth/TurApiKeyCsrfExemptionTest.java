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

package com.viglet.turing.spring.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.viglet.turing.persistence.model.dev.token.TurDevToken;
import com.viglet.turing.persistence.repository.dev.token.TurDevTokenRepository;

/**
 * T1124 — CSRF is lifted for a request a usable API key authenticates, and for
 * nothing else.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurApiKeyCsrfExemptionTest {

    private final TurDevTokenRepository repository = mock(TurDevTokenRepository.class);
    private final TurApiKeyCsrfExemption exemption = new TurApiKeyCsrfExemption(repository);

    private static MockHttpServletRequest post(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/summary");
        if (key != null) {
            request.addHeader(TurAuthTokenHeaderFilter.KEY, key);
        }
        return request;
    }

    private static TurDevToken token(boolean usable) {
        TurDevToken token = mock(TurDevToken.class);
        when(token.isUsable()).thenReturn(usable);
        return token;
    }

    @Test
    void aUsableKeyLiftsCsrf() {
        TurDevToken usable = token(true);
        when(repository.findByToken("good")).thenReturn(Optional.of(usable));

        assertThat(exemption.matches(post("good"))).isTrue();
    }

    @Test
    void anUnknownDisabledOrMissingKeyKeepsCsrf() {
        TurDevToken disabled = token(false);
        when(repository.findByToken("ghost")).thenReturn(Optional.empty());
        when(repository.findByToken("off")).thenReturn(Optional.of(disabled));

        assertThat(exemption.matches(post("ghost"))).isFalse();
        assertThat(exemption.matches(post("off"))).isFalse();
        assertThat(exemption.matches(post(" "))).isFalse();
        assertThat(exemption.matches(post(null))).isFalse();
    }

    /** The production chain is {@code @Profile("production")}, so the guard is over its source. */
    @Test
    void theProductionChainExemptsKeyAuthenticatedRequests() throws IOException {
        String config = Files.readString(Path.of(
                "src/main/java/com/viglet/turing/spring/security/TurSecurityConfigProduction.java"),
                StandardCharsets.UTF_8);

        assertThat(config).contains(".ignoringRequestMatchers(").contains("turApiKeyCsrfExemption,");
    }
}
