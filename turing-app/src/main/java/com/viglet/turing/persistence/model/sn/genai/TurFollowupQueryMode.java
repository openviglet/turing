/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.persistence.model.sn.genai;

/**
 * T1157 — how a site's chat turns a follow-up ("Where do I send it?") into the
 * query its retrieval searches. The answer always reads the visitor's own words;
 * only the search query changes.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public enum TurFollowupQueryMode {
    /** The latest user message alone, the behaviour before T1157. */
    LATEST_ONLY,
    /** The last few user turns joined into one query; no LLM call. */
    JOINED,
    /** A short LLM call rewrites the latest turn as a standalone question. */
    REWRITE
}
