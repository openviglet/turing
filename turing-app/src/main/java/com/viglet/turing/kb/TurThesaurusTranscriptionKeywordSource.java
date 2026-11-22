/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.kb;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.kb.persistence.repository.TurThesaurusTermRepository;
import com.viglet.turing.transcription.TurTranscriptionHints;
import com.viglet.turing.transcription.TurTranscriptionKeywordSource;

import lombok.extern.slf4j.Slf4j;

/**
 * T1322m — kb's side of the transcription keyword SPI: the Thesaurus's enabled
 * preferred-term labels, broadest descriptors first. The same terms the search
 * engine expands are handed to the ASR. An install with no Thesaurus gets an
 * empty list, never an error.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurThesaurusTranscriptionKeywordSource implements TurTranscriptionKeywordSource {

    private final TurThesaurusTermRepository thesaurusTermRepository;

    public TurThesaurusTranscriptionKeywordSource(TurThesaurusTermRepository thesaurusTermRepository) {
        this.thesaurusTermRepository = thesaurusTermRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> keywords(int limit) {
        int capped = Math.clamp(limit, 0, TurTranscriptionHints.MAX_KEYWORDS);
        if (capped == 0) {
            return List.of();
        }
        try {
            return thesaurusTermRepository.findEnabledLabelsBroadestFirst(
                    PageRequest.of(0, capped));
        } catch (RuntimeException e) {
            log.debug("[Transcription] could not read Thesaurus keywords: {}", e.getMessage());
            return List.of();
        }
    }
}
