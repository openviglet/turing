package com.viglet.turing.sn.persistence.repository.merge;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurSNSiteMergeProvidersRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSNSiteMergeProvidersRepository.class);
    }
}
