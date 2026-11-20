package com.viglet.turing.sn.persistence.repository.field;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurSNSiteFieldRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSNSiteFieldRepository.class);
    }
}
