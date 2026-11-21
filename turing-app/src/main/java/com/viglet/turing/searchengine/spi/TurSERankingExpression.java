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
 * T1310 — a site's ranking expression as the engines read it: a boost of
 * {@code weight} for the documents that match every one of its conditions.
 *
 * @param name       the expression's name, for logs
 * @param weight     the boost
 * @param conditions the ANDed conditions; an expression with none boosts nothing
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSERankingExpression(String name, float weight, List<TurSERankingCondition> conditions) {

    public TurSERankingExpression {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
