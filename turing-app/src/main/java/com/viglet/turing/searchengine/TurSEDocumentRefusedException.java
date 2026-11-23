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
 * T1343 — the engine refused one document (an HTTP 4xx from Solr or
 * Elasticsearch: a value its field type cannot parse, a field it does not know).
 * Retrying cannot help, unlike a transient I/O failure, so an indexing caller
 * records it against that document and moves on.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public class TurSEDocumentRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String docId;

    public TurSEDocumentRefusedException(String docId, String reason, Throwable cause) {
        super(reason, cause);
        this.docId = docId;
    }

    /** The refused document's id, or {@code null} when it had none. */
    public String getDocId() {
        return docId;
    }
}
