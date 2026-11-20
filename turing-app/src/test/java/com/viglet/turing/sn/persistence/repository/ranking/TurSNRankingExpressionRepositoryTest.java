package com.viglet.turing.sn.persistence.repository.ranking;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurSNRankingExpressionRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSNRankingExpressionRepository.class);
    }
}
