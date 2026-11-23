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

import java.util.Locale;
import java.util.Map;

import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1322h — the site's vector store as sn's import paths write it: embed what a
 * job indexes, drop what it deletes. Declared by sn and implemented by the
 * answer module's {@code TurSNGenAi} (ADR 0005 section 6: answer sits above sn).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNSiteEmbeddings {

    /** Embeds the job's documents into their sites' stores; returns how many embeddings failed. */
    int addDocuments(TurSNJobItems turSNJobItems);

    /** Removes one document from the site's store for {@code locale}. */
    boolean removeDocument(TurSNSite turSNSite, Locale locale, String id);

    /** Removes the documents whose metadata matches every entry of {@code match}. */
    boolean removeDocuments(TurSNSite turSNSite, Locale locale, Map<String, String> match);
}
