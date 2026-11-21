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

/**
 * T1310 — reads the integer switches a site stores ({@code 1} on, anything else
 * off), so the engines test a site's facet, highlighting and more-like-this
 * settings without importing {@code sn}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSEFlags {

    private TurSEFlags() {
    }

    /** Whether {@code value} is the stored "on", {@code 1}; {@code null} is off. */
    public static boolean isTrue(Integer value) {
        return Integer.valueOf(1).equals(value);
    }
}
