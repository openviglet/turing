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
package com.viglet.turing.searchengine.spi;

/**
 * T1310 — one condition of a ranking expression, as the engines read it: the
 * field, how it is tested and the value it is tested against. A DATE field with
 * the value {@code asc} or {@code upcoming} is a scored recency form rather than a
 * date (T1219).
 *
 * @param attribute the field name
 * @param kind      how the field is tested
 * @param value     the value; unused by {@link Kind#EXISTS}
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSERankingCondition(String attribute, Kind kind, String value) {

    /** How a condition tests its field. */
    public enum Kind {
        /** The field holds the value. */
        IS,
        /** The field does not hold the value (T1070). */
        IS_NOT,
        /** The field holds any value. */
        EXISTS
    }
}
