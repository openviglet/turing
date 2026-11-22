package com.viglet.turing.persona.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.viglet.turing.llm.mcp.persistence.repository.TurMcpServerRepository;
import com.viglet.turing.persona.authoring.TurAiAuthoringService;
import com.viglet.turing.persona.persistence.dto.TurPersonaDto;
import com.viglet.turing.persona.persistence.mapper.TurPersonaMapper;
import com.viglet.turing.persona.persistence.model.TurPersona;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;
import com.viglet.turing.llm.persistence.repository.store.TurStoreInstanceRepository;
import com.viglet.turing.shared.exception.TurConflictException;

/**
 * T1319 — a persona save made from an older version is refused, and the answer
 * carries the version the next save sends back.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurPersonaAPIVersionTest {

    private final TurPersonaRepository repository = mock(TurPersonaRepository.class);
    private final TurPersonaAPI api = new TurPersonaAPI(repository, Mappers.getMapper(TurPersonaMapper.class),
            mock(TurStoreInstanceRepository.class), mock(TurMcpServerRepository.class),
            mock(TurAiAuthoringService.class));

    private static TurPersona persisted(long version) {
        TurPersona persona = new TurPersona();
        persona.setId("p1");
        persona.setName("Voice");
        persona.setVersion(version);
        return persona;
    }

    @Test
    void aSaveFromAnOlderVersionIsRefused() {
        when(repository.findById("p1")).thenReturn(Optional.of(persisted(4L)));
        TurPersonaDto stale = new TurPersonaDto();
        stale.setName("Mine");
        stale.setVersion(3L);

        assertThatThrownBy(() -> api.turPersonaUpdate("p1", stale)).isInstanceOf(TurConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aSaveFromTheCurrentVersionAnswersTheSavedVersion() {
        when(repository.findById("p1")).thenReturn(Optional.of(persisted(4L)));
        when(repository.save(any())).thenAnswer(inv -> {
            TurPersona merged = persisted(5L);
            merged.setName(((TurPersona) inv.getArgument(0)).getName());
            return merged;
        });
        TurPersonaDto current = new TurPersonaDto();
        current.setName("Mine");
        current.setVersion(4L);

        TurPersonaDto answer = api.turPersonaUpdate("p1", current);

        assertThat(answer.getVersion()).isEqualTo(5L);
        assertThat(answer.getName()).isEqualTo("Mine");
    }
}
