package com.viglet.turing.onstartup.system;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
        when(turConfigVarRepository.findById("GLOBAL_DECIMAL_SEPARATOR")).thenReturn(Optional.empty());

        turConfigVarOnStartup.createDefaultRows();

        ArgumentCaptor<TurConfigVar> captor = ArgumentCaptor.forClass(TurConfigVar.class);
        verify(turConfigVarRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<TurConfigVar> savedVars = captor.getAllValues();
        assertTrue(savedVars.stream().anyMatch(var -> "FIRST_TIME".equals(var.getId())
                && "/system".equals(var.getPath())
                && "true".equals(var.getValue())));
        assertTrue(savedVars.stream().anyMatch(var -> "GLOBAL_DECIMAL_SEPARATOR".equals(var.getId())
                && "/system/global".equals(var.getPath())
                && "DOT".equals(var.getValue())));
    }

    @Test
    void shouldNotCreateConfigVarWhenAlreadyExists() {
        when(turConfigVarRepository.findById("FIRST_TIME")).thenReturn(Optional.of(new TurConfigVar()));
        when(turConfigVarRepository.findById("GLOBAL_DECIMAL_SEPARATOR"))
                .thenReturn(Optional.of(new TurConfigVar()));

        turConfigVarOnStartup.createDefaultRows();

        verify(turConfigVarRepository, never()).save(org.mockito.ArgumentMatchers.any(TurConfigVar.class));
    }
}
