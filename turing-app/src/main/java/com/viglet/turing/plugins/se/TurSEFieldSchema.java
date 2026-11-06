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
package com.viglet.turing.plugins.se;

import com.viglet.turing.commons.se.field.TurSEFieldType;

/**
 * T1243 — a field as an engine's own schema holds it, for the console's field
 * check and repair.
 *
 * @param engineType  the engine's own type name (Solr's {@code string}, {@code text_general}, ...)
 * @param type        the Turing type that engine type stands for, or {@code null} when none does
 * @param multiValued whether the engine holds several values for the field
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSEFieldSchema(String engineType, TurSEFieldType type, boolean multiValued) {

    /** Whether the engine's type is the one it uses for {@code expected}. */
    public boolean hasType(TurSEFieldType expected) {
        return expected != null && expected == type;
    }
}
