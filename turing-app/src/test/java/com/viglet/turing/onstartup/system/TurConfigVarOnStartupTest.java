package com.viglet.turing.onstartup.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.persistence.model.system.TurConfigVar;
import com.viglet.turing.persistence.repository.system.TurConfigVarRepository;

@ExtendWith(MockitoExtension.class)
class TurConfigVarOnStartupTest {

    @Mock
    private TurConfigVarRepository turConfigVarRepository;

    @InjectMocks
    private TurConfigVarOnStartup turConfigVarOnStartup;

    @Test
    void shouldCreateFirstTimeConfigVarWhenMissing() {
        when(turConfigVarRepository.findById("FIRST_TIME")).thenReturn(Optional.empty());

        turConfigVarOnStartup.createDefaultRows();

        ArgumentCaptor<TurConfigVar> captor = ArgumentCaptor.forClass(TurConfigVar.class);
        verify(turConfigVarRepository).save(captor.capture());

        TurConfigVar saved = captor.getValue();
        assertEquals("FIRST_TIME", saved.getId());
        assertEquals("/system", saved.getPath());
        assertEquals("true", saved.getValue());
    }

    @Test
    void shouldNotCreateConfigVarWhenAlreadyExists() {
        when(turConfigVarRepository.findById("FIRST_TIME")).thenReturn(Optional.of(new TurConfigVar()));

        turConfigVarOnStartup.createDefaultRows();

        verify(turConfigVarRepository, never()).save(org.mockito.ArgumentMatchers.any(TurConfigVar.class));
    }
}
