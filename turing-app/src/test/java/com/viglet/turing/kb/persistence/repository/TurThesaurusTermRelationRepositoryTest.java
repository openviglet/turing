package com.viglet.turing.kb.persistence.repository;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurThesaurusTermRelationRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurThesaurusTermRelationRepository.class);
    }
}
