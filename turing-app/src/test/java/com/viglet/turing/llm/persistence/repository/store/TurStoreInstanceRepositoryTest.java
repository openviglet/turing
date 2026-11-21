package com.viglet.turing.llm.persistence.repository.store;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurStoreInstanceRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurStoreInstanceRepository.class);
    }
}
