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

/**
 * T1310 — the site a search-engine plugin indexes into or reads from, as the
 * engine sees it: an id to look its search configuration up by, a name to
 * resolve its cores by, and the SE instance that serves it. The site module
 * builds it from its own entity, so the plugin contract takes no sn type.
 *
 * @param id           the site's id
 * @param name         the site's name
 * @param seInstanceId the id of the site's SE instance; {@code null} when it names none
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSESite(String id, String name, String seInstanceId) {
}
