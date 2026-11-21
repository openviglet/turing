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
 * T1310 — the site search configuration SPI: what an engine reads about a site while
 * it builds a query, declared by the search-engine module and implemented by the
 * module that owns the sites.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@org.springframework.modulith.NamedInterface("spi")
package com.viglet.turing.searchengine.spi;
