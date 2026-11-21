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
package com.viglet.turing.sn.kb;

import java.util.Locale;
import java.util.Map;

import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1322i — the index-time step that adds a site's thesaurus terms to a document
 * before it is written, declared by sn and implemented by kb (ADR 0005 section
 * 6: kb sits above sn).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNThesaurusEnrichment {

    /**
     * Adds to {@code attributes} the terms the site's thesauri recognize in the
     * document, for {@code locale}; leaves them as they are when the site has none.
     */
    void enrich(TurSNSite turSNSite, Locale locale, Map<String, Object> attributes);
}
