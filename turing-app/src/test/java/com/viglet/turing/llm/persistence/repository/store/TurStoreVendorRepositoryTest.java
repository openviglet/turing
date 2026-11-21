package com.viglet.turing.llm.persistence.repository.store;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurStoreVendorRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurStoreVendorRepository.class);
    }
}
