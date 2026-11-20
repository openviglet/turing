/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.shared.exception;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * T1319 — the one optimistic-lock check the console saves share.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurStaleVersionTest {

    @Test
    void aSaveFromAnOlderVersionIsRefusedNamingTheRecord() {
        assertThatThrownBy(() -> TurStaleVersion.refuse(3L, 4L, "search engine"))
                .isInstanceOf(TurConflictException.class)
                .hasMessageContaining("This search engine was changed by someone else");
    }

    @Test
    void theCurrentVersionPasses() {
        assertThatCode(() -> TurStaleVersion.refuse(4L, 4L, "store")).doesNotThrowAnyException();
    }

    @Test
    void aSaveThatNamesNoVersionPassesAsBeforeTheAggregateHadOne() {
        assertThatCode(() -> TurStaleVersion.refuse(null, 4L, "store")).doesNotThrowAnyException();
        assertThatCode(() -> TurStaleVersion.refuse(4L, null, "store")).doesNotThrowAnyException();
    }
}
