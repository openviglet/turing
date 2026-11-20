/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * T1310 — the engine-neutral query DSL (query, request, response, aggregations,
 * caller filters) and the executor contract each engine implements, part of the
 * module's API. The executors themselves sit with their engines.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@org.springframework.modulith.NamedInterface("dsl")
package com.viglet.turing.searchengine.dsl;