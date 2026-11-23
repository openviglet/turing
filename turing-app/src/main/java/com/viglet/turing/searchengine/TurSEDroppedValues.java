/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.searchengine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * T1343 — the field values an engine dropped while indexing (T1338: a value its
 * field type cannot parse), collected for the caller that indexed them. The
 * caller opens a collection with {@link #begin()} on its thread, runs the
 * engine's index call, and {@link #end()} hands back what was dropped, per
 * document id, so the indexing log can say so instead of a bare INDEXED.
 *
 * <p>Per thread, since an engine indexes synchronously on the caller's thread.
 * Outside a collection {@link #record} does nothing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSEDroppedValues {

    /** One dropped value: the document, its field, the field's type and the value sent. */
    public record Dropped(String docId, String field, String type, String value) {

        /** {@code releaseDate '2017T00:00:00Z' (not a DATE)}. */
        public String describe() {
            return field + " '" + value + "' (not a " + type + ")";
        }
    }

    private static final ThreadLocal<List<Dropped>> CURRENT = new ThreadLocal<>();

    private TurSEDroppedValues() {
    }

    /** Opens a collection on this thread, discarding any left open. */
    public static void begin() {
        CURRENT.set(new ArrayList<>());
    }

    /** Records a dropped value; a no-op outside a collection. */
    public static void record(String docId, String field, String type, String value) {
        List<Dropped> dropped = CURRENT.get();
        if (dropped != null) {
            dropped.add(new Dropped(docId, field, type, value));
        }
    }

    /** Closes this thread's collection and returns what was dropped, by document id. */
    public static Map<String, List<Dropped>> end() {
        List<Dropped> dropped = CURRENT.get();
        CURRENT.remove();
        if (dropped == null) {
            return Map.of();
        }
        return dropped.stream().collect(Collectors.groupingBy(Dropped::docId, LinkedHashMap::new,
                Collectors.toList()));
    }

    /** The indexing-log detail for one document's drops, or {@code null} when there are none. */
    public static String detail(List<Dropped> dropped) {
        if (dropped == null || dropped.isEmpty()) {
            return null;
        }
        return "indexed; dropped " + dropped.stream().map(Dropped::describe)
                .collect(Collectors.joining(", "));
    }
}
