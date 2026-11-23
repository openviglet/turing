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
package com.viglet.turing.sn.api;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * T1322h — how well a text fits an audience persona, for SN's content-fit
 * indexing and coverage report. Declared by sn and implemented by the persona
 * module (ADR 0005 section 6: persona sits above sn).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNAudienceFit {

    /** Whether {@code personaId} names a persona usable as an audience. */
    boolean isAudience(String personaId);

    /** The 0–100 fit of {@code text} for that audience, or empty when it is not one. */
    OptionalDouble fitScore(String personaId, String text);

    /** The persona's name, or empty when there is none with that id. */
    Optional<String> personaName(String personaId);
}
