/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.testutil;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * T1325 / ADR 0005 — base class for a module slice integration test, which also carries
 * {@link TuringModuleTest}. Like {@link AbstractTuringSpringIT}, it gives the slice its
 * own H2 file and an in-memory Artemis, so it never touches the dev store or another
 * JVM's queue lock.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public abstract class AbstractTuringModuleIT {

    @DynamicPropertySource
    static void isolateDatabaseAndQueue(DynamicPropertyRegistry registry) {
        AbstractTuringSpringIT.overrideDatasourceWithEphemeralH2(registry);
        AbstractTuringSpringIT.overrideArtemisForTestIsolation(registry);
    }
}
