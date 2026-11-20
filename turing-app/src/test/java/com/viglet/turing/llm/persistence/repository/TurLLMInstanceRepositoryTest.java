package com.viglet.turing.llm.persistence.repository;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurLLMInstanceRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurLLMInstanceRepository.class);
    }
}
