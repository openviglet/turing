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

package com.viglet.turing.agent.persistence.model;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.viglet.turing.agent.event.TurAIAgentChangedEvent;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Publishes a {@link TurAIAgentChangedEvent} on every persist, update or remove
 * of an agent, whichever service or controller wrote it, so the modules that
 * cache agent-derived state listen instead of the entity naming their listeners
 * (T1316). Their own eviction still lands after commit.
 *
 * <p>Hibernate resolves the listener through the application context, so
 * constructor injection works; the static fallback covers a no-arg instance.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurAIAgentChangeListener {

    private static final AtomicReference<ApplicationEventPublisher> publisherStatic = new AtomicReference<>();

    private final ApplicationEventPublisher publisher;

    public TurAIAgentChangeListener() {
        this.publisher = null;
    }

    @Autowired
    public TurAIAgentChangeListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        publisherStatic.set(publisher);
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void onChange(Object entity) {
        ApplicationEventPublisher target = publisher != null ? publisher : publisherStatic.get();
        if (target == null) {
            return;
        }
        String id = entity instanceof TurAIAgent agent ? agent.getId() : null;
        target.publishEvent(new TurAIAgentChangedEvent(id));
    }
}