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
package com.viglet.turing.system.web.exception;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * T1339 — the one field of a well-formed JSON body that Jackson could not bind:
 * its JSON path ({@code [0].type}), the type it needs, and for an enum the values
 * it accepts and the value that was sent. Read off the binding exception, so the
 * 400 names what a caller has to change instead of "missing or malformed".
 *
 * @param field    the JSON path of the field, e.g. {@code [0].type}
 * @param expected the simple name of the type the field binds to
 * @param accepted the enum constants the field accepts; empty when not an enum
 * @param value    the rejected value, capped at {@value #MAX_VALUE} characters; may be null
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
record TurUnbindableBodyField(String field, String expected, List<String> accepted, String value) {

    static final int MAX_VALUE = 100;
    private static final int MAX_CAUSE_DEPTH = 16;

    /** The field a body failed on, when the cause chain names one. */
    static Optional<TurUnbindableBodyField> of(Throwable failure) {
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++, cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
                return Optional.of(from(mismatch));
            }
        }
        return Optional.empty();
    }

    private static TurUnbindableBodyField from(MismatchedInputException mismatch) {
        Class<?> target = mismatch.getTargetType();
        List<String> accepted = target != null && target.isEnum()
                ? Arrays.stream(target.getEnumConstants()).map(c -> ((Enum<?>) c).name()).toList()
                : List.of();
        String value = mismatch instanceof InvalidFormatException invalid && invalid.getValue() != null
                ? cap(String.valueOf(invalid.getValue()))
                : null;
        return new TurUnbindableBodyField(jsonPath(mismatch.getPath()),
                target != null ? target.getSimpleName() : "another type", accepted, value);
    }

    /** The {@code detail} sentence of the problem. */
    String detail() {
        if (!accepted.isEmpty()) {
            return "Field '" + field + "' must be one of " + String.join(", ", accepted) + ".";
        }
        return "Field '" + field + "' must be of type " + expected + ".";
    }

    static String jsonPath(List<JacksonException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : path) {
            if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            } else if (ref.getPropertyName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getPropertyName());
            }
        }
        return sb.toString();
    }

    private static String cap(String value) {
        return value.length() > MAX_VALUE ? value.substring(0, MAX_VALUE) : value;
    }
}
