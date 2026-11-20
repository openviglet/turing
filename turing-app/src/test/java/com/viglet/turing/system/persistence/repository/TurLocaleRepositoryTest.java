package com.viglet.turing.system.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurLocaleRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurLocaleRepository.class);
    }

    @Test
    void shouldExposeExpectedDefaultLocaleConstants() {
        assertThat(TurLocaleRepository.EN_US).isEqualTo("en_US");
        assertThat(TurLocaleRepository.EN_GB).isEqualTo("en_GB");
        assertThat(TurLocaleRepository.PT_BR).isEqualTo("pt_BR");
        assertThat(TurLocaleRepository.CA).isEqualTo("ca");
    }
}
