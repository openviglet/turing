/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.rag.research;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;
import com.viglet.turing.rag.persistence.model.research.TurResearchStudy;
import com.viglet.turing.rag.persistence.repository.research.TurResearchInterviewRepository;
import com.viglet.turing.rag.persistence.repository.research.TurResearchStudyPersonaRepository;
import com.viglet.turing.rag.persistence.repository.research.TurResearchStudyRepository;
import com.viglet.turing.rag.research.dto.TurResearchStudyDto;
import com.viglet.turing.shared.exception.TurConflictException;

/**
 * T1319 — a research study save made from an older version is refused; the
 * match and dialogue projects run the same check.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurResearchStudyServiceVersionTest {

    private final TurResearchStudyRepository studyRepository = mock(TurResearchStudyRepository.class);
    private final TurResearchStudyService service = new TurResearchStudyService(studyRepository,
            mock(TurResearchStudyPersonaRepository.class), mock(TurResearchInterviewRepository.class),
            mock(TurPersonaRepository.class), mock(TurLLMInstanceRepository.class),
            mock(TurAIAgentRepository.class));

    @Test
    void aSaveFromAnOlderVersionIsRefused() {
        TurResearchStudy stored = new TurResearchStudy();
        stored.setId("study-1");
        stored.setName("Study");
        stored.setVersion(4L);
        when(studyRepository.findById("study-1")).thenReturn(Optional.of(stored));
        TurResearchStudyDto stale = new TurResearchStudyDto("study-1", "Mine", null, null, null, true,
                null, null, List.of(), 5, null, null, null, null, null, null, null, null, 0, 0,
                List.of(), List.of(), 3L);

        assertThatThrownBy(() -> service.update("study-1", stale)).isInstanceOf(TurConflictException.class);
        verify(studyRepository, never()).save(any());
    }
}
