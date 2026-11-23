/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * T1327 — per-module tracing is imported only when its flag is on, and the class it
 * imports by name still exists in the Spring Modulith on the classpath.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurModuleObservabilityConfigTest {

    private static String[] importsWith(MockEnvironment environment) {
        TurModuleObservabilityConfig.WhenEnabled selector = new TurModuleObservabilityConfig.WhenEnabled();
        selector.setEnvironment(environment);
        return selector.selectImports(null);
    }

    @Test
    void offByDefault() {
        assertThat(importsWith(new MockEnvironment())).isEmpty();
    }

    @Test
    void theFlagImportsSpringModulithsModuleObservability() throws Exception {
        String[] imports = importsWith(new MockEnvironment()
                .withProperty(TurModuleObservabilityConfig.ENABLED, "true"));

        assertThat(imports).containsExactly(TurModuleObservabilityConfig.MODULE_OBSERVABILITY,
                TurModuleObservabilityConfig.FiltersStayUnwrapped.class.getName());
        assertThat(Class.forName(imports[0])).isNotNull();
    }
}
