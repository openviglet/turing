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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * T1213 — the filters a caller asked a whole operation to stay inside (field to
 * accepted values, the shape the chat's {@code filters} takes since T1086). The
 * catalog copilot, its kit and nl-filters make many reads per call: rather than
 * thread the map through each one, the endpoint runs the operation inside
 * {@link #with(Map, Supplier)}, and {@link TurDslSearchService} and the hybrid
 * reorder add these filters to every read made on that thread.
 *
 * <p>Unlike a partitioned key (T1133) this is a scope the caller chooses, not a
 * security boundary: an anonymous browser on a shared SN site sends
 * {@code {site_key: [a]}} so it cites and stages only its own site's documents.
 * Values of one field are alternatives; fields are all required. No filter, or
 * only blank entries, is today's behaviour.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurDslCallerFilters {

    private static final ThreadLocal<Map<String, List<String>>> CURRENT = new ThreadLocal<>();

    private TurDslCallerFilters() {
    }

    /**
     * Runs {@code operation} with {@code filters} applied to every DSL read it
     * makes on this thread, restoring whatever applied before.
     */
    public static <T> T with(Map<String, List<String>> filters, Supplier<T> operation) {
        Map<String, List<String>> previous = CURRENT.get();
        Map<String, List<String>> normalized = normalize(filters);
        if (normalized.isEmpty()) {
            CURRENT.remove();
        } else {
            CURRENT.set(normalized);
        }
        try {
            return operation.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** The filters applying on this thread; empty when none. */
    public static Map<String, List<String>> current() {
        Map<String, List<String>> filters = CURRENT.get();
        return filters == null ? Map.of() : filters;
    }

    /** {@code filters} without blank fields or values, and without fields left empty. */
    static Map<String, List<String>> normalize(Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        filters.forEach((field, values) -> {
            if (field == null || field.isBlank() || values == null) {
                return;
            }
            List<String> kept = values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::strip)
                    .distinct()
                    .toList();
            if (!kept.isEmpty()) {
                normalized.put(field.strip(), kept);
            }
        });
        return Map.copyOf(normalized);
    }

    /** One filter clause per field: a term for one value, terms for several. */
    static List<TurDslQuery> clauses(Map<String, List<String>> filters) {
        List<TurDslQuery> clauses = new ArrayList<>();
        filters.forEach((field, values) -> clauses.add(values.size() == 1
                ? new TurDslQuery.Term(field, values.getFirst())
                : new TurDslQuery.Terms(field, List.copyOf(values))));
        return clauses;
    }
}
