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

package com.viglet.turing.persistence.utils;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;

/**
 * Custom UUID generator that allows pre-assigned identifiers.
 * <p>
 * This generator behaves like Hibernate's built-in {@code UuidGenerator}
 * but returns {@code true} from {@link #allowAssignedIdentifiers()},
 * which tells Hibernate to accept pre-set IDs during {@code persist()} or
 * {@code save()}, instead of treating entities with non-null IDs as "detached".
 * <p>
 * Used via the {@link TurAssignableUuidGenerator} annotation.
 *
 * @author Alexandre Oliveira
 * @since 0.3.9
 */
public class TurUuidGenerator implements BeforeExecutionGenerator {

    public TurUuidGenerator(TurAssignableUuidGenerator config, Member idMember,
            GeneratorCreationContext creationContext) {
        // Required by @IdGeneratorType
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner,
            Object currentValue, EventType eventType) {
        if (currentValue != null) {
            return currentValue;
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean generatedOnExecution() {
        return false;
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }
}
