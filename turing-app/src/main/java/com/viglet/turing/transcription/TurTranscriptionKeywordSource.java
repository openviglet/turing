/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.transcription;

import java.util.List;

/**
 * T847 / §LXIII.2 — the source of the transcription {@code keywords} hint: a
 * vocabulary Turing already curates, handed to the ASR so product names, model
 * ids and jargon are not transcribed phonetically.
 *
 * <p>T1322m — declared here and implemented by the module that owns the
 * vocabulary (kb's Thesaurus), so transcription sits beside llm and imports no
 * kb type (ADR 0005 section 6).
 *
 * <p><b>Opt-in and bounded</b> by design ({@code turing.transcription.keywords-from-thesaurus},
 * default {@code false}): a keyword list is a hint, not a dictionary dump.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public interface TurTranscriptionKeywordSource {

    /**
     * Up to {@code limit} terms (capped at {@link TurTranscriptionHints#MAX_KEYWORDS}),
     * broadest first. Fail-open: any read problem yields an empty list so
     * transcription still runs — a missing hint costs accuracy, never the
     * transcript.
     */
    List<String> keywords(int limit);
}
