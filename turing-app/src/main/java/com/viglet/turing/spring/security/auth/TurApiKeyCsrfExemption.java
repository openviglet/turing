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

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.dev.token.TurDevToken;
import com.viglet.turing.persistence.repository.dev.token.TurDevTokenRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The requests CSRF protection does not apply to because they authenticate with a
 * usable API key in the {@code Key} header (T1124).
 *
 * <p>CSRF protects what a browser sends on its own: the session cookie. A {@code Key}
 * header is never sent on its own, and a cross-site page cannot add one without a
 * CORS preflight. So a server-to-server connector call, which has no cookie jar to
 * round-trip a CSRF token, is authenticated by its key instead, on every endpoint
 * rather than on a path list each new connector call has to be added to.
 *
 * <p>The key must resolve to a usable token, as {@link TurAuthTokenHeaderFilter}
 * checks it. The CSRF filter runs before that filter, and a header naming no valid
 * key must not lift CSRF from a request that a session cookie then authenticates.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Component
public class TurApiKeyCsrfExemption implements RequestMatcher {

    private final TurDevTokenRepository turDevTokenRepository;

    public TurApiKeyCsrfExemption(TurDevTokenRepository turDevTokenRepository) {
        this.turDevTokenRepository = turDevTokenRepository;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String key = request.getHeader(TurAuthTokenHeaderFilter.KEY);
        return StringUtils.hasText(key)
                && turDevTokenRepository.findByToken(key).filter(TurDevToken::isUsable).isPresent();
    }
}
