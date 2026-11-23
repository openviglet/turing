/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.system.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.viglet.core.webhook.VigletWebhookDispatcher;

/**
 * T378 / Block Q — registers the shared {@link VigletWebhookDispatcher}
 * (viglet-core) that outbound webhooks are delivered through: agent's chat webhooks
 * and human-approval notices, persona match drift alerts, observability's LangSmith
 * export. A single pooled JDK {@code HttpClient} backs every dispatch;
 * {@code destroyMethod = "close"} tears down the (lazily created) async pool on
 * context shutdown.
 *
 * <p>T1325 — moved from agent to system, the lowest module that uses it: observability
 * (layer 2) injects it, and a module slice of anything below agent could not start
 * while only agent defined it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Configuration
public class TurWebhookDispatcherConfig {

    /** TCP connect timeout for a webhook delivery attempt. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean(destroyMethod = "close")
    VigletWebhookDispatcher vigletWebhookDispatcher() {
        return new VigletWebhookDispatcher(CONNECT_TIMEOUT);
    }
}
