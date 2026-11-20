package com.viglet.turing.system.onstartup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.system.persistence.model.TurConfigVar;
import com.viglet.turing.system.persistence.repository.TurConfigVarRepository;
import com.viglet.turing.shared.onstartup.TurFirstTimeSeeder;
import com.viglet.turing.shared.onstartup.TurStartupReconciler;

/**
 * T1311 — the runner reaches the modules' seeders only through the shared
 * contracts: first-time seeders in their order on a fresh install only, and the
 * reconciliations in their order on every boot.
 */
@ExtendWith(MockitoExtension.class)
class TurOnStartupTest {

    @Mock
    private TurConfigVarRepository turConfigVarRepository;

    private final List<String> calls = new ArrayList<>();

    private TurFirstTimeSeeder seeder(String name, int order) {
        return new TurFirstTimeSeeder() {
            @Override
            public int seedOrder() {
                return order;
            }

            @Override
            public void createDefaultRows() {
                calls.add("seed:" + name);
            }
        };
    }

    private TurStartupReconciler reconciler(String name, int order) {
        return new TurStartupReconciler() {
            @Override
            public int reconcileOrder() {
                return order;
            }

            @Override
            public void reconcileOnStartup() {
                calls.add("reconcile:" + name);
            }
        };
    }

    private TurOnStartup runner() {
        // Handed over out of order: the runner sorts them.
        return new TurOnStartup(turConfigVarRepository,
                List.of(seeder("users", 40), seeder("locales", 10), seeder("roles", 20)),
                List.of(reconciler("embeddings", 30), reconciler("admin", 10), reconciler("llm", 20)));
    }

    @Test
    void aFreshInstallSeedsInOrderThenReconciles() {
        when(turConfigVarRepository.findById(TurOnStartup.FIRST_TIME)).thenReturn(Optional.empty());

        runner().run(null);

        assertThat(calls).containsExactly("seed:locales", "seed:roles", "seed:users",
                "reconcile:admin", "reconcile:llm", "reconcile:embeddings");
    }

    @Test
    void anExistingInstallOnlyReconciles() {
        when(turConfigVarRepository.findById(TurOnStartup.FIRST_TIME))
                .thenReturn(Optional.of(new TurConfigVar()));

        runner().run(null);

        assertThat(calls).containsExactly("reconcile:admin", "reconcile:llm", "reconcile:embeddings");
    }
}
