package com.viglet.turing.llm.persistence.repository;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurLLMVendorRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurLLMVendorRepository.class);
    }
}
