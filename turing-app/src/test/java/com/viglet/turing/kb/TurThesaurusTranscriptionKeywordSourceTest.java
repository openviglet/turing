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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.viglet.turing.kb.persistence.repository.TurThesaurusTermRepository;
import com.viglet.turing.transcription.TurTranscriptionHints;

/**
 * kb answers transcription's keyword SPI from the Thesaurus (T1322m).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@ExtendWith(MockitoExtension.class)
class TurThesaurusTranscriptionKeywordSourceTest {

    @Mock
    private TurThesaurusTermRepository repository;

    @Test
    void keywordsAreTheBroadestEnabledLabelsCappedAtTheHintLimit() {
        when(repository.findEnabledLabelsBroadestFirst(PageRequest.of(0, TurTranscriptionHints.MAX_KEYWORDS)))
                .thenReturn(List.of("Turing", "Lucene"));

        assertThat(new TurThesaurusTranscriptionKeywordSource(repository)
                .keywords(TurTranscriptionHints.MAX_KEYWORDS + 50)).containsExactly("Turing", "Lucene");
    }

    @Test
    void aZeroLimitReadsNothing() {
        assertThat(new TurThesaurusTranscriptionKeywordSource(repository).keywords(0)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void aFailedReadIsAnEmptyList() {
        when(repository.findEnabledLabelsBroadestFirst(PageRequest.of(0, 5)))
                .thenThrow(new IllegalStateException("no table"));

        assertThat(new TurThesaurusTranscriptionKeywordSource(repository).keywords(5)).isEmpty();
    }
}
