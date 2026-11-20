package com.viglet.turing.searchengine.persistence.repository;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurSEInstanceRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSEInstanceRepository.class);
    }
}
