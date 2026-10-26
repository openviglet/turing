/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.commons.sn.search;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * T1120 — what kind of search a request is. One flag the server and the SDK both
 * read for a search box that updates while the visitor types.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public enum TurSNSearchMode {

    /** A submitted search: the query as typed, and logged in the search metrics. */
    STANDARD,

    /**
     * An as-you-type search: the last term matches as a prefix, and the query is
     * kept out of the search log, since it is a word the visitor has not finished.
     */
    INSTANT;

    /** Parses the flag case-insensitively; blank is {@link #STANDARD}, unknown throws (a 400). */
    @JsonCreator
    public static TurSNSearchMode of(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }
}
