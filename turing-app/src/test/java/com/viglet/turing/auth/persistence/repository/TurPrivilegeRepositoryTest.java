package com.viglet.turing.auth.persistence.repository;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurPrivilegeRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurPrivilegeRepository.class);
    }
}
