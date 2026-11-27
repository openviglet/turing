/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.rag.answer;

/**
 * T1370 — what each retrieval pass of one answer returned, carried on the answer
 * stream's {@code trace} event. An answer with no sources says which pass came back
 * empty: the vector search, the site's lexical search, the lexical documents' chunks,
 * or the STRICT_RAG judge that refused what was retrieved.
 *
 * @param vectorDocuments  distinct documents the vector pass returned
 * @param lexicalHits      document ids the site's lexical search returned, or
 *                         {@code -1} when it did not run (the vector pass was not thin,
 *                         or the site has no lexical search)
 * @param lexicalDocuments distinct lexical documents whose chunks the vector store
 *                         returned, or {@code -1} when it did not run
 * @param documents        distinct documents the answer was grounded on, after the
 *                         period, rerank and pinned-document steps
 * @param answerable       the STRICT_RAG judge's verdict, or {@code null} when it did
 *                         not run
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurAnswerRetrieval(
        int vectorDocuments,
        int lexicalHits,
        int lexicalDocuments,
        int documents,
        Boolean answerable) {
}
