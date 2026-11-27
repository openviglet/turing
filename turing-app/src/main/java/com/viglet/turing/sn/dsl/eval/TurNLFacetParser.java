/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.dsl.eval;

import java.util.List;
import java.util.Map;

import com.viglet.turing.searchengine.dsl.TurDslQueryRequest;

/**
 * The NL→facet parsing seam (T385 / §XX.5): turns a prose query into a structured
 * {@link TurDslQueryRequest} (facet filters + ranges + sort) over a declared field
 * schema. The production implementation ({@link TurLlmNLFacetParser}) drives the
 * configured default LLM with a constrained prompt; tests inject a deterministic
 * fake so the scorer and fixtures run in CI without an LLM.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public interface TurNLFacetParser {

    /**
     * Parse {@code request.query()} into a structured query body over the
     * declared schema.
     *
     * @throws TurNLFacetParseException when no usable LLM is available or the
     *         model output cannot be parsed into a query body.
     */
    TurDslQueryRequest parse(ParseRequest request);

    /** True when the parser can run (e.g. a usable default LLM is configured). */
    boolean isAvailable();

    /**
     * @param index  the target SN site / index.
     * @param locale locale code (may be null).
     * @param query  the natural-language prose to parse.
     * @param schema the declared fields the parser may map onto.
     * @param values T1359 — the values each facet field holds, the most common
     *               first, so the parser can map a noun of the query ("palestras")
     *               onto the value the site files it under ("Palestra"); empty when
     *               the caller read none.
     */
    record ParseRequest(String index, String locale, String query, List<TurNLFacetField> schema,
            Map<String, List<String>> values) {

        public ParseRequest {
            values = values == null ? Map.of() : values;
        }

        public ParseRequest(String index, String locale, String query, List<TurNLFacetField> schema) {
            this(index, locale, query, schema, Map.of());
        }
    }

    /** Thrown when parsing cannot proceed or produce a valid query body. */
    class TurNLFacetParseException extends RuntimeException {
        public TurNLFacetParseException(String message) {
            super(message);
        }

        public TurNLFacetParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
