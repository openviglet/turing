/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * T1322l — the counter routes a resource key to the module count that owns it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurVigletResourceCounterTest {

    private static TurTenantResourceCount count(String key, long value) {
        return new TurTenantResourceCount() {
            @Override
            public String resourceKey() {
                return key;
            }

            @Override
            public long count(String tenantId) {
                return "t-1".equals(tenantId) ? value : -1;
            }
        };
    }

    @Test
    void eachKeyIsAnsweredByItsOwnModuleAndAnUnknownKeyIsZero() {
        TurVigletResourceCounter counter = new TurVigletResourceCounter(List.of(
                count(TurVigletResourceCounter.AGENTS, 2), count(TurVigletResourceCounter.SITES, 5)));

        assertThat(counter.count("t-1", TurVigletResourceCounter.AGENTS)).isEqualTo(2);
        assertThat(counter.count("t-1", TurVigletResourceCounter.SITES)).isEqualTo(5);
        assertThat(counter.count("t-1", TurVigletResourceCounter.MONTHLY_TOKENS)).isZero();
    }
}
