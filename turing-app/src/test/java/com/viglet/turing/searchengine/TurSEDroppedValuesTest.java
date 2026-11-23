/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.searchengine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * T1343 — what an indexing caller learns about values the engine dropped.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSEDroppedValuesTest {

    @Test
    void aCollectionGroupsTheDropsByDocumentAndEndsWithTheCall() {
        TurSEDroppedValues.begin();
        TurSEDroppedValues.record("g1", "releaseDate", "DATE", "2017T00:00:00Z");
        TurSEDroppedValues.record("g1", "price", "DOUBLE", "free");
        TurSEDroppedValues.record("g2", "releaseDate", "DATE", "soon");

        var dropped = TurSEDroppedValues.end();

        assertThat(dropped).containsOnlyKeys("g1", "g2");
        assertThat(TurSEDroppedValues.detail(dropped.get("g1"))).isEqualTo(
                "indexed; dropped releaseDate '2017T00:00:00Z' (not a DATE), price 'free' (not a DOUBLE)");
        assertThat(TurSEDroppedValues.end()).isEmpty();
    }

    @Test
    void outsideACollectionARecordIsANoOpAndNoDropsMeanNoDetail() {
        TurSEDroppedValues.record("g1", "releaseDate", "DATE", "x");

        assertThat(TurSEDroppedValues.end()).isEmpty();
        assertThat(TurSEDroppedValues.detail(null)).isNull();
        assertThat(TurSEDroppedValues.detail(List.of())).isNull();
    }
}
