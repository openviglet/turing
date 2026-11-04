/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T1213 — the caller's filters apply only inside {@link TurDslCallerFilters#with},
 * blank entries are dropped, and the previous scope comes back afterwards.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurDslCallerFiltersTest {

    @Test
    @DisplayName("Filters apply inside the scope and not after it")
    void appliesOnlyInsideTheScope() {
        Map<String, List<String>> inside = TurDslCallerFilters.with(Map.of("site_key", List.of("a")),
                TurDslCallerFilters::current);

        assertThat(inside).isEqualTo(Map.of("site_key", List.of("a")));
        assertThat(TurDslCallerFilters.current()).isEmpty();
    }

    @Test
    @DisplayName("No filters, null or only blanks, is the whole site")
    void blankFiltersAreNone() {
        Map<String, List<String>> blanks = new HashMap<>();
        blanks.put("site_key", Arrays.asList("", " ", null));
        blanks.put(" ", List.of("a"));

        assertThat(TurDslCallerFilters.with(null, TurDslCallerFilters::current)).isEmpty();
        assertThat(TurDslCallerFilters.with(blanks, TurDslCallerFilters::current)).isEmpty();
    }

    @Test
    @DisplayName("A nested scope restores the outer one, even when it throws")
    void nestedScopeRestoresTheOuter() {
        TurDslCallerFilters.with(Map.of("site_key", List.of("a")), () -> {
            assertThatThrownBy(() -> TurDslCallerFilters.with(Map.of("site_key", List.of("b")), () -> {
                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(TurDslCallerFilters.current()).isEqualTo(Map.of("site_key", List.of("a")));
            return null;
        });
        assertThat(TurDslCallerFilters.current()).isEmpty();
    }

    @Test
    @DisplayName("One value is a term, several are terms")
    void clausesPerField() {
        assertThat(TurDslCallerFilters.clauses(Map.of("site_key", List.of("a"))))
                .containsExactly(new TurDslQuery.Term("site_key", "a"));
        assertThat(TurDslCallerFilters.clauses(Map.of("site_key", List.of("a", "b"))))
                .containsExactly(new TurDslQuery.Terms("site_key", List.of("a", "b")));
    }
}
