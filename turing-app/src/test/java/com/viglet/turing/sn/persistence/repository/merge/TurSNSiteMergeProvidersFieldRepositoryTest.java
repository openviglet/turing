package com.viglet.turing.sn.persistence.repository.merge;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurSNSiteMergeProvidersFieldRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSNSiteMergeProvidersFieldRepository.class);
    }
}
