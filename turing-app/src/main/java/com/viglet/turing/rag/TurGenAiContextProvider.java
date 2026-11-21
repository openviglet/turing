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
package com.viglet.turing.rag;

import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;

/**
 * T1322g — the RAG context of an SN site's GenAI binding, declared by rag and
 * implemented in agent (the context comes from the site's effective agent, falling
 * back to the Default AI Agent), so modules below agent build one without
 * importing it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurGenAiContextProvider {

    /** The context for {@code turSNSiteGenAi}; disabled when RAG is not available. */
    default TurGenAiContext build(TurSNSiteGenAi turSNSiteGenAi) {
        return build(turSNSiteGenAi, null);
    }

    /** As {@link #build(TurSNSiteGenAi)}, over another vector collection when given. */
    default TurGenAiContext build(TurSNSiteGenAi turSNSiteGenAi, String collectionNameOverride) {
        return build(turSNSiteGenAi, collectionNameOverride, null);
    }

    /**
     * As {@link #build(TurSNSiteGenAi, String)}, attributing the context's calls to
     * {@code siteName} in the cost ledger (T1127); {@code null} for none.
     */
    TurGenAiContext build(TurSNSiteGenAi turSNSiteGenAi, String collectionNameOverride, String siteName);
}
