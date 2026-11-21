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
package com.viglet.turing.searchengine.spi;

import java.util.List;

/**
 * T1310 — the site configuration an engine reads while it builds a query, declared
 * here and implemented by the module that owns the sites (ADR 0005 section 6,
 * option 1 of T1310). The engines keep building queries; they reach the
 * configuration through this interface instead of importing {@code sn}.
 *
 * <p>Each method takes the site's id and answers in engine-neutral records.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSESiteSearchConfig {

    /**
     * The site's ranking expressions, ordered by name ignoring case, each with its
     * conditions; empty when the site has none.
     */
    List<TurSERankingExpression> rankingExpressions(String siteId);
}
