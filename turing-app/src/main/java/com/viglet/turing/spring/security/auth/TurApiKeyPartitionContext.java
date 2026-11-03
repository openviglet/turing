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

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.persistence.model.dev.token.TurDevToken;

import jakarta.servlet.http.HttpServletRequest;

/**
 * T1133 — the partition of a shared SN site that the current request's API key
 * is bound to. {@link TurAuthTokenHeaderFilter} records it on the request when
 * it authenticates the key; search and import read it back from here, and
 * {@link TurApiKeyPartitionGuardFilter} refuses the key everywhere else.
 *
 * <p>Outside a request (a JMS consumer, a scheduled job) there is no partition.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public final class TurApiKeyPartitionContext {

    static final String ATTRIBUTE = TurApiKeyPartitionContext.class.getName();

    private TurApiKeyPartitionContext() {
    }

    /** Records the key's partition on the request, when it has one. */
    static void bind(HttpServletRequest request, TurDevToken token) {
        if (token.isPartitioned()) {
            request.setAttribute(ATTRIBUTE,
                    new TurSEPartition(token.getPartitionField().trim(), token.getPartitionValue().trim()));
        }
    }

    /** The partition bound to {@code request}'s key, or (T1210) to the session it replays, if any. */
    public static Optional<TurSEPartition> of(HttpServletRequest request) {
        return Optional.ofNullable(request.getAttribute(ATTRIBUTE))
                .filter(TurSEPartition.class::isInstance)
                .map(TurSEPartition.class::cast)
                .or(TurApiKeyPartitionContext::authenticated);
    }

    /** The partition bound to the current request's key, or (T1210) to its session, if any. */
    public static Optional<TurSEPartition> current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .filter(TurSEPartition.class::isInstance)
                .map(TurSEPartition.class::cast)
                .or(TurApiKeyPartitionContext::authenticated);
    }

    /**
     * T1210 — the partition the current authentication was made with. A key's
     * authentication outlives its request in the session, and a request that
     * sends only the session's cookie must stay inside the same partition.
     */
    private static Optional<TurSEPartition> authenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication)
                .map(Authentication::getDetails)
                .filter(TurApiKeyAuthenticationDetails.class::isInstance)
                .map(details -> ((TurApiKeyAuthenticationDetails) details).getPartition());
    }
}
