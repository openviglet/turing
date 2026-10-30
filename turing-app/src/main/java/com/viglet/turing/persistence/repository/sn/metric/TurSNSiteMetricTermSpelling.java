/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.persistence.repository.sn.metric;

/**
 * T1179 — one spelling a search was typed in ({@code term}), how often
 * ({@code count}), and the folded {@code key} it is grouped under.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
public record TurSNSiteMetricTermSpelling(String key, String term, long count) {
}
