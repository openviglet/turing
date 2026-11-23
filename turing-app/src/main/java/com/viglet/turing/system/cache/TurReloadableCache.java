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
package com.viglet.turing.system.cache;

/**
 * T1322l — a cache a feature module holds in a bean rather than in the
 * {@code CacheManager}, declared here so the system cache API can clear it
 * without importing the module that owns it. "Clear all caches" reloads every
 * one, and each can be reloaded alone at {@code /api/system/cache/<path>/reload}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurReloadableCache {

    /** The key the cache API reports this cache's reload count under, e.g. {@code rankingLexicons}. */
    String name();

    /** The path segment of this cache's own reload endpoint, e.g. {@code ranking-lexicons}. */
    String path();

    /**
     * Drops what the cache holds, so the next read rebuilds it.
     *
     * @return how many entries were dropped
     */
    int reload();
}
