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

import org.springframework.ai.embedding.EmbeddingModel;

/**
 * T1322h — the Global Settings' default embedding model, for sn features that
 * compare texts (synonym mining). Declared by sn and implemented by rag's
 * {@code TurRagContextBuilder} (ADR 0005 section 6: rag sits above sn).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNDefaultEmbeddingModel {

    /** The default embedding model, or empty when Global Settings names none. */
    Optional<EmbeddingModel> defaultEmbeddingModel();
}
